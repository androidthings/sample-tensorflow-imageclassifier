/*
 * Copyright 2018 The Android Things Samples Authors.
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

import android.util.Size;
import junit.framework.Assert;
import org.junit.Test;

public class CameraSizeInstrumentationTest {
    private static final Size SIZE_FULL_HD = new Size(1920, 1080);
    private static final Size SIZE_720P = new Size(1280, 720);
    private static final Size SIZE_SD = new Size(640, 480);

    private static final Size[] ALL_RESOLUTIONS = new Size[] {SIZE_FULL_HD, SIZE_720P, SIZE_SD};
    private static final Size[] ALL_RESOLUTIONS_INVERSE =
            new Size[] {SIZE_SD, SIZE_720P, SIZE_FULL_HD};

    /**
     * Tests that we can get the correct size.
     */
    @Test
    public void testGetLargestResolution() {
        Size cameraResolution = CameraHandler.getBestCameraSize(ALL_RESOLUTIONS, SIZE_FULL_HD);
        Assert.assertEquals(SIZE_FULL_HD, cameraResolution);
    }

    /**
     * Tests that the correct size is chosen even with an out-of-order array.
     */
    @Test
    public void testSortAndLargestResolution() {
        Size cameraResolution = CameraHandler.getBestCameraSize(ALL_RESOLUTIONS_INVERSE,
                SIZE_FULL_HD);
        Assert.assertEquals(SIZE_FULL_HD, cameraResolution);
    }

    /**
     * Tests that the correct size is chosen with a smaller maximum area.
     */
    @Test
    public void testGetMediumResolution() {
        Size cameraResolution = CameraHandler.getBestCameraSize(ALL_RESOLUTIONS, SIZE_720P);
        Assert.assertEquals(SIZE_720P, cameraResolution);
    }

    /**
     * Tests that the correct size is chosen with the smallest area.
     */
    @Test
    public void testGetSmallResolution() {
        Size cameraResolution = CameraHandler.getBestCameraSize(ALL_RESOLUTIONS, SIZE_SD);
        Assert.assertEquals(SIZE_SD, cameraResolution);
    }
}
