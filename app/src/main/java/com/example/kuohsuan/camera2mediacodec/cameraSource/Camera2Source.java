package com.example.kuohsuan.camera2mediacodec.cameraSource;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;

import com.example.kuohsuan.camera2mediacodec.Interface.ICameraAction;
import com.example.kuohsuan.camera2mediacodec.Interface.IYuvDataCallback;
import com.example.kuohsuan.camera2mediacodec.View.CameraPreviewTextureView;
import com.example.kuohsuan.camera2mediacodec.bean.BestPictureSizeResultBean;
import com.example.kuohsuan.camera2mediacodec.bean.CameraSourceSizeResultBean;
import com.example.kuohsuan.camera2mediacodec.bean.PictureInfoBean;
import com.example.kuohsuan.camera2mediacodec.bean.QuarterNV21Bean;
import com.example.kuohsuan.camera2mediacodec.bean.QuarterNV21ResultBean;
import com.example.kuohsuan.camera2mediacodec.bean.StartCameraSourceBean;
import com.example.kuohsuan.camera2mediacodec.util.ScreenUtil;
import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import java.io.IOException;
import java.lang.Thread.State;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import static android.os.Looper.getMainLooper;

/**
 * Camera2Source: Created by Ezequiel Adrian Minniti. Buenos Aires.
 *
 * This work is the evolution of the original CameraSource from GoogleSamples.
 * Made by ♥ for the community. You are free to use it anywhere.
 * Just show my name on the credits :)
 *
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Source implements ICameraAction {
    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;
    private int mFacing = CAMERA_FACING_BACK;

    public static final int CAMERA_FLASH_OFF = CaptureRequest.CONTROL_AE_MODE_OFF;
    public static final int CAMERA_FLASH_ON = CaptureRequest.CONTROL_AE_MODE_ON;
    public static final int CAMERA_FLASH_AUTO = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
    public static final int CAMERA_FLASH_ALWAYS = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
    public static final int CAMERA_FLASH_REDEYE = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
    private int mFlashMode = CAMERA_FLASH_AUTO;

    public static final int CAMERA_AF_AUTO = CaptureRequest.CONTROL_AF_MODE_AUTO;
    public static final int CAMERA_AF_EDOF = CaptureRequest.CONTROL_AF_MODE_EDOF;
    public static final int CAMERA_AF_MACRO = CaptureRequest.CONTROL_AF_MODE_MACRO;
    public static final int CAMERA_AF_OFF = CaptureRequest.CONTROL_AF_MODE_OFF;
    public static final int CAMERA_AF_CONTINUOUS_PICTURE = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
    public static final int CAMERA_AF_CONTINUOUS_VIDEO = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
    //    private int mFocusMode = CAMERA_AF_AUTO;
    private int mFocusMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;

    private static final String TAG = "Camera2Source";
    private static final double ratioTolerance = 0.1;
    private static final double maxRatioTolerance = 0.18;
    private Context mContext;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private boolean cameraStarted = false;
    private int mSensorOrientation;
    private Handler zbarHandler;


    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;


    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler,mainHandler;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    private int mDisplayOrientation;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link Size} of Media Recorder.
     */
    private Size mVideoSize;

    private MediaRecorder mMediaRecorder;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    private String videoFile;
    private VideoStartCallback videoStartCallback;
    private VideoStopCallback videoStopCallback;
    private VideoErrorCallback videoErrorCallback;

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private CameraPreviewTextureView mTextureView;
    private SurfaceTexture previewSurfaceTexture;
    private SurfaceTexture offScreenSurfaceTexture;
    private int streamingResolutionH;
    private int streamingResolutionW;

    private ShutterCallback mShutterCallback;

    private AutoFocusCallback mAutoFocusCallback;

    private IYuvDataCallback mIYuvDataCallback;

    private Rect sensorArraySize;
    private boolean isMeteringAreaAFSupported = false;
    private boolean swappedDimensions = false;

    private CameraManager manager = null;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
//    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private Thread mProcessingThread;
    private FrameProcessingRunnable mFrameProcessor;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReaderStill;

    /**
     * An {@link ImageReader} that handles live preview.
     */
    private ImageReader mImageReaderPreview;

//    private ImageReader imageReaderZbar;

