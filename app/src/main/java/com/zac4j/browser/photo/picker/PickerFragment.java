package com.zac4j.browser.photo.picker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
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
import com.zac4j.browser.photo.PhotoManager;
import com.zac4j.browser.util.FileUtil;
import com.zac4j.browser.util.ImageUtil;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Image pick page.
 * Created by Zaccc on 2018/2/6.
 */

public class PickerFragment extends Fragment {

  private PermissionsDelegate mPermissionsDelegate;
  private boolean mHasStoragePermission;

  private static final String TAG = PickerFragment.class.getSimpleName();

  public static final int INPUT_FILE_REQUEST_CODE = 1;

  private WebView mWebView;
  private ValueCallback<Uri[]> mFilePathCallback;
  private String mCameraPhotoPath;
  private File mCompressedFile;
  private boolean mIsCreateTempPhoto;

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

        createImageChooser();

        return true;
      }
    });

    // Load the local index.html file
    if (mWebView.getUrl() == null) {
      mWebView.loadUrl("file:///android_asset/image_picker/index.html");
    }

    return rootView;
  }

  private void createImageChooser() {
    Intent takePhotoIntent = PhotoManager.createTakePhotoIntent(getActivity());

    Intent[] intentArray;
    if (takePhotoIntent != null) {
      intentArray = new Intent[] { takePhotoIntent };
    } else {
      intentArray = new Intent[0];
    }

    Intent selectImageIntent = PhotoManager.createSelectImageIntent();

    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
    chooserIntent.putExtra(Intent.EXTRA_INTENT, selectImageIntent);
    chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

    startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    destroyWebView();
    //deleteTempFile();
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
    // create compressed file reference to allow delete it conveniently.
    mCompressedFile = null;

    // Check that the response is a good one
    if (resultCode == Activity.RESULT_OK) {
      if (data == null) {
        // If there is not data, then we may have taken a photo
        Uri photoUri = Uri.parse(mCameraPhotoPath);
        results = new Uri[]{photoUri};
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
          results = new Uri[]{uri};
        }

        clearEmptyFile();
        if (mIsCreateTempPhoto) {
          System.out.println(TAG + " haven't delete created temp photo file");
        } else {
          System.out.println(TAG + " have delete created temp photo file");
        }
      }

      processImageResults(getActivity(), results);
    } else {
      clearEmptyFile();
    }

    //if (mCompressedFile != null) {
    //  results = new Uri[] { Uri.fromFile(mCompressedFile) };
    //}
    //// results equals to null is legal..
    //mFilePathCallback.onReceiveValue(results);
    //mFilePathCallback = null;
    //return;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (mPermissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {
      // todo permission success logic
    }
  }

  private void processImageResults(final Context context, Uri[] uris) {
    Observable.fromArray(uris)
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .filter(new Predicate<Uri>() {
          @Override
          public boolean test(Uri uri) throws Exception {
            return uri != null && !TextUtils.isEmpty(FileUtil.getPath(context, uri));
          }
        })
        .flatMap(new Function<Uri, ObservableSource<File>>() {
          @Override
          public ObservableSource<File> apply(Uri uri) throws Exception {
            String filePath = FileUtil.getPath(context, uri);
            File image = compressImage(filePath);
            return Observable.just(image);
          }
        })
        .filter(new Predicate<File>() {
          @Override
          public boolean test(File file) throws Exception {
            return !(file == null || !file.exists() || file.length() == 0);
          }
        })
        .toList()
        .subscribe(new Consumer<List<File>>() {
          @Override
          public void accept(List<File> files) throws Exception {
            int i = 0;
            for (File file : files) {
              if (i == 0) {
                // results equals to null is legal..
                mFilePathCallback.onReceiveValue(new Uri[] { Uri.fromFile(file) });
                mFilePathCallback = null;
              }
              System.out.println("file path -> " + file.getAbsolutePath());
              i++;
            }
          }
        });
  }

  /**
   * Get compressed file from given data path.
   *
   * @param dataPath data path from capture image callback.
   * @return compressed image file.
   */
  private File getCompressedImage(String dataPath) {
    if (TextUtils.isEmpty(dataPath)) {
      Logger.e(TAG, "There is no image data get back");
      return null;
    }

    String filePath = FileUtil.getPath(getActivity(), Uri.parse(dataPath));

    if (TextUtils.isEmpty(filePath)) {
      return null;
    }

    return compressImage(filePath);
  }

  /**
   * Compress image file by given image file path.
   *
   * @param imageFilePath image resource file path.
   * @return compressed image file.
   */
  private File compressImage(String imageFilePath) {
    File file = null;
    try {
      File imageFile = new File(imageFilePath);
      if (imageFile.length() == 0) {
        return null;
      }
      logFileInfo(imageFile);

      String filename = imageFile.getName();
      String destinationPath =
          Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
              .getAbsolutePath()
              + File.separator
              + getString(R.string.app_name)
              + File.separator
              + filename;

      // 已压缩过的图片，不再压缩
      if (TextUtils.equals(imageFilePath, destinationPath)) {
        return imageFile;
      }

      file = ImageUtil.compressImage(imageFilePath, 720, 1280, Bitmap.CompressFormat.JPEG, 80,
          destinationPath);
    } catch (IOException e) {
      Logger.e(TAG, e.getMessage());
    }

    return file;
  }

  /**
   * Log current infos.
   *
   * @param file given file to print infos.
   */
  private void logFileInfo(File file) {
    Logger.d(TAG,
        "File path: " + file.getAbsolutePath() + " compressed file size: " + FileUtil.readFileSize(
            file.length()));
  }

  private void clearEmptyFile() {
    if (!TextUtils.isEmpty(mCameraPhotoPath)) {
      String filePath = FileUtil.getPath(getActivity(), Uri.parse(mCameraPhotoPath));
      if (!TextUtils.isEmpty(filePath)) {
        File photo = new File(filePath);
        System.out.println(
            "delete photo file exist: " + photo.exists() + ", is delete: " + photo.delete());
        mIsCreateTempPhoto = photo.exists();
      }
    }
  }

  /**
   * delete temp created file.
   */
  private void deleteTempFile() {
    if (mCompressedFile != null && mCompressedFile.delete()) {
      mCompressedFile = null;
    }
  }
}
