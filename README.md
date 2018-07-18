# Camera2MediaCodec
=================================
 
```
1.use async mediaCodec
2.use camera2
3.use zbar to scan barcode
```
usage
-------
*gradle
````````
put these in gradle

//zbar
    compile 'com.yanzhenjie.zbar:zbar:1.0.0'
    compile 'com.yanzhenjie.zbar:camera:1.0.0'
    compile 'com.yanzhenjie:permission:1.0.7'
//draw the view if you want
    compile 'com.github.ChillingVan:android-openGL-canvas:v1.2.2.3'
//event bus 
  compile 'org.greenrobot:eventbus:3.1.1'
  
````````
*camera2
`````
first: init camera2
   
    if(usingFrontCamera){
            FrontCamera frontCamera = new FrontCamera();
            frontCamera.buildCamera2();// frontCamera.buildCamera1();

        }else{
            BackCamera backCamera = new BackCamera();
            backCamera.buildCamera2();// backCamera.buildCamera1();
        }
        
second: startCamera when surfaceTexture is available or onSet
  
``````



*zbar detect resolution set
````
can set resoluition to zbar library
if your device support 1920*1080 .  e.g STREAMING_RESOLUTION = 1920

`````


*zbar
  *Add interface callback code
  * [IYuvDataCallback.java](app/src/main/java/com/example/kuohsuan/camera2mediacodec/Interface/IYuvDataCallback.java)
  
  *Add ZbarProcessorRunnable Code
  * [ZbarProcessorRunnable.java](Camera2MediaCodec/app/src/main/java/com/example/kuohsuan/camera2mediacodec/ZbarProcessorRunnable.java)
  
  *Add EventBus bean
  * [ZbarResultEventBus.java](Camera2MediaCodec/app/src/main/java/com/example/kuohsuan/camera2mediacodec/myeventbus/ZbarResultEventBus.java)
 
 `````
 1. init ZbarProcessorRunnable
 2. put the byte[] data to ZbarProcessorRunnable
 3. register EventBus for zbar result 
  
`````
  
`````
//init zbarProcessorRunnable
  if(zbarProcessorRunnable == null && imageWidth >0 && imageHeight >0 ){
            zbarProcessorRunnable = new ZbarProcessorRunnable(
                    imageWidth,//image width 
                    imageHeight,
                    previewSize.getWidth(),//camera2 preview width 
                    previewSize.getHeight()
            );
        }
        
  //set data to zbar
  if(zbarProcessorRunnable!=null) {
       zbarProcessorRunnable.setNextFrame(data);
   }  
`````


preview 
-----
￼￼￼![alt text](https://serving.photos.photobox.com/314167626a7d55376ab63d583d9df972d8aa18ff8601dd85abc2f4b2b5e02e352335433c.jpg)
 draw the canvas on preview 
android-openGL-canvas (https://github.com/ChillingVan/android-openGL-canvas)
