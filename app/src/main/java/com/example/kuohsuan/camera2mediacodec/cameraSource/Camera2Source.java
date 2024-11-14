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
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseIntArray;
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

import java.lang.Thread.State;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

    private static final String TAG = "aaa_Camera2Source";
    private static final double ratioTolerance = 0.1;
    private static final double maxRatioTolerance = 0.18;
    private Context context;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private boolean cameraStarted = false;
    private int mSensorOrientation;
    private Handler zbarHandler;


    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice cameraDevice;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;


    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler backgroundHandler,mainHandler;

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

    private int displayOrientation;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder previewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #previewRequestBuilder}
     */
    private CaptureRequest previewRequest;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession captureSession;

    /**
     * The {@link Size} of camera preview.
     */
    private Size previewSize;

    /**
     * The {@link Size} of Media Recorder.
     */
    private Size videoSize;

    private MediaRecorder mediaRecorder;
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

    private CameraPreviewTextureView mTextureView;
    private SurfaceTexture previewSurfaceTexture;
    private SurfaceTexture offScreenSurfaceTexture;
    private int imageReaderH;
    private int imageReaderW;
    private int offscreenStreamingH;
    private int offscreenStreamingW;

    private IYuvDataCallback IYuvDataCallback;

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
    private boolean flashSupported;

    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private Thread processingThread;
    private FrameProcessingRunnable frameProcessor;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader imageReaderStill;

    /**
     * An {@link ImageReader} that handles live preview.
     */
    private ImageReader imageReaderPreview;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {

        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
//            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {

        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {

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
            if(IYuvDataCallback !=null ){
//                zbarProcessor(mImage.getWidth(),mImage.getHeight(),convertYUV420888ToNV21Grey(mImage));
                IYuvDataCallback.getYuv(convertYUV420888ToNV21Grey(mImage),mImage.getWidth(),mImage.getHeight());
            }
            mImage.close();
            //這邊如果要兩個都用會壞掉！！！ 所以只能擇一 IYuvDataCallback or frameProcessor
//            if(frameProcessor!=null) {
//                frameProcessor.setNextFrame(convertYUV420888ToNV21(mImage));
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
            Camera2Source.this.cameraDevice = cameraDevice;
            createCameraPreviewSession();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
//            mCameraOpenCloseLock.release();
            cameraDevice.close();
            Camera2Source.this.cameraDevice = null;
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
//            mCameraOpenCloseLock.release();
            cameraDevice.close();
            Camera2Source.this.cameraDevice = null;
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
            mCameraSource.context = context;
        }
        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }

            mDetector = null;
            mCameraSource.context = context;
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
            mCameraSource.IYuvDataCallback = (IYuvDataCallback)mCameraSource.context;
            return this;
        }


        /**
         * Creates an instance of the camera source.
         */
        public Camera2Source build() {
            if(mDetector!=null){
                mCameraSource.frameProcessor = mCameraSource.new FrameProcessingRunnable(mDetector);
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


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        mainHandler = new Handler(getMainLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        try {
            if(backgroundThread != null) {
                backgroundThread.quitSafely();
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
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
        if(frameProcessor !=null) {
            frameProcessor.release();
        }
        stopCameraSource();
    }



    @Override
    public void startCameraSource(StartCameraSourceBean startCameraSourceBean) {

        if(startCameraSourceBean!=null){
            StartCameraSourceBean.Camera2SourceBean camera2SourceBean= startCameraSourceBean.getCamera2SourceBean();
            if(camera2SourceBean!=null){

                displayOrientation = camera2SourceBean.getDisplayOrientation();
                if(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    if (cameraStarted) {
//                return this;
                        Log.d(TAG,"startCameraSource cameraStarted");
                    }
                    cameraStarted = true;
                    startBackgroundThread();

                    if(frameProcessor !=null) {
                        processingThread = new Thread(frameProcessor);
                        frameProcessor.setActive(true);
                        processingThread.start();
                    }

                    mTextureView = camera2SourceBean.getTextureView();
                    previewSurfaceTexture = camera2SourceBean.getPreviewSurfaceTexture();
                    offScreenSurfaceTexture = camera2SourceBean.get_offScreenSurfaceTexture();
                    imageReaderH = camera2SourceBean.getImageReaderH();
                    imageReaderW = camera2SourceBean.getImageReaderW();
                    offscreenStreamingH = camera2SourceBean.getOffScreenResolutionH();
                    offscreenStreamingW = camera2SourceBean.getOffscreenResolutionW();
                    if (mTextureView.isAvailable()) {
                        setUpCameraOutputs(mTextureView.getWidth(), mTextureView.getHeight());
                    }
                }
            }
        }


    }

    @Override
    public void stopCameraSource() {

        if(frameProcessor !=null) {
            frameProcessor.setActive(false);
            if (processingThread != null) {
                try {
                    // Wait for the thread to complete to ensure that we can't have multiple threads
                    // executing at the same time (i.e., which would happen if we called start too
                    // quickly after stop).
                    processingThread.join();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Frame processing thread interrupted on release.");
                }
                processingThread = null;
            }
        }
        if (null != captureSession) {
            captureSession.close();
            captureSession = null;
        }
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReaderPreview) {
            imageReaderPreview.close();
            imageReaderPreview = null;
        }
        if (null != imageReaderStill) {
            imageReaderStill.close();
            imageReaderStill = null;
        }

        stopBackgroundThread();

    }

    @Override
    public void autoFocus() {

    }

    @Override
    public void stopVideoCameraSource() {
        //Stop recording
        mediaRecorder.stop();
        mediaRecorder.reset();
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
            resultBean.setCamera2ResultBean(yuv);
            return resultBean;
        }
    }

    @Override
    public BestPictureSizeResultBean getBestAspectPictureSizes(PictureInfoBean bean) {
        float targetRatio = ScreenUtil.getScreenRatio(context);
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
        cameraSourceSizeResultBean.setPreviewSizes(previewSize);
        return cameraSourceSizeResultBean;
    }

    @Override
    public Integer getCameraFacings() {
        return mFacing;
    }

    public boolean isCamera2Native() {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {return false;}
            manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            mCameraId = manager.getCameraIdList()[mFacing];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            //CHECK CAMERA HARDWARE LEVEL. IF CAMERA2 IS NOT NATIVELY SUPPORTED, GO BACK TO CAMERA1

            boolean isSupportCamera2 =false;
            isSupportCamera2 = isHardwareLevelSupported(characteristics,CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);


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
        if (null == mTextureView || null == previewSize) {
            return;
        }
        int rotation = displayOrientation;
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
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
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {return;}

            if(manager == null) manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
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
            imageReaderStill = ImageReader.newInstance(camera2Size.getWidth(), camera2Size.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
            imageReaderStill.setOnImageAvailableListener(mOnImageAvailableListener, backgroundHandler);

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
                switch (displayOrientation) {
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
                        Log.e(TAG, "Display rotation is invalid: " + displayOrientation);
                }
            }

            Point displaySize = new Point(ScreenUtil.getScreenWidth(context), ScreenUtil.getScreenHeight(context));
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
            previewSize = chooseOptimalSize(outputSizes, rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, camera2Size);
            videoSize = chooseVideoSize(outputSizesMediaRecorder);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = displayOrientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }

            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            flashSupported = available == null ? false : available;

            configureTransform(width, height);

            manager.openCamera(mCameraId, mStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Log.d(TAG, "Camera Error: "+e.getMessage());
        }
    }


    private void closePreviewSession() {
        if(captureSession != null) {
            captureSession.close();
            captureSession = null;
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
            previewTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            offScreenSurfaceTexture.setDefaultBufferSize(offscreenStreamingW, offscreenStreamingH);
            imageReaderPreview = ImageReader.newInstance(imageReaderW, imageReaderH, ImageFormat.YUV_420_888, 2);
//            imageReaderPreview = ImageReader.newInstance(largestJpeg.getWidth(), largestJpeg.getHeight(), ImageFormat.YUV_420_888, 1);
            imageReaderPreview.setOnImageAvailableListener(mOnPreviewAvailableListener, backgroundHandler);


            // This is the output Surface we need to start preview.
            Surface previewSurface = new Surface(previewTexture);
//            Surface offScreenSurface = new Surface(offScreenSurfaceTexture);

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);
//            previewRequestBuilder.addTarget(offScreenSurface);
            previewRequestBuilder.addTarget(imageReaderPreview.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice.createCaptureSession(Arrays.asList(imageReaderPreview.getSurface(),previewSurface, imageReaderStill.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }

                    // When the session is ready, we start displaying the preview.
                    captureSession = cameraCaptureSession;

                    try {
                        // Auto focus should be continuous for camera preview.
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mFocusMode);
                        if(flashSupported) {
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, mFlashMode);
                        }

                        // Finally, we start displaying the camera preview.
                        previewRequest = previewRequestBuilder.build();
                        captureSession.setRepeatingRequest(previewRequest, mCaptureCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {Log.d(TAG, "Camera Configuration failed!");}
            }, backgroundHandler);
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
            assert (processingThread.getState() == State.TERMINATED);
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
                    quarterNV21Bean.setImageWidth(previewSize.getWidth());
                    quarterNV21Bean.setImageHeight(previewSize.getHeight());


                    outputFrame = new Frame.Builder()
                            .setImageData(ByteBuffer.wrap(quarterNV21CameraSource(quarterNV21Bean).getCamera2ResultBean()), previewSize.getWidth()/4, previewSize.getHeight()/4, ImageFormat.NV21)
//                            .setImageData(ByteBuffer.wrap(mPendingFrameData), previewSize.getWidth(), previewSize.getHeight(), ImageFormat.NV21)
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
