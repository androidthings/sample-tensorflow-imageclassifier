Android Things TensorFlow image classifier sample
=====================================

The Android Things TensorFlow image classifier sample demonstrates how to classify images.
It uses camera to capture images and run TensorFlow inference on board to tell what kinds
of dogs and cats in the images.  

This project is based on the [TensorFlow Android Camera Demo](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/),
where the TensorFlow training was done using a type of convolutional neural network on ImageNet
dataset codenamed Inception V3. The resulting data set is loaded into the sample app and
it runs inference via TensorFlow Android Inference APIs. This simplified sample does not require
native code and NDK and its only dependency on TensorFlow is a link to the TensorFlow Android
Inference library in the form of an .aar file in build.gradle, which is provided and packaged
into the project. 


Pre-requisites
--------------

- Android Things compatible board
- Android Things compatible camera (for example, the Raspberry Pi 3 camera module)
- Android Studio 2.2+
- "Google Repository" from the Android SDK Manager


Setup and Build
===============

To setup, follow these steps below.

- Set up adb connection to your device
- Set up camera module
- Set up the project in Android Studio
- Inception model assets will be downloaded during build step


Running
=======

To run the `app` module on an Android Things board:

1. Build the project within Android Studio and deploy to device via adb 
2. Reboot the device to get all permissions granted; see [Known issues in release notes](https://developer.android.com/things/preview/releases.html#known_issues)
3. Point camera to some images of dogs or cats
4. See generated labels for your image in adb logcat e.g. Result: samoyed 

If you have display like a TV, you will see images captured inside imageView with generated
labels above inside a View.

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
