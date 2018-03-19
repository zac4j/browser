package com.zac4j.browser.photo;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import com.zac4j.browser.Logger;
import com.zac4j.browser.util.FileUtil;
import com.zac4j.browser.util.ImageUtil;
import java.io.File;
import java.io.IOException;

/**
 * Helper class for manage photo pick and other process.
 * Created by Zac on 2018/3/8.
 */

public class PhotoManager {

    private static final String TAG = "PhotoManager";
    public static final int REQUEST_CODE_IMAGE_CHOOSER = 0x01;

    public static String sCameraPhotoPath;

    /**
     * Create a camera intent for capture image.
     *
     * @param context context
     * @return a camera intent.
     */
    private static Intent createTakePhotoIntent(Context context) {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePhotoIntent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = ImageUtil.createImageFile();
                takePhotoIntent.putExtra("PhotoPath", sCameraPhotoPath);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Logger.e(TAG, "Unable to create Image File", ex);
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                sCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } else {
                takePhotoIntent = null;
            }
        }
        return takePhotoIntent;
    }

    /**
     * Create an album intent for pick image.
     *
     * @return an album intent.
     */
    private static Intent createSelectImageIntent() {
        Intent selectImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        selectImageIntent.addCategory(Intent.CATEGORY_OPENABLE);
        selectImageIntent.setType("image/*");
        return selectImageIntent;
    }

    /**
     * Create image chooser.
     *
     * @param fragment fragment which invoke image chooser.
     */
    public static void createImageChooser(Fragment fragment) {
        Intent takePhotoIntent = createTakePhotoIntent(fragment.getActivity());

        Intent[] intentArray;
        if (takePhotoIntent != null) {
            intentArray = new Intent[] { takePhotoIntent };
        } else {
            intentArray = new Intent[0];
        }

        Intent selectImageIntent = createSelectImageIntent();

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, selectImageIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        fragment.startActivityForResult(chooserIntent, REQUEST_CODE_IMAGE_CHOOSER);
    }

    /**
     * Compress image file by given image file path.
     *
     * @param imageFilePath image resource file path.
     * @return compressed image file.
     */
    public static File compressImage(String imageFilePath) {
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
                    .getAbsolutePath() + File.separator + "browser" + File.separator + filename;

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
    private static void logFileInfo(File file) {
        Logger.d(TAG, "File path: "
            + file.getAbsolutePath()
            + " compressed file size: "
            + FileUtil.readFileSize(file.length()));
    }

    public static void clearEmptyFile(Context context) {
        if (!TextUtils.isEmpty(sCameraPhotoPath)) {
            String filePath = FileUtil.getPath(context, Uri.parse(sCameraPhotoPath));
            if (!TextUtils.isEmpty(filePath)) {
                File photo = new File(filePath);
                System.out.println("delete photo file exist: "
                    + photo.exists()
                    + ", is delete: "
                    + photo.delete());
            }
        }
    }
}
