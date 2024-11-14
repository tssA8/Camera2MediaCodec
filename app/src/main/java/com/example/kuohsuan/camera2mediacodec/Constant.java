package com.example.kuohsuan.camera2mediacodec;

import android.hardware.camera2.CaptureRequest;

/**
 * Created by kuohsuan on 2018/1/22.
 */

public class Constant {

    //camera
    public static final double RATIO_TOLERANCE = 0.1;
    public static final double MAXRATIO_TOLERANCE = 0.18;
    public static final int MAX_PREVIEW_WIDTH = 1920;
    public static final int MAX_PREVIEW_HEIGHT = 1080;
    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;
    public static final int CAMERA_AF_AUTO = CaptureRequest.CONTROL_AF_MODE_AUTO;
    public static final int CAMERA_FLASH_AUTO = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
    public static final String ENCODE_VIDEO_SAVE_PATH ="camera2mediacodec0.264";
    public static final String SDCARD_FILE_PATH_OUTPUT_DIR = "/sdcard/temp/videorecord/";

    //ZBAR
    public static final String ZBAR_BARCODE_BOUND1="bound1";
    public static final String ZBAR_BARCODE_BOUND2="bound2";
    public static final String ZBAR_BARCODE_BOUND3="bound3";
    public static final String ZBAR_BARCODE_BOUND4="bound4";
    public static final String ZBAR_BARCODE_SCAN_RESULT="scanStr";
    public static final String ZBAR_BARCODE_IMAGE_HIGHT = "imageH";
    public static final String ZBAR_BARCODE_IMAGE_WEIGHT = "imageW";
    public static long start_time =-1;
    public static int end_time =-1;

}
