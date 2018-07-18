package com.example.kuohsuan.camera2mediacodec.Activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.kuohsuan.camera2mediacodec.R;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    private RxPermissions rxPermissions;
    private AlertDialog intentSettingAlertDialog;
    private String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addPermission();
    }

    private void addPermission(){

        rxPermissions = new RxPermissions(this);
        rxPermissions.requestEachCombined(
                Manifest.permission.CAMERA
                ,Manifest.permission.WRITE_EXTERNAL_STORAGE
                ,Manifest.permission.READ_EXTERNAL_STORAGE
                ,Manifest.permission.RECORD_AUDIO)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) {

                        if(permission.granted){
                            setPermissionHasBeenAskedOrNot(true);
                        }else if(permission.shouldShowRequestPermissionRationale){
                            // Denied permission without ask never again
                            setPermissionHasBeenAskedOrNot(false);
                            Log.d(TAG, "aaa_shouldShowRequestPermissionRationale result " + permission);
                        }else{
                            // Denied permission with ask never again
                            setPermissionHasBeenAskedOrNot(false);
                            Log.d(TAG, "aaa_Denied result " + permission);
                        }

                    }
                });




    }

    @Override
    protected void onResume() {
        super.onResume();
//        Log.d(TAG,"onResume");

        if(isPermissionHasBeenAsked()==true){
            Log.d(TAG,"AAA_onResume isPermissionHasBeenAsked ");
            startActivity(new Intent(this, SurfaceTextureCamera2Activity.class));
            finish();
        }else{
            // Need to go to the settings
            showSettingDialog(this);
        }
    }

    public void showSettingDialog(Context context) {

        String message = context.getString(R.string.message_permission_always_failed);
        intentSettingAlertDialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton(R.string.action_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }).show();

    }


    private static boolean isAsked = false;
    private static void setPermissionHasBeenAskedOrNot(@NonNull boolean _isAsk){
        isAsked = _isAsk;
    }
    private static boolean isPermissionHasBeenAsked(){
        return isAsked;
    }


}
