package com.example.kuohsuan.camera2mediacodec.stream;

import android.media.MediaFormat;
import android.util.Log;

/**
 * Created by User on 2016/8/15.
 */
public class MuxerManagement {
    private final String TAG = this.getClass().getSimpleName();
    private final boolean VERBOSE = false;           // lots of logging
    private final long DEFAULT_FILE_DURATION_LENGTH_MICRO_SEC = 5 * 1000*1000;

    public enum MONITORING_MODE{
        NONE,RTC_START , RTC_STOP, MOTION_TRIGGER
    }


    private MediaMuxerWrapper muxerWrapper;
    private TrackInfo videoTrackInfo;
    private TrackInfo audioTrackInfo;
    private MediaFormat videoFormat;
    private MediaFormat audioFormat;

    private int leadingChunk = 1;

    private int outPutFormat;
    private String filePathAndName;

    private boolean isExecuteMuxerJob = false;

    private MONITORING_MODE monitoringModeEnum = MONITORING_MODE.NONE;
    private long fileDurationLengthMicroSec = DEFAULT_FILE_DURATION_LENGTH_MICRO_SEC;

    private static MuxerManagement instance = null;
    public static MuxerManagement getInstance() {
        if (instance == null){
            synchronized(MuxerManagement.class){
                if(instance == null) {
                    instance = new MuxerManagement();

                }
            }
        }
        return instance;
    }
    private MuxerManagement(){
        isExecuteMuxerJob = true;
        //startExecuteMuxerJob();
    }

    public boolean initMuxerInfo(String outPutDir , int outPutFormat ){
        boolean result = false;

        this.outPutFormat = outPutFormat;
        this.filePathAndName = outPutDir + "VideoRecord";

        videoTrackInfo = new TrackInfo();
        audioTrackInfo = new TrackInfo();
        boolean initResult = initMuxerWrapper();
        if(initResult) {
            reSetMuxerWrapper();
            result = true;
        }else{
            result = false;
        }
        return result;
    }
    public void setVideoFormat(MediaFormat videoFormat) {
        if (muxerWrapper.started) {
            Log.e(TAG, "format changed after muxer start! Can we ignore?");
            throw new RuntimeException("format changed after muxer start");
        }else{
            this.videoFormat = videoFormat;
            //videoTrackInfo.index = muxerWrapper.addTrack(videoFormat);
        }
    }
    public void setAudioFormat(MediaFormat audioFormat) {
        if (muxerWrapper.started) {
            Log.e(TAG, "format changed after muxer start! Can we ignore?");
            throw new RuntimeException("format changed after muxer start");
        }else {
            this.audioFormat = audioFormat;
            //audioTrackInfo.index = muxerWrapper.addTrack(audioFormat);
        }
    }

    public MONITORING_MODE getMonitoringModeEnum() {
        return monitoringModeEnum;
    }

    public boolean setMonitoringModeEnum(MONITORING_MODE monitoringModeEnum) {
        boolean isSuccess = false;
        switch (monitoringModeEnum) {
            case NONE:
                isSuccess = true;
                break;
            case RTC_START:
                fileDurationLengthMicroSec = DEFAULT_FILE_DURATION_LENGTH_MICRO_SEC;
                if(muxerWrapper == null || outPutFormat<0)
                    isSuccess = false;
                else if(videoFormat == null || audioFormat==null)
                    isSuccess = false;
                else if(videoTrackInfo == null || audioTrackInfo == null)
                    isSuccess = false;
                else{
                    if(muxerWrapper.muxer !=null){
                        //表示已經有init過了!! 所以不從新restart

                        //videoTrackInfo.index = muxerWrapper.addTrack(videoFormat);
                        //audioTrackInfo.index = muxerWrapper.addTrack(audioFormat);
                    }else{
                        //muxer重啟
//                        boolean initResult = initMuxerWrapper();
//                        if(initResult) {
//                            reSetMuxerWrapper();
//                            videoTrackInfo.index = muxerWrapper.addTrack(videoFormat);
//                            audioTrackInfo.index = muxerWrapper.addTrack(audioFormat);
//                        }else{
//                            Log.d(TAG,"initMuxerWrapper failed!");
//                        }
                        muxerWrapper.restart(outPutFormat, ++leadingChunk); // prepare muxer for next chunk, but don't alter leadingChunk

                    }
                    videoTrackInfo.index = muxerWrapper.addTrack(videoFormat);
                    audioTrackInfo.index = muxerWrapper.addTrack(audioFormat);

                    isSuccess = true;
                }

                break;
            case RTC_STOP:
                fileDurationLengthMicroSec = 0;
                isSuccess = true;
                break;
            case MOTION_TRIGGER:
                fileDurationLengthMicroSec = 0;
                isSuccess = true;
                break;
            default:
                monitoringModeEnum = MONITORING_MODE.NONE;
                isSuccess = true;
                Log.d(TAG,"設定MonitoringMode 錯誤,還原成預設值");

        }
        if(isSuccess==false){
            this.monitoringModeEnum = MONITORING_MODE.NONE;
            this.fileDurationLengthMicroSec = 0 ;
        }else {
            this.monitoringModeEnum = monitoringModeEnum;
        }
        return isSuccess;
    }
    public void startMotionTrigger(){
        fileDurationLengthMicroSec = DEFAULT_FILE_DURATION_LENGTH_MICRO_SEC;
    }
    public void stopMotionTrigger(){
        fileDurationLengthMicroSec = 0;
    }

