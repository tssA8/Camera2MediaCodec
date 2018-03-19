package com.example.kuohsuan.camera2mediacodec;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SurfaceTextureCamera2Activity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener {
    HandlerThread mThreadHandler;
    Handler mHandler, mainHandler;
    TextureView mTextureView;
    private Size mPreviewSize;
    CameraCaptureSession mSession;
    CaptureRequest.Builder mPreviewBuilder;
    ImageView iv_show;
    CameraDevice mCameraDevice;
    ImageReader mImageReader;
    Surface mEncoderSurface;
    BufferedOutputStream outputStream;
    private MediaCodec mCodec, mAudioEncoder,mCodec2;
    boolean isEncode = false;

    //codec2
    Surface mEncoderSurface2;
    BufferedOutputStream outputStream2;
    boolean isEncode2 = false;
    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;


    private String TAG = "SurfaceTexture";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int WRITE_EXTERNAL_STORAGE = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private Handler mBackgroundHandler;//如果沒寫的話,就會用當下的thread
    long startWhen;

    // Audio
    public static final int SAMPLE_RATE = 44100;
    public static final int SAMPLES_PER_FRAME = 1024; // AAC
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord;

    //Media Record
    Surface mEncoderSurfaceAudio;
    MediaRecorder mMediaRecorder;
    private long lastEncodedAudioTimeStamp = 0;
    boolean isRecording = false;
    private long audioStartRecordWhen;
    private static String OUTPUT_DIR = "/sdcard/temp/videorecord/";

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    ///为了使照片竖直显示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThreadSavingImage() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_texture_camera2);

        try {
            audioRecord = setupAudioRecord();
            prepareAudioEncoder(audioRecord);
            //開始錄音
            startAudioRecord(audioRecord);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mThreadHandler = new HandlerThread("CAMERA2");
        mThreadHandler.start();
        mHandler = new Handler(mThreadHandler.getLooper());
        mainHandler = new Handler(getMainLooper());
        mTextureView = (TextureView) findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);

        Button btn = (Button) findViewById(R.id.takePhoto);
        btn.setOnClickListener(this);
        iv_show = (ImageView) findViewById(R.id.show_photo);

        // For still image captures, we use the largest available size.
        mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG,1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() { //可以在这里处理拍照得到的临时照片 例如，写入本地
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.i(TAG, "onImageAvailable");
                // mCameraDevice.close();
                //mTextureView.setVisibility(View.GONE);
                iv_show.setVisibility(View.VISIBLE);
                // 拿到拍照照片数据
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);//由缓冲区存入字节数组
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    iv_show.setImageBitmap(bitmap);
                    image.close();
                }
            }
        }, mainHandler);


    }


    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        takePicture();
    }

    private void closePreviewSession() {
        if (mSession != null) {
            mSession.close();
            mSession = null;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startCodec(int width, int height, int bitRate) {
//        String backupDir = Environment.getExternalStorageDirectory() + Constants.SDACRD_DIR_DB_BACKUP_DIR;
//        mkDirs(backupDir);
        startWhen = System.nanoTime();
        File f = new File(Environment.getExternalStorageDirectory(), "camera2mediacodec0.264");
        if(!f.exists()){
            try {
                f.createNewFile();
                Log.e(TAG, "create a file");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else{
            if(f.delete()){
                try {
                    f.createNewFile();
                    Log.e(TAG, "delete and create a file");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(f));
            Log.i("Encoder", "outputStream initialized");
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            mCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);//create encoder
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,width,height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);//500kbps
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        //ediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface); //COLOR_FormatSurface
        //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
        //      MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);  //COLOR_FormatYUV420Planar
        //mediaFormat.setInteger(MediaFormat.KEY_ROTATION, 90);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); //锟截硷拷帧锟斤拷锟绞憋拷锟� 锟斤拷位s
        mCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoderSurface = mCodec.createInputSurface();

        //method 1
        mCodec.setCallback(new EncoderCallback());
        mCodec.start();



    }

    private void prepareAudioEncoder(AudioRecord audioRecord)  throws Exception{
        Log.d(TAG, "audioRecord:" + audioRecord.getAudioFormat());


        try {
            mAudioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //1000000
        //audio
        MediaFormat mAudioFormat = new MediaFormat();
        mAudioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mAudioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
        mAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        mAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);


        mAudioEncoder.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        mEncoderSurfaceAudio = mAudioEncoder.createInputSurface();

        mAudioEncoder.setCallback(new EncoderCallbackAudio());
        mAudioEncoder.start();


    }

    public void stopCodec() {
        try {
            if(isEncode) {
                isEncode = false;
            }else {
                mCodec.stop();
                mCodec.release();
                mCodec = null;

                mAudioEncoder.stop();
                mAudioEncoder.release();
                mAudioEncoder = null;

                if(audioRecord!=null)
                    audioRecord.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mCodec = null;
            mAudioEncoder = null;
        }
    }

    private void startAudioRecord(AudioRecord audioRecord){
        if(audioRecord != null) {
            audioStartRecordWhen = System.nanoTime();
            audioRecord.startRecording();
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class EncoderCallback extends MediaCodec.Callback{
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            //
        }
        int serial = 0;
        byte[] frameByteData ;
        byte[] ppsFrameData ;
        int ppsSize = 0;
        boolean isNeedCutFile = false;

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            //ByteBuffer encodedData = codec.getOutputBuffer(index);
            //encodedData.position(info.offset);
            //encodedData.limit(info.offset + info.size);
            Log.d(TAG, "onOutputBufferAvailable, info.size: " + info.size);
            ByteBuffer outPutByteBuffer = mCodec.getOutputBuffer(index);
            byte[] outDate = new byte[info.size];
            outPutByteBuffer.get(outDate);

            try {
                if(outputStream!=null){
                    if(info.size != 0){
                        outPutByteBuffer.position(info.offset);
                        outPutByteBuffer.limit(info.offset + info.size);
//                        if(encoder == mAudioEncoder){
//                            if(info.presentationTimeUs < lastEncodedAudioTimeStamp)
//                                info.presentationTimeUs = lastEncodedAudioTimeStamp += 23219; // Magical AAC encoded frame time
//                            lastEncodedAudioTimeStamp = info.presentationTimeUs;
//                        }
                        if(info.presentationTimeUs < 0){
                            info.presentationTimeUs = 0;
                        }

                        int remainingSize = outPutByteBuffer.remaining();
                        int capacitySize = outPutByteBuffer.capacity();
                        if(remainingSize!=info.size){
                            Log.e(TAG, "remainingSize!=mBufferInfo.size!!!!!");
                        }
                        if(frameByteData==null || frameByteData.length < remainingSize){
                            frameByteData = new byte[capacitySize];
                            Log.d(TAG,"new byte , size is:"+capacitySize);
                        }

                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        outPutByteBuffer.position(info.offset);
                        outPutByteBuffer.limit(info.offset + info.size);
                        outPutByteBuffer.get(frameByteData, 0,  remainingSize);
                        Log.d(TAG," mBufferInfo.offset:"+ info.offset  + ",mBufferInfo.size:" + info.size);

                        boolean isKey;
                        if (info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME)
                            isKey = true;
                        else
                            isKey = false;

                        //把PPS&SPS存下來 ,之後 key_frame 要塞在前面
                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
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
                        //把h264影像丟給 ssvr_lite lib
                        serial++;

                    }
                    outputStream.write(outDate, 0, outDate.length);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mCodec.releaseOutputBuffer(index, false);

        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
//            Log.d(TAG, "Error: " + e);
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
//            Log.d(TAG, "encoder output format changed: " + format);
        }
    }


    private class EncoderCallbackAudio extends MediaCodec.Callback{
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

            try {
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                inputBuffer.clear();
                long presentationTimeNs = System.nanoTime();
                int inputLength =  audioRecord.read(inputBuffer, SAMPLES_PER_FRAME );
                presentationTimeNs -= (inputLength / SAMPLE_RATE ) / 1000000000;
                if(inputLength == AudioRecord.ERROR_INVALID_OPERATION)
                    Log.e(TAG, "Audio read error");

                //long presentationTimeUs = (presentationTimeNs - audioStartRecordWhen) / 1000;
                long presentationTimeUs = (presentationTimeNs - audioStartRecordWhen) / 1000;

                mAudioEncoder.queueInputBuffer(index, 0, inputLength, presentationTimeUs, 0);



            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index, @NonNull MediaCodec.BufferInfo info) {

            Log.d(TAG, "onOutputBufferAvailable AUDIO, info.size: " + info.size);
            ByteBuffer outPutByteBuffer = mAudioEncoder.getOutputBuffer(index);
            byte[] outDate = new byte[info.size];
            outPutByteBuffer.get(outDate);


            outPutByteBuffer.position(info.offset);
            outPutByteBuffer.limit(info.offset + info.size);

            if(info.presentationTimeUs < lastEncodedAudioTimeStamp)
                info.presentationTimeUs = lastEncodedAudioTimeStamp += 23219; // Magical AAC encoded frame time

            lastEncodedAudioTimeStamp = info.presentationTimeUs;

            if(info.presentationTimeUs < 0){
                info.presentationTimeUs = 0;
            }


            mAudioEncoder.releaseOutputBuffer(index, false);

        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, "Error: " + e);
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.d(TAG, "encoder output format changed: " + mediaFormat);
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThreadSavingImage();
    }

    @Override
    public void onPause() {
//        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("ResourceType")
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        //check permission
        String[] permission = {Manifest.permission.CAMERA
                , Manifest.permission.WRITE_EXTERNAL_STORAGE
                , Manifest.permission.READ_EXTERNAL_STORAGE
                , Manifest.permission.RECORD_AUDIO};

        int PERMISSION_ALL = 1;
        if(!hasPermissions(this, permission)){
            ActivityCompat.requestPermissions(this, permission, PERMISSION_ALL);
        }

        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            Log.i(TAG, "onSurfaceTextureAvailable:  width = " + width + ", height = " + height);
            String[] CameraIdList = cameraManager.getCameraIdList();
            //获取可用相机设备列表
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(CameraIdList[0]);
            //在这里可以通过CameraCharacteristics设置相机的功能,当然必须检查是否支持
            characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            //就像这样
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }

            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());

            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, largest);

            int encBitRate = 1000000;// 1000000;      // bps
            startCodec(VIDEO_WIDTH, VIDEO_HEIGHT, encBitRate);

            String cameraId = cameraManager.getCameraIdList()[0];

            cameraManager.openCamera(cameraId, mCameraDeviceStateCallback, mHandler);


        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopCodec();
//        stopCodecNum2();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            try {
//                Log.i(TAG, "       CameraDevice.StateCallback  onOpened            ");
                mCameraDevice = camera;
                startPreview(camera);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            if (null != mCameraDevice) {
                mCameraDevice.close();
                SurfaceTextureCamera2Activity.this.mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {}
    };

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    private void startPreview(CameraDevice camera) throws CameraAccessException {
        if (null == camera ) {
            return;
        }
        closePreviewSession();
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        Log.i(TAG,"startPreview");
        try {
            mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW); //CameraDevice.TEMPLATE_STILL_CAPTURE
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);
        mPreviewBuilder.addTarget(mEncoderSurface);

        List<Surface> surfaces = new ArrayList<>();
        surfaces.add(surface);
        surfaces.add(mEncoderSurface);

        surfaces.add(mImageReader.getSurface());
        camera.createCaptureSession(surfaces
                , new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        try {
                            if (null == mCameraDevice) {
                                return;
                            }
                            Log.d(TAG,"onConfigured"+"123");
                            //session.capture(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler);
                            mSession = cameraCaptureSession;
                            // 自动对焦
                            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            // 打开闪光灯
//                            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                            int rotation = getWindowManager().getDefaultDisplay().getRotation();
                            mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
                            cameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler); //null
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        final Activity activity = SurfaceTextureCamera2Activity.this;
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
                , null);
    }


    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback0 =new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Toast.makeText(SurfaceTextureCamera2Activity.this, "take picture success！", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            Toast.makeText(SurfaceTextureCamera2Activity.this, "take picture failed！", Toast.LENGTH_SHORT).show();
        }
    };



    private AudioRecord setupAudioRecord(){
        int min_buffer_size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        int buffer_size = SAMPLES_PER_FRAME * 10;
        if (buffer_size < min_buffer_size)
            buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,       // source
                SAMPLE_RATE,                         // sample rate, hz
                CHANNEL_CONFIG,                      // channels
                AUDIO_FORMAT,                        // audio format
                buffer_size);
        // buffer size (bytes)
        return audioRecord;
    }


    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }



    private void takePicture() {
        if (mCameraDevice == null) return;
        // 创建拍照需要的CaptureRequest.Builder
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取手机方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //拍照
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mSession.capture(mCaptureRequest, mSessionCaptureCallback0, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mTextureView.isShown()) {
                mTextureView.setVisibility(View.VISIBLE);
                iv_show.setVisibility(View.GONE);
                return true;
            }
            if(iv_show.isShown()) {
                iv_show.setVisibility(View.GONE);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e("aaa", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends android.support.v4.app.DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

        public void show(String fragmentDialog) {

        }
    }


}