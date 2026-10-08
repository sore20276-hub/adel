package shop.sntat.mobile;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import java.util.Locale;

public final class SplashActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private TextToSpeech tts;
    private boolean advanced = false;
    private final Runnable next = this::openSite;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_splash);
        applyInsets(findViewById(R.id.splash_root));
        playWelcomeSound();
        // Four seconds from the opening of this Activity, with no external webpage in the splash.
        handler.postDelayed(next, 4000L);
    }

    private void applyInsets(View root) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets sys = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
    }

    private void playWelcomeSound() {
        try {
            player = MediaPlayer.create(this, R.raw.welcome_chime);
            if (player != null) {
                player.setVolume(0.38f, 0.38f);
                player.start();
            }
        } catch (RuntimeException ignored) { /* never block opening the site */ }
        try {
            tts = new TextToSpeech(getApplicationContext(), result -> {
                if (result != TextToSpeech.SUCCESS || tts == null || advanced) return;
                int availability = tts.setLanguage(new Locale("ar"));
                if (availability >= TextToSpeech.LANG_AVAILABLE) {
                    tts.setSpeechRate(1.02f);
                    tts.speak("أهلاً بكم في سنتات", TextToSpeech.QUEUE_FLUSH, null, "sntat_welcome");
                }
            });
        } catch (RuntimeException ignored) { /* chime remains the fallback */ }
    }

    private void openSite() {
        if (advanced || isFinishing() || isDestroyed()) return;
        advanced = true;
        startActivity(new Intent(this, WebActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(next);
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (IllegalStateException ignored) {}
            player.release();
            player = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }
}
