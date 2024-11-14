package com.example.kuohsuan.camera2mediacodec.Activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


import com.example.kuohsuan.camera2mediacodec.R;

public class MainActivity extends AppCompatActivity {
    private AlertDialog intentSettingAlertDialog;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addPermission();
    }

    private void addPermission() {
        if (hasPermissions()) {
            setPermissionHasBeenAskedOrNot(true);
            proceedToNextActivity();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            boolean showRationale = false;

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        showRationale = true;
                    }
                }
            }

            if (allGranted) {
                setPermissionHasBeenAskedOrNot(true);
                proceedToNextActivity();
            } else if (showRationale) {
                // 權限被拒絕且不再詢問
                setPermissionHasBeenAskedOrNot(false);
                showSettingDialog(this);
            } else {
                // 權限被拒絕但可以再次請求
                setPermissionHasBeenAskedOrNot(false);
                Log.d(TAG, "Permission denied without 'Don't ask again'");
            }
        }
    }

    private void proceedToNextActivity() {
        startActivity(new Intent(this, SurfaceTextureCamera2Activity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPermissionHasBeenAsked()) {
            proceedToNextActivity();
        } else {
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

    private static void setPermissionHasBeenAskedOrNot(@NonNull boolean _isAsk) {
        isAsked = _isAsk;
    }

    private static boolean isPermissionHasBeenAsked() {
        return isAsked;
    }
}
