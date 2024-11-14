package com.example.kuohsuan.camera2mediacodec.util;

/**
 * Created by kuohsuan on 2018/4/20.
 * a flag for RunningAppProcess
 */

public class ViewStateUtil {
    private boolean isRunningFrontProcess = false;
    private static ViewStateUtil instance = null;

    private ViewStateUtil() {
    }

    public static ViewStateUtil getInstance() {
        if (instance == null) {
            synchronized (ViewStateUtil.class) {
                if (instance == null) {
                    instance = new ViewStateUtil();
                }
            }
        }
        return instance;
    }

    public boolean isRunningAppProcess() {
        return isRunningFrontProcess;
    }

    public void setRunningStateProcess(boolean isRunningFrontProcess) {
        this.isRunningFrontProcess = isRunningFrontProcess;
    }
}
