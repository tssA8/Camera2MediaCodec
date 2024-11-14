package com.example.kuohsuan.camera2mediacodec.util;

import com.example.kuohsuan.camera2mediacodec.bean.ZBarCodeBean;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by kuohsuan on 2018/4/20.
 */

public class ZbarQueue {
    //FIFO
    private ConcurrentLinkedQueue<ZBarCodeBean> queue = new ConcurrentLinkedQueue<>();
    private static ZbarQueue instance = null;
    private ZbarQueue(){}
    public static ZbarQueue getInstance(){
        if(instance==null){
            synchronized (ZbarQueue.class){
                if(instance==null){
                    instance = new ZbarQueue();
                }
            }
        }
        return instance;
    }

    public void addQueue(ZBarCodeBean bean){
        queue.offer(bean);
    }

    public ZBarCodeBean getBeanInQueue(){
        ZBarCodeBean zBarcodeBean = queue.poll();//poll = peek+remove
        return zBarcodeBean;
    }

}
