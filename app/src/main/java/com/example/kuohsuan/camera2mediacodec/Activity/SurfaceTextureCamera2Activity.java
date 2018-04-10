
package com.example.kuohsuan.camera2mediacodec.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.chillingvan.canvasgl.textureFilter.HueFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.example.kuohsuan.camera2mediacodec.Constant;
import com.example.kuohsuan.camera2mediacodec.Interface.ICameraAction;
import com.example.kuohsuan.camera2mediacodec.Interface.IYuvDataCallback;
import com.example.kuohsuan.camera2mediacodec.MyGlRenderFilter;
import com.example.kuohsuan.camera2mediacodec.R;
import com.example.kuohsuan.camera2mediacodec.View.CameraPreviewTextureView;
import com.example.kuohsuan.camera2mediacodec.View.EncoderCanvas;
import com.example.kuohsuan.camera2mediacodec.View.IAutoFixView;
import com.example.kuohsuan.camera2mediacodec.bean.StartCameraSourceBean;
import com.example.kuohsuan.camera2mediacodec.cameraSource.Camera2Source;
import com.example.kuohsuan.camera2mediacodec.stream.MuxerManagement;
import com.example.kuohsuan.camera2mediacodec.stream.encoder.MyAudioEncoder;
import com.example.kuohsuan.camera2mediacodec.stream.encoder.MyBaseEncoder;
import com.example.kuohsuan.camera2mediacodec.stream.encoder.MyVideoEncoder;
import com.example.kuohsuan.camera2mediacodec.util.FileUtils;
import com.example.kuohsuan.camera2mediacodec.util.ImageUtil;
import com.example.kuohsuan.camera2mediacodec.util.ScreenUtil;
import com.yanzhenjie.zbar.Config;
import com.yanzhenjie.zbar.ImageScanner;
import com.yanzhenjie.zbar.Symbol;
import com.yanzhenjie.zbar.SymbolSet;
import com.yanzhenjie.zbar.camera.ScanCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class SurfaceTextureCamera2Activity extends AppCompatActivity implements IYuvDataCallback {

    private String TAG = this.getClass().getSimpleName();
    private final boolean IS_EXECUTE_RECORD = false;
    private final boolean DEBUG = false;
    private static final int START_FOR_RESULT = 1;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;
    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private final String OUTPUT_DIR = "/sdcard/temp/videorecord/";

    //view
    private BufferedOutputStream videoStreamOutput;
    private Size stream1Size, stream2Size;
    //    private AutoFitTextureView txv_cameraPreview;
    private MyVideoEncoder myVideoEncoderStream1, myVideoEncoderStream2;
    private MyAudioEncoder myAudioEncoder;
    private AudioRecord audioRecord;

    private CameraPreviewTextureView txv_cameraPreviewWithFilter;
    private SurfaceTexture previewSurfaceTexture;
    private RawTexture previewRawTexture;
    private EglContextWrapper previewEglCtx;

    private Map<TextureView, Boolean> displayViewInitStatusMap = new HashMap<>(); // value: 是否已經被create
    private OrientationEventListener orientationListener;
    private EncoderCanvas offScreenCanvasWithFilter;
    private SurfaceTexture offScreenSurfaceTexture;
    private RawTexture offScreenRawTexture;
    private Size previewSize;
    private String cameraId;

    // DEFAULT CAMERA BEING OPENED
    private boolean usingFrontCamera = false;
    // MUST BE CAREFUL USING THIS VARIABLE.
    // ANY ATTEMPT TO START CAMERA2 ON API < 21 WILL CRASH.
    private boolean useCamera2 = true;
    // CAMERA VERSION ONE DECLARATIONS
//    private CameraSource mCameraSource = null;
    // CAMERA VERSION TWO DECLARATIONS
    private Camera2Source mCamera2Source = null;
    private ICameraAction mCameraDelegate;
    //zBar
    private ImageScanner zbarImageScanner;
    private Handler mZabrHandler;
    private ScanCallback mZbarCallback;

    /**
     * The {@link android.util.Size} of camera preview.
     */

    ///為了使照片豎直顯示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_texture_camera2);
        Log.d(TAG, "AAA_onCreate");
        FileUtils.mkDirs(OUTPUT_DIR);//create folder
        FileUtils.delAllFile(OUTPUT_DIR);
        initViewEncoder();
        ///在這裡才可以正式開啟camera
        initCamera();
        findView();
        setListener();
        initZbarScanner();
        setZabrScanCallback(new ScanCallback() {
            @Override
            public void onScanResult(String qrCodeResult) {

                Log.d(TAG,"A___onScanResult");

//                Log.d(TAG,"ZBAR onScanResult : "+content+" Thread id : "+Thread.currentThread().getId());
                setZbarResult(qrCodeResult);
            }
        });

    }

    private void initCamera(){
        //////////Abstract Factory //只需要知道要請求什麼物件,就可以直接使用;無須擔心實作方法.
        if(usingFrontCamera){
            FrontCamera frontCamera = new FrontCamera();
            frontCamera.buildCamera2();// frontCamera.buildCamera1();

        }else{
            BackCamera backCamera = new BackCamera();
            backCamera.buildCamera2();// backCamera.buildCamera1();
        }

    }
    private void initViewEncoder() {
        long startRecordWhenNs = System.nanoTime();
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            cameraId = cameraIdList[0];
            //獲取可用相機設備列表
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//            Size[] mediaRecorderSize = map.getOutputSizes(MediaRecorder.class);
            Size[] surfaceTextureSize = map.getOutputSizes(SurfaceTexture.class);
            int findIndex = 0;
            for (Size size : surfaceTextureSize) {
                if (size.getWidth() <= 1920) {
                    break;
                }
                findIndex++;
            }
            stream1Size = surfaceTextureSize[findIndex];
            stream1Size = new Size(stream1Size.getHeight(), stream1Size.getWidth());
            // stream1Size = new Size(1440,1920);
            stream2Size = surfaceTextureSize[surfaceTextureSize.length - 1];
            int stream1BitRate = 5 * 1000 * 1000;
//            int stream2BitRate = (int) (0.1 * 1000 * 1000);
            myVideoEncoderStream1 = new MyVideoEncoder(startRecordWhenNs);
            myVideoEncoderStream1.prepareVideoEncoder(stream1Size.getWidth(), stream1Size.getHeight(), stream1BitRate, "camera2mediacodec0.264", true);
            myVideoEncoderStream1.setMyEncoderCallBackFunction(myEncoderCallBackFunction);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            audioRecord = setupAudioRecord();
            startAudioRecord(audioRecord);//開始錄音
            //啟動decoder
            myAudioEncoder = new MyAudioEncoder(startRecordWhenNs);
            myAudioEncoder.prepareAudioEncoder(audioRecord, AUDIO_SAMPLES_PER_FRAME);
            myAudioEncoder.startCodec();
            myAudioEncoder.setMyEncoderCallBackFunction(myEncoderCallBackFunction);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //
        if (IS_EXECUTE_RECORD) {
            MuxerManagement myMuxerManagement = MuxerManagement.getInstance();
            myMuxerManagement.initMuxerInfo(OUTPUT_DIR, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }

        //設定MyGlRenderFilter
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        MyGlRenderFilter.setLogoBitmap(bitmap);
        TextureFilter textureFilter = new HueFilter(180);
        MyGlRenderFilter.setTextureFilter(textureFilter);

    }

    private void findView() {

        txv_cameraPreviewWithFilter = (CameraPreviewTextureView) findViewById(R.id.txv_camera_preview_with_filter);

        orientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (txv_cameraPreviewWithFilter != null && txv_cameraPreviewWithFilter.isAvailable()) {
                    configureTransform(txv_cameraPreviewWithFilter, txv_cameraPreviewWithFilter.getWidth(), txv_cameraPreviewWithFilter.getHeight());
                }

            }
        };

    }

    private void setListener() {

        txv_cameraPreviewWithFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        txv_cameraPreviewWithFilter.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
                Log.d(TAG, "AAA_txv_cameraPreviewWithFilter_onCreate");
                previewEglCtx = eglContext;


            }
        });
        txv_cameraPreviewWithFilter.setOnSurfaceTextureSet(new CameraPreviewTextureView.OnSurfaceTextureSet() {
            @Override
            public void onSet(SurfaceTexture surfaceTexture, RawTexture surfaceTextureRelatedTexture) {
                Log.d(TAG, "AAA_txv_cameraPreviewWithFilter_onSet");
                //set Preview
                previewSurfaceTexture = surfaceTexture;
                previewRawTexture = surfaceTextureRelatedTexture;
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        //Log.d(TAG,"preview_onFrameAvailable");
                        txv_cameraPreviewWithFilter.requestRenderAndWait();
                        if (offScreenCanvasWithFilter != null)
                            offScreenCanvasWithFilter.requestRenderAndWait();
                    }
                });

                configureTransform(txv_cameraPreviewWithFilter, txv_cameraPreviewWithFilter.getWidth(), txv_cameraPreviewWithFilter.getHeight());
                Log.d(TAG,"txv_cameraPreviewWithFilter.getWidth() : "+txv_cameraPreviewWithFilter.getWidth()
                        +" getHeight() : "+txv_cameraPreviewWithFilter.getHeight()  );
                if (useCamera2) {
                    offScreenCanvasWithFilter = new EncoderCanvas(myVideoEncoderStream1.getMyMediaCodecWrapper().width, myVideoEncoderStream1.getMyMediaCodecWrapper().height, myVideoEncoderStream1.getMyMediaCodecWrapper().encoderSurface);
                } else {
                    offScreenCanvasWithFilter = new EncoderCanvas(myVideoEncoderStream1.getMyMediaCodecWrapper().width, myVideoEncoderStream1.getMyMediaCodecWrapper().height, previewEglCtx, myVideoEncoderStream1.getMyMediaCodecWrapper().encoderSurface);
                    offScreenCanvasWithFilter.setSharedTexture(previewRawTexture, previewSurfaceTexture);
                }
                offScreenCanvasWithFilter.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
                    @Override
                    public void onCreate(EglContextWrapper eglContext) {
                        Log.d(TAG, "aaa_offScreenCanvas_oncreate");
                    }
                });
                offScreenCanvasWithFilter.setOnSurfaceTextureSet(new GLSurfaceTextureProducerView.OnSurfaceTextureSet() {
                    @Override
                    public void onSet(SurfaceTexture surfaceTexture, RawTexture surfaceTextureRelatedTexture) {
                        Log.d(TAG, "aaa_offScreenCanvas_onSet");
                        offScreenSurfaceTexture = surfaceTexture;
                        offScreenRawTexture = surfaceTextureRelatedTexture;
                        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                            @Override
                            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                                offScreenCanvasWithFilter.requestRenderAndWait();
                            }
                        });

                        startCameraSource();

                    }
                });

                offScreenCanvasWithFilter.start();
                offScreenCanvasWithFilter.onResume();

            }
        });


    }

    @Override
    public void getYuv(byte[] data,int imageWidth, int imageHeight) {
        com.yanzhenjie.zbar.Image barcode = new com.yanzhenjie.zbar.Image(imageWidth, imageHeight, "Y800");
        barcode.setData(data);
        startZbarScan(barcode, imageWidth,  imageHeight);
        barcode.destroy();

    }

    private void setZbarResult(final String result){
//        Log.d(TAG,"onScanResult: " + result);
        new Thread(new Runnable(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    JSONArray jsonArray = new JSONArray(result);
                    for(int i = 0 ; i< jsonArray.length() ; i++){
                        barId = barId+1;
                        JSONObject scanResultJSONObject = jsonArray.getJSONObject(i);
                        String scanStr = scanResultJSONObject.getString(Constant.ZBAR_BARCODE_SCAN_RESULT);
                        int bound1 = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_BOUND1);
                        int bound2 = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_BOUND2);
                        int bound3 = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_BOUND3);
                        int bound4 = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_BOUND4);
                        int zbarImageH = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_IMAGE_HIGHT);
                        int zbarImageW = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_IMAGE_WEIGHT);
                        Log.d("onScanResult","result : "+scanStr);

                        Bitmap textZBitmap = ImageUtil.drawText(scanStr , 300 , 50 , Color.TRANSPARENT);
                        MyGlRenderFilter.ZBarcodeBean barcodeBean = new MyGlRenderFilter.ZBarcodeBean(scanStr,textZBitmap);
                        MyGlRenderFilter.getZBarcodeBeanMap().put(barId , barcodeBean);
                        barcodeBean.setZbarcodeString(scanStr);

                        Log.d(TAG,"ZZZ_ZBAR bound1 : "+bound1+" bound2 : "+bound2
                                +" bound3 : "+bound3+" bound4 :"+bound4+" scanStr : "+scanStr);

                        barcodeBean.setBound1(bound1);
                        barcodeBean.setBound2(bound2);
                        barcodeBean.setBound3(bound3);
                        barcodeBean.setBound4(bound4);
                        barcodeBean.setImageReaderCreateHeight(zbarImageH);
                        barcodeBean.setImageReaderCreateWeight(zbarImageW);

                        MyGlRenderFilter.setBarIdList(barId);
