package com.example.kuohsuan.camera2mediacodec.bean;

import android.graphics.SurfaceTexture;

import com.example.kuohsuan.camera2mediacodec.View.CameraPreviewTextureView;


/**
 * Created by kuohsuan on 2018/3/29.
 */

public class StartCameraSourceBean {
    Camera1SourceBean camera1SourceBean;
    Camera2SourceBean camera2SourceBean;


    public Camera1SourceBean getCamera1SourceBean() {
        return camera1SourceBean;
    }

    public void setCamera1SourceBean(Camera1SourceBean camera1SourceBean) {
        this.camera1SourceBean = camera1SourceBean;
    }

    public Camera2SourceBean getCamera2SourceBean() {
        return camera2SourceBean;
    }

    public void setCamera2SourceBean(Camera2SourceBean camera2SourceBean) {
        this.camera2SourceBean = camera2SourceBean;
    }

    public static class  Camera1SourceBean{

        SurfaceTexture previewSurfaceTexture;

        public SurfaceTexture getPreviewSurfaceTexture() {
            return previewSurfaceTexture;
        }

        public void setPreviewSurfaceTexture(SurfaceTexture previewSurfaceTexture) {
            this.previewSurfaceTexture = previewSurfaceTexture;
        }
    }

    public static class Camera2SourceBean {
        CameraPreviewTextureView textureView;
        SurfaceTexture previewSurfaceTexture;
        SurfaceTexture _offScreenSurfaceTexture;
        int displayOrientation;

        public CameraPreviewTextureView getTextureView() {
            return textureView;
        }

        public void setTextureView(CameraPreviewTextureView textureView) {
            this.textureView = textureView;
        }

        public SurfaceTexture getPreviewSurfaceTexture() {
            return previewSurfaceTexture;
        }

        public void setPreviewSurfaceTexture(SurfaceTexture previewSurfaceTexture) {
            this.previewSurfaceTexture = previewSurfaceTexture;
        }

        public SurfaceTexture get_offScreenSurfaceTexture() {
            return _offScreenSurfaceTexture;
        }

        public void set_offScreenSurfaceTexture(SurfaceTexture _offScreenSurfaceTexture) {
            this._offScreenSurfaceTexture = _offScreenSurfaceTexture;
        }

        public int getDisplayOrientation() {
            return displayOrientation;
        }

        public void setDisplayOrientation(int displayOrientation) {
            this.displayOrientation = displayOrientation;
        }
    }


}
