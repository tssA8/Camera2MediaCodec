package com.example.kuohsuan.camera2mediacodec.Interface;


import com.example.kuohsuan.camera2mediacodec.bean.BestPictureSizeResultBean;
import com.example.kuohsuan.camera2mediacodec.bean.CameraSourceSizeResultBean;
import com.example.kuohsuan.camera2mediacodec.bean.PictureInfoBean;
import com.example.kuohsuan.camera2mediacodec.bean.QuarterNV21Bean;
import com.example.kuohsuan.camera2mediacodec.bean.QuarterNV21ResultBean;
import com.example.kuohsuan.camera2mediacodec.bean.StartCameraSourceBean;

/**
 * Created by kuohsuan on 2018/3/26.
 */

public interface ICameraAction {
    void startCameraSource(StartCameraSourceBean startCameraSourceBean);
    void stopCameraSource();
    void autoFocus();
    void stopVideoCameraSource();
    void recordVideoCameraSource();
    QuarterNV21ResultBean quarterNV21CameraSource(QuarterNV21Bean quarterNV21Bean);
    BestPictureSizeResultBean getBestAspectPictureSizes(PictureInfoBean pictureInfoBean);//return size
    CameraSourceSizeResultBean getPreviewSizes();//return size
    Integer getCameraFacings();//return int back or front

}

//startCameraSource ----> start
//stopCameraSource  ----> stop