//                        Log.d(TAG,"A____ZZZ_ZBAR bound1");
                        Constant.start_time = System.currentTimeMillis();

                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "AAA_onStart");
        setFullScreenSticky();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "AAA_onStop");

    }

    @Override
    protected void onResume() {
        super.onResume();
        //// TODO: 2018/1/29 當Setting按下結束連線回來時,要做onActivityResult偷連或是不要讓他跑codec之類的東東會死
        Log.d(TAG, "AAA_onResume");
        if (orientationListener != null && orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        }
        if (txv_cameraPreviewWithFilter != null)
            txv_cameraPreviewWithFilter.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "AAA_onPause");
        if (orientationListener != null) {
            orientationListener.disable();
        }
        if (txv_cameraPreviewWithFilter != null) {
            txv_cameraPreviewWithFilter.onPause();
        }


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "AAA_onDestroy");

        //故意放這裡,才可以當 螢幕啟動休眠時, 背景還可以繼續執行camera preview
        if (offScreenCanvasWithFilter != null) {
            offScreenCanvasWithFilter.onPause();
            offScreenCanvasWithFilter.end();
        }
        stopAudioRecord(audioRecord);
        //stop codec
        if (myVideoEncoderStream1 != null)
            myVideoEncoderStream1.stopCodec();
        if (myVideoEncoderStream2 != null)
            myVideoEncoderStream2.stopCodec();
        if (myAudioEncoder != null)
            myAudioEncoder.stopCodec();

        stopCameraSource();
    }




    protected Matrix mViewMatrix;
    protected Matrix decodeMatrix;
    private void configureTransform(TextureView textureView, int viewWidth, int viewHeight) {
        if (previewRawTexture == null)
            return;

        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            //Log.i(TAG, "onSurfaceTextureAvailable:  width = " + width + ", height = " + height);
            //String[] CameraIdList = cameraManager.getCameraIdList();
            //獲取可用相機設備列表
            String[] cameraIdList = cameraManager.getCameraIdList();
            cameraId = cameraIdList[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // For still image captures, we always use the largest available size.
            //Size largestJpeg = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            Size largestJpeg = ImageUtil.getBestAspectPictureSize(this, map.getOutputSizes(ImageFormat.JPEG));
            // Find the rotation of the device relative to the native device orientation.
            int deviceRotation = getWindowManager().getDefaultDisplay().getRotation();
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);

            // Find the rotation of the device relative to the camera sensor's orientation.
            int totalRotation = sensorToDeviceRotation(characteristics, deviceRotation);
            MyGlRenderFilter.setDeviceRotation(totalRotation);

            // Swap the view dimensions for calculation as needed if they are rotated relative to
            // the sensor.
            boolean swappedDimensions = totalRotation == 90 || totalRotation == 270;
            int rotatedViewWidth = viewWidth;
            int rotatedViewHeight = viewHeight;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedViewWidth = viewHeight;
                rotatedViewHeight = viewWidth;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            // Preview should not be larger than display size and 1080p.
            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            // Find the best preview size for these view dimensions and configured JPEG size.
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedViewWidth, rotatedViewHeight, maxPreviewWidth, maxPreviewHeight,
                    largestJpeg);

            if (swappedDimensions) {
                if (textureView instanceof IAutoFixView) {
                    ((IAutoFixView) textureView).setAspectRatio(
                            previewSize.getHeight(), previewSize.getWidth());
                }
            } else {
                if (textureView instanceof IAutoFixView) {
                    ((IAutoFixView) textureView).setAspectRatio(
                            previewSize.getWidth(), previewSize.getHeight());
                }
            }

            if (swappedDimensions) {
                Size rotatedSize = new Size(previewSize.getHeight(), previewSize.getWidth());
                MyGlRenderFilter.setPreviewSize(rotatedSize);
            } else {
                Size rotatedSize = new Size(previewSize.getWidth(), previewSize.getHeight());
                MyGlRenderFilter.setPreviewSize(rotatedSize);
            }


            int rotation = (characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT) ?
                    (360 + ORIENTATIONS.get(deviceRotation)) % 360 :
                    (360 - ORIENTATIONS.get(deviceRotation)) % 360;

            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();

            // Initially, output stream images from the Camera2 API will be rotated to the native
            // device orientation from the sensor's orientation, and the TextureView will default to
            // scaling these buffers to fill it's view bounds.  If the aspect ratios and relative
            // orientations are correct, this is fine.
            //
            // However, if the device orientation has been rotated relative to its native
            // orientation so that the TextureView's dimensions are swapped relative to the
            // native device orientation, we must do the following to ensure the output stream
            // images are not incorrectly scaled by the TextureView:
            //   - Undo the scale-to-fill from the output buffer's dimensions (i.e. its dimensions
            //     in the native device orientation) to the TextureView's dimension.
            //   - Apply a scale-to-fill from the output buffer's rotated dimensions
            //     (i.e. its dimensions in the current device orientation) to the TextureView's
            //     dimensions.
            //   - Apply the rotation from the native device orientation to the current device
            //     rotation.

            if (Surface.ROTATION_90 == deviceRotation || Surface.ROTATION_270 == deviceRotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) viewHeight / previewSize.getHeight(),
                        (float) viewWidth / previewSize.getWidth());
                matrix.postScale(scale, scale, centerX, centerY);

            }
            matrix.postRotate(rotation, centerX, centerY);
            textureView.setTransform(matrix);


            //for 調整 人臉辨識後的座標
            mViewMatrix = new Matrix();
            Rect mActiveArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            Size pixelSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
            float ratio = Math.max(((float) rotatedViewWidth) / mActiveArray.width(), ((float) rotatedViewHeight) / mActiveArray.height());
            float offsetX = (mActiveArray.width() - (rotatedViewWidth / ratio)) / 2;
            float offsetY = (mActiveArray.height() - (rotatedViewHeight / ratio)) / 2;
            mViewMatrix.postTranslate(-offsetX, -offsetY);
            mViewMatrix.postScale(ratio, ratio);
            mViewMatrix.postRotate(totalRotation);
            if (totalRotation == 90) { //翻到第2象限
                mViewMatrix.postTranslate(rotatedViewHeight, 0);
                //mViewMatrix.postScale(1, 1);
            } else if (totalRotation == 270) {//翻到第4象限
                mViewMatrix.postTranslate(0, rotatedViewWidth);
                //mViewMatrix.postScale(1, 1);
            } else if (totalRotation == 180) {//翻到第3象限
                mViewMatrix.postTranslate(rotatedViewHeight, rotatedViewWidth);
                //mViewMatrix.postScale(1, 1);
            }


            //test for hardware decode streaming canvas
            decodeMatrix = new Matrix();
            float rotatedViewWidths = stream1Size.getWidth();
            float rotatedViewHeights = stream1Size.getHeight();
            float arrayW = mActiveArray.width();
            float arrayH = mActiveArray.height();

            float ratio2 = Math.max(((float) rotatedViewWidths) / arrayW, ((float) rotatedViewHeights) / arrayH);
            float offsetX2 = (arrayW - (rotatedViewWidths / ratio2)) / 2;
            float offsetY2 = (arrayH - (rotatedViewHeights / ratio2)) / 2;
            decodeMatrix.postTranslate(-offsetX2, -offsetY2);
            decodeMatrix.postScale(ratio2, ratio2);
            decodeMatrix.postRotate(totalRotation);
            if (totalRotation == 90) { //翻到第2象限
                decodeMatrix.postTranslate(rotatedViewHeights, 0);//y,x
                //mViewMatrix.postScale(1, 1);
            } else if (totalRotation == 270) {//翻到第4象限
                decodeMatrix.postTranslate(0, rotatedViewWidths);
                //mViewMatrix.postScale(1, 1);
            } else if (totalRotation == 180) {//翻到第3象限
                decodeMatrix.postTranslate(rotatedViewHeights, rotatedViewWidths);
                //mViewMatrix.postScale(1, 1);
            }

