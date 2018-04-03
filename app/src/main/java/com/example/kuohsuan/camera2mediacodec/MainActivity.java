package com.example.kuohsuan.camera2mediacodec;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i =new Intent();
        i.setClass(this,SurfaceTextureCamera2Activity.class);
        startActivity(i);
    }
}