//    private CameraScanAnalysis cameraScanAnalysis;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
//            switch (mState) {
//                case STATE_PREVIEW: {
//                    // We have nothing to do when the camera preview is working normally.
//                    break;
//                }
//                case STATE_WAITING_LOCK: {
//                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
//                    if (afState == null) {
//                        captureStillPicture();
//                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState
//                            || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
//                            || CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED == afState
//                            || CaptureRequest.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState
//                            || CaptureRequest.CONTROL_AF_STATE_INACTIVE == afState) {
//                        // CONTROL_AE_STATE can be null on some devices
//                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
//                            mState = STATE_PICTURE_TAKEN;
//                            captureStillPicture();
//                        } else {
//                            runPrecaptureSequence();
//                        }
//                    }
//                    break;
//                }
//                case STATE_WAITING_PRECAPTURE: {
//                    // CONTROL_AE_STATE can be null on some devices
//                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                    if (aeState == null ||
//                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
//                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
//                        mState = STATE_WAITING_NON_PRECAPTURE;
//                    }
//                    break;
//                }
//                case STATE_WAITING_NON_PRECAPTURE: {
//                    // CONTROL_AE_STATE can be null on some devices
//                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
//                        mState = STATE_PICTURE_TAKEN;
//                        captureStillPicture();
//                    }
//                    break;
//                }
//            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
//            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
//            if(request.getTag() == ("FOCUS_TAG")) {
//                //The focus trigger is complete!
//                //Resume repeating request, clear AF trigger.
//                mAutoFocusCallback.onAutoFocus(true);
//                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
//                mPreviewRequestBuilder.setTag("");
//                mPreviewRequest = mPreviewRequestBuilder.build();
//                try {
//                    mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
//                } catch(CameraAccessException ex) {
//                    Log.d(TAG, "AUTO FOCUS FAILURE: "+ex);
//                }
//            } else {
//                process(result);
//            }
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
//            if(request.getTag() == "FOCUS_TAG") {
//                Log.d(TAG, "Manual AF failure: "+failure);
//                mAutoFocusCallback.onAutoFocus(false);
//            }
        }

    };

    /**
     * This is a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * preview frame is ready to be processed.
     */
    private final ImageReader.OnImageAvailableListener mOnPreviewAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {

//            Log.d(TAG,"AAA_OnImageAvailableListener mOnPreviewAvailableListener "+" Thread id : "+Thread.currentThread().getId());

            final Image mImage = reader.acquireNextImage();

            if(mImage == null) {
                return;
            }
////            Log.d(TAG,"AAA_mImage.getWidth() : "+mImage.getWidth()+" mImage.getHeight() : "+mImage.getHeight());
            if(mIYuvDataCallback !=null ){
//                zbarProcessor(mImage.getWidth(),mImage.getHeight(),convertYUV420888ToNV21Grey(mImage));
                mIYuvDataCallback.getYuv(convertYUV420888ToNV21Grey(mImage),mImage.getWidth(),mImage.getHeight());
            }
            mImage.close();
            //這邊如果要兩個都用會壞掉！！！ 所以只能擇一 mIYuvDataCallback or mFrameProcessor
//            if(mFrameProcessor!=null) {
//                mFrameProcessor.setNextFrame(convertYUV420888ToNV21(mImage));
//                mImage.close();
//            }
        }
    };


    /**
     * This is a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private PictureDoneCallback mOnImageAvailableListener = new PictureDoneCallback();

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
//            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
//            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
//            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
    };



    //==============================================================================================
    // Builder
    //==============================================================================================

    /**
     * Builder for configuring and creating an associated camera source.
     */
    public static class Builder {
        private final Detector<?> mDetector;
        private Camera2Source mCameraSource = new Camera2Source();


        /**
         * Creates a camera source builder with the supplied context and detector.  Camera preview
         * images will be streamed to the associated detector upon starting the camera source.
         */
        public Builder(Context context, Detector<?> detector) {
            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }
            if (detector == null) {
                throw new IllegalArgumentException("No detector supplied.");
            }

            mDetector = detector;
            mCameraSource.mContext = context;
        }
        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }

            mDetector = null;
            mCameraSource.mContext = context;
        }
        public Builder setFocusMode(int mode) {
            mCameraSource.mFocusMode = mode;
            return this;
        }

        public Builder setFlashMode(int mode) {
            mCameraSource.mFlashMode = mode;
            return this;
        }

        /**
         * Sets the camera to use (either {@link #CAMERA_FACING_BACK} or
         * {@link #CAMERA_FACING_FRONT}). Default: back facing.
         */
        public Builder setFacing(int facing) {
            if ((facing != CAMERA_FACING_BACK) && (facing != CAMERA_FACING_FRONT)) {
                throw new IllegalArgumentException("Invalid camera: " + facing);
            }
            mCameraSource.mFacing = facing;
            return this;
        }

        public Builder setZbarHandler(Handler handler){
            mCameraSource.zbarHandler = handler;
            return this;
        }

        public Builder setYuvCallBack(){
            mCameraSource.mIYuvDataCallback = (IYuvDataCallback)mCameraSource.mContext;
            return this;
        }


        /**
         * Creates an instance of the camera source.
         */
        public Camera2Source build() {
            if(mDetector!=null){
                mCameraSource.mFrameProcessor = mCameraSource.new FrameProcessingRunnable(mDetector);
            }
            return mCameraSource;
        }
    }

    //==============================================================================================
    // Bridge Functionality for the Camera2 API
    //==============================================================================================

    /**
     * Callback interface used to signal the moment of actual image capture.
     */
    public interface ShutterCallback {
        /**
         * Called as near as possible to the moment when a photo is captured from the sensor. This
         * is a good opportunity to play a shutter sound or give other feedback of camera operation.
         * This may be some time after the photo was triggered, but some time before the actual data
         * is available.
         */
        void onShutter();
    }

    /**
     * Callback interface used to supply image data from a photo capture.
     */
    public interface PictureCallback {
        /**
         * Called when image data is available after a picture is taken.  The format of the data
         * is a JPEG Image.
         */
        void onPictureTaken(Image image);
    }

    /**
     * Callback interface used to indicate when video Recording Started.
     */
    public interface VideoStartCallback {
        void onVideoStart();
    }
    public interface VideoStopCallback {
        //Called when Video Recording stopped.
        void onVideoStop(String videoFile);
    }
    public interface VideoErrorCallback {
        //Called when error ocurred while recording video.
        void onVideoError(String error);
    }

    /**
     * Callback interface used to notify on completion of camera auto focus.
     */
    public interface AutoFocusCallback {
        /**
         * Called when the camera auto focus completes.  If the camera
         * does not support auto-focus and autoFocus is called,
         * onAutoFocus will be called immediately with a fake value of
         * <code>success</code> set to <code>true</code>.
         * <p/>
         * The auto-focus routine does not lock auto-exposure and auto-white
         * balance after it completes.
         *
         * @param success true if focus was successful, false if otherwise
         */
        void onAutoFocus(boolean success);
    }


    //==============================================================================================
    // Public
    //==============================================================================================

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mainHandler = new Handler(getMainLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        try {
            if(mBackgroundThread != null) {
                mBackgroundThread.quitSafely();
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            }
            if(mainHandler!=null)
                mainHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the camera and releases the resources of the camera and underlying detector.
     */
    public void release() {
        if(mFrameProcessor!=null) {
            mFrameProcessor.release();
        }
        stopCameraSource();
    }

    /**
     * Closes the camera and stops sending frames to the underlying frame detector.
     * <p/>
     * This camera source may be restarted again by calling {@link #start}.
     * <p/>
     * Call {@link #release()} instead to completely shut down this camera source and release the
     * resources of the underlying detector.
     */
//    public void stop() {
//        //TODO:之後要用Factory實作
//        stopCameraSource();
////        try {
////            if(mFrameProcessor!=null) {
////                mFrameProcessor.setActive(false);
////                if (mProcessingThread != null) {
////                    try {
////                        // Wait for the thread to complete to ensure that we can't have multiple threads
////                        // executing at the same time (i.e., which would happen if we called start too
////                        // quickly after stop).
////                        mProcessingThread.join();
////                    } catch (InterruptedException e) {
////                        Log.d(TAG, "Frame processing thread interrupted on release.");
////                    }
////                    mProcessingThread = null;
////                }
////            }
////            mCameraOpenCloseLock.acquire();
////            if (null != mCaptureSession) {
////                mCaptureSession.close();
////                mCaptureSession = null;
////            }
////            if (null != mCameraDevice) {
////                mCameraDevice.close();
////                mCameraDevice = null;
////            }
////            if (null != mImageReaderPreview) {
////                mImageReaderPreview.close();
////                mImageReaderPreview = null;
////            }
////            if (null != mImageReaderStill) {
////                mImageReaderStill.close();
////                mImageReaderStill = null;
////            }
////        } catch (InterruptedException e) {
////            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
////        } finally {
////            mCameraOpenCloseLock.release();
////            stopBackgroundThread();
////        }
//    }

    @Override
    public void startCameraSource(StartCameraSourceBean startCameraSourceBean) {

        if(startCameraSourceBean!=null){
            StartCameraSourceBean.Camera2SourceBean camera2SourceBean= startCameraSourceBean.getCamera2SourceBean();
            if(camera2SourceBean!=null){

                mDisplayOrientation = camera2SourceBean.getDisplayOrientation();
                if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    if (cameraStarted) {
//                return this;
                        Log.d(TAG,"startCameraSource cameraStarted");
                    }
                    cameraStarted = true;
                    startBackgroundThread();

                    if(mFrameProcessor!=null) {
                        mProcessingThread = new Thread(mFrameProcessor);
                        mFrameProcessor.setActive(true);
                        mProcessingThread.start();
                    }

                    mTextureView = camera2SourceBean.getTextureView();
                    previewSurfaceTexture = camera2SourceBean.getPreviewSurfaceTexture();
                    offScreenSurfaceTexture = camera2SourceBean.get_offScreenSurfaceTexture();
                    streamingResolutionH = camera2SourceBean.getStreamingResolutionH();
                    streamingResolutionW = camera2SourceBean.getStreamingResolutionW();
                    if (mTextureView.isAvailable()) {
                        setUpCameraOutputs(mTextureView.getWidth(), mTextureView.getHeight());
                    }
                }
            }
        }


    }

    @Override
    public void stopCameraSource() {

        if(mFrameProcessor!=null) {
            mFrameProcessor.setActive(false);
            if (mProcessingThread != null) {
                try {
                    // Wait for the thread to complete to ensure that we can't have multiple threads
                    // executing at the same time (i.e., which would happen if we called start too
                    // quickly after stop).
                    mProcessingThread.join();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Frame processing thread interrupted on release.");
                }
                mProcessingThread = null;
            }
        }
//            mCameraOpenCloseLock.acquire();
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReaderPreview) {
            mImageReaderPreview.close();
            mImageReaderPreview = null;
        }
        if (null != mImageReaderStill) {
            mImageReaderStill.close();
            mImageReaderStill = null;
        }

        stopBackgroundThread();

    }

    @Override
    public void autoFocus() {

    }

    @Override
    public void stopVideoCameraSource() {
        //Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        videoStopCallback.onVideoStop(videoFile);
        closePreviewSession();
        createCameraPreviewSession();
    }

    @Override
    public void recordVideoCameraSource() {

    }

    @Override
    public QuarterNV21ResultBean quarterNV21CameraSource(QuarterNV21Bean bean) {
        QuarterNV21ResultBean resultBean = new QuarterNV21ResultBean();
        if(bean==null){
            return null;
        }else {
            int iWidth = bean.getImageWidth();
            int iHeight = bean.getImageHeight();
            byte[] data = bean.getCamera2Source();

            // Reduce to quarter size the NV21 frame
            byte[] yuv = new byte[iWidth / 4 * iHeight / 4 * 3 / 2];
            // halve yuma
            int i = 0;
            for (int y = 0; y < iHeight; y += 4) {
                for (int x = 0; x < iWidth; x += 4) {
                    yuv[i] = data[y * iWidth + x];
                    i++;
                }
            }
            // halve U and V color components
        /*
        for (int y = 0; y < iHeight / 2; y+=4) {
            for (int x = 0; x < iWidth; x += 8) {
                yuv[i] = data[(iWidth * iHeight) + (y * iWidth) + x];
                i++;
                yuv[i] = data[(iWidth * iHeight) + (y * iWidth) + (x + 1)];
                i++;
            }
        }*/
            resultBean.setCamera2ResultBean(yuv);
            return resultBean;
        }
    }

    @Override
    public BestPictureSizeResultBean getBestAspectPictureSizes(PictureInfoBean bean) {
        float targetRatio = ScreenUtil.getScreenRatio(mContext);
        BestPictureSizeResultBean bestPictureSizeResultBean = new BestPictureSizeResultBean();
        Size bestSize = null;
        TreeMap<Double, List<android.util.Size>> diffs = new TreeMap<>();
        android.util.Size[] supportedPictureSizes = null;
        if(bean!=null){
            supportedPictureSizes = bean.getCamera2Data().getSize();
        }
        if(supportedPictureSizes==null){
            return null;
        }
        //Select supported sizes which ratio is less than ratioTolerance
        for (android.util.Size size : supportedPictureSizes) {
            float ratio = (float) size.getWidth() / size.getHeight();
            double diff = Math.abs(ratio - targetRatio);
            if (diff < ratioTolerance){
                if (diffs.keySet().contains(diff)){
                    //add the value to the list
                    diffs.get(diff).add(size);
                } else {
                    List<android.util.Size> newList = new ArrayList<>();
                    newList.add(size);
                    diffs.put(diff, newList);
                }
            }
        }

        //If no sizes were supported, (strange situation) establish a higher ratioTolerance
        if(diffs.isEmpty()) {
            for (android.util.Size size : supportedPictureSizes) {
                float ratio = (float)size.getWidth() / size.getHeight();
                double diff = Math.abs(ratio - targetRatio);
                if (diff < maxRatioTolerance){
                    if (diffs.keySet().contains(diff)){
                        //add the value to the list
                        diffs.get(diff).add(size);
                    } else {
                        List<android.util.Size> newList = new ArrayList<>();
                        newList.add(size);
                        diffs.put(diff, newList);
                    }
                }
            }
        }

        //Select the highest resolution from the ratio filtered ones.
        for (Map.Entry entry: diffs.entrySet()){
            List<?> entries = (List) entry.getValue();
            for (int i=0; i<entries.size(); i++) {
                android.util.Size s = (android.util.Size) entries.get(i);
                if(bestSize == null) {
                    bestSize = new Size(s.getWidth(), s.getHeight());
                } else if(bestSize.getWidth() < s.getWidth() || bestSize.getHeight() < s.getHeight()) {
                    bestSize = new Size(s.getWidth(), s.getHeight());
                }
            }
        }
        bestPictureSizeResultBean.setSizeCamera1(null);
        bestPictureSizeResultBean.setSizeCamera2(bestSize);
        return bestPictureSizeResultBean;
    }

    @Override
    public CameraSourceSizeResultBean getPreviewSizes() {
        CameraSourceSizeResultBean cameraSourceSizeResultBean = new CameraSourceSizeResultBean();
        cameraSourceSizeResultBean.setPreviewSizes(mPreviewSize);
        return cameraSourceSizeResultBean;
    }

    @Override
    public Integer getCameraFacings() {
        return mFacing;
    }

    public boolean isCamera2Native() {
        try {
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {return false;}
            manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            mCameraId = manager.getCameraIdList()[mFacing];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            //CHECK CAMERA HARDWARE LEVEL. IF CAMERA2 IS NOT NATIVELY SUPPORTED, GO BACK TO CAMERA1

            boolean isSupportCamera2 =false;
            isSupportCamera2 = isHardwareLevelSupported(characteristics,CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);


//            Integer deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            Log.d(TAG,"AAA_ 是否支援 INFO_SUPPORTED_HARDWARE_LEVEL : "+isSupportCamera2);

            return isSupportCamera2;
        }
        catch (CameraAccessException ex) {return false;}
        catch (NullPointerException e) {return false;}
        catch (ArrayIndexOutOfBoundsException ez) {
            return false;
        }
    }

    /**
     * https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics.html
     *
     *  LEGACY devices operate in a backwards-compatibility mode for older Android devices, and have very limited capabilities.
     LIMITED devices represent the baseline feature set, and may also include additional capabilities that are subsets of FULL.
     FULL devices additionally support per-frame manual control of sensor, flash, lens and post-processing settings, and image capture at a high rate.
     LEVEL_3 devices additionally support YUV reprocessing and RAW image capture, along with additional output stream configurations.

     * */
    // Returns true if the device supports the required hardware level, or better.
    private  boolean isHardwareLevelSupported(CameraCharacteristics c, int requiredLevel) {
        int deviceLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    /**
     * Opens the camera and starts sending preview frames to the underlying detector.  The supplied
     * texture view is used for the preview so frames can be displayed to the user.
     *
     * @param textureView the surface holder to use for the preview frames
     * @param displayOrientation the display orientation for a non stretched preview
     * @throws IOException if the supplied texture view could not be used as the preview display
     */
//    @RequiresPermission(Manifest.permission.CAMERA)
//    public Camera2Source start(@NonNull CameraPreviewTextureView textureView, @NonNull SurfaceTexture surfaceTexture ,SurfaceTexture _offScreenSurfaceTexture,int displayOrientation) throws IOException {
////        mDisplayOrientation = displayOrientation;
////        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
////            if (cameraStarted) {
////                return this;
////            }
////            cameraStarted = true;
////            startBackgroundThread();
////
////            if(mFrameProcessor!=null) {
////                mProcessingThread = new Thread(mFrameProcessor);
////                mFrameProcessor.setActive(true);
////                mProcessingThread.start();
////            }
////
////            mTextureView = textureView;
////            previewSurfaceTexture = surfaceTexture;
////            offScreenSurfaceTexture = _offScreenSurfaceTexture;
////            if (mTextureView.isAvailable()) {
////                setUpCameraOutputs(mTextureView.getWidth(), mTextureView.getHeight());
////            }
////        }
//        //TODO:之後要用Factory method去實作
//        startCameraSource(textureView,surfaceTexture,_offScreenSurfaceTexture,displayOrientation);
//        return this;
//    }


    /**
     * Returns the preview size that is currently in use by the underlying camera.
     */
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    /**
     * Returns the selected camera; one of {@link #CAMERA_FACING_BACK} or
     * {@link #CAMERA_FACING_FRONT}.
     */
    public int getCameraFacing() {
        return mFacing;
    }

    public void autoFocus(@Nullable AutoFocusCallback cb, MotionEvent pEvent, int screenW, int screenH) {
        if(cb != null) {
            mAutoFocusCallback = cb;
        }
        if(sensorArraySize != null) {
            final int y = (int)pEvent.getX() / screenW * sensorArraySize.height();
            final int x = (int)pEvent.getY() / screenH * sensorArraySize.width();
            final int halfTouchWidth = 150;
            final int halfTouchHeight = 150;
            MeteringRectangle focusAreaTouch = new MeteringRectangle(
                    Math.max(x-halfTouchWidth, 0),
                    Math.max(y-halfTouchHeight, 0),
                    halfTouchWidth*2,
                    halfTouchHeight*2,
                    MeteringRectangle.METERING_WEIGHT_MAX - 1);

            try {
                mCaptureSession.stopRepeating();
                //Cancel any existing AF trigger (repeated touches, etc.)
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);

                //Now add a new AF trigger with focus region
                if(isMeteringAreaAFSupported) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
                }
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                mPreviewRequestBuilder.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview!
                //Then we ask for a single request (not repeating!)
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            } catch(CameraAccessException ex) {
                Log.d("ASD", "AUTO FOCUS EXCEPTION: "+ex);
            }
        }
    }

    /**
     * Initiate a still image capture. The camera preview is suspended
     * while the picture is being taken, but will resume once picture taking is done.
     */
    public void takePicture(ShutterCallback shutter, PictureCallback picCallback) {
        mShutterCallback = shutter;
        mOnImageAvailableListener.mDelegate = picCallback;
        lockFocus();
    }

    public void recordVideo(VideoStartCallback videoStartCallback, VideoStopCallback videoStopCallback, VideoErrorCallback videoErrorCallback) {
        try {
            this.videoStartCallback = videoStartCallback;
            this.videoStopCallback = videoStopCallback;
            this.videoErrorCallback = videoErrorCallback;
            if(mCameraDevice == null || !mTextureView.isAvailable() || mPreviewSize == null){
                this.videoErrorCallback.onVideoError("Camera not ready.");
                return;
            }
            videoFile = Environment.getExternalStorageDirectory() + "/" + formatter.format(new Date()) + ".mp4";
            mMediaRecorder = new MediaRecorder();
            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(videoFile);
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            if(swappedDimensions) {
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(mDisplayOrientation));
            } else {
                mMediaRecorder.setOrientationHint(ORIENTATIONS.get(mDisplayOrientation));
            }
            mMediaRecorder.prepare();
            closePreviewSession();
            createCameraRecordSession();
        } catch(IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
    }

    public void stopVideo() {
        //Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        videoStopCallback.onVideoStop(videoFile);
        closePreviewSession();
        createCameraPreviewSession();
    }



