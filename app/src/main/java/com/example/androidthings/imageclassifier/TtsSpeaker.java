/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.imageclassifier;

import android.speech.tts.TextToSpeech;

import com.example.androidthings.imageclassifier.classifier.Classifier.Recognition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class TtsSpeaker {

    private static final String UTTERANCE_ID
            = "com.example.androidthings.imageclassifier.UTTERANCE_ID";
    private static final float HUMOR_THRESHOLD = 0.2f;
    private static final Random RANDOM = new Random();

    private static final List<Utterance> SHUTTER_SOUNDS = new ArrayList<>();
    private static final List<Utterance> JOKES = new ArrayList<>();
    static {
        SHUTTER_SOUNDS.add(new ShutterUtterance("Click!"));
        SHUTTER_SOUNDS.add(new ShutterUtterance("Cheeeeese!"));
        SHUTTER_SOUNDS.add(new ShutterUtterance("Smile!"));

        JOKES.add(new SimpleUtterance("It's a bird! It's a plane! It's... it's..."));
        JOKES.add(new SimpleUtterance("Oops, someone left the lens cap on! Just kidding..."));
        JOKES.add(new SimpleUtterance("Hey, that looks like me! Just kidding..."));
        JOKES.add(new ISeeDeadPeopleUtterance());
    }

    public void speakReady(TextToSpeech tts) {
        tts.speak("I'm ready!", TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
    }

    public void speakShutterSound(TextToSpeech tts) {
        randomElement(SHUTTER_SOUNDS).speak(tts);
    }

    public void speakResults(TextToSpeech tts, List<Recognition> results) {
        boolean humorBefore = speakerHasSenseOfHumor();
        if (humorBefore) {
            randomElement(JOKES).speak(tts);
        }

        if (results.isEmpty()) {
            tts.speak("I don't understand what I see.", TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
            if (!humorBefore && speakerHasSenseOfHumor()) {
                tts.speak("Please don't unplug me, I'll do better next time.",
                        TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
            }
        } else if (results.size() == 1 || results.get(0).getConfidence() > 0.4f) {
            tts.speak(String.format(Locale.getDefault(), "I see a %s", results.get(0).getTitle()),
                    TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        } else {
            tts.speak(String.format(Locale.getDefault(), "This is a %s, or maybe a %s",
                    results.get(0).getTitle(), results.get(1).getTitle()),
                    TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        }
    }

    private static <T> T randomElement(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    private static boolean speakerHasSenseOfHumor() {
        return RANDOM.nextFloat() < HUMOR_THRESHOLD;
    }

    interface Utterance {

        void speak(TextToSpeech tts);
    }

    private static class SimpleUtterance implements Utterance {

        private final String mMessage;

        SimpleUtterance(String message) {
            mMessage = message;
        }

        @Override
        public void speak(TextToSpeech tts) {
            tts.speak(mMessage, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        }
    }

    private static class ShutterUtterance extends SimpleUtterance {

        ShutterUtterance(String message) {
            super(message);
        }

        @Override
        public void speak(TextToSpeech tts) {
            tts.setPitch(1.5f);
            tts.setSpeechRate(1.5f);
            super.speak(tts);
            tts.setPitch(1f);
            tts.setSpeechRate(1f);
        }
    }

    private static class ISeeDeadPeopleUtterance implements Utterance {

        @Override
        public void speak(TextToSpeech tts) {
            tts.setPitch(0.2f);
            tts.speak("I see dead people...", TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
            tts.setPitch(1);
            tts.speak("Just kidding...", TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        }
    }
}
