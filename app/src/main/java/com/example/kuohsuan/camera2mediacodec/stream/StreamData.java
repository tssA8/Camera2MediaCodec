//package com.example.kuohsuan.camera2mediacodec.stream;
//
//import android.media.MediaCodec;
//import android.util.Log;
//
//import java.nio.ByteBuffer;
//import java.util.concurrent.atomic.AtomicInteger;
//
//
//
///**
// * Created by User on 2016/8/15.
// */
//public class StreamData {
//    private final String TAG = this.getClass().getSimpleName();
//
//    public enum StreamDataType{
//        AUDIO,VIDEO
//    }
//
//    //private int trackIndex;
//    private ByteBuffer byteBuf;
//    private MediaCodec.BufferInfo bufferInfo;
//    private StreamDataType dataType;
//    private int frameCount ;
//    public StreamData(ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo, StreamDataType dataType , int frameCount){
//        //copy bufferInfo
//        MediaCodec.BufferInfo newBufferInfo = new  MediaCodec.BufferInfo();
//        newBufferInfo.flags =  bufferInfo.flags;
//        newBufferInfo.offset = bufferInfo.offset;
//        newBufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs;
//        newBufferInfo.size = bufferInfo.size;
//        this.bufferInfo = newBufferInfo;
//
//        //copy buffers
//        //this.byteBuf = byteBuf.duplicate(); //not work!!
//        this.byteBuf = clone(byteBuf);
//
//
//        //this.trackIndex = trackIndex;
//        this.dataType= dataType;
//        this.frameCount = frameCount;
//    }
//
//    public int getFrameCount() {
//        return frameCount;
//    }
//
//    public void setFrameCount(int frameCount) {
//        this.frameCount = frameCount;
//    }
//
//
//    public ByteBuffer getByteBuf() {
//        return byteBuf;
//    }
//
//    public void setByteBuf(ByteBuffer byteBuf) {
//        this.byteBuf = byteBuf;
//    }
//
//    public MediaCodec.BufferInfo getBufferInfo() {
//        return bufferInfo;
//    }
//
//    public void setBufferInfo(MediaCodec.BufferInfo bufferInfo) {
//        this.bufferInfo = bufferInfo;
//    }
//
//    public StreamDataType getDataType() {
//        return dataType;
//    }
//
//    public void setDataType(StreamDataType dataType) {
//        this.dataType = dataType;
//    }
//
//    public void freeNativeBuffer(){
//        if(byteBuf!=null) {
//
//            totalAllocBufferSize.addAndGet(- (byteBuf.capacity()));
//            lib.freeNativeBuffer(byteBuf);
//            byteBuf = null;
//
//        }
//    }
//    private static AtomicInteger totalAllocBufferSize = new AtomicInteger(0) ;
//
//    private ByteBuffer clone(ByteBuffer original) {
//
//
//       // ByteBuffer clone = ByteBuffer.allocateDirect(original.capacity());
//        ByteBuffer clone = (ByteBuffer)lib.allocNativeBuffer(original.capacity());
//        totalAllocBufferSize.addAndGet(original.capacity()) ;
//        Log.d(TAG,"original.capacity():"+original.capacity() +", totalAllocBufferSize:" + (totalAllocBufferSize.get()/1000/1000)+" MB"  + ",isKey:"+ bufferInfo.flags );
//
//        original.rewind();//copy from the beginning
//        clone.put(original);
//        original.rewind();
//        clone.flip();
//
//
//        return clone;
//    }
//
//
//}
