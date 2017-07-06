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
        float[] pixels = Helper.getPixels(image);

        // Feed the pixels of the image into the TensorFlow Neural Network
        inferenceInterface.feed(Helper.INPUT_NAME, pixels,
                Helper.NETWORK_STRUCTURE);

        // Run the TensorFlow Neural Network with the provided input
        inferenceInterface.run(Helper.OUTPUT_NAMES);

        // Extract the output from the neural network back into an array of confidence per category
        float[] outputs = new float[Helper.NUM_CLASSES];
        inferenceInterface.fetch(Helper.OUTPUT_NAME, outputs);

        // Find the best classifications.
        PriorityQueue<Recognition> pq = new PriorityQueue<>(3,
                new Comparator<Recognition>() {
                    @Override
                    public int compare(Recognition lhs, Recognition rhs) {
                        // Intentionally reversed to put high confidence at the head of the queue.
                        return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                    }
                });
        for (int i = 0; i < outputs.length; ++i) {
            if (outputs[i] > Helper.RESULT_CONFIDENCE_THRESHOLD) {
                pq.add(new Recognition("" + i, labels[i], outputs[i], null));
            }
        }
        ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        int recognitionsSize = Math.min(pq.size(), Helper.MAX_BEST_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

}