//    private Size getBestAspectPictureSize(android.util.Size[] supportedPictureSizes) {
//        float targetRatio = ScreenUtil.getScreenRatio(mContext);
//        Size bestSize = null;
//        TreeMap<Double, List<android.util.Size>> diffs = new TreeMap<>();
//
//        //Select supported sizes which ratio is less than ratioTolerance
//        for (android.util.Size size : supportedPictureSizes) {
//            float ratio = (float) size.getWidth() / size.getHeight();
//            double diff = Math.abs(ratio - targetRatio);
//            if (diff < ratioTolerance){
//                if (diffs.keySet().contains(diff)){
//                    //add the value to the list
//                    diffs.get(diff).add(size);
//                } else {
//                    List<android.util.Size> newList = new ArrayList<>();
//                    newList.add(size);
//                    diffs.put(diff, newList);
//                }
//            }
//        }
//
//        //If no sizes were supported, (strange situation) establish a higher ratioTolerance
//        if(diffs.isEmpty()) {
//            for (android.util.Size size : supportedPictureSizes) {
//                float ratio = (float)size.getWidth() / size.getHeight();
//                double diff = Math.abs(ratio - targetRatio);
//                if (diff < maxRatioTolerance){
//                    if (diffs.keySet().contains(diff)){
//                        //add the value to the list
//                        diffs.get(diff).add(size);
//                    } else {
//                        List<android.util.Size> newList = new ArrayList<>();
//                        newList.add(size);
//                        diffs.put(diff, newList);
//                    }
//                }
//            }
//        }
//
//        //Select the highest resolution from the ratio filtered ones.
//        for (Map.Entry entry: diffs.entrySet()){
//            List<?> entries = (List) entry.getValue();
//            for (int i=0; i<entries.size(); i++) {
//                android.util.Size s = (android.util.Size) entries.get(i);
//                if(bestSize == null) {
//                    bestSize = new Size(s.getWidth(), s.getHeight());
//                } else if(bestSize.getWidth() < s.getWidth() || bestSize.getHeight() < s.getHeight()) {
//                    bestSize = new Size(s.getWidth(), s.getHeight());
//                }
//            }
//        }
//        return bestSize;
//    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * We choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 16 / 9)
            {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[0];
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = mDisplayOrientation;
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        try {
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {return;}
//            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
//                throw new RuntimeException("Time out waiting to lock camera opening.");
//            }
            if(manager == null) manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            mCameraId = manager.getCameraIdList()[mFacing];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {return;}

            // For still image captures, we use the largest available size.
            PictureInfoBean pictureInfoBean = new PictureInfoBean();
            PictureInfoBean.Camera2Data camera2Data = new PictureInfoBean.Camera2Data();
            camera2Data.setSize(map.getOutputSizes(ImageFormat.JPEG));
            pictureInfoBean.setCamera2Data(camera2Data);
            pictureInfoBean.setCamera1Data(null);
            BestPictureSizeResultBean bestPictureSizeResultBean = getBestAspectPictureSizes(pictureInfoBean);

            Size camera2Size = bestPictureSizeResultBean.getSizeCamera2();
            mImageReaderStill = ImageReader.newInstance(camera2Size.getWidth(), camera2Size.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
            mImageReaderStill.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

            sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            Integer maxAFRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
            if(maxAFRegions != null) {
                isMeteringAreaAFSupported = maxAFRegions >= 1;
            }
            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if(sensorOrientation != null) {
                mSensorOrientation = sensorOrientation;
                switch (mDisplayOrientation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + mDisplayOrientation);
                }
            }

            Point displaySize = new Point(ScreenUtil.getScreenWidth(mContext), ScreenUtil.getScreenHeight(mContext));
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            Size[] outputSizes = ScreenUtil.sizeToSize(map.getOutputSizes(SurfaceTexture.class));
            Size[] outputSizesMediaRecorder = ScreenUtil.sizeToSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(outputSizes, rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, camera2Size);
            mVideoSize = chooseVideoSize(outputSizesMediaRecorder);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = mDisplayOrientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }

            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;

            configureTransform(width, height);

            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Log.d(TAG, "Camera Error: "+e.getMessage());
        }
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mPreviewRequest = mPreviewRequestBuilder.build();
            mCaptureSession.capture(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            if(mFlashSupported) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, mFlashMode);
            }
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            if(mShutterCallback != null) {
                mShutterCallback.onShutter();
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReaderStill.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, mFocusMode);
            if(mFlashSupported) {
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, mFlashMode);
            }

            // Orientation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(mDisplayOrientation));

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closePreviewSession() {
        if(mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private int barId =0 ;
    private void createCameraPreviewSession() {
        try {
            //SurfaceTexture texture = mTextureView.getSurfaceTexture();
            SurfaceTexture previewTexture = this.previewSurfaceTexture;
            SurfaceTexture offscreenTexture = this.offScreenSurfaceTexture;
            assert previewTexture != null;
            assert offscreenTexture != null;
            // We configure the size of default buffer to be the size of camera preview we want.
            previewTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            offScreenSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            mImageReaderPreview = ImageReader.newInstance(streamingResolutionW, streamingResolutionH, ImageFormat.YUV_420_888, 2);
//            mImageReaderPreview = ImageReader.newInstance(largestJpeg.getWidth(), largestJpeg.getHeight(), ImageFormat.YUV_420_888, 1);
            mImageReaderPreview.setOnImageAvailableListener(mOnPreviewAvailableListener, mBackgroundHandler);


            // This is the output Surface we need to start preview.
            Surface previewSurface = new Surface(previewTexture);
//            Surface offScreenSurface = new Surface(offScreenSurfaceTexture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(previewSurface);
//            mPreviewRequestBuilder.addTarget(offScreenSurface);
            mPreviewRequestBuilder.addTarget(mImageReaderPreview.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReaderPreview.getSurface(),previewSurface, mImageReaderStill.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }

                    // When the session is ready, we start displaying the preview.
                    mCaptureSession = cameraCaptureSession;

                    try {
                        // Auto focus should be continuous for camera preview.
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mFocusMode);
                        if(mFlashSupported) {
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, mFlashMode);
                        }

                        // Finally, we start displaying the camera preview.
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {Log.d(TAG, "Camera Configuration failed!");}
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraRecordSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            mImageReaderPreview = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 1);
            mImageReaderPreview.setOnImageAvailableListener(mOnPreviewAvailableListener, mBackgroundHandler);

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImageReaderPreview.getSurface());
            mPreviewRequestBuilder.addTarget(recorderSurface);

            // Start a capture session
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReaderPreview.getSurface(), recorderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // The camera is already closed
                    if (mCameraDevice == null) {
                        return;
                    }

                    // When the session is ready, we start displaying the preview.
                    mCaptureSession = cameraCaptureSession;

                    try {
                        // Auto focus should be continuous for camera preview.
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mFocusMode);
                        if(mFlashSupported) {
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, mFlashMode);
                        }

                        // Finally, we start displaying the camera preview.
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                    //Start recording
                    mMediaRecorder.start();
                    videoStartCallback.onVideoStart();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {Log.d(TAG, "Camera Configuration failed!");}
            }, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * This runnable controls access to the underlying receiver, calling it to process frames when
     * available from the camera.  This is designed to run detection on frames as fast as possible
     * (i.e., without unnecessary context switching or waiting on the next frame).
     * <p/>
     * While detection is running on a frame, new frames may be received from the camera.  As these
     * frames come in, the most recent frame is held onto as pending.  As soon as detection and its
     * associated processing are done for the previous frame, detection on the mostly recently
     * received frame will immediately start on the same thread.
     */
    private class FrameProcessingRunnable implements Runnable {
        private Detector<?> mDetector;
        private long mStartTimeMillis = SystemClock.elapsedRealtime();

        // This lock guards all of the member variables below.
        private final Object mLock = new Object();
        private boolean mActive = true;

        // These pending variables hold the state associated with the new frame awaiting processing.
        private long mPendingTimeMillis;
        private int mPendingFrameId = 0;
        private byte[] mPendingFrameData;

        FrameProcessingRunnable(Detector<?> detector) {
            mDetector = detector;
        }

        /**
         * Releases the underlying receiver.  This is only safe to do after the associated thread
         * has completed, which is managed in camera source's release method above.
         */
        @SuppressLint("Assert")
        void release() {
            assert (mProcessingThread.getState() == State.TERMINATED);
            if(mDetector!=null) {
                mDetector.release();
                mDetector = null;
            }
        }

        /**
         * Marks the runnable as active/not active.  Signals any blocked threads to continue.
         */
        void setActive(boolean active) {
            synchronized (mLock) {
                mActive = active;
                mLock.notifyAll();
            }
        }

        /**
         * Sets the frame data received from the camera.
         */
        void setNextFrame(byte[] data) {
            synchronized (mLock) {
                if (mPendingFrameData != null) {
                    mPendingFrameData = null;
                }

                // Timestamp and frame ID are maintained here, which will give downstream code some
                // idea of the timing of frames received and when frames were dropped along the way.
                mPendingTimeMillis = SystemClock.elapsedRealtime() - mStartTimeMillis;
                mPendingFrameId++;
                mPendingFrameData = data;

                // Notify the processor thread if it is waiting on the next frame (see below).
                mLock.notifyAll();
            }
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
            Frame outputFrame;

            while (true) {
                synchronized (mLock) {
                    while (mActive && (mPendingFrameData == null)) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            mLock.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }

                    if (!mActive) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return;
                    }
                    QuarterNV21Bean quarterNV21Bean = new QuarterNV21Bean();
                    quarterNV21Bean.setCamera1Source(null);
                    quarterNV21Bean.setCamera2Source(mPendingFrameData);
                    quarterNV21Bean.setImageWidth(mPreviewSize.getWidth());
                    quarterNV21Bean.setImageHeight(mPreviewSize.getHeight());


                    outputFrame = new Frame.Builder()
                            .setImageData(ByteBuffer.wrap(quarterNV21CameraSource(quarterNV21Bean).getCamera2ResultBean()), mPreviewSize.getWidth()/4, mPreviewSize.getHeight()/4, ImageFormat.NV21)
//                            .setImageData(ByteBuffer.wrap(mPendingFrameData), mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.NV21)
                            .setId(mPendingFrameId)
                            .setTimestampMillis(mPendingTimeMillis)
                            .setRotation(getDetectorOrientation(mSensorOrientation))
                            .build();

                    // We need to clear mPendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    mPendingFrameData = null;
                }

                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.
                if(mDetector!=null) {
                    try {
                        mDetector.receiveFrame(outputFrame);
                    } catch (Throwable t) {
                        Log.e(TAG, "Exception thrown from receiver.", t);
                    }
                }
            }
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private int getDetectorOrientation(int sensorOrientation) {
        switch (sensorOrientation) {
            case 0:
                return Frame.ROTATION_0;
            case 90:
                return Frame.ROTATION_90;
            case 180:
                return Frame.ROTATION_180;
            case 270:
                return Frame.ROTATION_270;
            case 360:
                return Frame.ROTATION_0;
            default:
                return Frame.ROTATION_90;
        }
    }

    byte[] data=null;
    private byte[] convertYUV420888ToNV21Grey(Image imgYUV420) {
        // Converting YUV_420_888 data to NV21.

        ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
        int buffer0_size = (imgYUV420.getHeight())*(imgYUV420.getWidth());
//        Log.d(TAG," buffer0 SIZE : "+buffer0_size);

        if(data==null)
            data = new byte[buffer0_size ];

        buffer0.get(data, 0, buffer0_size);

        return data;
    }

    private byte[] convertYUV420888ToNV21(Image imgYUV420) {
        // Converting YUV_420_888 data to NV21.
        byte[] data;
        ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = imgYUV420.getPlanes()[2].getBuffer();
        int buffer0_size = buffer0.remaining();
        int buffer2_size = buffer2.remaining();
        data = new byte[buffer0_size + buffer2_size];
        buffer0.get(data, 0, buffer0_size);
        buffer2.get(data, buffer0_size, buffer2_size);
        return data;
    }

