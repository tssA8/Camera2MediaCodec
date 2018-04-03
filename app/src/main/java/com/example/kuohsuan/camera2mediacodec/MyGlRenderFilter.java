package com.example.kuohsuan.camera2mediacodec;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.params.Face;
import android.util.Size;

import com.chillingvan.canvasgl.textureFilter.BasicTextureFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by f9021 on 2018/1/5.
 */

public class MyGlRenderFilter {
    private static TextureFilter textureFilter = new BasicTextureFilter();
    private static Bitmap logoBitmap;
    private static  RectF[] faceRectFList;
    private static  RectF[] decodeFaceRectFList;
    private static  Face[] faceList;
    private static  int deviceRotation;

    private static Map<Integer,com.google.android.gms.vision.face.Face> faceMap = new HashMap<>();
    private static Map<Integer,BarcodeBean> barcodeMap = new ConcurrentHashMap<>();
    private static Map<Integer,ZBarcodeBean> ZbarcodeMap = new ConcurrentHashMap<>();
    private static ArrayList<Integer> barIdList = new ArrayList<>();

    private static Size previewSize;

    public static void setTextureFilter(TextureFilter filter) {
        textureFilter = filter;
    }

    public static TextureFilter getTextureFilter() {
        return textureFilter;
    }

    public static Bitmap getLogoBitmap() {
        return logoBitmap;
    }

    public static void setLogoBitmap(Bitmap logoBitmap) {
        MyGlRenderFilter.logoBitmap = logoBitmap;
    }
    public static RectF[] getFaceResult() {
        return faceRectFList;
    }

    public static Face[] getFaceList() {
        return faceList;
    }


    public static RectF[] getDecodeFaceList() {
        return decodeFaceRectFList;
    }


    public static void setFaceDetectResult(Face[] _faceList , RectF[] _faceRectFList , RectF[] _decodeMatrix) {

        MyGlRenderFilter.faceList = _faceList;
        MyGlRenderFilter.faceRectFList = _faceRectFList;
        MyGlRenderFilter.decodeFaceRectFList = _decodeMatrix;
    }


    public static Map<Integer, com.google.android.gms.vision.face.Face> getFaceMap() {
        return faceMap;
    }
    public static Map<Integer, BarcodeBean> getBarcodeBeanMap() {
        return barcodeMap;
    }

    public static Map<Integer, ZBarcodeBean> getZBarcodeBeanMap() {
        return ZbarcodeMap;
    }


    public static void setBarIdList(Integer id){
        barIdList.add(id);
    }

    public static ArrayList<Integer> getBarIdList(){
        if(barIdList==null)
            barIdList = new ArrayList<>();
        return barIdList;
    }


    public static Size getPreviewSize() {
        return previewSize;
    }

    public static void setPreviewSize(Size _previewSize) {
        previewSize = _previewSize;
    }


    public static int getDeviceRotation() {
        return deviceRotation;
    }

    public static void setDeviceRotation(int deviceRotation) {
        MyGlRenderFilter.deviceRotation = deviceRotation;
    }

    static public class BarcodeBean{

        private Barcode barcode;
        private Bitmap textBitmap;

        public BarcodeBean(Barcode _barcode ,Bitmap _textBitmap ){
            barcode = _barcode;
            textBitmap = _textBitmap;
        }

        public Barcode getBarcode() {
            return barcode;
        }

        public void setBarcode(Barcode barcode) {
            this.barcode = barcode;
        }

        public Bitmap getTextBitmap() {
            return textBitmap;
        }

        public void setTextBitmap(Bitmap textBitmap) {
            this.textBitmap = textBitmap;
        }
    }



    static public class ZBarcodeBean{
        private String zbarcodeString;
        private Bitmap ztextBitmap;
        private Rect bound;
        private int bound1;//y
        private int bound2;//x
        private int bound3;//qrcode width
        private int bound4;//qrcode high
        private int ImageReaderCreateHeight;
        private int ImageReaderCreateWeight;


        public ZBarcodeBean(String _barcodeString , Bitmap _textBitmap ){
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

}
