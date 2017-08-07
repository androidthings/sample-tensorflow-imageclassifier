/*
 * Copyright 2017 The Android Things Samples Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.imageclassifier;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.util.Log;

import com.example.androidthings.imageclassifier.classifier.Classifier;

import junit.framework.Assert;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Helper functions for the TensorFlow image classifier.
 */
public class Helper {

    public static final int IMAGE_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String LABELS_FILE = "imagenet_comp_graph_label_strings.txt";
    public static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    public static final String INPUT_NAME = "input:0";
    public static final String OUTPUT_OPERATION = "output";
    public static final String OUTPUT_NAME = OUTPUT_OPERATION + ":0";
    public static final String[] OUTPUT_NAMES = {OUTPUT_NAME};
    public static final long[] NETWORK_STRUCTURE = {1, IMAGE_SIZE, IMAGE_SIZE, 3};
    public static final int NUM_CLASSES = 1008;

    private static final int MAX_BEST_RESULTS = 3;
    private static final float RESULT_CONFIDENCE_THRESHOLD = 0.1f;

    public static String[] readLabels(Context context) {
        AssetManager assetManager = context.getAssets();
        ArrayList<String> result = new ArrayList<>();
        try (InputStream is = assetManager.open(LABELS_FILE);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.add(line);
            }
            return result.toArray(new String[result.size()]);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read labels from " + LABELS_FILE);
        }
    }

    public static List<Classifier.Recognition> getBestResults(float[] confidenceLevels, String[] labels) {
        // Find the best classifications.
        PriorityQueue<Classifier.Recognition> pq = new PriorityQueue<>(MAX_BEST_RESULTS,
                new Comparator<Classifier.Recognition>() {
                    @Override
                    public int compare(Classifier.Recognition lhs, Classifier.Recognition rhs) {
                        // Intentionally reversed to put high confidence at the head of the queue.
                        return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                    }
                });

        for (int i = 0; i < confidenceLevels.length; ++i) {
            if (confidenceLevels[i] > RESULT_CONFIDENCE_THRESHOLD) {
                pq.add(new Classifier.Recognition("" + i, labels[i], confidenceLevels[i]));
            }
        }

        ArrayList<Classifier.Recognition> recognitions = new ArrayList<Classifier.Recognition>();
        int recognitionsSize = Math.min(pq.size(), MAX_BEST_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    public static String formatResults(String[] results) {
        String resultStr;
        if (results == null || results.length == 0) {
            resultStr = "I don't know what I see.";
        } else if (results.length == 1) {
            resultStr = "I see a " + results[0];
        } else {
            resultStr = "I see a " + results[0] + " or maybe a " + results[1];
        }
        return resultStr;
    }

    public static float[] getPixels(Bitmap bitmap, int[] intValues, float[] floatValues) {
        if (bitmap.getWidth() != IMAGE_SIZE || bitmap.getHeight() != IMAGE_SIZE) {
            // rescale the bitmap if needed
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, IMAGE_SIZE, IMAGE_SIZE);
        }

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
        }
        return floatValues;
    }

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     */
    public static void saveBitmap(final Bitmap bitmap) {
        final File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "tensorflow_preview.png");
        Log.d("ImageHelper", String.format("Saving %dx%d bitmap to %s.",
                bitmap.getWidth(), bitmap.getHeight(), file.getAbsolutePath()));

        if (file.exists()) {
            file.delete();
        }
        try (FileOutputStream fs = new FileOutputStream(file);
                BufferedOutputStream out = new BufferedOutputStream(fs)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
        } catch (final Exception e) {
            Log.w("ImageHelper", "Could not save image for debugging. " + e.getMessage());
        }
    }

    public static void cropAndRescaleBitmap(final Bitmap src, final Bitmap dst, int sensorOrientation) {
        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);

        // Rotate around the center if necessary.
        if (sensorOrientation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(sensorOrientation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }
}
