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
import com.example.kuohsuan.camera2mediacodec.Constant;
import com.example.kuohsuan.camera2mediacodec.Interface.ICameraAction;
import com.example.kuohsuan.camera2mediacodec.Interface.IYuvDataCallback;
import com.example.kuohsuan.camera2mediacodec.MyGlRenderFilter;
import com.example.kuohsuan.camera2mediacodec.R;
import com.example.kuohsuan.camera2mediacodec.View.CameraPreviewTextureView;
import com.example.kuohsuan.camera2mediacodec.View.EncoderCanvas;
import com.example.kuohsuan.camera2mediacodec.View.IAutoFixView;
import com.example.kuohsuan.camera2mediacodec.ZbarProcessorRunnable;
import com.example.kuohsuan.camera2mediacodec.bean.StartCameraSourceBean;
import com.example.kuohsuan.camera2mediacodec.bean.ZBarCodeBean;
import com.example.kuohsuan.camera2mediacodec.cameraSource.Camera2Source;
import com.example.kuohsuan.camera2mediacodec.myeventbus.ZbarResultEventBus;
import com.example.kuohsuan.camera2mediacodec.stream.MuxerManagement;
import com.example.kuohsuan.camera2mediacodec.stream.encoder.MyAudioEncoder;
import com.example.kuohsuan.camera2mediacodec.stream.encoder.MyBaseEncoder;
import com.example.kuohsuan.camera2mediacodec.stream.encoder.MyVideoEncoder;
import com.example.kuohsuan.camera2mediacodec.util.Camera2Util;
import com.example.kuohsuan.camera2mediacodec.util.FileUtils;
import com.example.kuohsuan.camera2mediacodec.util.ImageUtil;
import com.example.kuohsuan.camera2mediacodec.util.ScreenUtil;
import com.example.kuohsuan.camera2mediacodec.util.ViewStateUtil;
import com.example.kuohsuan.camera2mediacodec.util.ZbarQueue;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class SurfaceTextureCamera2Activity extends AppCompatActivity implements IYuvDataCallback {

    private String TAG = this.getClass().getSimpleName();
    private final boolean IS_EXECUTE_RECORD = false;
    private final boolean DEBUG = false;
    private static final int START_FOR_RESULT = 1;
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private static final int STREAMING_RESOLUTION = 1920;
    private static final int OFFSCREEN_RESOLUTION = 1920;//offscreen
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private final String OUTPUT_DIR = "/sdcard/temp/videorecord/";

    //decode encode
    private CameraPreviewTextureView txv_cameraPreviewWithFilter;
    private SurfaceTexture previewSurfaceTexture;
    private RawTexture previewRawTexture;
    private SurfaceTexture offScreenSurfaceTexture;
    private Size imageReaderSize, streamOffScreenSize, previewSize;
    private EglContextWrapper previewEglCtx;
    private OrientationEventListener orientationListener;
    private EncoderCanvas offScreenCanvasWithFilter;
    private RawTexture offScreenRawTexture;
    private MyVideoEncoder myVideoEncoderStream1;
    private MyAudioEncoder myAudioEncoder;
    private AudioRecord audioRecord;

    // camera
    private CameraCharacteristics cameraCharacteristics;
    private CameraManager cameraManager;
    private Camera2Source camera2Source = null;
    private ICameraAction cameraAction;
    private boolean isUsingFrontCamera = false;
    private boolean isUseCamera2 = true;
    private String cameraId;

    //zBar handler
//    private ScanCallback zbarCallback;
    private ZbarProcessorRunnable zbarProcessorRunnable;
    //activity state !!!
    private ViewStateUtil viewStateUtil = ViewStateUtil.getInstance();


    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

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

        long startRecordTime = System.nanoTime();

        setContentView(R.layout.activity_surface_texture_camera2);

        Log.d(TAG, "AAA_onCreate");

        createFileFolder();

        ///在這裡才可以正式開啟camera
        initCamera();

        findView();

        setListener();

        getViewSize(STREAMING_RESOLUTION, OFFSCREEN_RESOLUTION);

        initVideoEncoder(startRecordTime);

        initAudioEncoder(startRecordTime);

        setGlRenderBitmap();

        setMuxerSavePath();

    }

    private void createFileFolder() {
        FileUtils.mkDirs(Constant.SDCARD_FILE_PATH_OUTPUT_DIR);

        FileUtils.delAllFile(Constant.SDCARD_FILE_PATH_OUTPUT_DIR);
    }

    private void initCamera() {
        //////////Abstract Factory //只需要知道要請求什麼物件,就可以直接使用;無須擔心實作方法.
        if (isUsingFrontCamera) {
            FrontCamera frontCamera = new FrontCamera();
            frontCamera.buildCamera2();// frontCamera.buildCamera1();

        } else {
            BackCamera backCamera = new BackCamera();
            backCamera.buildCamera2();// backCamera.buildCamera1();
        }
    }

    private CameraCharacteristics getCharacteristics(CameraManager cameraManager) {
        if (cameraCharacteristics == null) {
            try {
                String cameraId = getCameraId(cameraManager);
                cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Camera2Util.setCameraCharacteristics(cameraCharacteristics);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }
        return cameraCharacteristics;
    }


    private void getViewSize(int streamingResolution, int offscreenResolution) {

        CameraManager cameraManager = getCameraManager();

        cameraId = getCameraId(cameraManager);

        //獲取可用相機設備列表
        CameraCharacteristics characteristics = getCharacteristics(cameraManager);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] surfaceTextureSize = map.getOutputSizes(ImageFormat.JPEG);

        int findIndex = 0;
        //find ImageReader Resolution
        for (Size size : surfaceTextureSize) {
            if (size.getWidth() <= streamingResolution) {
                break;
            }
            findIndex++;
        }

        Size size1 = surfaceTextureSize[findIndex];
        imageReaderSize = new Size(size1.getHeight(), size1.getWidth());
        //offscreen Resolution
        Size[] offScreenSize = map.getOutputSizes(SurfaceTexture.class);

        int findIndex2 = 0;
        //find streaming resolution
        for (Size size : offScreenSize) {
            if (size.getWidth() <= offscreenResolution) {
                break;
            }
            findIndex2++;
        }

        Size size2 = offScreenSize[findIndex2];

        streamOffScreenSize = new Size(size2.getHeight(), size2.getWidth());
    }

    private void setGlRenderBitmap() {
        //設定MyGlRenderFilter
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        MyGlRenderFilter.setLogoBitmap(bitmap);
    }


    private void setMuxerSavePath() {
        if (IS_EXECUTE_RECORD) {
            MuxerManagement myMuxerManagement = MuxerManagement.getInstance();
            myMuxerManagement.initMuxerInfo(Constant.SDCARD_FILE_PATH_OUTPUT_DIR, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }
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
//                        if (offScreenCanvasWithFilter != null)
//                            offScreenCanvasWithFilter.requestRenderAndWait();
                    }
                });


                configureTransform(txv_cameraPreviewWithFilter, txv_cameraPreviewWithFilter.getWidth(), txv_cameraPreviewWithFilter.getHeight());
                if (isUseCamera2) {
                    offScreenCanvasWithFilter = new EncoderCanvas(myVideoEncoderStream1.getMyMediaCodecWrapper().width, myVideoEncoderStream1.getMyMediaCodecWrapper().height, myVideoEncoderStream1.getMyMediaCodecWrapper().encoderSurface);

                } else {
                    offScreenCanvasWithFilter = new EncoderCanvas(myVideoEncoderStream1.getMyMediaCodecWrapper().width, myVideoEncoderStream1.getMyMediaCodecWrapper().height, previewEglCtx, myVideoEncoderStream1.getMyMediaCodecWrapper().encoderSurface);
                    offScreenCanvasWithFilter.setSharedTexture(previewRawTexture, previewSurfaceTexture);

                }

                offScreenCanvasWithFilter.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {

                    @Override
                    public void onCreate(EglContextWrapper eglContext) {

                        Log.d(TAG, "aaa_offScreenCanvas_oncreate");
                        previewEglCtx = eglContext;
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

    private CameraManager getCameraManager() {
        if (cameraManager == null) {
            cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            Camera2Util.setCameraManager(cameraManager);
        }
        return cameraManager;
    }


    private String getCameraId(CameraManager cameraManager) {


        if (TextUtils.isEmpty(cameraId)) {

            try {

                String[] cameraIdList = cameraManager.getCameraIdList();

                cameraId = cameraIdList[0];

                Camera2Util.setCameraId(cameraId);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        return cameraId;
    }

    @Subscribe
    public void ZbarResultEventBus(ZbarResultEventBus event) {
        if (event != null) {
            String result = event.getResultData();
            Log.d(TAG, "Zbar Event bus event : " + result + " thread id : " + Thread.currentThread().getId());
            setZbarResult(result);
        }

    }

    @Override
    public void getYuv(byte[] data, int imageWidth, int imageHeight) {


        if (zbarProcessorRunnable == null && imageWidth > 0 && imageHeight > 0) {
            zbarProcessorRunnable = new ZbarProcessorRunnable(
                    imageWidth
                    , imageHeight
                    , previewSize.getWidth()
                    , previewSize.getHeight()
            );
        }

        if (zbarProcessorRunnable != null) {
            zbarProcessorRunnable.setNextFrame(data);
        }

    }

    private void setZbarResult(final String result) {
//        Log.d(TAG,"onScanResult: " + result);
        new Thread(new Runnable() {
            @Override
            public void run() {

                // TODO Auto-generated method stub
                try {
                    ArrayList<String> resultBean = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(result);

                    //last time zbar result
                    ArrayList<String> lastResultBean = getLastTimeZbarResultList();
                    ArrayList<ZBarCodeBean> cloneZBarcodeBeanArrayList = new ArrayList<>();

                    //1 get result list
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject scanResultJSONObject = jsonArray.getJSONObject(i);
                        String scanStr = scanResultJSONObject.getString(Constant.ZBAR_BARCODE_SCAN_RESULT);
                        Log.d("onScanResult", "result : " + scanStr);
                        resultBean.add(scanStr);

                    }
                    setLastTimeZbarList(resultBean);

                    boolean isSame = isTwoArrayListSame(lastResultBean, resultBean);

                    if (isSame) {
                        //NOTHING draw same pic
//                        Log.d(TAG,"AAA_isSame :"+isSame);
                        ArrayList<ZBarCodeBean> list = getZbarBeanList();
                        for (int i = 0; i < list.size(); i++) {
                            barId = barId + 1;
                            String scanStr = list.get(i).getZbarcodeString();
                            int bound1 = list.get(i).getBound1();
                            int bound2 = list.get(i).getBound2();
                            int bound3 = list.get(i).getBound3();
                            int bound4 = list.get(i).getBound4();
                            int zbarImageH = list.get(i).getImageReaderCreateHeight();
                            int zbarImageW = list.get(i).getImageReaderCreateWeight();
                            Bitmap bitmap = list.get(i).getTextBitmap();

                            setBarCodeDataInMemory(
                                    scanStr,
                                    bound1,
                                    bound2,
                                    bound3,
                                    bound4,
                                    zbarImageH,
                                    zbarImageW,
                                    bitmap,
                                    resultBean,
                                    cloneZBarcodeBeanArrayList);
                        }

                    } else {
//                        Log.d(TAG,"AAA_isSame :"+isSame);
                        //save data
                        //2
                        for (int i = 0; i < jsonArray.length(); i++) {
                            barId = barId + 1;
                            JSONObject scanResultJSONObject = jsonArray.getJSONObject(i);
                            String scanStr = scanResultJSONObject.getString(Constant.ZBAR_BARCODE_SCAN_RESULT);
                            int bound1 = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_BOUND1);
                            int bound2 = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_BOUND2);
                            int bound3 = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_BOUND3);
                            int bound4 = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_BOUND4);
                            int zbarImageH = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_IMAGE_HIGHT);
                            int zbarImageW = scanResultJSONObject.getInt(Constant.ZBAR_BARCODE_IMAGE_WEIGHT);
                            Log.d("onScanResult", "result : " + scanStr);

//                          //Create Bitmap
                            Bitmap bitmap = ImageUtil.drawText(scanStr, 300, 50, Color.TRANSPARENT);
                            //save data in memory
                            setBarCodeDataInMemory(
                                    scanStr,
                                    bound1,
                                    bound2,
                                    bound3,
                                    bound4,
                                    zbarImageH,
                                    zbarImageW,
                                    bitmap,
                                    resultBean,
                                    cloneZBarcodeBeanArrayList);
                        }
                    }

                    //save clone list
                    setZbarBeanList(cloneZBarcodeBeanArrayList);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //clone last list
    ArrayList<ZBarCodeBean> zBarcodeBeanArrayList;

    private void setZbarBeanList(ArrayList<ZBarCodeBean> _zBarcodeBeanArrayList) {
        zBarcodeBeanArrayList = _zBarcodeBeanArrayList;
    }

    private ArrayList<ZBarCodeBean> getZbarBeanList() {
        return zBarcodeBeanArrayList;
    }

    ArrayList<String> lastTimeZabrResultList;

    private void setLastTimeZbarList(ArrayList<String> lastResultList) {
        lastTimeZabrResultList = lastResultList;
    }

    private ArrayList<String> getLastTimeZbarResultList() {
        return lastTimeZabrResultList;
    }

    private void setBarCodeDataInMemory(String scanStr, int bound1, int bound2
            , int bound3, int bound4, int zbarImageH, int zbarImageW
            , Bitmap textZBitmap, ArrayList<String> thisTimeList
            , ArrayList<ZBarCodeBean> cloneZBarcodeBeanArrayList) {

        ZBarCodeBean barcodeBean = new ZBarCodeBean(scanStr, textZBitmap);
//        MyGlRenderFilter.getZBarcodeBeanMap().put(barId , barcodeBean);
        barcodeBean.setZbarcodeString(scanStr);

        Log.d(TAG, "ZZZ_ZBAR bound1 : " + bound1 + " bound2 : " + bound2
                + " bound3 : " + bound3 + " bound4 :" + bound4 + " scanStr : " + scanStr);

        barcodeBean.setBound1(bound1);
        barcodeBean.setBound2(bound2);
        barcodeBean.setBound3(bound3);
        barcodeBean.setBound4(bound4);
        barcodeBean.setImageReaderCreateHeight(zbarImageH);
        barcodeBean.setImageReaderCreateWeight(zbarImageW);
        barcodeBean.setTextBitmap(textZBitmap);

        //preivew
        ZbarQueue zbarQueue = ZbarQueue.getInstance();
        zbarQueue.addQueue(barcodeBean);

        MyGlRenderFilter.setBarIdList(barId);
        thisTimeList.add(scanStr);
//      Log.d(TAG,"A____ZZZ_ZBAR bound1");
//      Constant.start_time = System.currentTimeMillis();
        cloneZBarcodeBeanArrayList.add(barcodeBean);
    }


    //containsAll is more faster
    private Boolean isTwoArrayListSame(ArrayList<String> lastData, ArrayList<String> data) {
        boolean isSame = false;

        if (lastData == null) {
            isSame = false;
        } else {

            if (lastData.size() == data.size()) {
                isSame = true;
            } else {
                Collection<String> before = new ArrayList(lastData);
                Collection<String> after = new ArrayList(data);

                List<String> beforeList = new ArrayList<String>(before);
                List<String> afterList = new ArrayList<String>(after);

                boolean a = beforeList.containsAll(afterList);
                boolean b = afterList.containsAll(beforeList);

                if (a == b)
                    isSame = true;
                else
                    isSame = false;
            }
//        Log.d(TAG,"AAA_resultList : "+isSame);
        }

        return isSame;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "AAA_onStart");
        setFullScreenSticky();
        viewStateUtil.setRunningStateProcess(true);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "AAA_onStop");
        viewStateUtil.setRunningStateProcess(false);
        EventBus.getDefault().unregister(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
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
        if (myAudioEncoder != null)
            myAudioEncoder.stopCodec();

        stopCameraSource();
    }


    protected Matrix matrix;
    protected Matrix decodeMatrix;

    private void configureTransform(TextureView textureView, int viewWidth, int viewHeight) {
        if (previewRawTexture == null)
            return;

        CameraManager cameraManager = getCameraManager();

        cameraId = getCameraId(cameraManager);

        CameraCharacteristics characteristics = getCharacteristics(cameraManager);

        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

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
        previewSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture.class),
                rotatedViewWidth,
                rotatedViewHeight,
                maxPreviewWidth,
                maxPreviewHeight,
                largestJpeg);

        if (swappedDimensions) {

            if (textureView instanceof IAutoFixView) {

                ((IAutoFixView) textureView).setAspectRatio(
                        previewSize.getHeight(),
                        previewSize.getWidth());
            }
        } else {

            if (textureView instanceof IAutoFixView) {

                ((IAutoFixView) textureView).setAspectRatio(
                        previewSize.getWidth(),
                        previewSize.getHeight());
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
        this.matrix = new Matrix();
        Rect mActiveArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Size pixelSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        float ratio = Math.max(((float) rotatedViewWidth) / mActiveArray.width(), ((float) rotatedViewHeight) / mActiveArray.height());
        float offsetX = (mActiveArray.width() - (rotatedViewWidth / ratio)) / 2;
        float offsetY = (mActiveArray.height() - (rotatedViewHeight / ratio)) / 2;
        this.matrix.postTranslate(-offsetX, -offsetY);
        this.matrix.postScale(ratio, ratio);
        this.matrix.postRotate(totalRotation);

        if (totalRotation == 90) {

            this.matrix.postTranslate(rotatedViewHeight, 0);


        } else if (totalRotation == 270) {

            this.matrix.postTranslate(0, rotatedViewWidth);


        } else if (totalRotation == 180) {

            this.matrix.postTranslate(rotatedViewHeight, rotatedViewWidth);

        }


        //test for hardware decode streaming canvas
        decodeMatrix = new Matrix();
        float rotatedViewWidths = imageReaderSize.getWidth();
        float rotatedViewHeights = imageReaderSize.getHeight();
        float arrayW = mActiveArray.width();
        float arrayH = mActiveArray.height();

        float ratio2 = Math.max(((float) rotatedViewWidths) / arrayW, ((float) rotatedViewHeights) / arrayH);
        float offsetX2 = (arrayW - (rotatedViewWidths / ratio2)) / 2;
        float offsetY2 = (arrayH - (rotatedViewHeights / ratio2)) / 2;

        decodeMatrix.postTranslate(-offsetX2, -offsetY2);
        decodeMatrix.postScale(ratio2, ratio2);
        decodeMatrix.postRotate(totalRotation);

        if (totalRotation == 90) {

            decodeMatrix.postTranslate(rotatedViewHeights, 0);//y,x

        } else if (totalRotation == 270) {

            decodeMatrix.postTranslate(0, rotatedViewWidths);

        } else if (totalRotation == 180) {

            decodeMatrix.postTranslate(rotatedViewHeights, rotatedViewWidths);

        }


    }


    private void initVideoEncoder(long startRecordWhenNs) {
        int stream1BitRate = 5 * 1000 * 1000;

        myVideoEncoderStream1 = new MyVideoEncoder(startRecordWhenNs);

        try {
            myVideoEncoderStream1.prepareVideoEncoder(
                    streamOffScreenSize.getWidth(),
                    streamOffScreenSize.getHeight(),
                    stream1BitRate,
                    Constant.ENCODE_VIDEO_SAVE_PATH,
                    true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        myVideoEncoderStream1.setMyEncoderCallBackFunction(myEncoderCallBackFunction);
    }


    private void initAudioEncoder(long startRecordWhenNs) {

        try {

            audioRecord = setupAudioRecord();

            startAudioRecord(audioRecord);//開始錄音

            myAudioEncoder = new MyAudioEncoder(startRecordWhenNs);

            myAudioEncoder.prepareAudioEncoder(audioRecord, AUDIO_SAMPLES_PER_FRAME);

            myAudioEncoder.startCodec();

            myAudioEncoder.setMyEncoderCallBackFunction(myEncoderCallBackFunction);

        } catch (Exception e) {
            e.printStackTrace();
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return null; // Return null and reattempt setup when permission is granted
        }

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


    private int barId = 0;

    private void startCameraSource() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (isUseCamera2 && cameraAction != null) {
            CameraManager cameraManager = getCameraManager();
            cameraId = getCameraId(cameraManager);
            //獲取可用相機設備列表
            CameraCharacteristics characteristics = getCharacteristics(cameraManager);
            int deviceRotation = getWindowManager().getDefaultDisplay().getRotation();
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            // Find the rotation of the device relative to the camera sensor's orientation.
            int totalRotation = sensorToDeviceRotation(characteristics, deviceRotation);
            boolean swappedDimensions = totalRotation == 90 || totalRotation == 270;

            int screenRotation = ScreenUtil.getScreenRotation(this);
            //mPreview.start(camera2Source, mGraphicOverlay);
            StartCameraSourceBean startCameraSourceBean = new StartCameraSourceBean();
            StartCameraSourceBean.Camera2SourceBean camera2SourceBean = new StartCameraSourceBean.Camera2SourceBean();
            camera2SourceBean.set_offScreenSurfaceTexture(offScreenSurfaceTexture);
            camera2SourceBean.setDisplayOrientation(screenRotation);
            camera2SourceBean.setPreviewSurfaceTexture(previewSurfaceTexture);
            camera2SourceBean.setTextureView(txv_cameraPreviewWithFilter);
            if (swappedDimensions) {
                camera2SourceBean.setImageReaderH(imageReaderSize.getWidth());
                camera2SourceBean.setImageReaderW(imageReaderSize.getHeight());
                camera2SourceBean.setOffScreenResolutionH(streamOffScreenSize.getWidth());
                camera2SourceBean.setOffscreenResolutionW(streamOffScreenSize.getHeight());
            } else {
                camera2SourceBean.setImageReaderH(imageReaderSize.getHeight());
                camera2SourceBean.setImageReaderW(imageReaderSize.getWidth());
                camera2SourceBean.setOffScreenResolutionH(streamOffScreenSize.getHeight());
                camera2SourceBean.setOffscreenResolutionW(streamOffScreenSize.getWidth());
            }
            startCameraSourceBean.setCamera2SourceBean(camera2SourceBean);
            startCameraSourceBean.setCamera1SourceBean(null);

            cameraAction.startCameraSource(startCameraSourceBean);
            //myVideoEncoderStream1.setEncoderCanvasInfo(previewEglCtx,previewRawTexture,previewSurfaceTexture);
            myVideoEncoderStream1.startCodec();
        } else {
            initCamera();
            startCameraSource();//recursive call
        }
    }

    private void stopCameraSource() {
        if (cameraAction != null) {
            cameraAction.stopCameraSource();
        }
    }

    private MyBaseEncoder.MyEncoderCallBackFunction myEncoderCallBackFunction = new MyBaseEncoder.MyEncoderCallBackFunction() {
        @Override
        public void encodeAudioSuccess(byte[] encodeDate, int encodeSize, int channelCount, int sampleBit, int sampleRate) {
            //Log.d(TAG,"bbb_encodeAudioSuccess:" + Thread.currentThread().getId());

        }

        @Override
        public void encodeVideoSuccess(byte[] encodeDate, int encodeSize, boolean isVideoKeyFrame, int width, int height) {
//            Log.d(TAG,"encodeVideoSuccess:isVideoKeyFrame:"+isVideoKeyFrame + ",w:"+width+",h:"+height);
        }
        @Override
        public void outputFormatChanged(MyBaseEncoder.GEO_ENCODER_TYPE encoderType, MediaFormat format) {
            if (IS_EXECUTE_RECORD) {
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
    private void setFullScreenSticky() {
        /**
         * Immersive Full-Screen(全屏沉浸模式) Mode only support android api 19 later.
         * */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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

    //abstract factory
    abstract class CameraFactory {
        //        abstract public Camera1Factory buildCamera1();
        abstract public Camera2Factory buildCamera2();
    }

    class Camera2Factory {
        public Camera2Factory() {
        }
    }

    class FrontCamera extends CameraFactory {
        @Override
        public Camera2Factory buildCamera2() {
            Camera2Factory factory = new Camera2Factory();
            camera2Source = new Camera2Source.Builder(SurfaceTextureCamera2Activity.this)
                    .setFocusMode(Camera2Source.CAMERA_AF_CONTINUOUS_VIDEO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_FRONT)
                    .setYuvCallBack().build();

            if (camera2Source.isCamera2Native()) {
                cameraAction = camera2Source;

            }
            return factory;

        }
    }

    class BackCamera extends CameraFactory {
        @Override
        public Camera2Factory buildCamera2() {
            Camera2Factory factory = new Camera2Factory();
            camera2Source = new Camera2Source.Builder(SurfaceTextureCamera2Activity.this)
                    .setFocusMode(Camera2Source.CAMERA_AF_CONTINUOUS_VIDEO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_BACK)
                    .setYuvCallBack().build();

            if (camera2Source.isCamera2Native()) {
                cameraAction = camera2Source;
            }
            return factory;
        }
    }

}


