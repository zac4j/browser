package com.zac4j.browser;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class PermissionsDelegate {

  private static final int REQUEST_CODE = 10;
  private final Activity activity;

  public PermissionsDelegate(Activity activity) {
    this.activity = activity;
  }

  public boolean hasCameraPermission() {
    int permissionCheckResult =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
    return permissionCheckResult == PackageManager.PERMISSION_GRANTED;
  }

  public boolean hasStoragePermission() {
    int permissionCheckResult =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    return permissionCheckResult == PackageManager.PERMISSION_GRANTED;
  }

  public void requestCameraPermission() {
    ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.CAMERA },
        REQUEST_CODE);
  }

  public void requestStoragePermission() {
    ActivityCompat.requestPermissions(activity,
        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_CODE);
  }

  public boolean storagePermissionGranted(int requestCode, String[] permissions,
      int[] grantResults) {
    return requestCode == REQUEST_CODE
        && grantResults.length >= 1
        && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        && grantResults[0] == PackageManager.PERMISSION_GRANTED;
  }
}
