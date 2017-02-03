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
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidthings.imageclassifier.classifier.Classifier;
import com.example.androidthings.imageclassifier.classifier.TensorFlowImageClassifier;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class ImageClassifierActivity extends Activity implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "ImageClassifierActivity";
    private static final int PERMISSIONS_REQUEST = 1;

    private static final String BUTTON_PIN = "BCM21";
    private static final String LED_PIN = "BCM6";

    private ImagePreprocessor mImagePreprocessor;
    private TextToSpeech mTtsEngine;
    private CameraHandler mCameraHandler;
    private TensorFlowImageClassifier mTensorFlowClassifier;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private ImageView mImage;
    private TextView[] mResultViews;

    private AtomicBoolean mReady = new AtomicBoolean(false);
    private ButtonInputDriver mButtonDriver;
    private Gpio mReadyLED;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);
        mImage = (ImageView) findViewById(R.id.imageView);
        mResultViews = new TextView[3];
        mResultViews[0] = (TextView) findViewById(R.id.result1);
        mResultViews[1] = (TextView) findViewById(R.id.result2);
        mResultViews[2] = (TextView) findViewById(R.id.result3);

        if (hasPermission()) {
            if (savedInstanceState == null) {
                init();
            }
        } else {
            requestPermission();
        }
    }

    private void init() {
        try {
            mButtonDriver = new ButtonInputDriver(BUTTON_PIN, Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            mButtonDriver.register();
            PeripheralManagerService service = new PeripheralManagerService();
            mReadyLED = service.openGpio(LED_PIN);
            mReadyLED.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.w(TAG, "Could not open GPIO", e);
        }

        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);
    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            mImagePreprocessor = new ImagePreprocessor(CameraHandler.IMAGE_WIDTH,
                    CameraHandler.IMAGE_HEIGHT, TensorFlowImageClassifier.INPUT_SIZE);

            mTtsEngine = new TextToSpeech(ImageClassifierActivity.this,
                    new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            if (status == TextToSpeech.SUCCESS) {
                                mTtsEngine.setLanguage(Locale.US);
                                mTtsEngine.setOnUtteranceProgressListener(utteranceListener);
                            } else {
                                mTtsEngine = null;
                            }
                        }
                    });
            mCameraHandler = CameraHandler.getInstance();
            mCameraHandler.initializeCamera(
                    ImageClassifierActivity.this, mBackgroundHandler,
                    ImageClassifierActivity.this);

            mTensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this);

            setReady(true);
        }
    };

    private UtteranceProgressListener utteranceListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            setReady(false);
        }

        @Override
        public void onDone(String utteranceId) {
            setReady(true);
        }

        @Override
        public void onError(String utteranceId) {
            setReady(true);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "Received key down: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mReady.get()) {
                setReady(false);
                mCameraHandler.takePicture();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setReady(boolean ready) {
        mReady.set(ready);
        if (mReadyLED != null) {
            try {
                mReadyLED.setValue(ready);
            } catch (IOException e) {
                Log.w(TAG, "Could not set LED", e);
            }
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Bitmap bitmap;
        try (Image image = reader.acquireNextImage()) {
            bitmap = mImagePreprocessor.preprocessImage(image);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImage.setImageBitmap(bitmap);
            }
        });

        final List<Classifier.Recognition> results = mTensorFlowClassifier.recognizeImage(bitmap);

        if (mTtsEngine != null) {
            // speak out loud the result of the image recognition
            if (Math.random() < 0.3) {
                mTtsEngine.setPitch(0.2f);
                mTtsEngine.speak("I see dead people...", TextToSpeech.QUEUE_ADD, null, "ID");
                mTtsEngine.setPitch(1);
                mTtsEngine.speak("just kidding...", TextToSpeech.QUEUE_ADD, null, "ID");
            } else {
                mTtsEngine.setPitch(1f);
                mTtsEngine.setVoice(mTtsEngine.getDefaultVoice());
            }
            String message;
            if (results.isEmpty()) {
                message = "I don't understand what I see. Am I using drugs?";
            } else if (results.size() == 1 || results.get(0).getConfidence() > 0.4f) {
                message = String.format(Locale.getDefault(), "I see a %s",
                        results.get(0).getTitle());
            } else {
                message = String.format(Locale.getDefault(),
                        "This is a %s or maybe a %s",
                        results.get(0).getTitle(), results.get(1).getTitle());
            }
            mTtsEngine.speak(message, TextToSpeech.QUEUE_ADD, null, "ID");
        } else {
            // if theres no TTS, we don't need to wait until the utterance is spoken, so we set
            // to ready right away.
           setReady(true);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i=0; i<mResultViews.length; i++) {
                    if (results.size() > i) {
                        Classifier.Recognition r = results.get(i);
                        mResultViews[i].setText(r.toString());
                    } else {
                        mResultViews[i].setText(null);
                    }
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (mBackgroundThread != null) mBackgroundThread.quit();
        } catch (Throwable t) {
            // close quietly
        }
        mBackgroundThread = null;
        mBackgroundHandler = null;

        try {
            if (mCameraHandler != null) mCameraHandler.shutDown();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            if (mTensorFlowClassifier != null) mTensorFlowClassifier.close();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            if (mButtonDriver != null) mButtonDriver.close();
        } catch (Throwable t) {
            // close quietly
        }
    }

    // Permission-related methods. This is not needed for Android Things, where permissions are
    // automatically granted. However, it is kept here in case the developer
    // needs to test on a regular Android device
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    requestPermission();
                }
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(CAMERA) ||
                    shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(ImageClassifierActivity.this,"Camera AND storage permission are " +
                        "required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
        }
    }
}
