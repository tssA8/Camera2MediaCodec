package com.example.kuohsuan.camera2mediacodec.bean;

import java.nio.ByteBuffer;

/**
 * Created by kuohsuan on 2018/3/29.
 */

public class QuarterNV21ResultBean {
    ByteBuffer camera1ResultBean;
    byte[] camera2ResultBean;

    public ByteBuffer getCamera1ResultBean() {
        return camera1ResultBean;
    }

    public void setCamera1ResultBean(ByteBuffer camera1ResultBean) {
        this.camera1ResultBean = camera1ResultBean;
    }

    public byte[] getCamera2ResultBean() {
        return camera2ResultBean;
    }

    public void setCamera2ResultBean(byte[] camera2ResultBean) {
        this.camera2ResultBean = camera2ResultBean;
    }
}
