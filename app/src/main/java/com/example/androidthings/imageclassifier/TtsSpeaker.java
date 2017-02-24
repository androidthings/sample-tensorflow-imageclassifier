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
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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

    /**
     * Don't play the same joke within this span of time
     */
    private static final long JOKE_COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(2);

    /**
     * For multiple results, speak only the first if it has at least this much confidence
     */
    private static final float SINGLE_ANSWER_CONFIDENCE_THRESHOLD = 0.4f;

    /**
     * Stores joke utterances keyed by time last spoken.
     */
    private NavigableMap<Long, Utterance> mJokes;

    /**
     * Controls where to use jokes or not. If true, jokes will be applied randomly. If false, no
     * joke will ever be played. Use {@link #setHasSenseOfHumor(boolean)} to change the mood.
     */
    private boolean mHasSenseOfHumor = true;

    public TtsSpeaker() {
        mJokes = new TreeMap<>();
        long key = 0L;
        for (Utterance joke : JOKES) {
            // can't insert them with same key
            mJokes.put(key++, joke);
        }
    }

    public void speakReady(TextToSpeech tts) {
        tts.speak("I'm ready!", TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
    }

    public void speakShutterSound(TextToSpeech tts) {
        getRandomElement(SHUTTER_SOUNDS).speak(tts);
    }

    public void speakResults(TextToSpeech tts, List<Recognition> results) {
        if (results.isEmpty()) {
            tts.speak("I don't understand what I see.", TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
            if (isFeelingFunnyNow()) {
                tts.speak("Please don't unplug me, I'll do better next time.",
                        TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
            }
        } else {
            if (isFeelingFunnyNow()) {
                playJoke(tts);
            }
            if (results.size() == 1
                    || results.get(0).getConfidence() > SINGLE_ANSWER_CONFIDENCE_THRESHOLD) {
                tts.speak(String.format(Locale.getDefault(),
                        "I see a %s", results.get(0).getTitle()),
                        TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
            } else {
                tts.speak(String.format(Locale.getDefault(), "This is a %s, or maybe a %s",
                        results.get(0).getTitle(), results.get(1).getTitle()),
                        TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
            }
        }

    }

    private boolean playJoke(TextToSpeech tts) {
        long now = System.currentTimeMillis();
        // choose a random joke whose last occurrence was far enough in the past
        SortedMap<Long, Utterance> availableJokes = mJokes.headMap(now - JOKE_COOLDOWN_MILLIS);
        Utterance joke = null;
        if (!availableJokes.isEmpty()) {
            int r = RANDOM.nextInt(availableJokes.size());
            int i = 0;
            for (Long key : availableJokes.keySet()) {
                if (i++ == r) {
                    joke = availableJokes.remove(key); // also removes from mJokes
                    break;
                }
            }
        }
        if (joke != null) {
            joke.speak(tts);
            // add it back with the current time
            mJokes.put(now, joke);
            return true;
        }
        return false;
    }

    private static <T> T getRandomElement(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    private boolean isFeelingFunnyNow() {
        return mHasSenseOfHumor && RANDOM.nextFloat() < HUMOR_THRESHOLD;
    }

    public void setHasSenseOfHumor(boolean hasSenseOfHumor) {
        this.mHasSenseOfHumor = hasSenseOfHumor;
    }

    public boolean hasSenseOfHumor() {
        return mHasSenseOfHumor;
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
