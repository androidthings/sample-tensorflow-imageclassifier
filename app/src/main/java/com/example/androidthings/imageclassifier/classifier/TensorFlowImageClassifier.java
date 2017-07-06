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
package com.example.androidthings.imageclassifier.classifier;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.androidthings.imageclassifier.Helper;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A classifier specialized to label images using TensorFlow.
 */
public class TensorFlowImageClassifier implements Classifier {

    private static final String TAG = "TFImageClassifier";

    private String[] labels;

    // Pre-allocated buffers.
    private float[] floatValues;
    private int[] intValues;
    private float[] outputs;

    private TensorFlowInferenceInterface inferenceInterface;

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param context The activity that instantiates this.
     */
    public TensorFlowImageClassifier(Context context) {
        this.inferenceInterface = new TensorFlowInferenceInterface(
                context.getAssets(),
                Helper.MODEL_FILE);
        this.labels = Helper.readLabels(context);

        // Pre-allocate buffers.
        intValues = new int[Helper.IMAGE_SIZE * Helper.IMAGE_SIZE];
        floatValues = new float[Helper.IMAGE_SIZE * Helper.IMAGE_SIZE * 3];
        outputs = new float[Helper.NUM_CLASSES];
    }

    /**
     * Clean up the resources used by the classifier.
     */
    public void destroyClassifier() {
        inferenceInterface.close();
    }


    /**
     * @param image Bitmap containing the image to be classified. The image can be
     *              of any size, but preprocessing might occur to resize it to the
     *              format expected by the classification process, which can be time
     *              and power consuming.
     */
    public List<Classifier.Recognition> doRecognize(Bitmap image) {
        float[] pixels = Helper.getPixels(image, intValues, floatValues);

        // Feed the pixels of the image into the TensorFlow Neural Network
        inferenceInterface.feed(Helper.INPUT_NAME, pixels,
                Helper.NETWORK_STRUCTURE);

        // Run the TensorFlow Neural Network with the provided input
        inferenceInterface.run(Helper.OUTPUT_NAMES);

        // Extract the output from the neural network back into an array of confidence per category
        inferenceInterface.fetch(Helper.OUTPUT_NAME, outputs);

        // Get the results with the highest confidence and map them to their labels
        return Helper.getBestResults(outputs, labels);
    }

}
