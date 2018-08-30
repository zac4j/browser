package com.zac4j.browser.util.photo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Base64;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.zac4j.browser.GlideApp;
import com.zac4j.browser.Logger;
import com.zac4j.browser.util.FileUtil;
import com.zac4j.browser.util.ImageUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;
import okio.Source;

/**
 * Helper class for manage photo pick and other process.
 * Created by Zac on 2018/3/8.
 */

public class PhotoManager {

    private static final String TAG = "PhotoManager";
    public static final int REQUEST_CODE_IMAGE_CHOOSER = 0x01;

    private static final String FILE_PROVIDER_AUTHORITY = "com.zac4j.browser.fileprovider";

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
                Uri uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, photoFile);
                sCurrentPhotoPath = photoFile.getAbsolutePath();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
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
            String destination =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .getAbsolutePath() + File.separator + "browser" + File.separator + filename;

            // 已压缩过的图片，不再压缩
            if (TextUtils.equals(imageFilePath, destination)) {
                return imageFile;
            }

            file = ImageUtil.compressImage(imageFilePath, 720, 1280, Bitmap.CompressFormat.JPEG, 80,
                destination);
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
     * @param encodedImage the image file which is encoded by Base64.
     * @return true if save the file, otherwise false.
     */
    public static boolean saveEncodedImage(String encodedImage) {
        String encodePrefix = "data:image/jpeg;base64,";
        int beginIndex = encodedImage.indexOf(encodePrefix) + encodePrefix.length();
        String imageCode = encodedImage.substring(beginIndex);
        byte[] imageBytes = Base64.decode(imageCode, Base64.DEFAULT);

        File file = new File(getPictureStorageDir(getCurrentPhotoName()));
        return saveImageSource(imageBytes, file);
    }

    /**
     * Save network image resource.
     *
     * @param context UI context.
     * @param imageUrl link url of network image.
     */
    public static void saveNetworkImage(Context context, String imageUrl) {
        GlideApp.with(context).asFile().load(imageUrl).into(new SimpleTarget<File>() {
            @Override
            public void onResourceReady(@NonNull File resource,
                @Nullable Transition<? super File> transition) {
                Logger.d(TAG, "image file source ready!");
                File destFile = new File(getPictureStorageDir(getCurrentPhotoName()));
                try {
                    saveImageSource(Okio.source(resource), destFile);
                } catch (FileNotFoundException e) {
                    Logger.e(TAG, "save file met an error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Save image source into file.
     *
     * @param source image resource.
     * @param dest destination file directory.
     * @return true if file saved successfully,otherwise false.
     */
    private static boolean saveImageSource(Source source, File dest) {
        File parentFile = dest.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        BufferedSink bufferedSink = null;
        try {
            Sink sink = Okio.sink(dest);
            bufferedSink = Okio.buffer(sink);
            bufferedSink.writeAll(source);
        } catch (IOException e) {
            Logger.e(TAG, e.getMessage());
        } finally {
            FileUtil.closeQuietly(bufferedSink);
        }
        return dest.exists();
    }

    /**
     * Save image source into file.
     *
     * @param source image resource.
     * @param dest destination file directory.
     * @return true if file saved successfully,otherwise false.
     */
    private static boolean saveImageSource(byte[] source, File dest) {
        File parentFile = dest.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        BufferedSink bufferedSink = null;
        try {
            Sink sink = Okio.sink(dest);
            bufferedSink = Okio.buffer(sink);
            bufferedSink.write(source);
        } catch (IOException e) {
            Logger.e(TAG, e.getMessage());
        } finally {
            FileUtil.closeQuietly(bufferedSink);
        }
        return dest.exists();
    }

    private static String getPictureStorageDir(String filename) {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .getAbsolutePath() + File.separator + "zaccc" + File.separator + filename;
    }

    private static String getCurrentPhotoName() {
        return new File(sCurrentPhotoPath).getName();
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
     * One or both of those should avoid the FileUriExposedException.
     */
    public static void clearEmptyFile() {
        if (!TextUtils.isEmpty(sCurrentPhotoPath)) {
            File photo = new File(sCurrentPhotoPath);
            System.out.println(
                "delete photo file exist: " + photo.exists() + ", is delete: " + photo.delete());
        }
    }
}
