package com.zac4j.browser.photo.picker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.zac4j.browser.AppExecutors;
import com.zac4j.browser.PermissionsDelegate;
import com.zac4j.browser.R;
import com.zac4j.browser.photo.PhotoManager;
import com.zac4j.browser.util.FileUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Image pick page.
 * Created by Zaccc on 2018/2/6.
 */

public class PickerFragment extends Fragment {

    private PermissionsDelegate mPermissionsDelegate;
    private boolean mHasStoragePermission;

    private WebView mWebView;
    private ValueCallback<Uri[]> mFilePathCallback;

    public PickerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
        Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_image_picker, container, false);

        mWebView = rootView.findViewById(R.id.image_picker_webview);

        mPermissionsDelegate = new PermissionsDelegate(getActivity());

        mHasStoragePermission = mPermissionsDelegate.hasStoragePermission();
        if (!mHasStoragePermission) {
            mPermissionsDelegate.requestStoragePermission();
        }

        setUpWebViewDefaults(mWebView);

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore the previous URL and history stack
            mWebView.restoreState(savedInstanceState);
        }

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

        // Load the local index.html file
        if (mWebView.getUrl() == null) {
            mWebView.loadUrl("file:///android_asset/image_picker/index.html");
        }

        return rootView;
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

        // We set the WebViewClient to ensure links are consumed by the WebView rather
        // than passed to a browser if it can
        mWebView.setWebViewClient(new WebViewClient());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PhotoManager.REQUEST_CODE_IMAGE_CHOOSER || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                Uri photoUri = Uri.parse(PhotoManager.sCameraPhotoPath);
                results = new Uri[] { photoUri };
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

                PhotoManager.clearEmptyFile(getActivity());
            }

            //processImageResults(getActivity(), results);

            Uri[] finalResults = results;
            AppExecutors.getInstance()
                .diskIO()
                .execute(() -> handleImageResults(getActivity(), finalResults));
        } else {
            PhotoManager.clearEmptyFile(getActivity());
        }
    }

    /**
     * Process image results in reactive way..
     *
     * @param context ctx
     * @param uris uris contains image results.
     */
    private void processImageResults(final Context context, Uri[] uris) {
        Observable.fromArray(uris)
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
                Uri[] uris1 = uriList.toArray(new Uri[uriList.size()]);
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
        Uri[] uris1 = uriList.toArray(new Uri[uriList.size()]);
        mFilePathCallback.onReceiveValue(uris1);
        mFilePathCallback = null;
    }
}
