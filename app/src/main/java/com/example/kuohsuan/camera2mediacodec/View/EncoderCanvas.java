package com.example.kuohsuan.camera2mediacodec.View;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.OffScreenCanvas;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.GLPaint;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;

/**
 * Created by f9021 on 2018/1/4.
 */

public class EncoderCanvas extends OffScreenCanvas {

    private OnDrawListener onDrawListener;
    private static GLPaint paint ;
    private static GLPaint _paint;
    private int rotation = 90;

    public EncoderCanvas(int width, int height, EglContextWrapper eglCtx  , Surface inputSurface ) {
        super(width, height, eglCtx, inputSurface);


    }
    public EncoderCanvas(int width, int height, Surface inputSurface ) {
        super(width, height, inputSurface);
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!! important ///////////
        setProducedTextureTarget(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
    }


    @Override
    protected void onGLDraw(ICanvasGL iCanvasGL, SurfaceTexture surfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
//        if(outsideTexture== null || outsideSurfaceTexture==null)
//            return;

        rawTexture.setIsFlippedVertically(true);
        if (onDrawListener != null) {
            onDrawListener.onGLDraw(iCanvasGL, surfaceTexture, rawTexture, outsideSurfaceTexture, outsideTexture);
        }
//        Size rotatedPreviewSize = MyGlRenderFilter.getPreviewSize();
//        TextureFilter textureFilter = MyGlRenderFilter.getTextureFilter();
//        Bitmap logoBitmap = MyGlRenderFilter.getLogoBitmap();
//        int canvasWidth = rawTexture.getWidth();
//        int canvasHeight = rawTexture.getHeight();
//        if(outsideTexture== null || outsideSurfaceTexture==null) //maybe no set setSharedTexture() ---->for camera 2
//            iCanvasGL.drawSurfaceTexture(rawTexture, surfaceTexture, 0, 0, rawTexture.getWidth(), rawTexture.getHeight(), textureFilter);
//        else //use setSharedTexture()---->for camera 1
//            iCanvasGL.drawSurfaceTexture(outsideTexture, outsideSurfaceTexture, 0, 0, canvasWidth , canvasHeight , textureFilter);
//
//        if(logoBitmap!=null)
//            iCanvasGL.drawBitmap(logoBitmap, 0, 0 , 200, 50);
//
//
//
//        RectF[] deoceFaceList = MyGlRenderFilter.getDecodeFaceList();
//
//        if(paint==null){
//            paint  = new GLPaint();
//            paint.setColor(Color.RED);
//            paint.setStyle(Paint.Style.STROKE);
//            paint.setLineWidth(3);
//        }
//
//
//        if(deoceFaceList!=null){
//            for(RectF rectF :deoceFaceList){
//                iCanvasGL.drawRect(rectF,paint);
//            }
//        }
//
//        //use google mobile vision SDK
//        Map<Integer, MyGlRenderFilter.BarcodeBean> barcodeMap =  MyGlRenderFilter.getBarcodeBeanMap();
//        Set<Integer> barcodeMapKeySet =  barcodeMap.keySet();
//        Iterator<Integer> barcodeMapKeySetIterator = barcodeMapKeySet.iterator();
//        while(barcodeMapKeySetIterator.hasNext()){
//            Integer key = barcodeMapKeySetIterator.next();
//            MyGlRenderFilter.BarcodeBean barcodeBean = barcodeMap.get(key);
//            if(barcodeBean!=null && rotatedPreviewSize!=null){
//                Barcode barcode = barcodeBean.getBarcode();
//                Bitmap textBitmap = barcodeBean.getTextBitmap();
//                float scaleFactorWidth = canvasWidth/(rotatedPreviewSize.getWidth()/4f);
//                float scaleFactorHeight = canvasHeight/(rotatedPreviewSize.getHeight()/4f);
//                RectF rect = new RectF(barcode.getBoundingBox());
//                iCanvasGL.drawBitmap(textBitmap , (int)(rect.centerX()*scaleFactorWidth) ,  (int)(rect.centerY()*scaleFactorHeight));
//                iCanvasGL.drawCircle(rect.centerX()*scaleFactorWidth,  rect.centerY()*scaleFactorHeight , 10, paint);
//            }
//        }
//
//
//        if(_paint==null) {
//            _paint = new GLPaint();
//            _paint.setColor(Color.GREEN);
//            _paint.setStyle(Paint.Style.STROKE);
//            _paint.setLineWidth(3);
//        }
//
//        //zbar
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
//                    Log.d(TAG,"AAA_ZBarcodeBean X : "+barcodeBean.getBound2()+" Y : "+barcodeBean.getBound1());
//
//                    int diff = (barcodeBean.getBound3()/2);
////                    int x = canvasWidth-barcodeBean.getBound2()-diff;
////                    int y =  barcodeBean.getBound1()+diff;
////
////                    int x = canvasWidth-barcodeBean.getBound2();
////                    int y =  barcodeBean.getBound1();
//
//                    //temp formula display rotate 270 for preview
//                    float s1 = rawTexture.getHeight() / barcodeBean.getImageReaderCreateWeight()  ;
//                    float s2 = rawTexture.getWidth() / barcodeBean.getImageReaderCreateHeight()  ;
//                    float ratio = Math.max(s1,s2);
//                    float offsetX = (barcodeBean.getImageReaderCreateWeight() - (rawTexture.getHeight()/ratio) )/2;
//                    float offsetY = (barcodeBean.getImageReaderCreateHeight() - (rawTexture.getWidth()/ratio) )/2;
//
////                    canvas.drawCircle(((barcodeBean.getBound2()+diff-offsetY) *ratio ) , (producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)) , 20,_paint);
////                    canvas.drawBitmap(textBitmap , (int) ((barcodeBean.getBound2()+diff-offsetY) *ratio), (int) (producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)));
////                    Log.w(TAG,"AAA_ZBarcodeBean ADJUST x:"+ (barcodeBean.getBound2()+diff-offsetY) *ratio + " y : "+(producedRawTexture.getHeight()-((barcodeBean.getBound1()+diff-offsetX) *ratio)));
//
////                    //rotate
//                    rotation = MyGlRenderFilter.getDeviceRotation();
////                    double sin = Math.abs(Math.sin(Math.toRadians(rotation))), cos = Math.abs(Math.cos(Math.toRadians(rotation)));
//                    boolean swappedDimensions = rotation == 90 || rotation == 270;
//                    if (swappedDimensions) {
//                        canvasHeight = rawTexture.getWidth();
//                        canvasWidth = rawTexture.getHeight();
//                        offsetX = (barcodeBean.getImageReaderCreateWeight() - (canvasWidth/ratio) )/2;
//                        offsetY = (barcodeBean.getImageReaderCreateHeight() - (canvasHeight/ratio) )/2;
//                    }else{
//                        canvasHeight = rawTexture.getHeight();
//                        canvasWidth = rawTexture.getWidth();
//                        offsetX = (barcodeBean.getImageReaderCreateWeight() - (canvasWidth/ratio) )/2;
//                        offsetY = (barcodeBean.getImageReaderCreateHeight() - (canvasHeight/ratio) )/2;
//                    }
////                    float ratio = Math.max(s1,s2);
//
//
//
//                    int moveX =0;
//                    int moveY =0;
//                    int caculateX = (int) ((barcodeBean.getBound2()+diff-offsetY) *ratio);
//                    int caculateY = (int) (((barcodeBean.getBound1()+diff-offsetX) *ratio));
//                    if(rotation==90){
//                        if (swappedDimensions) {
//                            moveX  =  canvasHeight;
//                        }else{
//                            moveX =  canvasWidth;
//                        }
//                        caculateX = moveX - caculateX;
//                    }else if(rotation==270){
//                        if (swappedDimensions) {
//                            moveY = canvasWidth;
//                        }else{
//                            moveY =  canvasHeight;
//                        }
//                        caculateY = moveY - caculateY;
//                    }else if(rotation==180){
//                        if (swappedDimensions) {
//                            moveX = canvasHeight;
//                            moveY = canvasWidth;
//                        }else{
//                            moveX =  canvasWidth;
//                            moveY = canvasHeight;
//                        }
//                        caculateX = moveX - caculateX;
//                        caculateY = moveY - caculateY;
//                    }
//
//
//                    Log.d(TAG,"AAA_ZBarcodeBean  rotation : "+rotation+" "+" matrix caculate caculateX : "+caculateX+ " caculateY : "+caculateY);
////
//                    iCanvasGL.drawBitmap(textBitmap , caculateX ,caculateY);
//                    iCanvasGL.drawCircle(caculateX,  caculateY , 10, _paint);
//
//
//
//
//                }
//            }
//            //if draw done , remove item from the Map  immediately!!!!!
//            ArrayList<Integer> zbarList = (ArrayList<Integer>) getBarIdList().clone();
//            getBarIdList().clear();
//            if(zbarList!=null && zbarList.size()>0){
//                for(Integer i : zbarList){
//                    MyGlRenderFilter.getZBarcodeBeanMap().remove(i);
//                }
//            }
//
//        }





    }



    public void setOnDrawListener(OnDrawListener l) {
        this.onDrawListener = l;
    }
    public interface OnDrawListener {
        void onGLDraw(ICanvasGL canvasGL, SurfaceTexture surfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture);
    }
}
