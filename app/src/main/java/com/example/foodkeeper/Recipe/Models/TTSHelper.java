package com.example.foodkeeper.Recipe.Models;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TTSHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = "TTSHelper";
    private static final String UTTERANCE_ID = "recipe_utterance";

    private TextToSpeech textToSpeech;
    private Context context;
    private volatile boolean isInitialized = false;
    private TTSListener ttsListener;
    private Handler mainHandler;

    // Interface for TTS events
    public interface TTSListener {
        void onTTSReady();
        void onTTSError(String error);
        void onSpeechStart();
        void onSpeechComplete();
    }

    public TTSHelper(Context context, TTSListener listener) {
        this.context = context.getApplicationContext();
        this.ttsListener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        initializeTTS();
    }

    private void initializeTTS() {
        try {
            textToSpeech = new TextToSpeech(context, this);
        } catch (Exception e) {
            notifyError("Failed to initialize TTS: " + e.getMessage());
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                int result = textToSpeech.setLanguage(Locale.US);

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    notifyError("Language not supported");
                } else {
                    isInitialized = true;
                    setupTTSSettings();
                    notifyReady();
                }
            } catch (Exception e) {
                notifyError("TTS setup failed: " + e.getMessage());
            }
        } else {
            notifyError("TTS initialization failed");
        }
    }

    private void setupTTSSettings() {
        try {
            textToSpeech.setSpeechRate(0.8f);

            textToSpeech.setPitch(1.0f);

            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    notifySpeechStart();
                }

                @Override
                public void onDone(String utteranceId) {
                    notifySpeechComplete();
                }

                @Override
                public void onError(String utteranceId) {
                    notifyError("Speech error occurred");
                }

                @Override
                public void onError(String utteranceId, int errorCode) {
                    notifyError("Speech error: " + errorCode);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up TTS settings", e);
        }
    }

    /**
     * Speak the given text
     * @param text Text to be spoken
     */
    public void speak(String text) {
        if (!isInitialized) {
            notifyError("Text-to-Speech not ready");
            return;
        }

        if (textToSpeech == null) {
            notifyError("TextToSpeech instance is null");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            notifyError("No text to speak");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        } catch (Exception e) {
            notifyError("Failed to speak text: " + e.getMessage());
        }
    }

    /**
     * Stop current speech
     */
    public void stop() {
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
                Log.d(TAG, "TTS stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping TTS", e);
            }
        }
    }

    /**
     * Clean up resources - must be called when done
     */
    public void shutdown() {
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
                textToSpeech.shutdown();
                Log.d(TAG, "TTS shut down successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during shutdown", e);
            } finally {
                textToSpeech = null;
                isInitialized = false;
            }
        }
    }

    /**
     * Check if TTS is available on device
     * @param context Application context
     * @return true if available, false otherwise
     */
    public static boolean isTTSAvailable(Context context) {
        try {
            TextToSpeech tts = new TextToSpeech(context.getApplicationContext(),
                    status -> {
                        // Callback for checking availability
                    });

            // Give it a moment to initialize
            Thread.sleep(100);

            boolean available = tts != null;
            if (tts != null) {
                tts.shutdown();
            }
            return available;
        } catch (Exception e) {
            Log.e(TAG, "Error checking TTS availability", e);
            return false;
        }
    }

    // Thread-safe notification methods
    private void notifyReady() {
        if (ttsListener != null) {
            mainHandler.post(() -> ttsListener.onTTSReady());
        }
    }

    private void notifyError(String error) {
        if (ttsListener != null) {
            mainHandler.post(() -> ttsListener.onTTSError(error));
        }
    }

    private void notifySpeechStart() {
        if (ttsListener != null) {
            mainHandler.post(() -> ttsListener.onSpeechStart());
        }
    }

    private void notifySpeechComplete() {
        if (ttsListener != null) {
            mainHandler.post(() -> ttsListener.onSpeechComplete());
        }
    }
}