package com.example.kuohsuan.camera2mediacodec;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yanzhenjie.zbar.Config;
import com.yanzhenjie.zbar.Image;
import com.yanzhenjie.zbar.ImageScanner;
import com.yanzhenjie.zbar.Symbol;
import com.yanzhenjie.zbar.SymbolSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by kuohsuan on 2018/4/23.
 */

public class ZbarProcessorRunnable implements Runnable {

    private static final String TAG = ZbarProcessorRunnable.class.getSimpleName();
    private long mStartTimeMillis = android.os.SystemClock.elapsedRealtime();

    // This lock guards all of the member variables below.
    private final Object mLock = new Object();
    private boolean mActive = true;
    private Image barcode ;
    // These pending variables hold the state associated with the new frame awaiting processing.
    private long mPendingTimeMillis;
    private int  mPendingFrameId = 0;
    private byte[] mPendingFrameData;
    private Thread mProcessingThread;
    private ImageScanner zbarImageScanner;
    private int imageH = 1080;
    private int imageW = 1920;
    private int  previewW,  previewH;
    private Handler zbarHander;
    private ZbarInPutDataQueue zbarInPutDataQueue = new ZbarInPutDataQueue();
    /**
     * Map to convert between a byte array, received from the camera, and its associated byte
     * buffer.  We use byte buffers internally because this is a more efficient way to call into
     * native code later (avoids a potential copy).
     */
//    private Map<byte[], ByteBuffer> mBytesToByteBuffer = new HashMap<>();
//    private Size mPreviewSize;

    public ZbarProcessorRunnable(Handler mZabrHandler
            , int width, int height, int  previewW, int  previewH, Image barcode) {
        this.zbarHander = mZabrHandler;
        zbarImageScanner = new ImageScanner();
        zbarImageScanner.setConfig(0, Config.X_DENSITY, 3);
        zbarImageScanner.setConfig(0, Config.Y_DENSITY, 3);
        mProcessingThread = new Thread(this);
        imageW = width;
        imageH = height;
        this.previewH = previewH;
        this.previewW = previewW;
        this.barcode = barcode;

    }

    /**
     * Releases the underlying receiver.  This is only safe to do after the associated thread
     * has completed, which is managed in camera source's release method above.
     */
    @android.annotation.SuppressLint("Assert")
    public void release() {
        assert (mProcessingThread.getState() == Thread.State.TERMINATED);
    }

