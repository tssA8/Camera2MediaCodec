# Camera2MediaCodec
=================================
 
```
1.use async mediaCodec
2.use camera2
3.use zbar to scan barcode
4.use opengl to draw zbar result (from android-openGL-canvas)
```

usage
-------
*camera2
`````
first: easy to use camera 
   
    if(usingFrontCamera){
            FrontCamera frontCamera = new FrontCamera();
            frontCamera.buildCamera2();// frontCamera.buildCamera1();

        }else{
            BackCamera backCamera = new BackCamera();
            backCamera.buildCamera2();// backCamera.buildCamera1();
        }
        
second: startCamera when surfaceTexture is available or onSet
  
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
           
                        startCameraSource();//this
                       

                    }
                });


``````



*zbar detect resolution set
`````
can set resoluition to zbar library

if your device support 1920*1080 .  e.g STREAMING_RESOLUTION = 1920
initViewEncoder(STREAMING_RESOLUTION);

`````


*zbar
  *Add interface callback code
  * [IYuvDataCallback.java](app/src/main/java/com/example/kuohsuan/camera2mediacodec/Interface/IYuvDataCallback.java)
`````
 1.set IYuvDataCallback in your ImageReader callback
  e.g
   if(mIYuvDataCallback !=null){
                mIYuvDataCallback.getYuv(convertYUV420888ToNV21Grey(mImage),mImage.getWidth(),mImage.getHeight());
            }
           
  2. init zbar scanner
  
  3. implements IYuvDataCallback in your class,then you can get the data from ImageReader callback
  
  4. use zbar to scan
     e.g
      Image barcode = new com.yanzhenjie.zbar.Image(imageWidth, imageHeight, "Y800");
      barcode.setData(data);
      int result = zbarImageScanner.scanImage(barcode);
      SymbolSet symSet = zbarImageScanner.getResults();
  
`````

preview
-----

￼￼￼![alt text](https://serving.photos.photobox.com/314167626a7d55376ab63d583d9df972d8aa18ff8601dd85abc2f4b2b5e02e352335433c.jpg)


ps: android-openGL-canvas can see(https://github.com/ChillingVan/android-openGL-canvas)
