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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.media.Image;

import junit.framework.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Class that process an Image and extracts a Bitmap in a format appropriate for
 * the TensorFlow model.
 */
public class ImagePreprocessor {
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;

    public ImagePreprocessor() {
        this.croppedBitmap = Bitmap.createBitmap(Helper.IMAGE_SIZE, Helper.IMAGE_SIZE,
                Config.ARGB_8888);
        this.rgbFrameBitmap = Bitmap.createBitmap(CameraHandler.IMAGE_WIDTH,
                CameraHandler.IMAGE_HEIGHT, Config.ARGB_8888);
    }

    public Bitmap preprocessImage(final Image image) {
        if (image == null) {
            return null;
        }

        Assert.assertEquals("Invalid size width", rgbFrameBitmap.getWidth(), image.getWidth());
        Assert.assertEquals("Invalid size height", rgbFrameBitmap.getHeight(), image.getHeight());

        if (croppedBitmap != null && rgbFrameBitmap != null) {
            ByteBuffer bb = image.getPlanes()[0].getBuffer();
            rgbFrameBitmap = BitmapFactory.decodeStream(new ByteBufferBackedInputStream(bb));
            Helper.cropAndRescaleBitmap(rgbFrameBitmap, croppedBitmap, 0);
        }

        image.close();

        // For debugging
        if (SAVE_PREVIEW_BITMAP) {
            Helper.saveBitmap(croppedBitmap);
        }
        return croppedBitmap;
    }

    private static class ByteBufferBackedInputStream extends InputStream {

        ByteBuffer buf;

        public ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        public int read(byte[] bytes, int off, int len)
                throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }
}