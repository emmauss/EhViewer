package com.hippo.gl.glrenderer;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.hippo.yorozuya.AssertUtils;

public class SpriteTexture extends TiledTexture {

    private int mCount;
    private int[] mRects;

    private RectF mTempSource = new RectF();
    private RectF mTempTarget = new RectF();

    public SpriteTexture(Bitmap bitmap, boolean isOpaque, int count, int[] rects) {
        super(bitmap, isOpaque);

        AssertUtils.assertEquals("rects.length must be count * 4", count * 4, rects.length);
        mCount = count;
        mRects = rects;
    }

    public int getCount() {
        return mCount;
    }

    public void drawSprite(GLCanvas canvas, int index, int x, int y) {
        int[] rects = mRects;
        int offset = index * 4;
        int sourceX = rects[offset];
        int sourceY = rects[offset + 1];
        int sourceWidth = rects[offset + 2];
        int sourceHeight = rects[offset + 3];
        mTempSource.set(sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight);
        mTempTarget.set(x, y, x + sourceWidth, y + sourceHeight);
        draw(canvas, mTempSource, mTempTarget);
    }

    public void drawSprite(GLCanvas canvas, int index, int x, int y, int width, int height) {
        int[] rects = mRects;
        int offset = index * 4;
        int sourceX = rects[offset];
        int sourceY = rects[offset + 1];
        mTempSource.set(sourceX, sourceY, sourceX + rects[offset + 2], sourceY + rects[offset + 3]);
        mTempTarget.set(x, y, x + width, y + height);
        draw(canvas, mTempSource, mTempTarget);
    }

    public void drawSpriteMixed(GLCanvas canvas, int index, int color, float ratio, int x, int y) {
        int[] rects = mRects;
        int offset = index * 4;
        int sourceX = rects[offset];
        int sourceY = rects[offset + 1];
        int sourceWidth = rects[offset + 2];
        int sourceHeight = rects[offset + 3];
        mTempSource.set(sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight);
        mTempTarget.set(x, y, x + sourceWidth, y + sourceHeight);
        drawMixed(canvas, color, ratio, mTempSource, mTempTarget);
    }

    public void drawSpriteMixed(GLCanvas canvas, int index, int color, float ratio,
            int x, int y, int width, int height) {
        int[] rects = mRects;
        int offset = index * 4;
        int sourceX = rects[offset];
        int sourceY = rects[offset + 1];
        mTempSource.set(sourceX, sourceY, sourceX + rects[offset + 2],
                sourceY + rects[offset + 3]);
        mTempTarget.set(x, y, x + width, y + height);
        drawMixed(canvas, color, ratio, mTempSource, mTempTarget);
    }
}
