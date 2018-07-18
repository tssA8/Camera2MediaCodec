# Camera2MediaCodec
=================================
 
```
1.use async mediaCodec
2.use camera2
3.use zbar to scan barcode
4.use opengl to draw zbar result (from android-openGL-canvas)
5.transform zbar result x ais and y ais to preview x ais and y ais location.(eg 3840*2160 to 1280*768 draw opengl )
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
  
  *Add ZbarProcessorRunnable Code
  *[ZbarProcessorRunnable.java](Camera2MediaCodec/app/src/main/java/com/example/kuohsuan/camera2mediacodec/ZbarProcessorRunnable.java)
  
  *Add EventBus bean
  *[ZbarResultEventBus.java](Camera2MediaCodec/app/src/main/java/com/example/kuohsuan/camera2mediacodec/myeventbus/ZbarResultEventBus.java)
`````
 1. init ZbarProcessorRunnable
 2. put the byte[] data to ZbarProcessorRunnable
 3. register EventBus for zbar result 
  
`````

preview
-----

￼￼￼![alt text](https://serving.photos.photobox.com/314167626a7d55376ab63d583d9df972d8aa18ff8601dd85abc2f4b2b5e02e352335433c.jpg)


ps: android-openGL-canvas can see(https://github.com/ChillingVan/android-openGL-canvas)