    /*****************
     *  private method
     * ****************/

//    private void startExecuteMuxerJob(){
//        new Thread(new Runnable(){
//            @Override
//            public void run() {
//
//                boolean isFirstKeyFrame = false;
//                long fileStartPresentationTimeUs = 0;
//
//                StreamDataHandler dataHandler = StreamDataHandler.getInstance();
//                while(isExecuteMuxerJob){
//                    if(monitoringModeEnum == null || MONITORING_MODE.NONE.equals(monitoringModeEnum)){
//                        //Log.d(TAG,"writeSampleData_MONITORING_MODE.NONE");
//                        continue;
//                    }
//
//                    //避免audio&video format 沒有設定完成
//                    if(audioFormat == null || videoFormat == null ){
//                        Log.d(TAG,"writeSampleData_startExecuteMuxerJob_audioFormat == null || videoFormat == null");
//                        continue;
//                    }
//                    if( muxerWrapper==null || muxerWrapper.started==false ){
//                        Log.d(TAG,"writeSampleData_startExecuteMuxerJob_muxerWrapper==null || muxerWrapper.started==false");
//                        continue;
//                    }
//
//                    StreamData streamData = dataHandler.popData();
//                    if(streamData == null) {
//                        //Log.d(TAG,"writeSampleData_startExecuteMuxerJob_streamData == null)");
//                        continue;
//                    }else{
//                        //Log.d(TAG,"startExecuteMuxerJob_write)");
//                    }
//
//                    ByteBuffer byteBuf = streamData.getByteBuf();
//                    MediaCodec.BufferInfo bufferInfo = streamData.getBufferInfo();
//                    StreamData.StreamDataType dataType = streamData.getDataType();
//                    int frameCount = streamData.getFrameCount();
//
//                    //略過CODEC_CONFIG資料
//                    if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
//                        Log.w(TAG,"writeSampleData(" + dataType + ")("+ frameCount +")" +" 略過CODEC_CONFIG資料!" );
//                        /*記得要free掉!!*/
//                        dataHandler.freeStreamData(streamData);
//                        continue;
//                    }
//
//                    //找到第一張key frame
//                    if(!isFirstKeyFrame && StreamData.StreamDataType.VIDEO == dataType) {
//                        if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0){
//                            isFirstKeyFrame = true;
//                            fileStartPresentationTimeUs = bufferInfo.presentationTimeUs;
//                        }
//                    }
//                    if(!isFirstKeyFrame){
//                        Log.w(TAG,"writeSampleData(" + dataType + ")("+ frameCount +") 略過!,因為還沒找到第一張key" +streamData.getBufferInfo().presentationTimeUs + ",iskey:"+streamData.getBufferInfo().flags );
//                        /*記得要free掉!!*/
//                        dataHandler.freeStreamData(streamData);
//                        continue;
//                    }
//
//                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                        Log.e(TAG, "writeSampleData("+dataType+")("+ frameCount +")"+ "強制停止錄影 ,bufferInfo.flags:" + bufferInfo.flags);
//                        muxerWrapper.stop();
//                        //清空queue
//                        dataHandler.clearData();
//                        //reSetMuxerWrapper();
//                        //
//                        isFirstKeyFrame = false;
//                        //clearMuxerInfo();
//
//                        monitoringModeEnum = MONITORING_MODE.NONE;
//
//                        /*記得要free掉!!*/
//                        dataHandler.freeStreamData(streamData);
//                        continue;
//                    }
//
//                    TrackInfo trackInfo;
//                    if(StreamData.StreamDataType.VIDEO == dataType) {
//                        trackInfo = videoTrackInfo;
//                    }else{
//                        trackInfo = audioTrackInfo;
//                    }
//                    Log.d(TAG,"writeSampleData("+dataType+")("+ frameCount +")" +streamData.getBufferInfo().presentationTimeUs + ",iskey:"+streamData.getBufferInfo().flags );
//                    //long writeSampleData_s = System.currentTimeMillis();
//                    muxerWrapper.muxer.writeSampleData(trackInfo.index, byteBuf, bufferInfo);
//
//                    //long writeSampleData_e = System.currentTimeMillis();
//                    //Log.d(TAG,"spend_time_writeSampleData:"+ (writeSampleData_e-writeSampleData_s));
//
//
//
//                    if(StreamData.StreamDataType.VIDEO == dataType) {
//
//                        //判斷是不是 key frame
//                        if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//                            //判斷是不是 該截檔了!
//                            if( fileDurationLengthMicroSec <= 0 || (bufferInfo.presentationTimeUs - fileStartPresentationTimeUs ) >= fileDurationLengthMicroSec){
//
//                                //使用者強制將RTC調成 stop.所以要停止錄影
//                                if(MONITORING_MODE.RTC_STOP.equals(monitoringModeEnum)){
//                                    muxerWrapper.stop();
//                                    isFirstKeyFrame = false;
//                                    //clearMuxerInfo();
//                                    //並且把mode調成none
//                                    monitoringModeEnum = MONITORING_MODE.NONE;
//                                }else if(MONITORING_MODE.RTC_START.equals(monitoringModeEnum)){//表示要繼續錄影
//                                    Log.e(TAG, "writeSampleData1("+dataType+")("+ frameCount +")"+ "finishTrack ,bufferInfo.flags:" + bufferInfo.flags + ",leadingChunk:"+ leadingChunk +",presentationTimeUs"+bufferInfo.presentationTimeUs);
//
//                                    //muxerWrapper.finishTrack();
//                                    muxerWrapper.restart(outPutFormat, ++leadingChunk); // prepare muxer for next chunk, but don't alter leadingChunk
//                                    Log.w(TAG, "writeSampleData2("+dataType+")("+ frameCount +")"+ "finishTrack ,bufferInfo.flags:" + bufferInfo.flags + ",leadingChunk:"+ leadingChunk+",presentationTimeUs"+bufferInfo.presentationTimeUs);
//
//                                    videoTrackInfo.index = muxerWrapper.addTrack(videoFormat);
//                                    //Log.d(TAG, "mVideoTrackInfo.index :" + mVideoTrackInfo.index);
//                                    audioTrackInfo.index = muxerWrapper.addTrack(audioFormat);
//                                    //Log.d(TAG, "mAudioTrackInfo.index :" + mAudioTrackInfo.index);
//
//                                    if (muxerWrapper.allTracksAdded() && trackInfo.muxerWrapper.started) {
//                                        //Log.e(TAG,"     finishTrack:frame_count,在一次key_frame");
//
//                                        //由於上面有執行 get(), 所以position&limit 值 有誤!! 故需要還原position&limit ,
//                                        byteBuf.position(bufferInfo.offset);
//                                        byteBuf.limit(bufferInfo.offset + bufferInfo.size);
//                                        //重新將key 寫入muxer
//                                        muxerWrapper.muxer.writeSampleData(trackInfo.index, byteBuf, bufferInfo);
//
//                                        fileStartPresentationTimeUs = bufferInfo.presentationTimeUs;
//
//
//                                    }
//                                }
//                                Log.e(TAG, "writeSampleData("+dataType+")("+ frameCount +")"+ "finishTrack ,bufferInfo.flags:" + bufferInfo.flags + ",leadingChunk:"+ leadingChunk);
//
//
//
//
//
//                            }
//                        }
//
//
//                    }
//                    /*記得要free掉!!*/
//                    dataHandler.freeStreamData(streamData);
//
//                }
//
//
//
//
//            }
//        }).start();
//
//    }
    private void stopExecuteMuxerJob(){
        isExecuteMuxerJob = false;
    }





