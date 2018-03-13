package com.zac4j.browser.photo.picker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import com.zac4j.browser.R;

/**
 * A WebView for pick image
 * Created by Zaccc on 2018/2/6.
 */

public class PickerActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_image_picker);
    if (savedInstanceState == null) {
      getFragmentManager().beginTransaction()
          .add(R.id.image_picker_container, new PickerFragment())
          .commit();
    }
  }
}
