# TensorFlow Lite IoT Image Classifier

This sample demonstrates how to run TensorFlow Lite inference on Android Things.
Push a button to capture an image with the camera, and TensorFlow Lite will tell
you what it is!
Follow the [Image Classifier Codelab](https://codelabs.developers.google.com/codelabs/androidthings-classifier)
step-by-step instructions on how to build a similar sample.

> **Note:** The Android Things Console will be turned down for non-commercial
> use on January 5, 2022. For more details, see the
> [FAQ page](https://developer.android.com/things/faq).

## Introduction

When a button is pushed or when the touchscreen is touched, the current image is captured from the
camera. The image is then converted and piped into a TensorFlow Lite classifier model that
identifies what is in the image. Up to three results with the highest confidence returned by the
classifier are shown on the screen, if there is an attached display. Also, the result is spoken out
loud using Text-To-Speech to the default audio output.

This project is based on the
[TensorFlow Android Camera Demo TF_Classify app](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/)
and was adapted to use [TensorFlow Lite](https://www.tensorflow.org/mobile/tflite/), a lightweight version of TensorFlow targeted at mobile
devices. The TensorFlow classifier model is
[MobileNet\_v1](https://github.com/tensorflow/models/blob/master/research/slim/nets/mobilenet_v1.md)
pre-trained on the [ImageNet ILSVRC2012](http://www.image-net.org/challenges/LSVRC/2012/) dataset.

This sample uses the [TensorFlow Lite inference library](https://bintray.com/google/tensorflow/tensorflow-lite)
and does not require any native build tools. You can add the TensorFlow Lite inference library to
your project by adding a dependency in your `build.gradle`, for example:
```
dependencies {
    compile 'org.tensorflow:tensorflow-lite:0.1.1'
}
```

Note: this sample requires a camera. Find an appropriate board in the
[documentation](https://developer.android.com/things/hardware/developer-kits.html).

## Screenshots

![TensorFlow Lite image classifier sample demo][demo-gif]

[(Watch the demo on YouTube)][demo-yt]

## Pre-requisites

- Android Things compatible board and an attached camera
- Android Studio 2.2+
- The following **optional** components:
    - one button and one resistor for triggering the camera
    - one LED and one resistor for the "ready" indicator
    - speaker or headphones for Text-To-Speech results
    - touchscreen or display for showing results

## Schematics

![Schematics](rpi3_schematics_tf.png)

## Run on Android Things Starter Kit

If you have an Android Things Starter Kit, you can easily run this sample on your i.MX7D development board from the [Android Things Toolkit](https://play.google.com/store/apps/details?id=com.google.android.things.companion&hl=en) app.

To run the sample on your i.MX7D development board:
  1. Set up your device using Toolkit
  2. Navigate to the Apps tab
  3. Select Run next to the Image Classifier sample.
  4. Press the "A" button on your Rainbow HAT or tap on the display to take a photo.

![Running Image Classifier Sample on Toolkit][toolkit-jpg]

## Build and Install

On Android Studio, click on the "Run" button.
If you prefer to run on the command line, type
```bash
./gradlew installDebug
adb shell am start com.example.androidthings.imageclassifier/.ImageClassifierActivity
```

If you have everything set up correctly:

1. Wait until the LED turns on
1. Point the camera to something like a dog, cat or a furniture
1. Push the button to take a picture
1. The LED should go off while running. In a Raspberry Pi 3, it takes about 500 millisecond to
   capture the picture and run it through TensorFlow, and some extra time to speak the results
   through Text-To-Speech
1. Inference results will show in logcat and, if there is a display connected,
   both the image and the results will be shown
1. If a speaker or headphones are connected, the results will be spoken via
   text to speech

## Enable auto-launch behavior

This sample app is currently configured to launch only when deployed from your
development machine. To enable the main activity to launch automatically on boot,
add the following `intent-filter` to the app's manifest file:

```xml
<activity ...>

   <intent-filter>
       <action android:name="android.intent.action.MAIN"/>
       <category android:name="android.intent.category.HOME"/>
       <category android:name="android.intent.category.DEFAULT"/>
   </intent-filter>

</activity>
```

## License

Copyright 2018 The Android Things Samples Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.

[demo-yt]: https://www.youtube.com/watch?v=8kxYlI9R2xo&list=PLWz5rJ2EKKc-GjpNkFe9q3DhE2voJscDT&index=11
[demo-gif]: demo1.gif
[toolkit-jpg]: toolkit_tensorflowsample.jpg

