package com.example.kuohsuan.camera2mediacodec.stream.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by f9021 on 2017/12/14.
 */

public class MyVideoEncoder extends MyBaseEncoder{
    private final String TAG = this.getClass().getSimpleName();
    private MyVideoCodecWrapper myMediaCodecWrapper;

    public MyVideoEncoder(long startRecordWhenNs){
        super.startRecordWhenNs = startRecordWhenNs;
    }

    public MyVideoCodecWrapper prepareVideoEncoder(int width, int height , int bitRate, String saveFileName , boolean isMainStreaming) throws Exception {
        if(width<=0 || height<=0 || bitRate<=0 || isFinish  )
            throw new Exception();

        myMediaCodecWrapper = new MyVideoCodecWrapper();

        MediaCodec codec = null;
        File f = new File(Environment.getExternalStorageDirectory(), saveFileName);
        if(!f.exists()){
            try {
                f.createNewFile();
                Log.e(TAG, "       create a file     ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            if(f.delete()){
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(f));
            Log.i(TAG, "videoStreamOutput initialized");
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,tv_preview.getWidth(), tv_preview.getHeight());
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,width,height);

        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface); //COLOR_FormatSurface
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);//500kbps
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        //ediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

        //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
        //      MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);  //COLOR_FormatYUV420Planar
        //mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 90);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); //錕截鹼拷幀錕斤拷錕絞憋拷錕� 錕斤拷位s
        codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Surface encoderSurface = codec.createInputSurface();
        //this.setEncoderCanvasInfo(encoderSurface);
        //method 1
        codec.setCallback(new VideoEncoderCallback(isMainStreaming , outputStream));
        //codec.start();

        super.mediaCodec = codec;
        myMediaCodecWrapper.mediaCodec = codec;
        myMediaCodecWrapper.encoderSurface = encoderSurface;
        myMediaCodecWrapper.width = width;
        myMediaCodecWrapper.height = height;


        return myMediaCodecWrapper;
    }

    public MyVideoCodecWrapper getMyMediaCodecWrapper() {
        return myMediaCodecWrapper;
    }

    @Override
    public void stopCodec(){
        super.stopCodec();
        //停止render
        /*if(offScreenCanvas!=null)
            offScreenCanvas.end();
*/

    }

    @Override
    public void startCodec(){
        super.startCodec();

        //開始render
       /* if(offScreenCanvas!=null)
            offScreenCanvas.start();*/

    }

    private class VideoEncoderCallback extends MediaCodec.Callback{
        private final String TAG = this.getClass().getSimpleName();
        private BufferedOutputStream outputStream;
        private int serial = 0;
        private byte[] frameByteData ;
        private byte[] ppsFrameData ;
        private int ppsSize = 0;
        private boolean isMainStreaming = false;
        public VideoEncoderCallback(boolean _isMainStreaming , BufferedOutputStream _outputStream){
            outputStream = _outputStream;
            isMainStreaming = _isMainStreaming;
        }
        public VideoEncoderCallback(BufferedOutputStream _outputStream){
            this(false, _outputStream);
        }
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            //
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo bufferInfo) {
            try {
                //ByteBuffer encodedData = codec.getOutputBuffer(index);
                //encodedData.position(info.offset);
                //encodedData.limit(info.offset + info.size);
                //Log.d(TAG, "onOutputBufferAvailable, info.size: " + info.size);
                ByteBuffer outPutByteBuffer = codec.getOutputBuffer(index);
/*//write to file
            byte[] outDate = new byte[bufferInfo.size];
            outPutByteBuffer.get(outDate);

            try {
                Log.d(TAG, " outDate.length : " + outDate.length);
                videoStreamOutput.write(outDate, 0, outDate.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
*/

                long presentationTimeUs = bufferInfo.presentationTimeUs - (startRecordWhenNs /1000);
                bufferInfo.presentationTimeUs = presentationTimeUs;

                if(bufferInfo.presentationTimeUs < 0){
                    bufferInfo.presentationTimeUs = 0;
                }

 /* 2018/01/03 Y3 :因為沒有錄影需求, 所以先關閉 這段code
            //copy frameData to StreamData.
            StreamData.StreamDataType enumDataType = StreamData.StreamDataType.VIDEO;
            StreamData streamData = new StreamData(outPutByteBuffer, bufferInfo, enumDataType, serial);
            StreamDataHandler streamDataHandler = StreamDataHandler.getInstance();
            streamDataHandler.pushData(streamData);
*/
                // adjust the ByteBuffer values to match BufferInfo (not needed?)
                outPutByteBuffer.position(bufferInfo.offset);
                outPutByteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                int remainingSize = outPutByteBuffer.remaining();
                int capacitySize = outPutByteBuffer.capacity();
                if(remainingSize!=bufferInfo.size){
                    Log.e(TAG, "remainingSize!=mBufferInfo.size!!!!!");
                }
                if(frameByteData==null || frameByteData.length < remainingSize){
                    frameByteData = new byte[capacitySize];
                    Log.d(TAG,"new byte , size is:"+capacitySize);
                }

                // adjust the ByteBuffer values to match BufferInfo (not needed?)
                outPutByteBuffer.position(bufferInfo.offset);
                outPutByteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                outPutByteBuffer.get(frameByteData, 0,  remainingSize);
                // Log.d(TAG," mBufferInfo.offset:"+ bufferInfo.offset  + ",mBufferInfo.size:" + bufferInfo.size);
                boolean isKey;
                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME)
                    isKey = true;
                else
                    isKey = false;

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    ppsFrameData = new byte[capacitySize];
                    ppsSize= remainingSize;
                    System.arraycopy(frameByteData,0,ppsFrameData,0,remainingSize);
                }

                //如果是key_frame, 要在data之前 塞 SPS&PPS資料
                if(isKey){
                    byte[] oldData = new byte[remainingSize];
                    System.arraycopy(frameByteData,0,oldData,0,remainingSize);
                    System.arraycopy(ppsFrameData,0,frameByteData,0,ppsSize);
                    System.arraycopy(oldData,0,frameByteData,ppsSize,remainingSize);
                    remainingSize = remainingSize + ppsSize;
                }
                int width = myMediaCodecWrapper.mediaCodec.getOutputFormat().getInteger(MediaFormat.KEY_WIDTH);
                int height = myMediaCodecWrapper.mediaCodec.getOutputFormat().getInteger(MediaFormat.KEY_HEIGHT);

                if(MyVideoEncoder.this.myEncoderCallBackFunction!=null)
                    MyVideoEncoder.this.myEncoderCallBackFunction.encodeVideoSuccess( frameByteData ,bufferInfo.size ,isKey , width ,height );

                serial++;

                codec.releaseOutputBuffer(index, false);
            }catch(IllegalStateException ex){
                ex.printStackTrace();
            }

        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Log.d(TAG, "Error: " + e);
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            Log.d(TAG, "Video_onOutputFormatChanged: " + format);
//            MuxerManagement.getInstance().setVideoFormat(format);
            if(MyVideoEncoder.this.myEncoderCallBackFunction!=null)
                MyVideoEncoder.this.myEncoderCallBackFunction.outputFormatChanged(GEO_ENCODER_TYPE.VIDEO ,format );


        }
    }

/*

    EncoderCanvas offScreenCanvas;
    public void setEncoderCanvasInfo(EglContextWrapper eglCtx  , BasicTexture outsideTexture, SurfaceTexture outsideSurfaceTexture ){
        Surface inputSurface =  myMediaCodecWrapper.encoderSurface;
        int width = myMediaCodecWrapper.width;
        int height = myMediaCodecWrapper.height;
        offScreenCanvas = new EncoderCanvas(width,height , eglCtx , inputSurface );

        offScreenCanvas.setSharedTexture(outsideTexture,outsideSurfaceTexture);

        //offScreenCanvas.setOnDrawListener(new EncoderCanvas.OnDrawListener() {
    }
    public void requestRenderAndWait() {
        if(offScreenCanvas!=null)
            offScreenCanvas.requestRenderAndWait();
    }
*/

}