//            Log.d(TAG,"AAA_totalRotation : "+totalRotation);

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }


    // Audio
    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_SAMPLES_PER_FRAME = 1024; // AAC
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_BIT_RATE = 64000;
    //private AudioRecord audioRecord;
    private long lastEncodedAudioTimeStamp = 0;


    private AudioRecord setupAudioRecord() {
        int min_buffer_size = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT);
        int buffer_size = AUDIO_SAMPLES_PER_FRAME * 10;
        if (buffer_size < min_buffer_size)
            buffer_size = ((min_buffer_size / AUDIO_SAMPLES_PER_FRAME) + 1) * AUDIO_SAMPLES_PER_FRAME * 2;

        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,       // source
                AUDIO_SAMPLE_RATE,                         // sample rate, hz
                AUDIO_CHANNEL_CONFIG,                      // channels
                AUDIO_FORMAT,                        // audio format
                buffer_size);                        // buffer size (bytes)
        return audioRecord;
    }

    private void stopAudioRecord(AudioRecord audioRecord) {
        if (audioRecord != null)
            audioRecord.stop();
    }

    private void startAudioRecord(AudioRecord audioRecord) {
        if (audioRecord != null) {

            audioRecord.startRecording();
        }
    }




    private int barId =0 ;
    private void startCameraSource() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (useCamera2 && mCameraDelegate != null) {
            int screenRotation = ScreenUtil.getScreenRotation(this);
            //mPreview.start(mCamera2Source, mGraphicOverlay);
            StartCameraSourceBean startCameraSourceBean = new StartCameraSourceBean();
            StartCameraSourceBean.Camera2SourceBean camera2SourceBean = new StartCameraSourceBean.Camera2SourceBean();
            camera2SourceBean.set_offScreenSurfaceTexture(offScreenSurfaceTexture);
            camera2SourceBean.setDisplayOrientation(screenRotation);
            camera2SourceBean.setPreviewSurfaceTexture(previewSurfaceTexture);
            camera2SourceBean.setTextureView(txv_cameraPreviewWithFilter);
            startCameraSourceBean.setCamera2SourceBean(camera2SourceBean);
            startCameraSourceBean.setCamera1SourceBean(null);

            mCameraDelegate.startCameraSource(startCameraSourceBean);

            //myVideoEncoderStream1.setEncoderCanvasInfo(previewEglCtx,previewRawTexture,previewSurfaceTexture);
            myVideoEncoderStream1.startCodec();

        }else{
            initCamera();
            startCameraSource();//recursive call
        }
    }

    private void stopCameraSource() {
        if (mCameraDelegate != null) {
            mCameraDelegate.stopCameraSource();
        }
    }




    private MyBaseEncoder.MyEncoderCallBackFunction  myEncoderCallBackFunction = new MyBaseEncoder.MyEncoderCallBackFunction() {
        @Override
        public void encodeAudioSuccess(byte[] encodeDate, int encodeSize , int channelCount,int sampleBit ,int sampleRate) {

            //Log.d(TAG,"bbb_encodeAudioSuccess:" + Thread.currentThread().getId());


        }

        @Override
        public void encodeVideoSuccess(byte[] encodeDate, int encodeSize, boolean isVideoKeyFrame, int width, int height) {
//            Log.d(TAG,"encodeVideoSuccess:isVideoKeyFrame:"+isVideoKeyFrame + ",w:"+width+",h:"+height);

        }

        @Override
        public void outputFormatChanged(MyBaseEncoder.GEO_ENCODER_TYPE encoderType, MediaFormat format) {
            if(IS_EXECUTE_RECORD) {
                if (MyBaseEncoder.GEO_ENCODER_TYPE.VIDEO.equals(encoderType)) {
                    MuxerManagement.getInstance().setVideoFormat(format);
                } else if (MyBaseEncoder.GEO_ENCODER_TYPE.AUDIO.equals(encoderType)) {
                    MuxerManagement.getInstance().setAudioFormat(format);
                } else {

                }
            }
        }

    };

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
    private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                   int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
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
     * Rotation need to transform from the camera sensor orientation to the device's current
     * orientation.
     *
     * @param c                 the {@link CameraCharacteristics} to query for the camera sensor
     *                          orientation.
     * @param deviceOrientation the current device orientation relative to the native device
     *                          orientation.
     * @return the total rotation from the sensor orientation to the current device orientation.
     */
    private int sensorToDeviceRotation(CameraCharacteristics c, int deviceOrientation) {
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
        // Get device orientation in degrees
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);

        // Reverse device orientation for front-facing cameras
        if (c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            deviceOrientation = -deviceOrientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    //Android 4.4以上才有收起status bar的效果
    //http://www.jcodecraeer.com/a/anzhuokaifa/developer/2014/1117/1997.html
    private void setFullScreenSticky(){
        /**
         * Immersive Full-Screen(全屏沉浸模式) Mode only support android api 19 later.
         * */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }


    private void initZbarScanner(){
        zbarImageScanner = new ImageScanner();
        zbarImageScanner.setConfig(0, Config.X_DENSITY, 3);
        zbarImageScanner.setConfig(0, Config.Y_DENSITY, 3);
        mZabrHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (mZbarCallback != null){
                    mZbarCallback.onScanResult((String) msg.obj);
                }
            }
        };
    }


    private void startZbarScan(com.yanzhenjie.zbar.Image barcode,int imageWidth, int imageHeight){
//            Log.d(TAG,"B_________result");
        long startTime = System.currentTimeMillis();
        int result = zbarImageScanner.scanImage(barcode);
        JSONArray jsonArrayObject = new JSONArray();

        String resultStr = "";
        if (result != 0) {
            SymbolSet symSet = zbarImageScanner.getResults();
            long endTime = System.currentTimeMillis();

            Log.d(TAG,"B_________symSet, spend time:" + (endTime-startTime)+" resultStr : "+resultStr);

            for (Symbol sym : symSet){
                try {
                    JSONObject scanResultJsonObject = new JSONObject();
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_SCAN_RESULT , sym.getData());
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND1 , sym.getBounds()[0]);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND2 , sym.getBounds()[1]);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND3 , sym.getBounds()[2]);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_BOUND4 , sym.getBounds()[3]);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_IMAGE_HIGHT , imageHeight);
                    scanResultJsonObject.put(Constant.ZBAR_BARCODE_IMAGE_WEIGHT , imageWidth);

                    jsonArrayObject.put(scanResultJsonObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                resultStr = resultStr  + " : " + sym.getData();
                int[] bounds = sym.getBounds();
            }

        }

        if (!TextUtils.isEmpty(resultStr) && result != 0) {
            Message message = mZabrHandler.obtainMessage();
            //                Bundle bundle = new Bundle();
            //                message.obj = resultStr;
            message.obj = jsonArrayObject.toString();
            message.sendToTarget();
        }


    }


    private void setZabrScanCallback(ScanCallback callback) {
        this.mZbarCallback = callback;
    }

    //abstract factory
    abstract  class CameraFactory{
//        abstract public Camera1Factory buildCamera1();
        abstract public Camera2Factory buildCamera2();
    }

