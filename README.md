Android Things TensorFlow image classifier sample
=====================================

The Android Things TensorFlow image classifier sample app demonstrates how to capture an
image by pushing a button, run TensorFlow on device to infer top three labels from the
captured image, and then convert the result of labels into speech using text-to-speech.

This project is based on the [TensorFlow Android Camera Demo TF_Classify app](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/),
where the TensorFlow training was done using Google inception model and the trained data set
is used to run inference and generate classification labels via TensorFlow Android Inference
APIs.

This simplified sample app does not require native code or NDK and it links to TensorFlow
via a gradle dependency on the TensorFlow Android Inference library in the form of
an .aar library, which is included in the project here. 


Pre-requisites
--------------

- Android Things compatible board e.g. Raspberry Pi 3
- Android Things compatible camera (for example, the Raspberry Pi 3 camera module)
- Android Studio 2.2+
- "Google Repository" from the Android SDK Manager
- The following individual components:
    - 1 push button
    - 2 resistors
    - 1 LED light
    - 1 breadboard
    - 1 speaker or earphone set
    - jumper wires
    - Optional: display e.g. TV

Schematics
----------

![Schematics](rpi3_schematics_tf.png)

Setup and Build
===============

To setup, follow these steps below.

- Set up adb connection to your device
- Set up camera module
- Set up the project in Android Studio
- Inception model assets will be downloaded during build step
- Connect push button to GPIO pin BCM21 (see schematics) 
- Connect LED light to GPIO pin BCM6 (see schematics)
- Connect speaker to audio jack (see schematics)


Running
=======

To run the `app` module on an Android Things board:

1. Build the project within Android Studio and deploy to device via adb 
2. Reboot the device to get all permissions granted; see [Known issues in release notes](https://developer.android.com/things/preview/releases.html#known_issues)
3. Push the button when LED is ON to take a picture of e.g. dogs or cats
4. Check result: LED light OFF during inference to prevent in a subsequent image advertently taken
  - See generated labels for your image in adb logcat output e.g. Result: samoyed 
  - If display is available e.g. via HDMI, see generated labels with respective confidence levels
  - If speaker or earphones connected, listen to speech output of the generated labels


License
-------

Copyright 2016 The Android Open Source Project, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.

[dp2_release_notes]: https://developer.android.com/things/preview/releases.html#developer_preview_2
