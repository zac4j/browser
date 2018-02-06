package com.zac4j.browser.picker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import com.zac4j.browser.PermissionsDelegate;
import com.zac4j.browser.R;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    mWebView = rootView.findViewById(R.id.image_picker_container);

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
            photoFile = createImageFile();
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

  /**
   * More info this method can be found at
   * http://developer.android.com/training/camera/photobasics.html
   *
   * @throws IOException
   */
  private File createImageFile() throws IOException {
    // Create an image file name
    String timeStamp =
        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    return File.createTempFile(imageFileName,  /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */);
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
          results = new Uri[] { Uri.parse(mCameraPhotoPath) };
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
