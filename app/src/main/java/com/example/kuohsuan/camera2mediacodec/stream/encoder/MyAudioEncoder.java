package com.example.kuohsuan.camera2mediacodec.stream.encoder;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.nio.ByteBuffer;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;

/**
 * Created by f9021 on 2017/12/14.
 */

public class MyAudioEncoder extends MyBaseEncoder{
    private final String TAG = this.getClass().getSimpleName();
    private long lastEncodedAudioTimeStamp;
    private AudioRecord audioRecord;
    private int aacSamplePreFrameSize;
    private HandlerThread mHandlerThread = new HandlerThread("CallbackThread");
    private Handler mHandler;
    MyAudioCodecWrapper myMediaCodecWrapper;

    public MyAudioEncoder(long startRecordWhenNs){
        super.startRecordWhenNs = startRecordWhenNs;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public MyAudioCodecWrapper prepareAudioEncoder(AudioRecord _audioRecord , int aacSamplePreFrameSize)  throws Exception {
        if(_audioRecord==null || aacSamplePreFrameSize<=0)
            throw new Exception();

        audioRecord = _audioRecord;
        Log.d(TAG, "audioRecord:" + audioRecord.getAudioFormat() + ",aacSamplePreFrameSize:" + aacSamplePreFrameSize);

        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        MediaFormat audioFormat = new MediaFormat();
        audioFormat.setString(MediaFormat.KEY_MIME, MIMETYPE_AUDIO_AAC);
        //audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE );
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, audioRecord.getSampleRate());//44100
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, audioRecord.getChannelCount());//1(單身道)
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
        MediaCodec codec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
        codec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            codec.setCallback(new AudioEncoderCallback(aacSamplePreFrameSize),mHandler);
        else
            codec.setCallback(new AudioEncoderCallback(aacSamplePreFrameSize));

        //codec.start();

        MyAudioCodecWrapper myMediaCodecWrapper = new MyAudioCodecWrapper();
        myMediaCodecWrapper.mediaCodec = codec;

        super.mediaCodec = codec;

        return myMediaCodecWrapper;

    }
    public MyAudioCodecWrapper getMyMediaCodecWrapper() {
        return myMediaCodecWrapper;
    }



    private class AudioEncoderCallback extends MediaCodec.Callback{
        private final String TAG = this.getClass().getSimpleName();
        private final boolean DEBUG = false;
        private final int aacSamplePreFrameSize;
        private byte[] frameByteData ;
        public AudioEncoderCallback(int _aacSamplePreFrameSize){
            aacSamplePreFrameSize = _aacSamplePreFrameSize;
        }
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
           // Log.d(TAG,"bbb_a_onInputBufferAvailable:" + Thread.currentThread().getId());
            try {
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                inputBuffer.clear();
                long presentationTimeNs = System.nanoTime();
                int inputLength =  audioRecord.read(inputBuffer, aacSamplePreFrameSize );

                //Log.d(TAG,"presentationTimeNs:"+presentationTimeNs+ ", audioRecordLength:"+(inputLength / SAMPLE_RATE ) / 1000000000);
                presentationTimeNs -= (inputLength / aacSamplePreFrameSize ) / 1000000000;
                if(inputLength == AudioRecord.ERROR_INVALID_OPERATION)
                    Log.e(TAG, "Audio read error");

                long presentationTimeUs = (presentationTimeNs - startRecordWhenNs) / 1000;
                //long presentationTimeUs = presentationTimeNs  / 1000;
                if (DEBUG) Log.i(TAG, "queueing " + inputLength + " audio bytes with pts " + presentationTimeUs);
//                if (endOfStream) {
//                    Log.i(TAG, "EOS received in sendAudioToEncoder");
//                    audioEncoder.queueInputBuffer(inputBufferIndex, 0, inputLength, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                    eosSentToAudioEncoder = true;
//                } else {
//                    audioEncoder.queueInputBuffer(inputBufferIndex, 0, inputLength, presentationTimeUs, 0);
//                }


                codec.queueInputBuffer(index, 0, inputLength, presentationTimeUs, 0);



            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo bufferInfo) {
            //Log.d(TAG,"bbb_a_onOutputBufferAvailable:" + Thread.currentThread().getId());
            try{
                ByteBuffer outPutByteBuffer = codec.getOutputBuffer(index);
                outPutByteBuffer.position(bufferInfo.offset);
                outPutByteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                int remainingSize = outPutByteBuffer.remaining();
                int capacitySize = outPutByteBuffer.capacity();
                if(frameByteData==null || frameByteData.length < remainingSize){
                    frameByteData = new byte[capacitySize];
                    Log.d(TAG,"new byte , size is:"+capacitySize);
                }
                outPutByteBuffer.position(bufferInfo.offset);
                outPutByteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                outPutByteBuffer.get(frameByteData, 0,  remainingSize);

                if(bufferInfo.presentationTimeUs < lastEncodedAudioTimeStamp)
                    bufferInfo.presentationTimeUs = lastEncodedAudioTimeStamp += 23219; // Magical AAC encoded frame time
                lastEncodedAudioTimeStamp = bufferInfo.presentationTimeUs;
                if(bufferInfo.presentationTimeUs < 0){
                    bufferInfo.presentationTimeUs = 0;
                }
            /* 2018/01/03 Y3 :因為沒有錄影需求, 所以先關閉 這段code
            StreamData.StreamDataType enumDataType = StreamData.StreamDataType.AUDIO;
            StreamData streamData = new StreamData(outPutByteBuffer, bufferInfo, enumDataType, -1);
            StreamDataHandler streamDataHandler = StreamDataHandler.getInstance();
            streamDataHandler.pushData(streamData);
*/

                if(audioRecord!=null) {
                    int audioFormatInt = audioRecord.getAudioFormat();
                    int channelCount = audioRecord.getChannelCount();
                    int sampleRate = audioRecord.getSampleRate();
                    int sampleBit = 16;
                    if (AudioFormat.ENCODING_PCM_16BIT == audioFormatInt) {
                        sampleBit = 16;
                    } else if (AudioFormat.ENCODING_PCM_8BIT == audioFormatInt) {
                        sampleBit = 8;
                    }

                    if (MyAudioEncoder.this.myEncoderCallBackFunction != null)
                        MyAudioEncoder.this.myEncoderCallBackFunction.encodeAudioSuccess(frameByteData, bufferInfo.size ,  channelCount, sampleBit , sampleRate);
                }

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
            Log.d(TAG, "Audio_onOutputFormatChanged: " + format);
//            MuxerManagement.getInstance().setAudioFormat(format);
            if(MyAudioEncoder.this.myEncoderCallBackFunction!=null)
                MyAudioEncoder.this.myEncoderCallBackFunction.outputFormatChanged(GEO_ENCODER_TYPE.AUDIO ,format );
        }

    }



}
