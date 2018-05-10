package com.example.kuohsuan.camera2mediacodec.Activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.kuohsuan.camera2mediacodec.R;
import com.tbruyelle.rxpermissions.RxPermissions;

import rx.functions.Action1;

import static android.R.attr.permission;

public class MainActivity extends AppCompatActivity {
    private RxPermissions rxPermissions;
    private String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addPermission();
    }

    private void addPermission(){

        rxPermissions = RxPermissions.getInstance(this);
        rxPermissions.setLogging(true);
        rxPermissions.getInstance(this);
        rxPermissions.request(
                Manifest.permission.CAMERA
                ,Manifest.permission.WRITE_EXTERNAL_STORAGE
                ,Manifest.permission.READ_EXTERNAL_STORAGE
                ,Manifest.permission.RECORD_AUDIO)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted){
                            //release camera
                            Toast.makeText(MainActivity.this,"Permission.granted !! ",Toast.LENGTH_SHORT).show();
                            // Start home activity
                            startActivity(new Intent(MainActivity.this, SurfaceTextureCamera2Activity.class));
                            // close splash activity
                            finish();
                        } else {
                            Log.i(TAG, "AAA_Permission denied, can`t enable the camera " + permission);
                            Toast.makeText(MainActivity.this,"Permission Denied !! ",Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });



    }
}
