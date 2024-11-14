package com.example.kuohsuan.camera2mediacodec.bean;

import android.content.Context;
import android.hardware.Camera;
import android.util.Size;

/**
 * Created by kuohsuan on 2018/3/27.
 */

public class PictureInfoBean {
    private Camera2Data camera2Data;
    private Camera1Data camera1Data;

    public Camera2Data getCamera2Data() {
        return camera2Data;
    }

    public void setCamera2Data(Camera2Data camera2Data) {
        this.camera2Data = camera2Data;
    }

    public Camera1Data getCamera1Data() {
        return camera1Data;
    }

    public void setCamera1Data(Camera1Data camera1Data) {
        this.camera1Data = camera1Data;
    }



    public static class Camera2Data{
        android.util.Size[] size;

        public Size[] getSize() {
            return size;
        }

        public void setSize(Size[] size) {
            this.size = size;
        }
    }


    public static class Camera1Data{
        Camera camera1;
        Context context;

        public Camera getCamera1() {
            return camera1;
        }

        public void setCamera1(Camera camera1) {
            this.camera1 = camera1;
        }
        public Context getContext() {
            return context;
        }

        public void setContext(Context context) {
            this.context = context;
        }

    }
}
