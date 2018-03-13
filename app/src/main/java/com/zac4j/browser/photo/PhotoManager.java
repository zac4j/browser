package com.zac4j.browser.photo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import com.zac4j.browser.Logger;
import com.zac4j.browser.util.ImageUtil;
import java.io.File;
import java.io.IOException;

/**
 * Created by Zac on 2018/3/8.
 */

public class PhotoManager {

  private static final String TAG = "PhotoManager";
  private static String mCameraPhotoPath;

  public static Intent createTakePhotoIntent(Context context) {
    Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePhotoIntent.resolveActivity(context.getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = null;
      try {
        photoFile = ImageUtil.createImageFile();
        takePhotoIntent.putExtra("PhotoPath", mCameraPhotoPath);
      } catch (IOException ex) {
        // Error occurred while creating the File
        Logger.e(TAG, "Unable to create Image File", ex);
      }

      // Continue only if the File was successfully created
      if (photoFile != null) {
        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
      } else {
        takePhotoIntent = null;
      }
    }
    return takePhotoIntent;
  }

  public static Intent createSelectImageIntent() {
    Intent selectImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
    selectImageIntent.addCategory(Intent.CATEGORY_OPENABLE);
    selectImageIntent.setType("image/*");
    return selectImageIntent;
  }
}
