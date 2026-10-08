package com.adel.app;

import android.app.Activity;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.text.InputType;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private static final int FILE_CHOOSER_REQUEST = 1101;
    private static final String PREFS_NAME = "adel_app_prefs";
    private static final String KEY_WELCOME_SHOWN = "welcome_shown";
    private static final String KEY_LOGIN_NOTICE_SHOWN = "login_notice_shown_v2";
    private static final String KEY_SAVED_COOKIES = "saved_platform_cookies_v1";
    private static final String KEY_LAST_PLATFORM_URL = "last_platform_url_v1";
    private static final String KEY_WEB_STORAGE_STATE = "web_storage_state_v1";
    private static final String KEY_PHONE_GATE_DONE = "phone_gate_done_v1";
    private static final String KEY_PHONE_NUMBER = "phone_number_v1";
    private static final long WELCOME_DURATION_MS = 2400L;

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean storageRestoreAttempted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showWelcomeScreen();
    }

    private void showWelcomeScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(28), dp(24), dp(28));
        root.setBackgroundColor(Color.rgb(4, 4, 7));

        // Outer glow layer
        GradientDrawable glowBg = new GradientDrawable();
        glowBg.setShape(GradientDrawable.OVAL);
        glowBg.setColor(Color.rgb(28, 20, 5));
        glowBg.setStroke(dp(5), Color.rgb(225, 181, 65));

        FrameLayout logoBox = new FrameLayout(this);
        logoBox.setBackground(glowBg);
        logoBox.setPadding(dp(9), dp(9), dp(9), dp(9));

        ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.welcome_zilzal);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable clip = new GradientDrawable();
        clip.setShape(GradientDrawable.OVAL);
        clip.setColor(Color.BLACK);
        image.setBackground(clip);
        image.setClipToOutline(true);
        logoBox.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(270), dp(270));
        logoParams.bottomMargin = dp(24);
        root.addView(logoBox, logoParams);

        // Smooth endless rotation
        ObjectAnimator rotate = ObjectAnimator.ofFloat(image, View.ROTATION, 0f, 360f);
        rotate.setDuration(4000);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setInterpolator(new android.view.animation.LinearInterpolator());
        rotate.start();

        // Neon-like breathing glow
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(logoBox, View.SCALE_X, 0.96f, 1.035f);
        pulseX.setDuration(850);
        pulseX.setRepeatCount(ValueAnimator.INFINITE);
        pulseX.setRepeatMode(ValueAnimator.REVERSE);
        pulseX.start();
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(logoBox, View.SCALE_Y, 0.96f, 1.035f);
        pulseY.setDuration(850);
        pulseY.setRepeatCount(ValueAnimator.INFINITE);
        pulseY.setRepeatMode(ValueAnimator.REVERSE);
        pulseY.start();
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logoBox, View.ALPHA, 0.78f, 1f);
        alpha.setDuration(700);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatMode(ValueAnimator.REVERSE);
        alpha.start();

        TextView welcome = new TextView(this);
        welcome.setText("زلزال يرحب بكم");
        welcome.setTextColor(Color.WHITE);
        welcome.setTextSize(28);
        welcome.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        welcome.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams welcomeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        welcomeParams.bottomMargin = dp(18);
        root.addView(welcome, welcomeParams);

        Button join = new Button(this);
        join.setText("انضم إلى قناتنا  snt41");
        join.setTextColor(Color.rgb(15, 15, 18));
        join.setTextSize(18);
        join.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        join.setAllCaps(false);
        GradientDrawable joinBg = new GradientDrawable();
        joinBg.setColor(Color.rgb(225, 181, 65));
        joinBg.setCornerRadius(dp(16));
        join.setBackground(joinBg);
        join.setOnClickListener(v -> openExternal(Uri.parse("https://t.me/snt41")));
        root.addView(join, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        TextView timer = new TextView(this);
        timer.setText("سيتم الدخول تلقائياً خلال 4 ثوانٍ");
        timer.setTextColor(Color.rgb(180, 180, 185));
        timer.setTextSize(14);
        timer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timerParams.topMargin = dp(16);
        root.addView(timer, timerParams);

        setContentView(root);

        // Auto enter after 4 seconds.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                rotate.cancel();
                pulseX.cancel();
                pulseY.cancel();
                alpha.cancel();
                showPhoneGateOrPlatform(null);
            }
        }, 4000);
    }

    private void showPhoneGateOrPlatform(Bundle savedInstanceState) {
        showPlatform(savedInstanceState);
    }

    private void showPhoneGate(Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(30), dp(28), dp(30));
        root.setBackgroundColor(Color.rgb(8, 8, 10));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        iconParams.bottomMargin = dp(22);
        root.addView(icon, iconParams);

        TextView title = new TextView(this);
        title.setText("تسجيل الدخول");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("أدخل رقم هاتفك للمتابعة إلى زلزال");
        hint.setTextColor(Color.rgb(175, 175, 185));
        hint.setTextSize(15);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(8);
        hintParams.bottomMargin = dp(26);
        root.addView(hint, hintParams);

        LinearLayout phoneRow = new LinearLayout(this);
        phoneRow.setOrientation(LinearLayout.HORIZONTAL);
        phoneRow.setGravity(Gravity.CENTER_VERTICAL);
        phoneRow.setPadding(dp(14), 0, dp(14), 0);
        GradientDrawable fieldBg = new GradientDrawable();
        fieldBg.setColor(Color.rgb(24, 24, 28));
        fieldBg.setCornerRadius(dp(14));
        fieldBg.setStroke(dp(1), Color.rgb(55, 55, 64));
        phoneRow.setBackground(fieldBg);

        TextView prefix = new TextView(this);
        prefix.setText("+964");
        prefix.setTextColor(Color.WHITE);
        prefix.setTextSize(18);
        prefix.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        prefix.setGravity(Gravity.CENTER);
        phoneRow.addView(prefix, new LinearLayout.LayoutParams(dp(64), dp(58)));

        EditText phone = new EditText(this);
        phone.setHint("7XX XXX XXXX");
        phone.setHintTextColor(Color.rgb(115, 115, 125));
        phone.setTextColor(Color.WHITE);
        phone.setTextSize(18);
        phone.setSingleLine(true);
        phone.setGravity(Gravity.CENTER_VERTICAL);
        phone.setBackgroundColor(Color.TRANSPARENT);
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        phone.setPadding(dp(8), 0, dp(4), 0);
        phoneRow.addView(phone, new LinearLayout.LayoutParams(0, dp(58), 1f));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        rowParams.bottomMargin = dp(18);
        root.addView(phoneRow, rowParams);

        Button enter = new Button(this);
        enter.setText("دخول");
        enter.setTextColor(Color.WHITE);
        enter.setTextSize(18);
        enter.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        enter.setAllCaps(false);
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setColor(Color.rgb(30, 130, 245));
        buttonBg.setCornerRadius(dp(14));
        enter.setBackground(buttonBg);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        root.addView(enter, buttonParams);

        TextView note = new TextView(this);
        note.setText("الدخول برقم الهاتف داخل التطبيق");
        note.setTextColor(Color.rgb(105, 105, 115));
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(16);
        root.addView(note, noteParams);

        enter.setOnClickListener(v -> {
            String digits = phone.getText().toString().replaceAll("[^0-9]", "");
            if (digits.startsWith("0")) digits = digits.substring(1);
            if (digits.length() != 10 || !digits.startsWith("7")) {
                Toast.makeText(this, "أدخل رقم عراقي صحيح مثل 7XX XXX XXXX", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putBoolean(KEY_PHONE_GATE_DONE, true)
                    .putString(KEY_PHONE_NUMBER, "+964" + digits)
                    .apply();
            showPlatform(savedInstanceState);
        });

        setContentView(root);
    }

    private void showPlatform(Bundle savedInstanceState) {
        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        // Prevent Android/Google password-manager overlays from appearing over the site.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }

        setContentView(webView);
        configureWebView();
        restoreSavedCookies();

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(getPreferredStartUrl());
        }
    }

    @SuppressWarnings("deprecation")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setSaveFormData(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settings.setOffscreenPreRaster(true);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(view, request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(view, Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (isPlatformUrl(url)) {
                    saveCookiesAndUrl(url);

                    // Some web platforms keep their auth token in localStorage/sessionStorage.
                    // Restore it once after a cold app start, then reload the page so the
                    // platform can detect the previous signed-in session.
                    if (restoreSavedWebStorageOnce(view)) {
                        return;
                    }

                    backupWebStorage(view);
                }

                CookieManager.getInstance().flush();
                maybeShowLoginNotice(url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }
                fileChooserCallback = filePathCallback;

                Intent intent;
                try {
                    intent = fileChooserParams.createIntent();
                } catch (Exception e) {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                }

                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileChooserCallback = null;
                    return false;
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                WebView child = new WebView(MainActivity.this);
                child.getSettings().setJavaScriptEnabled(true);
                child.getSettings().setDomStorageEnabled(true);
                child.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView childView,
                                                            WebResourceRequest request) {
                        Uri uri = request.getUrl();
                        if (isHttp(uri)) {
                            webView.loadUrl(uri.toString());
                        } else {
                            openExternal(uri);
                        }
                        childView.destroy();
                        return true;
                    }

                    @Override
                    @SuppressWarnings("deprecation")
                    public boolean shouldOverrideUrlLoading(WebView childView, String url) {
                        Uri uri = Uri.parse(url);
                        if (isHttp(uri)) {
                            webView.loadUrl(uri.toString());
                        } else {
                            openExternal(uri);
                        }
                        childView.destroy();
                        return true;
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(child);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                openExternal(Uri.parse(url));
            }
        });
    }

    private String getPreferredStartUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUrl = prefs.getString(KEY_LAST_PLATFORM_URL, null);
        if (isPlatformUrl(savedUrl)) {
            return savedUrl;
        }
        return getString(R.string.start_url);
    }

    private boolean isPlatformUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host != null && (host.equalsIgnoreCase("sntat.shop") || host.endsWith(".sntat.shop"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private void restoreSavedCookies() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cookies = prefs.getString(KEY_SAVED_COOKIES, null);
        if (cookies == null || cookies.trim().isEmpty()) return;

        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(true);

        String baseUrl = "https://sntat.shop/auth?mode=signin";
        String[] cookieParts = cookies.split(";\\s*");
        for (String cookie : cookieParts) {
            String value = cookie.trim();
            if (value.isEmpty() || !value.contains("=")) continue;
            manager.setCookie(baseUrl, value + "; Path=/; Secure");
        }
        manager.flush();
    }

    private void saveCookiesAndUrl(String url) {
        if (!isPlatformUrl(url)) return;

        CookieManager manager = CookieManager.getInstance();
        String cookies = manager.getCookie(url);

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        if (cookies != null && !cookies.trim().isEmpty()) {
            editor.putString(KEY_SAVED_COOKIES, cookies);
        }
        editor.putString(KEY_LAST_PLATFORM_URL, url);
        editor.apply();
        manager.flush();
    }

    private void backupWebStorage(WebView view) {
        if (view == null) return;
        String script = "(function(){try{" +
                "var l={},s={};" +
                "for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);l[k]=localStorage.getItem(k);}" +
                "for(var j=0;j<sessionStorage.length;j++){var q=sessionStorage.key(j);s[q]=sessionStorage.getItem(q);}" +
                "return JSON.stringify({local:l,session:s});" +
                "}catch(e){return null;}})();";

        view.evaluateJavascript(script, value -> {
            if (value == null || "null".equals(value) || "\"null\"".equals(value)) return;
            // evaluateJavascript returns a valid JavaScript string literal. Keeping that
            // literal lets us safely inject the exact JSON back on the next cold start.
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_WEB_STORAGE_STATE, value)
                    .apply();
        });
    }

    private boolean restoreSavedWebStorageOnce(WebView view) {
        if (storageRestoreAttempted || view == null) return false;
        storageRestoreAttempted = true;

        String savedLiteral = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_WEB_STORAGE_STATE, null);
        if (savedLiteral == null || savedLiteral.trim().isEmpty() || "null".equals(savedLiteral)) {
            return false;
        }

        String script = "(function(){try{" +
                "var raw=" + savedLiteral + ";" +
                "if(!raw)return false;" +
                "var data=JSON.parse(raw);" +
                "if(data.local){Object.keys(data.local).forEach(function(k){localStorage.setItem(k,data.local[k]);});}" +
                "if(data.session){Object.keys(data.session).forEach(function(k){sessionStorage.setItem(k,data.session[k]);});}" +
                "return true;" +
                "}catch(e){return false;}})();";

        view.evaluateJavascript(script, result -> {
            if ("true".equals(result) && webView != null && !isFinishing() && !isDestroyed()) {
                webView.reload();
            } else if (webView != null) {
                backupWebStorage(webView);
            }
        });
        return true;
    }

    private boolean handleNavigation(WebView view, Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return false;

        if (isHttp(uri)) {
            // Keep normal site links, registration and account pages inside the app.
            String host = uri.getHost();
            if (host != null && (host.equalsIgnoreCase("sntat.shop") || host.endsWith(".sntat.shop"))) {
                return false;
            }

            // External authentication/providers are opened normally so they are not blocked
            // by embedded-WebView restrictions.
            openExternal(uri);
            return true;
        }

        openExternal(uri);
        return true;
    }

    private boolean isHttp(Uri uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private void maybeShowLoginNotice(String url) {
        if (url == null || !url.contains("sntat.shop/auth")) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_LOGIN_NOTICE_SHOWN, false)) return;
        prefs.edit().putBoolean(KEY_LOGIN_NOTICE_SHOWN, true).apply();

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.login_notice_title))
                .setMessage(getString(R.string.login_notice_message))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            // Keep the app silent if no handler exists.
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileChooserCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            }
            fileChooserCallback.onReceiveValue(results);
            fileChooserCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            String currentUrl = webView.getUrl();
            if (isPlatformUrl(currentUrl)) {
                saveCookiesAndUrl(currentUrl);
                backupWebStorage(webView);
            }
        }
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
