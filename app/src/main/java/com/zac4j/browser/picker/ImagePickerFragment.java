package com.zac4j.browser.picker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.zac4j.browser.Logger;
import com.zac4j.browser.PermissionsDelegate;
import com.zac4j.browser.R;
import com.zac4j.browser.util.BitmapUtil;
import com.zac4j.browser.util.FileUtil;
import java.io.File;
import java.io.IOException;

/**
 * Image pick page.
 * Created by Zaccc on 2018/2/6.
 */

public class ImagePickerFragment extends Fragment {

  private PermissionsDelegate mPermissionsDelegate;
  private boolean mHasStoragePermission;

  private static final String TAG = ImagePickerFragment.class.getSimpleName();

  public static final int INPUT_FILE_REQUEST_CODE = 1;
  public static final String EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION";

  private WebView mWebView;
  private ValueCallback<Uri[]> mFilePathCallback;
  private String mCameraPhotoPath;

  public ImagePickerFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
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

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
          // Create the File where the photo should go
          File photoFile = null;
          try {
            photoFile = BitmapUtil.createImageFile();
            takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
          } catch (IOException ex) {
            // Error occurred while creating the File
            Log.e(TAG, "Unable to create Image File", ex);
          }

          // Continue only if the File was successfully created
          if (photoFile != null) {
            mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
          } else {
            takePictureIntent = null;
          }
        }

        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        Intent[] intentArray;
        if (takePictureIntent != null) {
          intentArray = new Intent[] { takePictureIntent };
        } else {
          intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

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
    if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
      super.onActivityResult(requestCode, resultCode, data);
      return;
    }

    Uri[] results = null;

    // Check that the response is a good one
    if (resultCode == Activity.RESULT_OK) {
      if (data == null) {
        // If there is not data, then we may have taken a photo
        if (mCameraPhotoPath != null) {
          Uri imgUri = Uri.parse(mCameraPhotoPath);
          Logger.d(TAG, "File path: " + mCameraPhotoPath);
          File compressedFile = BitmapUtil.compressImageWithDefaults(getActivity(), imgUri);
          Logger.d(TAG, "File after compress size: " + FileUtil.getFileSize(compressedFile) + "KB");
          results = new Uri[] { Uri.fromFile(compressedFile) };
        }
      } else {
        String dataString = data.getDataString();
        if (dataString != null) {
          results = new Uri[] { Uri.parse(dataString) };
        }
      }
    }

    mFilePathCallback.onReceiveValue(results);
    mFilePathCallback = null;
    return;
  }
}
