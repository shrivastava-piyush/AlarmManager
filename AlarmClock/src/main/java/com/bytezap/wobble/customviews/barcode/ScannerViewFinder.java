package com.bytezap.wobble.customviews.barcode;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

import com.bytezap.wobble.R;

import me.dm7.barcodescanner.core.DisplayUtils;
import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.core.R.integer;

public class ScannerViewFinder extends View implements IViewFinder {
    private Rect mFramingRect;
    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final float LANDSCAPE_WIDTH_RATIO = 0.625F;
    private static final float LANDSCAPE_HEIGHT_RATIO = 0.625F;
    private static final int LANDSCAPE_MAX_FRAME_WIDTH = 1200;
    private static final int LANDSCAPE_MAX_FRAME_HEIGHT = 675;
    private static final float PORTRAIT_WIDTH_RATIO = 0.875F;
    private static final float PORTRAIT_HEIGHT_RATIO = 0.375F;
    private static final int PORTRAIT_MAX_FRAME_WIDTH = 945;
    private static final int PORTRAIT_MAX_FRAME_HEIGHT = 720;
    private static final int POINT_SIZE = 10;
    private final int mDefaultLaserColor;
    private final int mDefaultMaskColor;
    private final int mDefaultBorderColor;
    private final int mDefaultBorderStrokeWidth;
    private final int mDefaultBorderLineLength;
    protected Paint mLaserPaint;
    protected Paint mFinderMaskPaint;
    protected Paint mBorderPaint;
    protected Paint mFramePaint;
    protected int mBorderLineLength;

    boolean isFirst = true;
    boolean isGoingDown = true;
    private boolean shouldDrawLaser = true;
    private int laserTop;
    private int laserBottom;

    public ScannerViewFinder(Context context) {
        super(context);
        this.mDefaultLaserColor = Color.parseColor("#EF5350");
        this.mDefaultMaskColor = Color.parseColor("#70000000");
        this.mDefaultBorderColor = Color.WHITE;
        this.mDefaultBorderStrokeWidth = this.getResources().getInteger(integer.viewfinder_border_width);
        this.mDefaultBorderLineLength = this.getResources().getInteger(integer.viewfinder_border_length);
        this.init();
    }

    public ScannerViewFinder(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDefaultLaserColor = Color.parseColor("#EF5350");
        this.mDefaultMaskColor = Color.parseColor("#70000000");
        this.mDefaultBorderColor = Color.WHITE;
        this.mDefaultBorderStrokeWidth = this.getResources().getInteger(integer.viewfinder_border_width);
        this.mDefaultBorderLineLength = this.getResources().getInteger(integer.viewfinder_border_length);
        this.init();
    }

    private void init() {
        this.mLaserPaint = new Paint();
        this.mLaserPaint.setColor(this.mDefaultLaserColor);
        this.mLaserPaint.setStyle(Style.FILL);
        this.mFinderMaskPaint = new Paint();
        this.mFinderMaskPaint.setColor(this.mDefaultMaskColor);
        this.mBorderPaint = new Paint();
        this.mBorderPaint.setColor(this.mDefaultBorderColor);
        this.mBorderPaint.setStyle(Style.STROKE);
        this.mBorderPaint.setStrokeWidth((float) this.mDefaultBorderStrokeWidth - 1);
        this.mFramePaint = new Paint();
        this.mFramePaint.setColor(Color.WHITE);
        this.mFramePaint.setStyle(Style.STROKE);
        this.mFramePaint.setStrokeJoin(Paint.Join.ROUND);
        this.mFramePaint.setStrokeCap(Paint.Cap.ROUND);
        this.mFramePaint.setStrokeWidth(mDefaultBorderStrokeWidth+2);
        this.mBorderLineLength = this.mDefaultBorderLineLength;
    }

    public void setShouldDrawLaser(boolean shouldDrawLaser) {
        this.shouldDrawLaser = shouldDrawLaser;
        invalidate();
    }

    public void setLaserColor(int laserColor) {
        this.mLaserPaint.setColor(laserColor);
    }

    public void setMaskColor(int maskColor) {
        this.mFinderMaskPaint.setColor(maskColor);
    }

    public void setBorderColor(int borderColor) {
        this.mBorderPaint.setColor(borderColor);
    }

    public void setBorderStrokeWidth(int borderStrokeWidth) {
        this.mBorderPaint.setStrokeWidth((float)borderStrokeWidth);
    }

    public void setBorderLineLength(int borderLineLength) {
        this.mBorderLineLength = borderLineLength;
    }

    @Override
    public void setLaserEnabled(boolean b) {

    }

    @Override
    public void setBorderCornerRounded(boolean b) {

    }

    @Override
    public void setBorderAlpha(float v) {

    }

    @Override
    public void setBorderCornerRadius(int i) {

    }

    @Override
    public void setViewFinderOffset(int i) {

    }

    @Override
    public void setSquareViewFinder(boolean b) {

    }

    public void setupViewFinder() {
        this.updateFramingRect();
        this.invalidate();
    }

    public Rect getFramingRect() {
        return this.mFramingRect;
    }

    public void onDraw(Canvas canvas) {
        if(this.mFramingRect != null) {
            this.drawMask(canvas);
            canvas.drawRect(mFramingRect.left, mFramingRect.top, mFramingRect.right, mFramingRect.bottom, mBorderPaint);
            this.drawBorder(canvas);
            if (shouldDrawLaser) {
                this.drawLaser(canvas);
            }
        }
    }

