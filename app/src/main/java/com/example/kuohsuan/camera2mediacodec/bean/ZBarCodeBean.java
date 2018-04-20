package com.example.kuohsuan.camera2mediacodec.bean;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Created by kuohsuan on 2018/4/20.
 */

public class ZBarCodeBean {
    private String zbarcodeString;
    private Bitmap ztextBitmap;
    private Rect bound;
    private int bound1;//y
    private int bound2;//x
    private int bound3;//qrcode width
    private int bound4;//qrcode high
    private int ImageReaderCreateHeight;
    private int ImageReaderCreateWeight;


    public ZBarCodeBean(String _barcodeString , Bitmap _textBitmap ){
        zbarcodeString = _barcodeString;
        ztextBitmap = _textBitmap;
    }

    public String getZbarcodeString() {
        return zbarcodeString;
    }

    public void setZbarcodeString(String zbarcodeString) {
        this.zbarcodeString = zbarcodeString;
    }

    public Bitmap getTextBitmap() {
        return ztextBitmap;
    }

    public void setTextBitmap(Bitmap textBitmap) {
        this.ztextBitmap = textBitmap;
    }

    public Rect getBound() {
        return bound;
    }

    public void setBound(Rect bound) {
        this.bound = bound;
    }

    public int getBound1() {
        return bound1;
    }

    public void setBound1(int bound1) {
        this.bound1 = bound1;
    }

    public int getBound2() {
        return bound2;
    }

    public void setBound2(int bound2) {
        this.bound2 = bound2;
    }

    public int getBound3() {
        return bound3;
    }

    public void setBound3(int bound3) {
        this.bound3 = bound3;
    }

    public int getBound4() {
        return bound4;
    }

    public void setBound4(int bound4) {
        this.bound4 = bound4;
    }

    public int getImageReaderCreateHeight() {
        return ImageReaderCreateHeight;
    }

    public void setImageReaderCreateHeight(int imageReaderCreateHeight) {
        ImageReaderCreateHeight = imageReaderCreateHeight;
    }

    public int getImageReaderCreateWeight() {
        return ImageReaderCreateWeight;
    }

    public void setImageReaderCreateWeight(int imageReaderCreateWeight) {
        ImageReaderCreateWeight = imageReaderCreateWeight;
    }
}