//    class Camera1Factory{
//        public Camera1Factory(){
//        }
//    }
    class Camera2Factory{
        public Camera2Factory(){
        }
    }

    class FrontCamera extends CameraFactory{

//        @Override
//        public Camera1Factory buildCamera1() {
//            Log.e(TAG,"AAA_use camera1");
//            Camera1Factory  factory = new Camera1Factory();
//            mCameraSource = new CameraSource.Builder(MainActivity.this)
//                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
//                    .setRequestedFps(30.0f)
//                    .setYuvCallBack().build();
//
//            mCameraDelegate = mCameraSource;
//            return factory;
//        }

        @Override
        public Camera2Factory buildCamera2() {
            Camera2Factory  factory = new Camera2Factory();
            mCamera2Source =  new Camera2Source.Builder(SurfaceTextureCamera2Activity.this)
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_FRONT)
                    .setYuvCallBack().build();

            if (mCamera2Source.isCamera2Native()) {
                mCameraDelegate = mCamera2Source;

            }
            return factory;

        }
    }

    class BackCamera extends CameraFactory{

//        @Override
//        public Camera1Factory buildCamera1() {
////            Log.e(TAG,"AAA_use camera1");
////            Camera1Factory  factory = new Camera1Factory();
////            mCameraSource = new CameraSource.Builder(MainActivity.this)
////                    .setFacing(CameraSource.CAMERA_FACING_BACK)
////                    .setRequestedFps(30.0f)
////                    .setYuvCallBack().build();
////
////            mCameraDelegate = mCameraSource;
//
//            return factory;
//        }

        @Override
        public Camera2Factory buildCamera2() {
            Camera2Factory  factory = new Camera2Factory();
            mCamera2Source =  new Camera2Source.Builder(SurfaceTextureCamera2Activity.this)
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_BACK)
                    .setYuvCallBack().build();

            if (mCamera2Source.isCamera2Native()) {
                mCameraDelegate = mCamera2Source;
            }
            return factory;

        }
    }



}