    public void drawMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        canvas.drawRect(0.0F, 0.0F, (float) width, (float) this.mFramingRect.top, this.mFinderMaskPaint);
        canvas.drawRect(0.0F, (float) this.mFramingRect.top, (float) this.mFramingRect.left, (float) (this.mFramingRect.bottom + 1), this.mFinderMaskPaint);
        canvas.drawRect((float) (this.mFramingRect.right + 1), (float) this.mFramingRect.top, (float) width, (float) (this.mFramingRect.bottom + 1), this.mFinderMaskPaint);
        canvas.drawRect(0.0F, (float) (this.mFramingRect.bottom + 1), (float) width, (float) height, this.mFinderMaskPaint);
    }

    public void drawBorder(Canvas canvas) {
        int padding = 3;
        canvas.drawLine((float)(this.mFramingRect.left - padding), (float)(this.mFramingRect.top - padding), (float)(this.mFramingRect.left - padding), (float)(this.mFramingRect.top - padding + this.mBorderLineLength), this.mFramePaint);
        canvas.drawLine((float)(this.mFramingRect.left - padding), (float)(this.mFramingRect.top - padding), (float)(this.mFramingRect.left - padding + this.mBorderLineLength), (float)(this.mFramingRect.top - padding), this.mFramePaint);
        canvas.drawLine((float)(this.mFramingRect.left - padding), (float)(this.mFramingRect.bottom + padding), (float)(this.mFramingRect.left - padding), (float)(this.mFramingRect.bottom + 1 - this.mBorderLineLength), this.mFramePaint);
        canvas.drawLine((float)(this.mFramingRect.left - padding), (float)(this.mFramingRect.bottom + padding), (float)(this.mFramingRect.left - padding + this.mBorderLineLength), (float)(this.mFramingRect.bottom + padding), this.mFramePaint);
        canvas.drawLine((float)(this.mFramingRect.right + padding), (float)(this.mFramingRect.top - padding), (float)(this.mFramingRect.right + padding), (float)(this.mFramingRect.top - 1 + this.mBorderLineLength), this.mFramePaint);
        canvas.drawLine((float)(this.mFramingRect.right + padding), (float)(this.mFramingRect.top - padding), (float)(this.mFramingRect.right + padding - this.mBorderLineLength), (float)(this.mFramingRect.top - padding), this.mFramePaint);
        canvas.drawLine((float) (this.mFramingRect.right + padding), (float) (this.mFramingRect.bottom + padding), (float) (this.mFramingRect.right + padding), (float) (this.mFramingRect.bottom + 1 - this.mBorderLineLength), this.mFramePaint);
        canvas.drawLine((float) (this.mFramingRect.right + padding), (float) (this.mFramingRect.bottom + padding), (float) (this.mFramingRect.right + padding - this.mBorderLineLength), (float) (this.mFramingRect.bottom + padding), this.mFramePaint);
    }

    public void drawLaser(Canvas canvas) {
        if (isFirst) {
            isFirst = false;
            laserTop = mFramingRect.height()/2 + mFramingRect.top;
            laserBottom = mFramingRect.bottom - 2;
        } else {
            laserTop = isGoingDown ? laserTop + 5 : laserTop - 5;
            if (laserTop >= laserBottom) {
                isGoingDown = false;
                laserTop = laserBottom;
            }
            if (laserTop <= (mFramingRect.top + 1)) {
                isGoingDown = true;
                laserTop = mFramingRect.top + 1;
            }
        }
        canvas.drawRect((float) (this.mFramingRect.left + 2), (float) (laserTop - 2), (float) (this.mFramingRect.right - 1), (float) (laserTop + 2), this.mLaserPaint);
        this.postInvalidateDelayed(25, this.mFramingRect.left - POINT_SIZE, this.mFramingRect.top - POINT_SIZE, this.mFramingRect.right + POINT_SIZE, this.mFramingRect.bottom + POINT_SIZE);
    }

    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        this.updateFramingRect();
    }

    public synchronized void updateFramingRect() {
        Point viewResolution = new Point(this.getWidth(), this.getHeight());
        int orientation = DisplayUtils.getScreenOrientation(this.getContext());
        int width;
        int height;
        if(orientation != 1) {
            Resources res = this.getContext().getResources();
            boolean isTablet = res.getBoolean(R.bool.config_isTablet);
            int resolution = isTablet ? viewResolution.y : viewResolution.y - res.getDimensionPixelSize(R.dimen.scanner_margin);
            width = findDesiredDimensionInRange(LANDSCAPE_WIDTH_RATIO, viewResolution.x, MIN_FRAME_WIDTH, LANDSCAPE_MAX_FRAME_WIDTH);
            height = findDesiredDimensionInRange(LANDSCAPE_HEIGHT_RATIO, resolution, MIN_FRAME_HEIGHT, LANDSCAPE_MAX_FRAME_HEIGHT);
        } else {
            width = findDesiredDimensionInRange(PORTRAIT_WIDTH_RATIO, viewResolution.x, MIN_FRAME_WIDTH, PORTRAIT_MAX_FRAME_WIDTH);
            height = findDesiredDimensionInRange(PORTRAIT_HEIGHT_RATIO, viewResolution.y, MIN_FRAME_HEIGHT, PORTRAIT_MAX_FRAME_HEIGHT);
        }

        int leftOffset = (viewResolution.x - width) / 2;
        int topOffset = (viewResolution.y - height) / 2;
        this.mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    }

    private int findDesiredDimensionInRange(float ratio, int resolution, int hardMin, int hardMax) {
        int dim = (int)(ratio * (float)resolution);
        return dim < hardMin?hardMin:(dim > hardMax?hardMax:dim);
    }
}
