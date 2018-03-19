package com.zac4j.browser;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.zac4j.browser.photo.picker.PickerActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void pickImage(View view) {
        if (view != null) {
            startActivity(new Intent(MainActivity.this, PickerActivity.class));
        }
    }
}
