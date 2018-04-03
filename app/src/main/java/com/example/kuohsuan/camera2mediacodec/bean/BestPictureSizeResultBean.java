package com.example.kuohsuan.camera2mediacodec.bean;

import android.hardware.Camera;
import com.google.android.gms.common.images.Size;


/**
 * Created by kuohsuan on 2018/3/27.
 */

public class BestPictureSizeResultBean {
    Camera.Size sizeCamera1;
    Size sizeCamera2;

    public Camera.Size getSizeCamera1() {
        return sizeCamera1;
    }

    public void setSizeCamera1(Camera.Size sizeCamera1) {
        this.sizeCamera1 = sizeCamera1;
    }

    public Size getSizeCamera2() {
        return sizeCamera2;
    }

    public void setSizeCamera2(Size sizeCamera2) {
        this.sizeCamera2 = sizeCamera2;
    }
}
