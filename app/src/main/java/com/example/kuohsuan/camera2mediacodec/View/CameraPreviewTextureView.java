/*
 *
 *  *
 *  *  * Copyright (C) 2016 ChillingVan
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package com.example.kuohsuan.camera2mediacodec.View;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.View;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.GLPaint;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.example.kuohsuan.camera2mediacodec.MyGlRenderFilter;
import com.example.kuohsuan.camera2mediacodec.bean.ZBarCodeBean;
import com.example.kuohsuan.camera2mediacodec.util.ZbarQueue;

/**
 * Created by Chilling on 2016/11/3.
 */

public class CameraPreviewTextureView extends GLSurfaceTextureProducerView implements IAutoFixView{
    private final String TAG = this.getClass().getSimpleName();
    private int ratioWidth = 0;
    private int ratioHeight = 0;
    private static GLPaint paint;
    private static GLPaint _paint;
    private int rotation = 90;



    public CameraPreviewTextureView(Context context) {
        super(context);
    }

    public CameraPreviewTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraPreviewTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        Log.d(TAG,"setAspectRatio  height : "+height +" width : "+width);
        ratioWidth = width;
        ratioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(width, height);
        } else {
            setMeasuredDimension(width,height);
//            Log.d(TAG,"setMeasuredDimension  , width : "+width
//                        +" height : "+height);
        }
    }



   @Override
    protected void onGLDraw(ICanvasGL canvas, SurfaceTexture producedSurfaceTexture, RawTexture producedRawTexture, @Nullable SurfaceTexture sharedSurfaceTexture, @Nullable BasicTexture sharedTexture) {
        //修正鏡像的問題
        producedRawTexture.setIsFlippedVertically(true);

        int canvasHeight = producedRawTexture.getHeight();
        int canvasWidth = producedRawTexture.getWidth();
        Size rotatedPreviewSize = MyGlRenderFilter.getPreviewSize();

        TextureFilter textureFilter = MyGlRenderFilter.getTextureFilter();
        Bitmap logoBitmap = MyGlRenderFilter.getLogoBitmap();
        canvas.drawSurfaceTexture(producedRawTexture, producedSurfaceTexture, 0, 0, producedRawTexture.getWidth(), producedRawTexture.getHeight(), textureFilter);


        if(logoBitmap!=null)
            canvas.drawBitmap(logoBitmap, 0, 0 , 200, 50);

        if(paint==null) {
            paint = new GLPaint();
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setLineWidth(3);
        }

        RectF[] faceList = MyGlRenderFilter.getFaceResult();
        if(faceList != null){
            for(RectF rectF :faceList){

                canvas.drawRect(rectF,paint);

//                GLPaint paint1 = new GLPaint();
//                paint1.setColor(Color.RED);
//                canvas.drawCircle(rectF.centerX(),rectF.centerY(),20,paint1);
            }

        }

/*
        20180212 y3 : S6的公式推算,
                要注意~~S6 左右相反,所以需要0變1440, 1440變0.
        Face[] faces = MyGlRenderFilter.getFaceList();
        if(faces != null){
            for(Face face :faces){
                GLPaint paint1 = new GLPaint();
                paint1.setColor(Color.YELLOW);
                paint1.setStyle(Paint.Style.STROKE);
                //canvas.drawRect(face.getBounds(),paint1);

                GLPaint paint = new GLPaint();
                paint.setColor(Color.RED);

                float s1 = producedRawTexture.getHeight() / 5328f  ;//1920
                float s2 = producedRawTexture.getWidth() / 2988f  ;//1440
                float ratio = Math.max(s1,s2);
                float offsetX = (5312f - (producedRawTexture.getHeight()/ratio) )/2;
                float offsetY = (2988f - (producedRawTexture.getWidth()/ratio) )/2;
                canvas.drawCircle(producedRawTexture.getWidth()- ((face.getBounds().centerY()-offsetY) *ratio ) , ((face.getBounds().centerX()-offsetX) *ratio) , 20,paint);
                Log.w(TAG,"faceList_ok2:" + ( producedRawTexture.getWidth()- ((face.getBounds().centerY()- offsetY) *ratio ) ) + "," + ( (face.getBounds().centerX()-offsetX) *ratio ) );
            }
        }
*/


        /**
         * 2018 04/19 Peter
         * zbar畫點計算方法 SOP
         *
         * 目的：要把zbar辨識到的結果畫到Preview上,並且要點到中心點.
         * (前置作業,需要將zbar的Bound1～Bound4的值,轉換成preview上所對應解析度的座標點(這邊是1280*768),無論使用多少解析度來做辨識)
         *
         * 參數：
         * Bound1: zbar的 Y
         * Bound2: zbar的 X
         * Bound3：qrcode的短邊
         * Bound4：qrcode的長邊
         *
         *
         * 作法：
         *
         * 1.先取得 qrcode的長邊以及短邊,並且除與2; 其目的是用在要把點畫在qrcode的中心
         * 2.取得preview上的長短邊; 其目的是要畫在preview上
         * 3.取得裝置是否有旋轉? 幾度？ ; 其目的是90或270度要把長短邊互換!
         * 4.取得變形後的長短邊; 意思是RawTexture不會是你預期的解析度,會因為比例要正確會有校正的問題.
         * 5.取的縮放倍率; 意思是RawTexture原圖解析度要縮放成 preview的解析度. 若沒有旋轉的話, s1 = 變形後的長 / preview長邊 ; 反之可求得s1 , 近一步再取得s1,s2比較大的值
         * 6.計算出變形後長短邊的誤差值,之後要扣掉用的; 若沒有旋轉的話, offsetX = (preview長邊 - (變形後的長/縮放倍率) )/2; 反之可求得offsetY
         * 7.最後在計算出x,y（要畫出的位置）,會因為裝置旋轉,要調整X,Y. ; 若沒有旋轉的話,
         *    int caculateX  = (int) ((zbar的X+ (qrcode的短邊/2)-變形後短邊的誤差值)*縮放倍率),反之可求得caculateY
         *    ７-1.旋轉90度->左右相反, 旋轉270度->上下相反, 旋轉180度->上下左右相反.
         * 8.畫上去
         * */

        if(_paint==null) {
            _paint = new GLPaint();
            _paint.setColor(Color.GREEN);
            _paint.setStyle(Paint.Style.STROKE);
            _paint.setLineWidth(3);
        }


        ZbarQueue zbarQueue = ZbarQueue.getInstance();
        if(zbarQueue!=null){
            ZBarCodeBean barcodeBean = zbarQueue.getBeanInQueue();
            if(barcodeBean!=null ){
                //MyGlRenderFilter.ZBarcodeBean barcode = barcodeBean.getBarcode();
                Bitmap textBitmap = barcodeBean.getTextBitmap();

//                    Log.d(TAG,"AAA_ZBarcodeBean X : "+barcodeBean.getBound2()+" Y : "+barcodeBean.getBound1());

                int diff = (barcodeBean.getBound3()/2);
                int diff2 = (barcodeBean.getBound4()/2);
//                    int x = canvasWidth-barcodeBean.getBound2()-diff;
//                    int y =  barcodeBean.getBound1()+diff;
//
//                    int x = canvasWidth-barcodeBean.getBound2();
//                    int y =  barcodeBean.getBound1();

                int barcodeWeight = barcodeBean.getImageReaderCreateWeight();
                int barcodeHeight = barcodeBean.getImageReaderCreateHeight();
                if(barcodeHeight==0 || barcodeWeight==0){
                    //divide by zero

                }else{

                    rotation = MyGlRenderFilter.getDeviceRotation();
                    //                       double sin = Math.abs(Math.sin(Math.toRadians(rotation))), cos = Math.abs(Math.cos(Math.toRadians(rotation)));
                    boolean swappedDimensions = rotation == 90 || rotation == 270;

                    float s1 = 0;
                    float s2 = 0  ;
                    float ratio =0;
                    float offsetX = 0;
                    float offsetY = 0;

                    //temp formula display rotate 270 for preview
                    if(swappedDimensions){

                        s1 = producedRawTexture.getHeight() / barcodeWeight  ;
                        s2 = producedRawTexture.getWidth() / barcodeHeight  ;
//                                offsetX = (barcodeWeight - (producedRawTexture.getHeight()/ratio) )/2;
//                                offsetY = (barcodeHeight - (producedRawTexture.getWidth()/ratio) )/2;

                    }else{
                        s1 = producedRawTexture.getHeight() / barcodeHeight  ;
                        s2 = producedRawTexture.getWidth() / barcodeWeight  ;
//                                offsetX = (barcodeHeight - (producedRawTexture.getHeight()/ratio) )/2;
//                                offsetY = (barcodeWeight - (producedRawTexture.getWidth()/ratio) )/2;

                    }
                    ratio = Math.max(s1,s2);

                    //                    canvas.drawCircle(((barcodeBean.getBound2()+diff-offsetY) *ratio ) , (producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)) , 20,_paint);
                    //                    canvas.drawBitmap(textBitmap , (int) ((barcodeBean.getBound2()+diff-offsetY) *ratio), (int) (producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)));
                    //                    Log.w(TAG,"AAA_ZBarcodeBean ADJUST x:"+ (barcodeBean.getBound2()+diff-offsetY) *ratio + " y : "+(producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)));

                    //                    //rotate

                    if (swappedDimensions) {
                        canvasHeight = producedRawTexture.getWidth();
                        canvasWidth = producedRawTexture.getHeight();
                        offsetX = (barcodeBean.getImageReaderCreateWeight() - (canvasWidth/ratio) )/2;
                        offsetY = (barcodeBean.getImageReaderCreateHeight() - (canvasHeight/ratio) )/2;
                    }else{
                        canvasHeight = producedRawTexture.getHeight();
                        canvasWidth = producedRawTexture.getWidth();
                        offsetX = (barcodeBean.getImageReaderCreateWeight() - (canvasWidth/ratio) )/2;
                        offsetY = (barcodeBean.getImageReaderCreateHeight() - (canvasHeight/ratio) )/2;
                    }
                    //                    float ratio = Math.max(s1,s2);

                    int moveX =0;
                    int moveY =0;
                    int caculateX = (int) ((barcodeBean.getBound2()+diff2-offsetY) *ratio);
                    int caculateY = (int) (((barcodeBean.getBound1()+diff-offsetX) *ratio));
                    if(rotation==90){
                        if (swappedDimensions) {
                            moveX  =  canvasHeight;
                        }else{
                            moveX =  canvasWidth;
                        }
                        caculateX = moveX - caculateX;
                    }else if(rotation==270){
                        if (swappedDimensions) {
                            moveY = canvasWidth;
                        }else{
                            moveY =  canvasHeight;
                        }
                        caculateY = moveY - caculateY;
                    }else if(rotation==180){
                        if (swappedDimensions) {
                            moveX = canvasHeight;
                            moveY = canvasWidth;
                        }else{
                            moveX =  canvasWidth;
                            moveY = canvasHeight;
                        }
                        caculateX = moveX - caculateX;
                        caculateY = moveY - caculateY;
                    }

//                            Log.d(TAG,"AAA_ZBarcodeBean  rotation : "+rotation+" "+" matrix caculate caculateX : "+caculateX+ " caculateY : "+caculateY);
                    //
                    canvas.drawBitmap(textBitmap , caculateX ,caculateY);
                    canvas.drawCircle(caculateX,  caculateY , 10, _paint);

                }
            }
        }


        //zbar
//        Map<Integer, MyGlRenderFilter.ZBarcodeBean> _zbarcodeMap =  MyGlRenderFilter.getZBarcodeBeanMap();
//        if(_zbarcodeMap!=null){
//            Set<Integer> zbarcodeMapKeySet =  _zbarcodeMap.keySet();
//            Iterator<Integer> zbarcodeMapKeySetIterator = zbarcodeMapKeySet.iterator();
//            while(zbarcodeMapKeySetIterator.hasNext()){
//                Integer key = zbarcodeMapKeySetIterator.next();
//                MyGlRenderFilter.ZBarcodeBean barcodeBean = _zbarcodeMap.get(key);
//                if(barcodeBean!=null ){
//                    //MyGlRenderFilter.ZBarcodeBean barcode = barcodeBean.getBarcode();
//                    Bitmap textBitmap = barcodeBean.getTextBitmap();
//
////                    Log.d(TAG,"AAA_ZBarcodeBean X : "+barcodeBean.getBound2()+" Y : "+barcodeBean.getBound1());
//
//                    int diff = (barcodeBean.getBound3()/2);
//                    int diff2 = (barcodeBean.getBound4()/2);
////                    int x = canvasWidth-barcodeBean.getBound2()-diff;
////                    int y =  barcodeBean.getBound1()+diff;
////
////                    int x = canvasWidth-barcodeBean.getBound2();
////                    int y =  barcodeBean.getBound1();
//
//                    int barcodeWeight = barcodeBean.getImageReaderCreateWeight();
//                    int barcodeHeight = barcodeBean.getImageReaderCreateHeight();
//                    if(barcodeHeight==0 || barcodeWeight==0){
//                        //divide by zero
//
//                    }else{
//
//                            rotation = MyGlRenderFilter.getDeviceRotation();
//    //                       double sin = Math.abs(Math.sin(Math.toRadians(rotation))), cos = Math.abs(Math.cos(Math.toRadians(rotation)));
//                            boolean swappedDimensions = rotation == 90 || rotation == 270;
//
//                            float s1 = 0;
//                            float s2 = 0  ;
//                            float ratio =0;
//                            float offsetX = 0;
//                            float offsetY = 0;
//
//                        //temp formula display rotate 270 for preview
//                            if(swappedDimensions){
//
//                                s1 = producedRawTexture.getHeight() / barcodeWeight  ;
//                                s2 = producedRawTexture.getWidth() / barcodeHeight  ;
////                                offsetX = (barcodeWeight - (producedRawTexture.getHeight()/ratio) )/2;
////                                offsetY = (barcodeHeight - (producedRawTexture.getWidth()/ratio) )/2;
//
//                            }else{
//                                s1 = producedRawTexture.getHeight() / barcodeHeight  ;
//                                s2 = producedRawTexture.getWidth() / barcodeWeight  ;
////                                offsetX = (barcodeHeight - (producedRawTexture.getHeight()/ratio) )/2;
////                                offsetY = (barcodeWeight - (producedRawTexture.getWidth()/ratio) )/2;
//
//                            }
//                            ratio = Math.max(s1,s2);
//
//        //                    canvas.drawCircle(((barcodeBean.getBound2()+diff-offsetY) *ratio ) , (producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)) , 20,_paint);
//        //                    canvas.drawBitmap(textBitmap , (int) ((barcodeBean.getBound2()+diff-offsetY) *ratio), (int) (producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)));
//        //                    Log.w(TAG,"AAA_ZBarcodeBean ADJUST x:"+ (barcodeBean.getBound2()+diff-offsetY) *ratio + " y : "+(producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)));
//
//        //                    //rotate
//
//                            if (swappedDimensions) {
//                                canvasHeight = producedRawTexture.getWidth();
//                                canvasWidth = producedRawTexture.getHeight();
//                                offsetX = (barcodeBean.getImageReaderCreateWeight() - (canvasWidth/ratio) )/2;
//                                offsetY = (barcodeBean.getImageReaderCreateHeight() - (canvasHeight/ratio) )/2;
//                            }else{
//                                canvasHeight = producedRawTexture.getHeight();
//                                canvasWidth = producedRawTexture.getWidth();
//                                offsetX = (barcodeBean.getImageReaderCreateWeight() - (canvasWidth/ratio) )/2;
//                                offsetY = (barcodeBean.getImageReaderCreateHeight() - (canvasHeight/ratio) )/2;
//                            }
//    //                    float ratio = Math.max(s1,s2);
//
//                            int moveX =0;
//                            int moveY =0;
//                            int caculateX = (int) ((barcodeBean.getBound2()+diff2-offsetY) *ratio);
//                            int caculateY = (int) (((barcodeBean.getBound1()+diff-offsetX) *ratio));
//                            if(rotation==90){
//                                if (swappedDimensions) {
//                                    moveX  =  canvasHeight;
//                                }else{
//                                    moveX =  canvasWidth;
//                                }
//                                caculateX = moveX - caculateX;
//                            }else if(rotation==270){
//                                if (swappedDimensions) {
//                                    moveY = canvasWidth;
//                                }else{
//                                    moveY =  canvasHeight;
//                                }
//                                caculateY = moveY - caculateY;
//                            }else if(rotation==180){
//                                if (swappedDimensions) {
//                                    moveX = canvasHeight;
//                                    moveY = canvasWidth;
//                                }else{
//                                    moveX =  canvasWidth;
//                                    moveY = canvasHeight;
//                                }
//                                caculateX = moveX - caculateX;
//                                caculateY = moveY - caculateY;
//                            }
//
////                            Log.d(TAG,"AAA_ZBarcodeBean  rotation : "+rotation+" "+" matrix caculate caculateX : "+caculateX+ " caculateY : "+caculateY);
//    //
//                            canvas.drawBitmap(textBitmap , caculateX ,caculateY);
//                            canvas.drawCircle(caculateX,  caculateY , 10, _paint);
//
//                        }
//                    }
//
//            }
//            //TODO add Queue or Stack to handle Map
////            //if draw done , remove item from the Map  immediately!!!!!
////            ArrayList<Integer> zbarList = (ArrayList<Integer>) getBarIdList().clone();
////            getBarIdList().clear();
////            if(zbarList!=null && zbarList.size()>0){
////                MyGlRenderFilter.getZBarcodeBeanMap().clear();
////            }
//
//
//        }



    }

}
