package com.example.kuohsuan.camera2mediacodec.util;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.support.annotation.NonNull;

public class Camera2Util
{
    private final String TAG = this.getClass().getSimpleName();
    private static CameraManager cameraManager=null;
    private static CameraCharacteristics cameraCharacteristics =null;
    private static String cameraId =null;


    public static CameraManager getCameraManager() {
        return cameraManager;
    }

    public static void setCameraManager(@NonNull CameraManager _cameraManager) {
        cameraManager = _cameraManager;
    }

    public static CameraCharacteristics getCameraCharacteristics() {
        return cameraCharacteristics;
    }

    public static void setCameraCharacteristics(@NonNull CameraCharacteristics _cameraCharacteristics) {
        cameraCharacteristics = _cameraCharacteristics;
    }

    public static String getCameraId() {
        return cameraId;
    }

    public static void setCameraId(@NonNull String _cameraId) {
        cameraId = _cameraId;
    }

}
