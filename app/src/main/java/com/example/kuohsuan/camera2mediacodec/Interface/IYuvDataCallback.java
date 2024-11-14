package com.example.kuohsuan.camera2mediacodec.Interface;

/**
 * Created by kuohsuan on 2018/3/27.
 */

public interface IYuvDataCallback {
    void getYuv(byte[] data, int imageWidth, int imageHeight);
}
