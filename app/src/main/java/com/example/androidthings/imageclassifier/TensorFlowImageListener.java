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
import android.graphics.Color;
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
import java.nio.ByteBuffer;
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
    private static final int NUM_CLASSES = 1001;
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input:0";
    private static final String OUTPUT_NAME = "output:0";

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private Integer sensorOrientation = 0;

    private final TensorFlowImageClassifier tensorflow = new TensorFlowImageClassifier();

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private boolean computing = false;
    private Handler handler;
    private Activity activity;

    private RecognitionScoreView scoreView;

    public void initialize(
            final AssetManager assetManager,
            final RecognitionScoreView scoreView,
            final Activity activity,
            final Handler handler,
            final Integer sensorOrientation) {
        Assert.assertNotNull(sensorOrientation);
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
        this.sensorOrientation = sensorOrientation;
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {
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

            rgbFrameBitmap = image2Bitmap(image);

            updateImageView(rgbFrameBitmap, activity);

            croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

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
            drawResizedBitmap(rgbFrameBitmap, croppedBitmap);
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
                        final List<Classifier.Recognition> results =
                                tensorflow.recognizeImage(croppedBitmap);

                        Log.v(TAG, results.size() + " results");
                        for (final Classifier.Recognition result : results) {
                            Log.v(TAG, "Result: " + result.getTitle());
                        }
                        scoreView.setResults(results);
                        computing = false;
                    }
                });

        Trace.endSection();
    }


    private Bitmap image2Bitmap(Image image) {
        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];
        final Bitmap img = Bitmap.createBitmap(image.getWidth(), image.getHeight(),
                Bitmap.Config.RGB_565);
        ByteBuffer yBuff = yPlane.getBuffer();
        ByteBuffer cbBuff = uPlane.getBuffer();
        ByteBuffer crBuff = vPlane.getBuffer();

        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                int pixelsIn = x + y * yPlane.getRowStride();
                int yVal = yBuff.get(pixelsIn);
                int cPixelsIn = x / 2 * uPlane.getPixelStride() + y / 2 * uPlane.getRowStride();
                int cbVal = cbBuff.get(cPixelsIn);
                int crVal = crBuff.get(cPixelsIn);
                // Java doesn't do unsigned, so we have to decode what Java
                // thinks is two's complement back to unsigned.
                if (yVal < 0) {
                    yVal += 128;
                    yVal += 128;
                }
                if (cbVal < 0) {
                    cbVal += 128;
                    cbVal += 128;
                }
                if (crVal < 0) {
                    crVal += 128;
                    crVal += 128;
                }

                // Values used for YUV --> RGB conversion.
                crVal -= 128;
                cbVal -= 128;
                double yF = 1.164 * (yVal - 16);

                int r = (int) (yF + 1.596 * (crVal));
                int g = (int) (yF - 0.391 * (cbVal) - 0.813 * (crVal));
                int b = (int) (yF + 2.018 * (cbVal));
                // Clamp RGB to [0,255]
                if (r < 0) {
                    r = 0;
                } else if (r > 255) {
                    r = 255;
                }
                if (g < 0) {
                    g = 0;
                } else if (g > 255) {
                    g = 255;
                }
                if (b < 0) {
                    b = 0;
                } else if (b > 255) {
                    b = 255;
                }

                img.setPixel(x, y, Color.rgb(r, g, b));

            }
        }

        return img;
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
