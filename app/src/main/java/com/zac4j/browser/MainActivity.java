package com.zac4j.browser;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.zac4j.browser.util.system.PermsSettingJumper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void pickImage(View view) {
        if (view != null) {
            //startActivity(new Intent(MainActivity.this, PickerActivity.class));
            PermsSettingJumper.goToPermsSettingPage(MainActivity.this);
        }
    }
}
