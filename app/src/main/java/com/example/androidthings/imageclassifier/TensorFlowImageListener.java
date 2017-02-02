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

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.Trace;
import android.util.Log;
import android.widget.ImageView;

import com.example.androidthings.imageclassifier.env.ImageUtils;

import junit.framework.Assert;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static android.content.ContentValues.TAG;

/**
 * Class that takes in preview frames and converts the image to Bitmaps to process with Tensorflow.
 */
public class TensorFlowImageListener implements OnImageAvailableListener {
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    // These are the settings for the original v1 Inception model. If you want to
    // use a model that's been produced from the TensorFlow for Poets codelab,
    // you'll need to set IMAGE_SIZE = 299, IMAGE_MEAN = 128, IMAGE_STD = 128,
    // INPUT_NAME = "Mul:0", and OUTPUT_NAME = "final_result:0".
    // You'll also need to update the MODEL_FILE and LABEL_FILE paths to point to
    // the ones you produced.

    // Note: the actual number of classes for Inception is 1001, but the output layer size is 1008.
    private static final int NUM_CLASSES = 1008;
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input:0";
    private static final String OUTPUT_NAME = "output:0";

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private int sensorOrientation = 0;

    private final TensorFlowImageClassifier tensorflow = new TensorFlowImageClassifier();

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private boolean computing = false;
    private Handler handler;
    private Activity activity;

    private RecognitionScoreView scoreView;

    private int previewWidth;
    private int previewHeight;

    private byte[][] cachedYuvBytes = new byte[3][];
    private int[] rgbBytes = null;

    public void initialize(
            final AssetManager assetManager,
            final RecognitionScoreView scoreView,
            final Activity activity,
            final Handler handler,
            final Integer sensorOrientation) {
        try {
            tensorflow.initializeTensorFlow(
                    assetManager, MODEL_FILE, LABEL_FILE, NUM_CLASSES, INPUT_SIZE, IMAGE_MEAN, IMAGE_STD,
                    INPUT_NAME, OUTPUT_NAME);
        } catch (IOException e) {
            Log.e(TAG, "Exception!");
        }
        this.scoreView = scoreView;
        this.activity = activity;
        this.handler = handler;
        this.sensorOrientation = sensorOrientation == null ? 0 : sensorOrientation;
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {

        Image image = null;
        try {
            image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }

            // No mutex needed as this method is not reentrant.
            if (computing) {
                image.close();
                return;
            }

            computing = true;

            Trace.beginSection("imageAvailable");

            // Initialize the storage bitmaps once when the resolution is known.
            if (previewWidth != image.getWidth() || previewHeight != image.getHeight()) {
                previewWidth = image.getWidth();
                previewHeight = image.getHeight();

                rgbBytes = new int[previewWidth * previewHeight];
                rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
                croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);
            }

            ImageUtils.convertImageToBitmap(image, previewWidth, previewHeight, rgbBytes, cachedYuvBytes);
            updateImageView(rgbFrameBitmap, activity);
            image.close();
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            Log.e(TAG, "Exception!");
            Trace.endSection();
            return;
        }

        if (croppedBitmap != null && rgbFrameBitmap != null) {
            rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
            ImageUtils.cropAndRescaleBitmap(rgbFrameBitmap, croppedBitmap, sensorOrientation);
        }

        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.clear();
            long secondsSinceEpoch = calendar.getTimeInMillis();
            if (secondsSinceEpoch % 1000 == 0)
                ImageUtils.saveBitmap(croppedBitmap);
        }

        handler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        final List<Classifier.Recognition> results;
                        try {
                            results =
                                    tensorflow.recognizeImage(croppedBitmap);
                            Log.v(TAG, results.size() + " results");
                        } finally {
                            computing = false;
                        }
                        for (final Classifier.Recognition result : results) {
                            Log.v(TAG, "Result: " + result.getTitle());
                        }
                        scoreView.setResults(results);
                    }
                });

        Trace.endSection();
    }



    private static void updateImageView(final Bitmap bmp, final Activity activity) {
        if (activity != null) {
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (bmp != null) {
                                ImageView view = (ImageView) activity.findViewById(R.id.imageView);
                                if (view != null) {
                                    view.setImageBitmap(bmp);
                                }

                            }
                        }
                    });
        }

    }
}
