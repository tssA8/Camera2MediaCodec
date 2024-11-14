package com.example.kuohsuan.camera2mediacodec.bean;

import java.nio.ByteBuffer;

/**
 * Created by kuohsuan on 2018/3/28.
 */

public class QuarterNV21Bean {
    byte[] camera2Source;
    ByteBuffer camera1Source;
    int imageWidth;
    int imageHeight;


    public byte[] getCamera2Source() {
        return camera2Source;
    }

    public void setCamera2Source(byte[] camera2Source) {
        this.camera2Source = camera2Source;
    }

    public ByteBuffer getCamera1Source() {
        return camera1Source;
    }

    public void setCamera1Source(ByteBuffer camera1Source) {
        this.camera1Source = camera1Source;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }
}
