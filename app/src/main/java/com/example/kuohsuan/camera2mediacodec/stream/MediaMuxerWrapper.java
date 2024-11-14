package com.example.kuohsuan.camera2mediacodec.stream;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;

/**
 * Created by User on 2016/8/15.
 */
 public class MediaMuxerWrapper {
    private final String TAG = this.getClass().getSimpleName();
    private final boolean VERBOSE = false;           // lots of logging

    String fileName;
    MediaMuxer muxer;
    final int TOTAL_NUM_TRACKS = 2;
    boolean started = false;
    int chunk;
    int numTracksAdded = 0;
    int numTracksFinished = 0;

    Object sync = new Object();

    public MediaMuxerWrapper(String fileName , int format, int chunk){
        this.fileName = fileName;
        this.chunk = chunk;
        restart(format, chunk);
    }

    public int addTrack(MediaFormat format){
        numTracksAdded++;
        int trackIndex = muxer.addTrack(format);
        if(numTracksAdded == TOTAL_NUM_TRACKS){
            //if (VERBOSE) Log.i(TAG, "All tracks added, starting " + ((this == mMuxerWrapper) ? "muxer1" : "muxer2") +"!");

            //Log.i(TAG, "All tracks added, starting " + ((this == mMuxerWrapper) ? "muxer1" : "muxer2") +"!");
            muxer.start();
            started = true;
        }
        return trackIndex;
    }

    public void finishTrack(){
        numTracksFinished++;
        if(numTracksFinished == TOTAL_NUM_TRACKS){
            //if (VERBOSE) Log.i(TAG, "All tracks finished, stopping " + ((this == mMuxerWrapper) ? "muxer1" : "muxer2") + "!");
            long finishTrack_s = System.currentTimeMillis();
            stop();
            long finishTrack_e = System.currentTimeMillis();
            if(VERBOSE)
                Log.d(TAG,"spend_time_input_video_frame:"+(finishTrack_e-finishTrack_s));

        }

    }

    public boolean allTracksAdded(){
        return (numTracksAdded == TOTAL_NUM_TRACKS);
    }

    public boolean allTracksFinished(){
        return (numTracksFinished == TOTAL_NUM_TRACKS);
    }


    public void stop(){
        if(muxer != null){
            if(!allTracksFinished()) Log.e(TAG, "Stopping Muxer before all tracks added!");
            if(!started) Log.e(TAG, "Stopping Muxer before it was started");
            muxer.stop();
            muxer.release();
            muxer = null;
            started = false;
            chunk = 0;
            numTracksAdded = 0;
            numTracksFinished = 0;
        }
    }

    private String outputPathForChunk(int chunk){
        //return OUTPUT_DIR + VIDEO_WIDTH + "x" + VIDEO_HEIGHT + "_" + chunk + ".mp4";
        return fileName  + "_" + chunk + ".mp4";
    }

    public void restart(int format, int chunk){
        stop();
        try {
            muxer = new MediaMuxer(outputPathForChunk(chunk), format);
        } catch (IOException e) {
            throw new RuntimeException("MediaMuxer creation failed", e);
        }
    }
}
