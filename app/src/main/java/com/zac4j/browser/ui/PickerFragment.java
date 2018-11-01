package com.zac4j.browser.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.zac4j.browser.Logger;
import com.zac4j.browser.R;
import com.zac4j.browser.util.FileUtil;
import com.zac4j.browser.util.photo.PhotoManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

/**
 * Image pick page.
 * Created by Zaccc on 2018/2/6.
 */

public class PickerFragment extends Fragment implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "PickerFragment";

    private WebView mWebView;
    private ValueCallback<Uri[]> mFilePathCallback;

    public static final int REQUEST_CODE_PERMS = 0x101;

    public PickerFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_photo_picker, container, false);

        mWebView = rootView.findViewById(R.id.image_picker_webview);

        setUpWebViewDefaults(mWebView);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore the previous URL and history stack
            mWebView.restoreState(savedInstanceState);
        }

        // Load the local index.html file
        if (mWebView.getUrl() == null) {
            mWebView.loadUrl("file:///android_asset/image_picker/index.html");
        }

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkOrRequestPerms();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void checkOrRequestPerms() {
        // For Android O, it's should request these two permissions.
        String[] perms = {
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (!EasyPermissions.hasPermissions(Objects.requireNonNull(getActivity()), perms)) {
            EasyPermissions.requestPermissions(
                new PermissionRequest.Builder(this, REQUEST_CODE_PERMS, perms).setRationale(
                    "This is request file permission rational")
                    .setPositiveButtonText("OK")
                    .setNegativeButtonText("Cancel")
                    .build());
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        // todo permission granted
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Logger.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        destroyWebView();
    }

    private void destroyWebView() {
        if (mWebView != null) {
            mWebView.clearHistory();

            // NOTE: clears RAM cache, if you pass true, it will also clear the disk cache.
            // Probably not a great idea to pass true if you have other WebViews still alive.
            mWebView.clearCache(true);

            // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
            mWebView.loadUrl("about:blank");

            mWebView.onPause();
            mWebView.removeAllViews();
            mWebView.destroyDrawingCache();

            // NOTE: This pauses JavaScript execution for ALL WebViews,
            // do not use if you have other WebViews still alive.
            // If you create another WebView after calling this,
            // make sure to call mWebView.resumeTimers().
            mWebView.pauseTimers();

            // NOTE: This can occasionally cause a segfault below API 17 (4.2)
            mWebView.destroy();

            // Null out the reference so that you don't end up re-using it.
            mWebView = null;
        }
    }

    /**
     * Convenience method to set some generic defaults for a
     * given WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setUpWebViewDefaults(WebView webView) {
        WebSettings settings = webView.getSettings();

        // Enable Javascript
        settings.setJavaScriptEnabled(true);

        // Use WideViewport and Zoom out if there is no viewport defined
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // Enable pinch to zoom without the zoom buttons
        settings.setBuiltInZoomControls(true);

        // Hide the zoom controls for HONEYCOMB+
        settings.setDisplayZoomControls(false);

        // Enable remote debugging via chrome://inspect
        WebView.setWebContentsDebuggingEnabled(true);

        setBrowserClient();
    }

    private void setBrowserClient() {
        // We set the WebViewClient to ensure links are consumed by the WebView rather
        // than passed to a browser if it can
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Logger.d(TAG, "Browser link url: " + url);
                if (url.startsWith("gtjayyz://saveImg")) {
                    String src = url.substring(url.indexOf("?src=") + 5);
                    boolean createSuccess = false;
                    // save this file into local storage.
                    if (src.startsWith("http")) {
                        PhotoManager.saveNetworkImage(getActivity(), src);
                    } else if (src.startsWith("data:image")) {
                        createSuccess = PhotoManager.saveEncodedImage(src);
                    }
                    if (createSuccess) {
                        Toast.makeText(getActivity(), "文件创建保存成功!", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Logger.d(TAG, "Browser link url: " + url);
                if (url.startsWith("gtjayyz://saveImg")) {
                    String src = url.substring(url.indexOf("?src=") + 5);
                    boolean createSuccess = false;
                    // save this file into local storage.
                    if (src.startsWith("http")) {
                        PhotoManager.saveNetworkImage(getActivity(), src);
                    } else if (src.startsWith("data:image")) {
                        createSuccess = PhotoManager.saveEncodedImage(src);
                    }
                    if (createSuccess) {
                        Toast.makeText(getActivity(), "文件创建保存成功!", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams) {

                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                PhotoManager.createImageChooser(PickerFragment.this);

                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            Toast.makeText(getActivity(), "Returned from app settings to activity",
                Toast.LENGTH_SHORT).show();
            return;
        }

        if (mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == PhotoManager.REQUEST_CODE_IMAGE_CHOOSER) {
            Uri[] results = null;
            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        results = new Uri[] { PhotoManager.getCurrentPhotoUri() };
                    } else {
                        File photoFile = new File(PhotoManager.getCurrentPhotoPath());
                        results = new Uri[] { Uri.fromFile(photoFile) };
                    }
                } else {
                    ClipData clipData = data.getClipData();
                    Uri uri = data.getData();

                    // Multiple image results in clip data.
                    int clipCount = clipData == null ? 0 : clipData.getItemCount();
                    if (clipCount > 0) {
                        results = new Uri[clipCount];
                        for (int i = 0; i < clipCount; i++) {
                            results[i] = clipData.getItemAt(i).getUri();
                        }
                    }

                    // Single image result
                    if (uri != null) {
                        results = new Uri[] { uri };
                    }
                }
                // send to web page.
                // handleImageResults(getActivity(), results);
                // send to photo crop screen.
                PhotoManager.createImageCropper(PickerFragment.this, results[0]);
            }
        } else if (requestCode == PhotoManager.REQUEST_CODE_IMAGE_CROPPER) {
            String filePath = PhotoManager.getCroppedPhotoPath();
            Uri[] uris = new Uri[] { Uri.fromFile(new File(filePath)) };
            handleImageResults(getActivity(), uris);
        }
    }

    /**
     * Process image results in a reactive way..
     *
     * @param context ctx
     * @param uris uris contains image results.
     */
    private Disposable processImageResults(final Context context, Uri[] uris) {
        return Observable.fromArray(uris)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .filter(uri -> uri != null && !TextUtils.isEmpty(FileUtil.getPath(context, uri)))
            .flatMap(uri -> {
                String filePath = FileUtil.getPath(context, uri);
                File image = PhotoManager.compressImage(filePath);
                if (image == null) {
                    return Observable.empty();
                }
                return Observable.just(image);
            })
            .filter(file -> !(file == null || !file.exists() || file.length() == 0))
            .map(Uri::fromFile)
            .toList()
            .subscribe(uriList -> {
                Uri[] uris1 = uriList.toArray(new Uri[0]);
                mFilePathCallback.onReceiveValue(uris1);
                mFilePathCallback = null;
            });
    }

    /**
     * Process image results in common way.
     *
     * @param context ctx
     * @param uris uris contains image results.
     */
    private void handleImageResults(Context context, Uri[] uris) {
        List<Uri> uriList = new ArrayList<>();

        if (uris == null || uris.length == 0) {
            mFilePathCallback.onReceiveValue(null);
            mFilePathCallback = null;
            return;
        }

        for (Uri uri : uris) {
            String filePath = FileUtil.getPath(context, uri);

            if (TextUtils.isEmpty(filePath)) {
                continue;
            }

            File image = PhotoManager.compressImage(filePath);
            if (image == null || !image.exists() || image.length() == 0) {
                continue;
            }
            uriList.add(Uri.fromFile(image));
        }
        Uri[] uris1 = uriList.toArray(new Uri[0]);
        mFilePathCallback.onReceiveValue(uris1);
        mFilePathCallback = null;
    }
}
