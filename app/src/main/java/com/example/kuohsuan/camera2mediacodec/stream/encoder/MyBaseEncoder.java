package com.example.kuohsuan.camera2mediacodec.stream.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

/**
 * Created by f9021 on 2017/12/14.
 */

public class MyBaseEncoder {
    protected MyEncoderCallBackFunction myEncoderCallBackFunction;
    protected MediaCodec mediaCodec;
    protected long startRecordWhenNs;
    protected boolean isFinish = false;

    public enum GEO_ENCODER_TYPE
    {
        VIDEO(0),
        AUDIO(1);
        private final int type;
        GEO_ENCODER_TYPE(int type) { this.type = type; }
        public int getType() { return type; }
    }

    public void startCodec() {
        if(!isFinish) {
            if(mediaCodec!=null) {
                mediaCodec.start();
            }
        }
    }
    public void stopCodec() {
        try {
            if(!isFinish) {
                isFinish = true;
                if(mediaCodec!=null) {
                    mediaCodec.stop();
                    mediaCodec.release();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public boolean isFinish() {
        return isFinish;
    }

    public void setFinish(boolean finish) {
        isFinish = finish;
    }

    public void setMyEncoderCallBackFunction(MyEncoderCallBackFunction _myEncoderCallBackFunction)
    {
        myEncoderCallBackFunction = _myEncoderCallBackFunction;
    }

    public interface MyEncoderCallBackFunction {

        void encodeAudioSuccess(byte[] encodeDate, int encodeSize, int channelCount, int sampleBit, int sampleRate);
        void encodeVideoSuccess(byte[] encodeDate, int encodeSize, boolean isVideoKeyFrame, int width, int height);
        void outputFormatChanged(GEO_ENCODER_TYPE encoderType, MediaFormat format);

    }


    public static class MyAudioCodecWrapper {
        public MediaCodec mediaCodec;
    }
    public static class MyVideoCodecWrapper {
        public MediaCodec mediaCodec;
        public Surface encoderSurface;
        public int width;
        public int height;

    }
}
