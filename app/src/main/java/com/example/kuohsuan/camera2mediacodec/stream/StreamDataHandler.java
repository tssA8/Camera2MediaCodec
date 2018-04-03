//package com.example.kuohsuan.camera2mediacodec.stream;
//
//import android.media.MediaCodec;
//import android.util.Log;
//
//import java.util.concurrent.ConcurrentLinkedQueue;
//
///**
// * Created by User on 2016/8/15.
// */
//public class StreamDataHandler {
//    private final String TAG = this.getClass().getSimpleName();
//    private final long DEFAULT_QUEUE_LENGTH_MICRO_SEC = 4 * 1000*1000;
//    private final boolean VERBOSE = false;           // lots of logging
//
//    private long queueLengthMicroSec = DEFAULT_QUEUE_LENGTH_MICRO_SEC;
//
//    private ConcurrentLinkedQueue<StreamData> streamDataQueue ;
//    private static StreamDataHandler instance = null;
//    private StreamDataHandler(){
//        streamDataQueue = new ConcurrentLinkedQueue();
//    }
//    public static StreamDataHandler getInstance() {
//        if (instance == null){
//            synchronized(StreamDataHandler.class){
//                if(instance == null) {
//                    instance = new StreamDataHandler();
//                }
//            }
//        }
//        return instance;
//    }
//
//    public synchronized boolean pushData(StreamData data){
//        //如果超過時間區間
//        boolean isOutOfTime = true;
//        while(isOutOfTime){
//            if(streamDataQueue.isEmpty())
//                break;
//            StreamData  headData = streamDataQueue.peek();
//            long headDataPresentationTimeUs = headData.getBufferInfo().presentationTimeUs;
//            StreamData.StreamDataType dataType = headData.getDataType();
//            MediaCodec.BufferInfo bufferInfo = headData.getBufferInfo();
//
//            long inputDataPresentationTimeUs = data.getBufferInfo().presentationTimeUs;
//
//            if( (inputDataPresentationTimeUs - headDataPresentationTimeUs) > queueLengthMicroSec ){
//                Log.d(TAG,"StreamDataHandler_1_超過queue時間長度("+(queueLengthMicroSec/1000/1000)+"秒), 移除資料:"+dataType +"("+headData.getFrameCount()+"),isKey:" + bufferInfo.flags);
//
//
//                /*
//                                    20160825 y3 這邊超詭異!!  如果用 streamDataQueue.remove() 居然會報error....但是改用poll() 就沒問題~~
//                                    我在想可能是 remove後 我們執行freeNativeBuffer後,   java還是會操作byteBuffer的資料,導致錯誤!!
//                                      怪哉~~~~
//                                            headData.freeNativeBuffer();
//                                            streamDataQueue.remove(headData);
//                            */
//                StreamData removeData = streamDataQueue.poll();
//                removeData.freeNativeBuffer();
//
//                //Log.d(TAG,"StreamDataHandler_1_queue1 size:"+streamDataQueue.size());
//            }else{
//                isOutOfTime = false;
//                break;
//            }
//        }
//        if(isOutOfTime) {
//            boolean isFindKeyFrame = false;
//            while (!isFindKeyFrame) {
//                if (streamDataQueue.isEmpty())
//                    break;
//                StreamData headData = streamDataQueue.peek();
//                StreamData.StreamDataType dataType = headData.getDataType();
//                MediaCodec.BufferInfo bufferInfo = headData.getBufferInfo();
//                Log.w(TAG, "StreamDataHandler_2_start:" + dataType + "(" + headData.getFrameCount() + "),isKey:" + bufferInfo.flags);
//                //直到key frame
//                if (StreamData.StreamDataType.VIDEO.equals(dataType)) {
//                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//                        //等到key frmae 了, 跳出迴圈 ,不從queue移除
//                        isFindKeyFrame = true;
//                        Log.e(TAG, "StreamDataHandler_2_找到key frame:" + dataType + "(" + headData.getFrameCount() + "),isKey:" + bufferInfo.flags);
////                }else if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
////                    //如果是 END_OF_STREAM ,也要跳出迴圈,不從queue移除
////                    isFindKeyFrame =true;
//                    } else {
//
//                        StreamData removeData = streamDataQueue.poll();
//                        StreamData.StreamDataType removeDataType = removeData.getDataType();
//                        MediaCodec.BufferInfo removeDataBufferInfo = removeData.getBufferInfo();
//                        removeData.freeNativeBuffer();
//                        isFindKeyFrame = false;
//                        Log.d(TAG, "StreamDataHandler_2_移除資料a:" + removeDataType + "(" + removeData.getFrameCount() + "),isKey:" + removeDataBufferInfo.flags + "," + removeDataBufferInfo.presentationTimeUs);
//                    }
//
//                } else {//不是video frame,無條件刪除
//
//                    StreamData removeData = streamDataQueue.poll();
//                    StreamData.StreamDataType removeDataType = removeData.getDataType();
//                    MediaCodec.BufferInfo removeDataBufferInfo = removeData.getBufferInfo();
//                    removeData.freeNativeBuffer();
//                    isFindKeyFrame = false;
//                    Log.d(TAG, "StreamDataHandler_2_移除資料b:" + removeDataType + "(" + removeData.getFrameCount() + "),isKey:" + removeDataBufferInfo.flags + "," + removeDataBufferInfo.presentationTimeUs);
//                }
//                //Log.d(TAG,"StreamDataHandler_queue_2 size:"+streamDataQueue.size());
//
//            }
//        }
//
//        if(streamDataQueue.isEmpty() == false) {
//            StreamData headData = streamDataQueue.peek();
//            StreamData.StreamDataType dataType = headData.getDataType();
//            MediaCodec.BufferInfo bufferInfo = headData.getBufferInfo();
//            Log.d(TAG, "StreamDataHandler_headData," + dataType + "(" + headData.getFrameCount() + "),isKey:" + bufferInfo.flags + "," + bufferInfo.presentationTimeUs);
//
//
//            StreamData.StreamDataType inputDataType = data.getDataType();
//            MediaCodec.BufferInfo inputDataBufferInfo = data.getBufferInfo();
//            Log.i(TAG, "StreamDataHandler_inputData," + inputDataType + "(" + data.getFrameCount() + "),isKey:" + inputDataBufferInfo.flags + "," + inputDataBufferInfo.presentationTimeUs + ",diff:" +(inputDataBufferInfo.presentationTimeUs -  bufferInfo.presentationTimeUs));
//        }
//        boolean result = streamDataQueue.offer(data);
//        if(VERBOSE) {
//            if (result)
//                Log.d(TAG, "streamData_pushData(success):" + data.getDataType() + ",frame:" + data.getFrameCount() + ",queueSize:" + streamDataQueue.size() );
//            else
//                Log.e(TAG, "streamData_pushData(error):" + data.getDataType() + ",frame:" + data.getFrameCount() + ",queueSize:" + streamDataQueue.size());
//        }
//        //data.freeNativeBuffer();
//        return result;
//    }
//    public synchronized StreamData popData(){
//        return streamDataQueue.poll();
//    }
//    public synchronized void clearData(){
//
//        //streamDataQueue.clear();
//        while(!streamDataQueue.isEmpty()) {
//            StreamData removeData = streamDataQueue.poll();
//            StreamData.StreamDataType removeDataType = removeData.getDataType();
//            MediaCodec.BufferInfo removeDataBufferInfo = removeData.getBufferInfo();
//            removeData.freeNativeBuffer();
//        }
//
//    }
//    public synchronized void freeStreamData(StreamData removeData){
//        removeData.freeNativeBuffer();
//    }
//    public synchronized boolean isEmpty(){
//        return streamDataQueue.isEmpty();
//    }
//
//    public long getQueueLengthMicroSec() {
//        return queueLengthMicroSec;
//    }
//
//    public void setQueueLengthSec(int queueLengthSec) {
//        this.queueLengthMicroSec = queueLengthSec * 1000 * 1000;
//    }
//}