    private void reSetMuxerWrapper(){
        videoTrackInfo.index = -1;
        videoTrackInfo.muxerWrapper = muxerWrapper;
        audioTrackInfo.index = -1;
        audioTrackInfo.muxerWrapper = muxerWrapper;
    }
    private boolean isSetMuxerInfo(){
        boolean setOk= true;
        if(filePathAndName==null || videoTrackInfo ==null  || audioTrackInfo == null)
            setOk = false;

        return setOk;
    }
    private void clearMuxerInfo(){
        filePathAndName=null;


    }

    private boolean initMuxerWrapper(){
        boolean result = false;
        if(isSetMuxerInfo()) {
            muxerWrapper = new MediaMuxerWrapper(filePathAndName, outPutFormat, ++leadingChunk);
            result = true;
        }

        return result;
    }

    public MediaMuxerWrapper getMuxerWrapper(){
        return muxerWrapper;
    }


/*
    private void advanceVideoMediaMuxer(){
        MediaMuxerWrapper videoMuxer = (videoTrackInfo.muxerWrapper == nowMuxerWrapper) ? muxerWrapper1 : muxerWrapper2;
        MediaMuxerWrapper audioMuxer = (audioTrackInfo.muxerWrapper == nowMuxerWrapper) ? muxerWrapper1 : muxerWrapper2;
        Log.i("advanceVideo", "video on " + ((videoTrackInfo.muxerWrapper == nowMuxerWrapper) ? "muxer1" : "muxer2"));
        if(videoMuxer == audioMuxer){
            // if both encoders are on same muxer, switch to other muxer
            leadingChunk++;
            if(videoMuxer == nowMuxerWrapper){
                Log.i("advanceVideo", "encoders on same muxer. swapping.");
                videoTrackInfo.muxerWrapper = muxerWrapper2;
                // testing: can we start next muxer immediately given MediaCodec.getOutputFormat() values?

            }else if(videoMuxer == muxerWrapper2){
                Log.i("advanceVideo", "encoders on same muxer. swapping.");
                videoTrackInfo.muxerWrapper = muxerWrapper1;
                // testing: can we start next muxer immediately given MediaCodec.getOutputFormat() values?
            }
            if(videoFormat != null && audioFormat != null){
                videoTrackInfo.muxerWrapper.addTrack(videoFormat);
                videoTrackInfo.muxerWrapper.addTrack(audioFormat);
            }else{
                Log.e(TAG, "mVideoOutputFormat or mAudioOutputFormat is null!");
            }
        }else{
            // if encoders are separate, finalize this muxer, and switch to others
            Log.i("advanceVideo", "encoders on diff muxers. restarting");
            videoTrackInfo.muxerWrapper.restart(outPutFormat, leadingChunk + 1); // prepare muxer for next chunk, but don't alter leadingChunk
            videoTrackInfo.muxerWrapper = audioTrackInfo.muxerWrapper;
        }
    }
    private void advanceAudioMediaMuxer(){
        MediaMuxerWrapper videoMuxer = (videoTrackInfo.muxerWrapper == nowMuxerWrapper) ? muxerWrapper1 : muxerWrapper2;
        MediaMuxerWrapper audioMuxer = (audioTrackInfo.muxerWrapper == nowMuxerWrapper) ? muxerWrapper1 : muxerWrapper2;
        Log.i("advanceAudio", "audio on " + ((audioTrackInfo.muxerWrapper == nowMuxerWrapper) ? "muxer1" : "muxer2"));
        if(videoMuxer == audioMuxer){
            // If both encoders are on same muxer, switch to other muxer
            Log.i("advanceAudio", "encoders on same muxer. swapping.");
            leadingChunk++;
            if(videoMuxer == nowMuxerWrapper){
                audioTrackInfo.muxerWrapper = muxerWrapper2;
            }else if(videoMuxer == muxerWrapper2){
                audioTrackInfo.muxerWrapper = muxerWrapper1;
            }
            if(videoFormat != null && audioFormat != null){
                audioTrackInfo.muxerWrapper.addTrack(videoFormat);
                audioTrackInfo.muxerWrapper.addTrack(audioFormat);
            }else{
                Log.e(TAG, "mVideoOutputFormat or mAudioOutputFormat is null!");
            }
        }else{
            // if encoders are separate, finalize this muxer, and switch to others
            Log.i("advanceAudio", "encoders on diff muxers. restarting");
            audioTrackInfo.muxerWrapper.restart(outPutFormat, leadingChunk + 1); // prepare muxer for next chunk, but don't alter leadingChunk
            audioTrackInfo.muxerWrapper = videoTrackInfo.muxerWrapper;
        }
    }*/
    /**
     * Releases encoder resources.
     */
    private void releaseEncodersAndMuxer() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");

        if (muxerWrapper != null) {
            synchronized (muxerWrapper.sync){
                muxerWrapper.stop();
                muxerWrapper = null;
            }
        }
//        if (muxerWrapper2 != null) {
//            synchronized (muxerWrapper2.sync){
//                muxerWrapper2.stop();
//                muxerWrapper2 = null;
//            }
//        }
    }

}
