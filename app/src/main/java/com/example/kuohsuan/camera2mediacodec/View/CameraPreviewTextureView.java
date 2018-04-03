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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.example.kuohsuan.camera2mediacodec.MyGlRenderFilter.getBarIdList;

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
//            if (width < height * ratioWidth / ratioHeight) {
//                setMeasuredDimension(width, width * ratioHeight / ratioWidth);
//                Log.d(TAG,"setMeasuredDimension width < height  , width : "+width * ratioHeight / ratioWidth
//                        +" height : "+width);
//            } else {
//                setMeasuredDimension(height * ratioWidth / ratioHeight, height);
//                Log.d(TAG,"setMeasuredDimension  , width : "+height * ratioWidth / ratioHeight
//                        +" height : "+height);
//            }

            setMeasuredDimension(width,height);
            Log.d(TAG,"setMeasuredDimension  , width : "+width
                        +" height : "+height);
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




        if(_paint==null) {
            _paint = new GLPaint();
            _paint.setColor(Color.GREEN);
            _paint.setStyle(Paint.Style.STROKE);
            _paint.setLineWidth(3);
        }

        //zbar
        Map<Integer, MyGlRenderFilter.ZBarcodeBean> _zbarcodeMap =  MyGlRenderFilter.getZBarcodeBeanMap();
        if(_zbarcodeMap!=null){
            Set<Integer> zbarcodeMapKeySet =  _zbarcodeMap.keySet();
            Iterator<Integer> zbarcodeMapKeySetIterator = zbarcodeMapKeySet.iterator();
            while(zbarcodeMapKeySetIterator.hasNext()){
                Integer key = zbarcodeMapKeySetIterator.next();
                MyGlRenderFilter.ZBarcodeBean barcodeBean = _zbarcodeMap.get(key);
                if(barcodeBean!=null ){
                    //MyGlRenderFilter.ZBarcodeBean barcode = barcodeBean.getBarcode();
                    Bitmap textBitmap = barcodeBean.getTextBitmap();

                    Log.d(TAG,"AAA_ZBarcodeBean X : "+barcodeBean.getBound2()+" Y : "+barcodeBean.getBound1());

                    int diff = (barcodeBean.getBound3()/2);
//                    int x = canvasWidth-barcodeBean.getBound2()-diff;
//                    int y =  barcodeBean.getBound1()+diff;
//
//                    int x = canvasWidth-barcodeBean.getBound2();
//                    int y =  barcodeBean.getBound1();

                    //temp formula display rotate 270 for preview
                    float s1 = producedRawTexture.getHeight() / barcodeBean.getImageReaderCreateWeight()  ;
                    float s2 = producedRawTexture.getWidth() / barcodeBean.getImageReaderCreateHeight()  ;
                    float ratio = Math.max(s1,s2);
                    float offsetX = (barcodeBean.getImageReaderCreateWeight() - (producedRawTexture.getHeight()/ratio) )/2;
                    float offsetY = (barcodeBean.getImageReaderCreateHeight() - (producedRawTexture.getWidth()/ratio) )/2;

//                    canvas.drawCircle(((barcodeBean.getBound2()+diff-offsetY) *ratio ) , (producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)) , 20,_paint);
//                    canvas.drawBitmap(textBitmap , (int) ((barcodeBean.getBound2()+diff-offsetY) *ratio), (int) (producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)));
//                    Log.w(TAG,"AAA_ZBarcodeBean ADJUST x:"+ (barcodeBean.getBound2()+diff-offsetY) *ratio + " y : "+(producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)));

//                    //rotate
                    rotation = MyGlRenderFilter.getDeviceRotation();
//                    double sin = Math.abs(Math.sin(Math.toRadians(rotation))), cos = Math.abs(Math.cos(Math.toRadians(rotation)));
                    boolean swappedDimensions = rotation == 90 || rotation == 270;
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
                    int caculateX = (int) ((barcodeBean.getBound2()+diff-offsetY) *ratio);
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


                    Log.d(TAG,"AAA_ZBarcodeBean  rotation : "+rotation+" "+" matrix caculate caculateX : "+caculateX+ " caculateY : "+caculateY);
//
                    canvas.drawBitmap(textBitmap , caculateX ,caculateY);
                    canvas.drawCircle(caculateX,  caculateY , 10, _paint);




                }
            }
            //if draw done , remove item from the Map  immediately!!!!!
            ArrayList<Integer> zbarList = (ArrayList<Integer>) getBarIdList().clone();
            getBarIdList().clear();
            if(zbarList!=null && zbarList.size()>0){
                for(Integer i : zbarList){
                    MyGlRenderFilter.getZBarcodeBeanMap().remove(i);
                }
            }

        }



    }

}