    /**
     * As long as the processing thread is active, this executes detection on frames
     * continuously.  The next pending frame is either immediately available or hasn't been
     * received yet.  Once it is available, we transfer the frame info to local variables and
     * run detection on that frame.  It immediately loops back for the next frame without
     * pausing.
     * <p/>
     * If detection takes longer than the time in between new frames from the camera, this will
     * mean that this loop will run without ever waiting on a frame, avoiding any context
     * switching or frame acquisition time latency.
     * <p/>
     * If you find that this is using more CPU than you'd like, you should probably decrease the
     * FPS setting above to allow for some idle time in between frames.
     */
    @Override
    public void run() {

        while (true) {
            synchronized (mLock) {
                while (mActive && (mPendingFrameData == null)) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (!mActive) {
                    return;
                }
                barcode.setData(mPendingFrameData);
                zbarInPutDataQueue.addQueue(barcode);
                mPendingFrameData = null;
            }

            Image barcode = zbarInPutDataQueue.getBeanInPutQueue();
            if(barcode!=null) {
                try {
                    startZbarScan(barcode, imageW, imageH);
                    Thread.sleep(3);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        }
    }


    public void start() {
        mProcessingThread = new Thread(this);
        setActive(true);
        mProcessingThread.start();
    }

    /**
     * Marks the runnable as active/not active.  Signals any blocked threads to continue.
     */
    private void setActive(boolean active) {
        synchronized (mLock) {
            mActive = active;
            mLock.notifyAll();
        }
    }

    /**
     * Sets the frame data received from the camera.  This adds the previous unused frame buffer
     * (if present) back to the camera, and keeps a pending reference to the frame data for
     * future use.
     */
    public void setNextFrame(byte[] data) {
        synchronized (mLock) {
            if (mPendingFrameData != null) {
                mPendingFrameData = null;
            }

            // Timestamp and frame ID are maintained here, which will give downstream code some
            // idea of the timing of frames received and when frames were dropped along the way.
            mPendingTimeMillis = android.os.SystemClock.elapsedRealtime() - mStartTimeMillis;
            mPendingFrameId++;
            mPendingFrameData =data;

            // Notify the processor thread if it is waiting on the next frame (see below).
            mLock.notifyAll();
        }
    }


    private void startZbarScan(Image barcode,int imageWidth, int imageHeight){
//            Log.d(TAG,"B_________result");
        //            Log.d(TAG,"B_________result");
        long startTime = System.currentTimeMillis();
        int result = zbarImageScanner.scanImage(barcode);
        JSONArray jsonArrayObject = new JSONArray();
        long endTime = System.currentTimeMillis();
        Log.d(TAG,"B_________symSet, spend time:" + (endTime-startTime)+" result : "+result);
        String resultStr = "";
//        Log.d(TAG,"B_________symSetresult"+result);
        if (result != 0) {
            SymbolSet symSet = zbarImageScanner.getResults();


            for (Symbol sym : symSet){
                try {

                    /**
                     * 轉換座標, 1920*1080 to previewSize
                     * */
                    int  previewW = this.previewW;
                    int  previewH = this.previewH;
                    double s1 = (double)previewW / (double)imageWidth;
                    double s2 = (double)previewH / (double)imageHeight;
//                        double ratio = Math.max(s1,s2);
//                        float offsetX = (1280 - (2688*ratio))/2;
//                        float offsetY = (768 - (1512*ratio))/2;

                    double b1 = ((double)(sym.getBounds()[0])*s1);
                    double b2 = ((double)(sym.getBounds()[1])*s2);
                    double b3 = ((double)(sym.getBounds()[2])*s1);
                    double b4 = ((double)(sym.getBounds()[3])*s2);
//                        int b4 = (int) ((sym.getBounds()[3]*ratio)+offsetX);
                        Log.d(TAG,"AAA_ZBarcodeBean ADJUST B1 : "+b1+" S1 : "+s1+" b2 : "
                                +b2+" s2 : "+s2+" b3 : "+b3+" b4 : "+b4);

                    long b1Round = Math.round(b1);
                    long b2Round = Math.round(b2);
                    long b3Round = Math.round(b3);
                    long b4Round = Math.round(b4);

//                        Log.d(TAG,"AAA_ZBarcodeBean Round B1 : "+b1Round+" b2 : "
//                                +b2Round+" b3 : "+b3Round+" b4 : "+b4Round);

                    JSONObject scanResultJsonObject = new JSONObject();
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_SCAN_RESULT , sym.getData());
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND1 , (int)b1Round);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND2 , (int)b2Round);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND3 , (int)b3Round);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND4 , (int)b4Round);
//                        scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND1 , sym.getBounds()[0]);
//                        scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND2 , sym.getBounds()[1]);
//                        scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND3 , sym.getBounds()[2]);
//                        scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND4 , sym.getBounds()[3]);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_IMAGE_HIGHT , previewH);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_IMAGE_WEIGHT , previewW);


                    jsonArrayObject.put(scanResultJsonObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                resultStr = resultStr  + " : " + sym.getData();
                int[] bounds = sym.getBounds();
            }
        }else{
            Log.d(TAG,"A____getYuv 1 ");
//                if(a<10 && data!=null){
//                    barcode.setData(data);
//                    savePhotoTask.doInBackground(data);
//                    a+=1;
//                }
        }

        Message message = zbarHander.obtainMessage();
        //                Bundle bundle = new Bundle();
        //                message.obj = resultStr;
        message.obj = jsonArrayObject.toString();
        message.sendToTarget();

        //free json
        jsonArrayObject = null;

//        Log.d(TAG,"A____getYuv 2 ");


    }



    static class ZbarInPutDataQueue {
        //FIFO
        private ConcurrentLinkedQueue<Image> queue = new ConcurrentLinkedQueue<>();
        private final Object mLock = new Object();
        private static ZbarInPutDataQueue instance = null;
        private ZbarInPutDataQueue(){}
        public static ZbarInPutDataQueue getInstance(){
            if(instance==null){
                synchronized (ZbarInPutDataQueue.class){
                    if(instance==null){
                        instance = new ZbarInPutDataQueue();
                    }
                }
            }
            return instance;
        }


        public void addQueue(Image image){
            synchronized (mLock) {
                if(queue.size()>30){
//                    Log.d(TAG,"AAA_ QUEUE IS OUT OF 30 FRAME ");
                    queue.poll();
                }
                queue.offer(image);
                mLock.notifyAll();
            }
        }

        public Image getBeanInPutQueue(){
            Image data = queue.poll();//poll = peek+remove
            return data;
        }

    }
}


