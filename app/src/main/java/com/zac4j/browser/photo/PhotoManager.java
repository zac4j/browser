package com.zac4j.browser.photo;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Base64;
import com.zac4j.browser.Logger;
import com.zac4j.browser.util.FileUtil;
import com.zac4j.browser.util.ImageUtil;
import java.io.File;
import java.io.IOException;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

/**
 * Helper class for manage photo pick and other process.
 * Created by Zac on 2018/3/8.
 */

public class PhotoManager {

    private static final String TAG = "PhotoManager";
    public static final int REQUEST_CODE_IMAGE_CHOOSER = 0x01;

    private static final String FILE_AUTHORITY = "com.zac4j.browser.fileprovider";

    // photo file path
    private static String sCurrentPhotoPath;

    /**
     * Create a camera intent for capture image.
     *
     * @param context context
     * @return a camera intent.
     */
    private static Intent createTakePhotoIntent(Context context) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = ImageUtil.createImageFile(context);
            } catch (Exception ex) {
                // Error occurred while creating the File
                Logger.e(TAG, "Unable to create Image File", ex);
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(context, FILE_AUTHORITY, photoFile);
                sCurrentPhotoPath = photoFile.getAbsolutePath();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            } else {
                intent = null;
            }
        }
        return intent;
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
     * Get path of current taken photo.
     *
     * @return The path of current taken photo.
     */
    public static String getCurrentPhotoPath() {
        return sCurrentPhotoPath;
    }

    /**
     * Decode encoded image and save into local directory.
     *
     * @param encodeImage the image file which is encoded by Base64.
     * @return true if save the file, otherwise false.
     */
    public static boolean decodeImageToFile(String encodeImage) {
        byte[] imageBytes = Base64.decode(encodeImage, Base64.DEFAULT);
        File file = new File(sCurrentPhotoPath);
        BufferedSink bufferedSink = null;
        try {
            Sink sink = Okio.sink(file);
            bufferedSink = Okio.buffer(sink);
            bufferedSink.write(imageBytes);
        } catch (IOException e) {
            Logger.d(TAG, e.getMessage());
        } finally {
            FileUtil.closeQuietly(bufferedSink);
        }
        return file.exists();
    }

    /**
     * Log current info.
     *
     * @param file given file to print info.
     */
    private static void logFileInfo(File file) {
        Logger.d(TAG, "File path: " + file.getAbsolutePath() + ",size: " + FileUtil.readFileSize(
            file.length()));
    }

    /**
     * Here met a problem that is get path from {@link FileProvider} created uri.
     *
     * {@link FileProvider} is only useful for delivering content to other apps. If you are not doing that,
     * get rid of FileProvider. If your concern is the FileUriExposedException, do not put a Uri in
     * the Intent, but instead put a String extra that contains the file path
     * (e.g., path.getAbsolutePath()), or pass the File object itself as a Serializable extra.
     * One or both of those should avoid the FileUriExposedException
     */
    public static void clearEmptyFile() {
        if (!TextUtils.isEmpty(sCurrentPhotoPath)) {
            File photo = new File(sCurrentPhotoPath);
            System.out.println(
                "delete photo file exist: " + photo.exists() + ", is delete: " + photo.delete());
        }
    }
}
