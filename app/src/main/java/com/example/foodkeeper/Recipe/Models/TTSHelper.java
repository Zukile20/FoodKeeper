package com.example.foodkeeper.Recipe.Models;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class TTSHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = "TTSHelper";
    private TextToSpeech textToSpeech;
    private Context context;
    private boolean isInitialized = false;
    private TTSListener ttsListener;

    // Interface for TTS events
    public interface TTSListener {
        void onTTSReady();
        void onTTSError(String error);
        void onSpeechStart();
        void onSpeechComplete();
    }
    public TTSHelper(Context context, TTSListener listener) {
        this.context = context;
        this.ttsListener = listener;
        initializeTTS();
    }
    private void initializeTTS() {
        textToSpeech = new TextToSpeech(context, this);
    }
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to US English (you can change this)
            int result = textToSpeech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
                if (ttsListener != null) {
                    ttsListener.onTTSError("Language not supported");
                }
            } else {
                isInitialized = true;
                setupTTSSettings();
                if (ttsListener != null) {
                    ttsListener.onTTSReady();
                }
                Log.d(TAG, "TTS Initialized successfully");
            }
        } else {
            Log.e(TAG, "TTS initialization failed");
            if (ttsListener != null) {
                ttsListener.onTTSError("TTS initialization failed");
            }
        }
    }
    private void setupTTSSettings() {
        // Set speech rate (0.8 = slightly slower for cooking instructions)
        textToSpeech.setSpeechRate(0.8f);

        // Set pitch (1.0 = normal pitch)
        textToSpeech.setPitch(1.0f);

        // Set up utterance progress listener
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "Started speaking recipe");
                if (ttsListener != null) {
                    ttsListener.onSpeechStart();
                }
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d(TAG, "Finished speaking recipe");
                if (ttsListener != null) {
                    ttsListener.onSpeechComplete();
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "Error speaking recipe");
                if (ttsListener != null) {
                    ttsListener.onTTSError("Speech error occurred");
                }
            }
        });
    }
    // Main speak method for the complete recipe
    public void speak(String text) {
        if (!isInitialized) {
            if (ttsListener != null) {
                ttsListener.onTTSError("Text-to-Speech not ready");
            }
            return;
        }

        if (text != null && !text.isEmpty()) {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "complete_recipe");
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        } else {
            if (ttsListener != null) {
                ttsListener.onTTSError("No text to speak");
            }
        }
    }
    // Stop speaking
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }
    // Clean up resources
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
    // Check if TTS is available on device
    public static boolean isTTSAvailable(Context context) {
        try {
            TextToSpeech tts = new TextToSpeech(context, status -> {
                // This is just for checking availability
            });
            boolean available = tts != null;
            if (tts != null) {
                tts.shutdown();
            }
            return available;
        } catch (Exception e) {
            return false;
        }
    }
}
