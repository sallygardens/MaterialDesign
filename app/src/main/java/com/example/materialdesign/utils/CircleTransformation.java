package com.example.materialdesign.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.squareup.picasso.Transformation;

public class CircleTransformation implements Transformation {

    private static final int STROKE_WIDTH = 6;

    @Override
    public Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());

        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

        Canvas canvas = new Canvas(bitmap);

        Paint avatarPaint = new Paint();
        //CLAMP这种模式在绘制的时候如果绘制区域超出提供的图片的尺寸，超出区域会使用超出部分的边缘颜色进行着色
        //REPEAT 这种模式在绘制的时候如果绘制区域超出提供的图片的尺寸，超出区域会重新使用完整的图片进行着色
        //MIRROR 这种模式在绘制的时候如果绘制区域超出提供的图片的尺寸，超出区域会重新使用完整的图片进行着色，并且呈镜面反转效果
        BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        avatarPaint.setShader(shader);

        Paint outlinePaint = new Paint();
        outlinePaint.setColor(Color.WHITE);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(STROKE_WIDTH);
        //抗锯齿
        outlinePaint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, avatarPaint);
        canvas.drawCircle(r, r, r - STROKE_WIDTH / 2, outlinePaint);

        squaredBitmap.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return "circleTransformation()";
    }
}
