package shop.sntat.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** SNTAT's in-app browser. Auth cookies/DOM storage belong only to Android WebView. */
public final class WebActivity extends Activity {
    private static final String LOGIN_URL = "https://sntat.shop/auth?mode=signin";
    private static final String PREFS = "sntat_local_navigation";
    private static final String LAST_URL = "last_safe_page";
    private static final int FILE_PICKER = 810;
    private static final int LOCATION_PERMISSION = 811;
    private static final int MEDIA_PERMISSION = 812;
    private FrameLayout root;
    private WebView browser;
    private ProgressBar progress;
    private LinearLayout errorPanel;
    private ValueCallback<Uri[]> fileCallback;
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;
    private PermissionRequest mediaRequest;
    private String[] mediaResources;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8, 9, 27));
        getWindow().setNavigationBarColor(Color.rgb(8, 9, 27));
        createInterface();
        configureBrowser();
        if (state != null && browser.restoreState(state) != null) {
            // Restore page / WebView history after rotation or system recreation.
        } else {
            browser.loadUrl(getStartUrl());
        }
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void createInterface() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8, 9, 27));
        browser = new WebView(this);
        browser.setBackgroundColor(Color.rgb(8, 9, 27));
        root.addView(browser, new FrameLayout.LayoutParams(-1, -1));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setIndeterminate(false);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(238, 176, 54)));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
        root.addView(progress, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));
        progress.setVisibility(View.GONE);

        errorPanel = new LinearLayout(this);
        errorPanel.setBackgroundColor(Color.rgb(8, 9, 27));
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(24), dp(24), dp(24), dp(24));
        errorPanel.setOrientation(LinearLayout.VERTICAL);

        TextView headline = new TextView(this);
        headline.setText(R.string.connection_error);
        headline.setTextColor(Color.WHITE);
        headline.setTextSize(22);
        headline.setGravity(Gravity.CENTER);
        headline.setTypeface(null, 1);
        errorPanel.addView(headline);

        TextView description = new TextView(this);
        description.setText(R.string.connection_error_detail);
        description.setTextColor(Color.rgb(186, 187, 196));
        description.setTextSize(15);
        description.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(12);
        errorPanel.addView(description, descParams);

        Button retry = new Button(this);
        retry.setText(R.string.try_again);
        retry.setTextColor(Color.rgb(16, 16, 23));
        retry.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(249, 185, 61)));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(180), dp(52));
        buttonParams.topMargin = dp(25);
        errorPanel.addView(retry, buttonParams);
        retry.setOnClickListener(v -> {
            errorPanel.setVisibility(View.GONE);
            String address = browser.getUrl();
            if (address == null || address.startsWith("about:")) browser.loadUrl(getStartUrl());
            else browser.reload();
        });
        root.addView(errorPanel, new FrameLayout.LayoutParams(-1, -1));
        errorPanel.setVisibility(View.GONE);
        setContentView(root);
        applySystemInsets(root);
    }

    private void applySystemInsets(View view) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets system = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(system.left, system.top, system.right, system.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
    }

    private void configureBrowser() {
        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true);     // Necessary for the website's login UI.
        settings.setDomStorageEnabled(true);     // Retains site auth state/localStorage.
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);    // Android picker-selected content:// files.
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setGeolocationEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);

        browser.setOverScrollMode(View.OVER_SCROLL_NEVER);
        browser.setVerticalScrollBarEnabled(false);
        browser.setHorizontalScrollBarEnabled(false);
        browser.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        browser.setSaveEnabled(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        // Some federated auth services depend on cross-domain cookies.
        cookies.setAcceptThirdPartyCookies(browser, true);

        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return blockUnsupportedNavigation(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return blockUnsupportedNavigation(Uri.parse(url));
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                errorPanel.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
                progress.setProgress(8);
            }
            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                rememberSitePage(url);
                CookieManager.getInstance().flush();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progress.setVisibility(View.GONE);
                    errorPanel.setVisibility(View.VISIBLE);
                }
            }
        });

        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                if (value == 100) progress.setVisibility(View.GONE);
            }
            @Override public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(WebActivity.this)
                        .setTitle("سنتات")
                        .setMessage(clip(message))
                        .setPositiveButton("حسناً", (dialog, which) -> result.confirm())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }
            @Override public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(WebActivity.this)
                        .setTitle("سنتات")
                        .setMessage(clip(message))
                        .setPositiveButton("موافق", (dialog, which) -> result.confirm())
                        .setNegativeButton("إلغاء", (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }
            @Override public boolean onJsPrompt(WebView view, String url, String message,
                                                String defaultValue, JsPromptResult result) {
                EditText input = new EditText(WebActivity.this);
                input.setSingleLine(true);
                input.setText(defaultValue == null ? "" : defaultValue);
                int pad = dp(22);
                FrameLayout fieldBox = new FrameLayout(WebActivity.this);
                fieldBox.setPadding(pad, 0, pad, 0);
                fieldBox.addView(input);
                new AlertDialog.Builder(WebActivity.this)
                        .setTitle("سنتات")
                        .setMessage(clip(message))
                        .setView(fieldBox)
                        .setPositiveButton("موافق", (dialog, which) -> result.confirm(input.getText().toString()))
                        .setNegativeButton("إلغاء", (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }
            @Override public boolean onShowFileChooser(WebView view,
                                  ValueCallback<Uri[]> result, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = result;
                try {
                    Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    String[] accepts = params.getAcceptTypes();
                    String type = "*/*";
                    if (accepts != null && accepts.length == 1 && accepts[0] != null
                            && accepts[0].contains("/")) type = accepts[0];
                    contentIntent.setType(type);
                    contentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                            params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                    startActivityForResult(Intent.createChooser(contentIntent, "اختيار ملف - سنتات"), FILE_PICKER);
                    return true;
                } catch (Exception exception) {
                    fileCallback = null;
                    result.onReceiveValue(null);
                    return true;
                }
            }
            @Override public boolean onCreateWindow(WebView view, boolean isDialog,
                                                    boolean isUserGesture, Message resultMsg) {
                if (!isUserGesture) return false;
                // Open target="_blank" links in the same SNTAT view, never in a browser app.
                final WebView popup = new WebView(WebActivity.this);
                root.addView(popup, new FrameLayout.LayoutParams(1, 1));
                popup.setWebViewClient(new WebViewClient() {
                    private boolean handled = false;
                    private void loadInside(String url) {
                        if (handled || url == null || url.equals("about:blank")) return;
                        handled = true;
                        Uri uri = Uri.parse(url);
                        if (!blockUnsupportedNavigation(uri)) browser.loadUrl(url);
                        popup.post(() -> {
                            root.removeView(popup);
                            popup.destroy();
                        });
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        loadInside(request.getUrl().toString());
                        return true;
                    }
                    @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                        loadInside(url);
                    }
                });
                ((WebView.WebViewTransport) resultMsg.obj).setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin,
                                                                     GeolocationPermissions.Callback callback) {
                promptForLocation(origin, callback);
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> promptForMedia(request));
            }
            @Override public void onPermissionRequestCanceled(PermissionRequest request) {
                if (request == mediaRequest) {
                    mediaRequest = null;
                    mediaResources = null;
                }
            }
        });

        browser.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                Uri uri = Uri.parse(url);
                if (!"https".equalsIgnoreCase(uri.getScheme())) {
                    internalMessage("هذا التنزيل غير متاح بصورة آمنة.");
                    return;
                }
                DownloadManager.Request request = new DownloadManager.Request(uri);
                String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                request.setTitle(filename);
                request.setMimeType(mimeType);
                request.setAllowedOverMetered(true);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                request.setDestinationInExternalFilesDir(WebActivity.this, Environment.DIRECTORY_DOWNLOADS, filename);
                request.addRequestHeader("User-Agent", userAgent);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) request.addRequestHeader("Cookie", cookie);
                ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
                internalMessage("بدأ تنزيل الملف داخل مجلد سنتات.");
            } catch (Exception error) {
                internalMessage("تعذّر تنزيل الملف حالياً.");
            }
        });
    }

    private static String clip(String text) {
        if (text == null) return "";
        return text.length() <= 1600 ? text : text.substring(0, 1600);
    }

    private boolean blockUnsupportedNavigation(Uri uri) {
        if (uri == null || uri.getScheme() == null) return true;
        String scheme = uri.getScheme().toLowerCase(Locale.US);
        if (scheme.equals("https")) return false;
        if (scheme.equals("http")) {
            // Prevent silently loading cleartext pages in the embedded browser.
            internalMessage("الرابط غير مشفّر؛ افتح رابط HTTPS آمن.");
            return true;
        }
        if (scheme.equals("about") || scheme.equals("blob") || scheme.equals("data")) return false;
        internalMessage("الرابط لا يدعم الفتح الآمن داخل سنتات.");
        return true;
    }

    private boolean isTrustedOrigin(String origin) {
        if (origin == null) return false;
        Uri uri = Uri.parse(origin);
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) && host != null
                && (host.equalsIgnoreCase("sntat.shop") || host.toLowerCase(Locale.US).endsWith(".sntat.shop"));
    }

    private void promptForLocation(String origin, GeolocationPermissions.Callback callback) {
        if (!isTrustedOrigin(origin)) { callback.invoke(origin, false, false); return; }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            callback.invoke(origin, true, true);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("إذن الموقع - سنتات")
                .setMessage("تحتاج المنصة إلى إذن الموقع إذا أردت استخدام الخدمات المعتمدة عليه.")
                .setPositiveButton("متابعة", (dialog, which) -> {
                    locationOrigin = origin;
                    locationCallback = callback;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION);
                })
                .setNegativeButton("ليس الآن", (dialog, which) -> callback.invoke(origin, false, false))
                .setOnCancelListener(dialog -> callback.invoke(origin, false, false))
                .show();
    }

    private void promptForMedia(PermissionRequest request) {
        if (!isTrustedOrigin(request.getOrigin().toString())) { request.deny(); return; }
        List<String> required = new ArrayList<>();
        for (String name : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(name)) {
                if (!required.contains(Manifest.permission.CAMERA)) required.add(Manifest.permission.CAMERA);
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(name)) {
                if (!required.contains(Manifest.permission.RECORD_AUDIO)) required.add(Manifest.permission.RECORD_AUDIO);
            } else {
                request.deny();
                return;
            }
        }
        if (required.isEmpty()) { request.deny(); return; }
        boolean granted = true;
        for (String permission : required) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) granted = false;
        }
        if (granted) { request.grant(request.getResources()); return; }
        new AlertDialog.Builder(this)
                .setTitle("إذن الوسائط - سنتات")
                .setMessage("هل تسمح للمنصة باستخدام الكاميرا أو الميكروفون عند الحاجة؟")
                .setPositiveButton("متابعة", (dialog, which) -> {
                    mediaRequest = request;
                    mediaResources = request.getResources();
                    requestPermissions(required.toArray(new String[0]), MEDIA_PERMISSION);
                })
                .setNegativeButton("رفض", (dialog, which) -> request.deny())
                .setOnCancelListener(dialog -> request.deny())
                .show();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        boolean granted = results.length > 0;
        for (int result : results) if (result != PackageManager.PERMISSION_GRANTED) granted = false;
        if (code == LOCATION_PERMISSION && locationCallback != null) {
            locationCallback.invoke(locationOrigin, granted, granted);
            locationCallback = null;
            locationOrigin = null;
        } else if (code == MEDIA_PERMISSION && mediaRequest != null) {
            if (granted) mediaRequest.grant(mediaResources);
            else mediaRequest.deny();
            mediaRequest = null;
            mediaResources = null;
        }
    }

    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code != FILE_PICKER || fileCallback == null) return;
        Uri[] files = null;
        if (result == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int n = data.getClipData().getItemCount();
                files = new Uri[n];
                for (int i = 0; i < n; i++) files[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) files = new Uri[]{data.getData()};
        }
        fileCallback.onReceiveValue(files);
        fileCallback = null;
    }

    private String getStartUrl() {
        String page = getSharedPreferences(PREFS, MODE_PRIVATE).getString(LAST_URL, "");
        return isSafeRememberedUrl(page) ? page : LOGIN_URL;
    }

    private static boolean isSafeRememberedUrl(String link) {
        if (link == null || link.isEmpty()) return false;
        Uri parsed = Uri.parse(link);
        return "https".equalsIgnoreCase(parsed.getScheme())
                && "sntat.shop".equalsIgnoreCase(parsed.getHost())
                && !isAuthPath(parsed.getPath());
    }

    private static boolean isAuthPath(String path) {
        if (path == null) return false;
        String p = path.toLowerCase(Locale.US);
        return p.startsWith("/auth") || p.contains("logout") || p.contains("signout")
                || p.contains("callback") || p.contains("oauth");
    }

    private void rememberSitePage(String url) {
        if (url == null) return;
        Uri uri = Uri.parse(url);
        SharedPreferences.Editor edit = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        if ("sntat.shop".equalsIgnoreCase(uri.getHost()) && isAuthPath(uri.getPath())) {
            // Don't restore potentially expired auth callbacks or logout flows.
            if (uri.getPath() != null && (uri.getPath().contains("logout") || uri.getPath().contains("signout")))
                edit.remove(LAST_URL).apply();
        } else if (isSafeRememberedUrl(url)) {
            // No secrets/query access tokens persisted; WebView owns the auth cookies.
            String safe = uri.buildUpon().encodedQuery(null).fragment(null).build().toString();
            edit.putString(LAST_URL, safe).apply();
        }
    }

    private void internalMessage(String text) {
        new AlertDialog.Builder(this).setTitle("سنتات").setMessage(text)
                .setPositiveButton("حسناً", (dialog, which) -> {}).show();
    }

    @Override public void onBackPressed() {
        if (errorPanel.getVisibility() == View.VISIBLE) {
            errorPanel.setVisibility(View.GONE);
            browser.loadUrl(getStartUrl());
        } else if (browser != null && browser.canGoBack()) browser.goBack();
        else moveTaskToBack(true);
    }

    @Override protected void onPause() {
        super.onPause();
        if (browser != null) browser.onPause();
        CookieManager.getInstance().flush();
    }

    @Override protected void onResume() {
        super.onResume();
        if (browser != null) browser.onResume();
    }

    @Override protected void onSaveInstanceState(Bundle output) {
        if (browser != null) browser.saveState(output);
        super.onSaveInstanceState(output);
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }
        if (locationCallback != null) {
            locationCallback.invoke(locationOrigin, false, false);
            locationCallback = null;
        }
        if (mediaRequest != null) {
            mediaRequest.deny();
            mediaRequest = null;
        }
        if (browser != null) {
            CookieManager.getInstance().flush();
            root.removeView(browser);
            browser.stopLoading();
            browser.setWebChromeClient(null);
            browser.setWebViewClient(new WebViewClient());
            browser.destroy();
            browser = null;
        }
        super.onDestroy();
    }
}