//    private byte[] quarterNV21(byte[] data, int iWidth, int iHeight) {
//        // Reduce to quarter size the NV21 frame
//        byte[] yuv = new byte[iWidth/4 * iHeight/4 * 3 / 2];
//        // halve yuma
//        int i = 0;
//        for (int y = 0; y < iHeight; y+=4) {
//            for (int x = 0; x < iWidth; x+=4) {
//                yuv[i] = data[y * iWidth + x];
//                i++;
//            }
//        }
//        // halve U and V color components
//        /*
//        for (int y = 0; y < iHeight / 2; y+=4) {
//            for (int x = 0; x < iWidth; x += 8) {
//                yuv[i] = data[(iWidth * iHeight) + (y * iWidth) + x];
//                i++;
//                yuv[i] = data[(iWidth * iHeight) + (y * iWidth) + (x + 1)];
//                i++;
//            }
//        }*/
//        return yuv;
//    }

    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }


    //https://www.polarxiong.com/archives/Android-YUV_420_888%E7%BC%96%E7%A0%81Image%E8%BD%AC%E6%8D%A2%E4%B8%BAI420%E5%92%8CNV21%E6%A0%BC%E5%BC%8Fbyte%E6%95%B0%E7%BB%84.html
    private static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
//        if (VERBOSE) Log.v(TAG, "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
//            if (VERBOSE) {
//                Log.v(TAG, "pixelStride " + pixelStride);
//                Log.v(TAG, "rowStride " + rowStride);
//                Log.v(TAG, "width " + width);
//                Log.v(TAG, "height " + height);
//                Log.v(TAG, "buffer size " + buffer.remaining());
//            }
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
//            if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }


    private class PictureDoneCallback implements ImageReader.OnImageAvailableListener {
        private PictureCallback mDelegate;

        @Override
        public void onImageAvailable(ImageReader reader) {
            if(mDelegate != null) {
                mDelegate.onPictureTaken(reader.acquireNextImage());
            }
        }

    };
}
