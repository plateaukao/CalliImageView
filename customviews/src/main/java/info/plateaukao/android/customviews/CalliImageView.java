package info.plateaukao.android.customviews;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.Paint.Join;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RSRuntimeException;
import android.support.v8.renderscript.RenderScript;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import info.plateaukao.android.customviews.rs.ScriptC_calli;
import info.plateaukao.android.customviews.utils.BitmapUtils;
import info.plateaukao.android.customviews.utils.ResourceUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CalliImageView extends SquareImageView {

    public enum DRAW_TYPE {
        NORMAL,
        CONTOUR,
        SKELETON
    }

    private DRAW_TYPE drawType = DRAW_TYPE.NORMAL;

    public enum CHAR_IMG_SOURCE {
        IMAGE,
        FONT
    }

    private CHAR_IMG_SOURCE imgSource = CHAR_IMG_SOURCE.IMAGE;

    public enum GRID_TYPE {
        GRID_9,
        DIAGNAL
    }

    private GRID_TYPE gridType = GRID_TYPE.GRID_9;

    private Map<DRAW_TYPE, Drawable> processedDrawables = new HashMap<DRAW_TYPE, Drawable>();

    // for char from font
    private char ch;
    private String fontPath;

    private int borderSize = 20;
    private int crossSize = 5;

    private boolean bShowChar = true;

    private int COLOR_COMPARISON_PRECISENESS = 50;

    private int contour_color = Color.RED;
    //private int original_bgcolor = Color.WHITE;
    private int original_fgcolor = Color.BLACK;
    private int gridline_color = Color.RED;
    private int contour_bgcolor = Color.rgb(220, 220, 220);
    /*
	private int contour_bgcolor= Color.argb(30,
	                                        Color.red(original_fgcolor),
	                                        Color.green(original_fgcolor),
	                                        Color.blue(original_fgcolor));
	                                        */

    private Paint paint = new Paint();
    private boolean isDrawGridLine = true;
    private Drawable originalDrawable;

    private boolean enableRenderScript;

    public CalliImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public CalliImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CalliImageView(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CalliImageView);
        try {
            borderSize = (int) a.getDimension(R.styleable.CalliImageView_borderSize, 20);
            crossSize = (int) a.getDimension(R.styleable.CalliImageView_crossSize, 5);
        } finally {
            a.recycle();
        }

        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    private RenderScript mRS;
    private Allocation mInAllocation;
    private Allocation mOutAllocation;
    private ScriptC_calli mCalliScript;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private Bitmap runScript(Bitmap in, DRAW_TYPE type) {
        Bitmap out = Bitmap.createBitmap(in.getWidth(), in.getHeight()
                                         ,Bitmap.Config.ARGB_8888);
        mRS = RenderScript.create(getContext());
        mInAllocation = Allocation.createFromBitmap(mRS,
                in, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        mOutAllocation =  Allocation.createTyped(mRS, mInAllocation.getType());

        mCalliScript = new ScriptC_calli(mRS, getResources(), R.raw.calli);
        mCalliScript.set_gIn(mInAllocation);
        mCalliScript.set_gOut(mOutAllocation);
        mCalliScript.set_gbgColor(BitmapUtils.convertColor(contour_bgcolor));
        mCalliScript.set_width(in.getWidth());
        mCalliScript.set_height(in.getHeight());
        if (type == DRAW_TYPE.NORMAL) {
            mCalliScript.forEach_transparent(mInAllocation, mOutAllocation);
        } else if (type == DRAW_TYPE.CONTOUR) {
            mCalliScript.forEach_contour(mInAllocation, mOutAllocation);
        }
	          /*
	          Allocation mOut2Allocation = Allocation.createFromBitmap(sRS, out,
	                  Allocation.MipmapControl.MIPMAP_NONE,
	                  Allocation.USAGE_SCRIPT);
	          ScriptC_oned_filter mSmoothScript = new ScriptC_oned_filter(sRS, getResources(), R.raw.oned_filter);
	          mSmoothScript.set_gIn(mOutAllocation);
	          mSmoothScript.set_gOut(mOut2Allocation);
	          mSmoothScript.set_gScript(mSmoothScript);
	          mSmoothScript.set_width(in.getWidth());
	          mSmoothScript.set_height(in.getHeight());
	          mSmoothScript.invoke_filter();
	          mOut2Allocation.copyTo(out);
	          */
        mOutAllocation.copyTo(out);
        mInAllocation.destroy();
        mOutAllocation.destroy();
        mRS.destroy();
        return out;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // modify inner character padding size
        int paddingOffset = (int) ((r - l) * 0.10);
        setPadding(paddingOffset, paddingOffset, paddingOffset, paddingOffset);
    }

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        contour_bgcolor = color;
        Drawable tempDrawable = originalDrawable;
        originalDrawable = null;
        processedDrawables.clear();
        setImageDrawable(tempDrawable);
    }

    // -----
    // functions
    // -----
    public boolean isDrawGridLine() {
        return isDrawGridLine;
    }

    public void setIsDrawGridLine(boolean isDrawGridLine) {
        this.isDrawGridLine = isDrawGridLine;
        invalidate();
    }

    public boolean isShowChar() {
        return bShowChar;
    }

    public void setIsShowChar(boolean isShowChar) {
        if (bShowChar != isShowChar) {
            this.bShowChar = isShowChar;
            invalidate();
        }
    }
    // -----


    // setters from outside: clear bitmap cache
    @Override
    public void setImageDrawable(Drawable drawable) {
        if (isAnimating && null != anim) {
            isAnimating = false;
            anim.cancel();
            anim = null;
        }

        if (originalDrawable == drawable)
            return;
        originalDrawable = drawable;

        for (DRAW_TYPE type : processedDrawables.keySet()) {
            ((BitmapDrawable) processedDrawables.get(type)).getBitmap().recycle();
        }
        processedDrawables.clear();

        super.setImageDrawable(adjust(drawable));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setCharInfo(char ch, String fontPath) {
        this.ch = ch;
        this.fontPath = fontPath;
        this.imgSource = CHAR_IMG_SOURCE.FONT;
        if (Build.VERSION.SDK_INT >= 11)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setDrawType(DRAW_TYPE drawType) {
        this.drawType = drawType;

        // no image for image source type
        if (imgSource == CHAR_IMG_SOURCE.IMAGE) {
            if (null == originalDrawable) {
                return;
            } else {
                //super.setImageDrawable(adjust(originalDrawable));
                adjust(originalDrawable);
            }
        } else {
        }

        if (Build.VERSION.SDK_INT < 11 || drawType == DRAW_TYPE.SKELETON) {
            super.setImageDrawable(processedDrawables.get(drawType));
            invalidate();
        } else {
            startTypeChangeAnimation();
        }
    }

    // animation codes
    private float transitionRatio = 0f;
    private boolean isAnimating = false;

    public void setTransitionRatio(float ratio) {
        transitionRatio = ratio;
        invalidate();

    }

    ObjectAnimator anim;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void startTypeChangeAnimation() {
        ObjectAnimator oldAnim = null;
        if (anim != null && anim.isRunning()) {
            oldAnim = anim;
        }

        if (drawType == DRAW_TYPE.CONTOUR) {
            anim = ObjectAnimator.ofFloat(this, "transitionRatio", (isAnimating) ? transitionRatio : 1.0f, 0.0f);
            anim.setInterpolator(new AccelerateInterpolator());
        } else {
            anim = ObjectAnimator.ofFloat(this, "transitionRatio", (isAnimating) ? transitionRatio : 0.0f, 1.0f);
            anim.setInterpolator(new AccelerateInterpolator());
        }

        if (oldAnim != null) {
            oldAnim.cancel();
        }

        anim.setDuration(400);
        anim.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
                isAnimating = true;
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                isAnimating = false;
                CalliImageView.super.setImageDrawable(processedDrawables.get(drawType));
            }

            @Override
            public void onAnimationCancel(Animator arg0) {
                isAnimating = false;
                invalidate();
            }
        });
        anim.start();
    }

    public DRAW_TYPE getDrawType() {
        return drawType;
    }

    public void setGridType(GRID_TYPE gridType) {
        this.gridType = gridType;

        if (isDrawGridLine)
            invalidate();
    }

    public GRID_TYPE getGridType() {
        return gridType;
    }

    @SuppressWarnings("deprecation")
    private Drawable adjust(Drawable d) {
        if (null != processedDrawables.get(drawType))
            return processedDrawables.get(drawType);

        //Need to copy to ensure that the bitmap is mutable.
        Bitmap src = ((BitmapDrawable) d).getBitmap();
        if (null == src)
            return d;

        if(src.getConfig() == Bitmap.Config.RGB_565) {
            src = src.copy(Bitmap.Config.ARGB_8888, false);
            Log.v("format", "565");
        }
        //Bitmap bitmap = src.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap bitmap = null;

        if (drawType == DRAW_TYPE.NORMAL) {
            bitmap = transparent(src);
        } else if (drawType == DRAW_TYPE.CONTOUR) { //contour
            bitmap = contour(src);
        } else if (drawType == DRAW_TYPE.SKELETON) {
            bitmap = skeletonByCPU(src);
        }

        Drawable drawable = new BitmapDrawable(bitmap);
        if (null == processedDrawables.get(drawType))
            processedDrawables.put(drawType, drawable);

        return drawable;
    }

    Rect src = new Rect();
    Rect dst = new Rect();

    //final DrawFilter filter = new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,Paint.DITHER_FLAG);
    @Override
    public void onDraw(Canvas canvas) {
        if (isDrawGridLine)
            drawGridLine(canvas);

        if (bShowChar) {
            if (imgSource == CHAR_IMG_SOURCE.IMAGE) {
                if (isAnimating == false) {
                    super.onDraw(canvas);
                } else {
                    // animation
                    int mPaddingTop = this.getPaddingTop();
                    int mPaddingLeft = this.getPaddingLeft();
                    int mPaddingRight = this.getPaddingRight();
                    int mPaddingBottom = this.getPaddingBottom();

                    int saveCount = canvas.getSaveCount();
                    canvas.save();

    	            /*
    	            canvas.clipRect(mPaddingLeft,mPaddingTop,
    	                            canvas.getWidth() - mPaddingRight,
    	                            canvas.getHeight() - mPaddingBottom);
    	                            */
                    canvas.translate(mPaddingLeft, mPaddingTop);
    	            /*
    	            if (mDrawMatrix != null) {
    	                canvas.concat(mDrawMatrix);
    	            }
    	            */

                    int realWidth = canvas.getWidth() - mPaddingLeft - mPaddingRight;
                    int realHeight = canvas.getHeight() - mPaddingTop - mPaddingBottom;

                    Bitmap b = ((BitmapDrawable) processedDrawables.get(DRAW_TYPE.NORMAL)).getBitmap();

                    float scaleX = 1.0f, scaleY = 1.0f;

                    float w = (float) realWidth / b.getWidth();
                    float h = (float) realHeight / b.getHeight();
                    if (w > h) {
                        scaleX = (float) realHeight / b.getHeight();

                        src.set(0, 0, (int) b.getWidth(), (int) (b.getHeight() * transitionRatio));
                        dst.set((int) ((realWidth - b.getWidth() * scaleX) / 2), 0, (int) ((realWidth - b.getWidth() * scaleX) / 2 + b.getWidth() * scaleX), (int) (realHeight * transitionRatio));
                        canvas.drawBitmap(b, src, dst, paint);

                        b = ((BitmapDrawable) processedDrawables.get(DRAW_TYPE.CONTOUR)).getBitmap();
                        src.set(0, (int) (b.getHeight() * transitionRatio), (int) b.getWidth(), (int) b.getHeight());
                        dst.set((int) ((realWidth - b.getWidth() * scaleX) / 2), (int) (realHeight * transitionRatio), (int) ((realWidth - b.getWidth() * scaleX) / 2 + b.getWidth() * scaleX), realHeight);
                        canvas.drawBitmap(b, src, dst, paint);
                    } else {
                        scaleY = (float) realWidth / b.getWidth();

                        src.set(0, 0, (int) b.getWidth(), (int) (b.getHeight() * transitionRatio));
                        dst.set(0, (int) ((realHeight - b.getHeight() * scaleY) / 2), realWidth, (int) ((realHeight - b.getHeight() * scaleY) / 2 + b.getHeight() * scaleY * transitionRatio));
                        canvas.drawBitmap(b, src, dst, paint);

                        b = ((BitmapDrawable) processedDrawables.get(DRAW_TYPE.CONTOUR)).getBitmap();
                        src.set(0, (int) (b.getHeight() * transitionRatio), (int) b.getWidth(), (int) b.getHeight());
                        dst.set(0, (int) ((realHeight - b.getHeight() * scaleY) / 2 + b.getHeight() * scaleY * transitionRatio), realWidth, (int) ((realHeight - b.getHeight() * scaleY) / 2 + b.getHeight() * scaleY));
                        canvas.drawBitmap(b, src, dst, paint);

                    }

                    canvas.restoreToCount(saveCount);
                }
            } else if (imgSource == CHAR_IMG_SOURCE.FONT) {
                drawFontChar(canvas);
            }
        }
    }

    private void drawGridLine(Canvas canvas) {
        paint.setColor(gridline_color);

        paint.setStrokeWidth(borderSize);

        // border
        canvas.drawLine(0, 0, getMeasuredWidth(), 0, paint);
        canvas.drawLine(0, 0, 0, getMeasuredHeight(), paint);
        canvas.drawLine(getMeasuredWidth(), 0, getMeasuredWidth(), getMeasuredHeight(), paint);
        canvas.drawLine(0, getMeasuredHeight(), getMeasuredWidth(), getMeasuredHeight(), paint);

        // inside
        switch (gridType) {
            case GRID_9: {
                float stepWidth = getMeasuredWidth() / 3;
                float stepHeight = getMeasuredHeight() / 3;

                paint.setStrokeWidth(crossSize);

                canvas.drawLine(0, stepHeight, getMeasuredWidth(), stepHeight, paint);
                canvas.drawLine(0, stepHeight * 2, getMeasuredWidth(), stepHeight * 2, paint);
                canvas.drawLine(stepWidth, 0, stepWidth, getMeasuredHeight(), paint);
                canvas.drawLine(stepWidth * 2, 0, stepWidth * 2, getMeasuredHeight(), paint);
                break;
            }
            case DIAGNAL: {
                paint.setStrokeWidth(crossSize);
                // diagnal
                canvas.drawLine(0, 0, getMeasuredWidth(), getMeasuredHeight(), paint);
                canvas.drawLine(0, getMeasuredHeight(), getMeasuredWidth(), 0, paint);


                // cross
                float stepWidth = getMeasuredWidth() / 2;
                float stepHeight = getMeasuredHeight() / 2;


                canvas.drawLine(0, stepHeight, getMeasuredWidth(), stepHeight, paint);
                canvas.drawLine(stepWidth, 0, stepWidth, getMeasuredHeight(), paint);
                break;
            }
            default:
                break;
        }
    }

    public Bitmap getBitmap() {
        setDrawingCacheEnabled(true);
        buildDrawingCache();
        Bitmap bmp = Bitmap.createBitmap(this.getDrawingCache());
        setDrawingCacheEnabled(false);

        return bmp;
    }

    private Typeface typeface;
    private Paint strokePaint;
    private Paint textPaint;

    private void drawFontChar(Canvas canvas) {
        if (fontPath != null && null == typeface)
            typeface = ResourceUtils.getTypeface(fontPath);

        if (null == strokePaint) {
            strokePaint = new Paint();
            strokePaint.setColor(contour_color);
            strokePaint.setTextAlign(Paint.Align.CENTER);
            if (null != typeface)
                strokePaint.setTypeface(typeface);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setAntiAlias(true);
            strokePaint.setSubpixelText(true);
            strokePaint.setStrokeJoin(Join.MITER);
            strokePaint.setStrokeWidth(2);
        }

        if (null == textPaint) {
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setSubpixelText(true);
            textPaint.setTextAlign(Paint.Align.CENTER);
            if (null != typeface)
                textPaint.setTypeface(typeface);
        }

        int baseline = adjustTextSize(canvas, strokePaint, this.ch + "");
        baseline = adjustTextSize(canvas, textPaint, this.ch + "");

        if (drawType == DRAW_TYPE.NORMAL) {
            textPaint.setColor(original_fgcolor);
            canvas.drawText(this.ch + "", canvas.getWidth() / 2, canvas.getHeight() - baseline, textPaint);
        } else {
            // char fill
            textPaint.setColor(contour_bgcolor);
            canvas.drawText(this.ch + "", canvas.getWidth() / 2, canvas.getHeight() - baseline, textPaint);

            // char contour: use path
            Path path = new Path();
            textPaint.getTextPath(this.ch + "", 0, 1, canvas.getWidth() / 2, canvas.getHeight() - baseline, path);
            canvas.drawPath(path, strokePaint);

        }
    }

    private int adjustTextSize(Canvas canvas, Paint paint, String str) {
        paint.setTextSize(100);
        paint.setTextScaleX(1.0f);
        Rect bounds = new Rect();
        // ask the paint for the bounding rect if it were to draw this
        // text
        paint.getTextBounds(str, 0, str.length(), bounds);

        // get the height that would have been produced
        int h = bounds.bottom - bounds.top;

        // make the text text up 70% of the height
        float target = (float) canvas.getHeight() * .8f;

        // figure out what textSize setting would create that height of text
        float size = ((target / h) * 100f);
        // and set it into the paint
        paint.setTextSize(size);

        // do calculation with scale of 1.0 (no scale)
        paint.setTextScaleX(1.0f);
        bounds = new Rect();

        // ask the paint for the bounding rect if it were to draw this // text.
        paint.getTextBounds(str, 0, str.length(), bounds);

        // determine the width
        int w = bounds.right - bounds.left;

        // determine how much to scale the width to fit the view
        float xscale = ((float) (canvas.getWidth() - getPaddingLeft() - getPaddingRight())) / w;

        // set the scale for the text paint
        if (xscale < 1.0f)
            paint.setTextSize(size * xscale);

        // do calculation with scale of 1.0 (no scale)
        paint.setTextScaleX(1.0f);
        bounds = new Rect();

        // ask the paint for the bounding rect if it were to draw this // text.
        paint.getTextBounds(str, 0, str.length(), bounds);

        // determine the width
        w = bounds.right - bounds.left;

        // calculate the baseline to use so that the
        // entire text is visible including the descenders
        int text_h = bounds.bottom - bounds.top;
        int baseline = bounds.bottom + ((canvas.getHeight() - text_h) / 2);

        return baseline;
    }

    private Matrix mMatrix;
    private Matrix mDrawMatrix = null;
    @SuppressWarnings("unused")
    private RectF mTempSrc = new RectF();
    @SuppressWarnings("unused")
    private RectF mTempDst = new RectF();

    @SuppressWarnings("unused")
    private void configureBounds(Drawable drawable) {

        int dwidth = drawable.getIntrinsicWidth();
        int dheight = drawable.getIntrinsicHeight();

        int mPaddingTop = this.getPaddingTop();
        int mPaddingLeft = this.getPaddingLeft();
        int mPaddingRight = this.getPaddingRight();
        int mPaddingBottom = this.getPaddingBottom();

        int vwidth = getWidth() - mPaddingLeft - mPaddingRight;
        int vheight = getHeight() - mPaddingTop - mPaddingBottom;

        boolean fits = (dwidth < 0 || vwidth == dwidth) &&
                (dheight < 0 || vheight == dheight);

        if (dwidth <= 0 || dheight <= 0 || ScaleType.FIT_XY == getScaleType()) {
            /* If the drawable has no intrinsic size, or we're told to
                scaletofit, then we just fill our entire view.
            */
            drawable.setBounds(0, 0, vwidth, vheight);
            mDrawMatrix = null;
        } else {
            // We need to do the scaling ourself, so have the drawable
            // use its native size.
            drawable.setBounds(0, 0, dwidth, dheight);

            if (ScaleType.MATRIX == getScaleType()) {
                // Use the specified matrix as-is.
                if (mMatrix.isIdentity()) {
                    mDrawMatrix = null;
                } else {
                    mDrawMatrix = mMatrix;
                }
            } else if (fits) {
                // The bitmap fits exactly, no transform needed.
                mDrawMatrix = null;
            } else if (ScaleType.CENTER == getScaleType()) {
                // Center bitmap in view, no scaling.
                mDrawMatrix = mMatrix;
                mDrawMatrix.setTranslate((vwidth - dwidth) * 0.5f,
                        (vheight - dheight) * 0.5f);
            } else if (ScaleType.CENTER_CROP == getScaleType()) {
                mDrawMatrix = mMatrix;

                float scale;
                float dx = 0, dy = 0;

                if (dwidth * vheight > vwidth * dheight) {
                    scale = (float) vheight / (float) dheight;
                    dx = (vwidth - dwidth * scale) * 0.5f;
                } else {
                    scale = (float) vwidth / (float) dwidth;
                    dy = (vheight - dheight * scale) * 0.5f;
                }

                mDrawMatrix.setScale(scale, scale);
                mDrawMatrix.postTranslate(dx, dy);
            } else if (ScaleType.CENTER_INSIDE == getScaleType()) {
                mDrawMatrix = mMatrix;
                float scale;
                float dx;
                float dy;

                if (dwidth <= vwidth && dheight <= vheight) {
                    scale = 1.0f;
                } else {
                    scale = Math.min((float) vwidth / (float) dwidth,
                            (float) vheight / (float) dheight);
                }

                dx = (vwidth - dwidth * scale) * 0.5f;
                dy = (vheight - dheight * scale) * 0.5f;

                mDrawMatrix.setScale(scale, scale);
                mDrawMatrix.postTranslate(dx, dy);
            } else {
                // hard to handle
                // Generate the required transform.
                /*
                mTempSrc.set(0, 0, dwidth, dheight);
                mTempDst.set(0, 0, vwidth, vheight);
                
                mDrawMatrix = mMatrix;
                mDrawMatrix.setRectToRect(mTempSrc, mTempDst, scaleTypeToScaleToFit(getScaleType()));
               */
            }
        }
    }

    private Bitmap transparent(Bitmap src) {
        if(!enableRenderScript){
            return transparentByCPU(src);
        } else {
            Bitmap bitmap;
            try {
                    bitmap = runScript(src, drawType);
            } catch (RSRuntimeException e) {
                e.printStackTrace();
                bitmap = transparentByCPU(src);
            }
            return bitmap;
        }
    }

    private Bitmap contour(Bitmap src) {
        if(!enableRenderScript) {
            return contourByCPU(src);
        } else {
            Bitmap bitmap;
            try {
                //long start = System.currentTimeMillis();
                bitmap = runScript(src, drawType);
                //long rs = System.currentTimeMillis();
                //long cpuPixels = System.currentTimeMillis();
                /*
                if(bitmap.getWidth()>160)
                    Log.v("time", "rs:" + (rs - start) + "   cpu:" + (cpuPixels - rs));
                */
            } catch (RSRuntimeException e) {
                e.printStackTrace();
                bitmap = contourByCPU(src);
            }
            return bitmap;
        }
    }

    private Bitmap skeleton(Bitmap src) {
        return skeletonByCPU(src);
    }

    private Bitmap transparentByCPU(Bitmap src) {
        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        int width = src.getWidth();
        int height = src.getHeight();
        int picsize = width * height;
        int[] pixels = new int[picsize];
        src.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (BitmapUtils.equalsColor(pixels[x + y * width], original_fgcolor, COLOR_COMPARISON_PRECISENESS)) {
                    pixels[x + y * width] = original_fgcolor;
                } else {
                    pixels[x + y * width] = Color.TRANSPARENT;
                }
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private Bitmap contourByCPU(Bitmap src) {
        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        int width = src.getWidth();
        int height = src.getHeight();
        int picsize = width * height;
        int[] pixels = new int[picsize];
        src.getPixels(pixels, 0, width, 0, 0, width, height);
        int previousColor = 0;
        // get outline
        // left to right, top to down
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int currentColor = pixels[x + y * width];

                if (!BitmapUtils.equalsColor(previousColor, currentColor, 30)) {
                    pixels[x + y * width] = contour_color;
                } else {
                    if (BitmapUtils.equalsColor(previousColor, original_fgcolor, COLOR_COMPARISON_PRECISENESS)) {
                        pixels[x + y * width] = contour_bgcolor;
                    } else {
                        pixels[x + y * width] = Color.TRANSPARENT;
                    }
                }
                previousColor = currentColor;
            }
            if (BitmapUtils.equalsColor(src.getPixel(x, height - 1), original_fgcolor, COLOR_COMPARISON_PRECISENESS))
                pixels[x + (height - 1) * width] = contour_color;
        }

        // 2nd phase
        // top to down, left to right
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int currentColor = src.getPixel(x, y);

                if (!BitmapUtils.equalsColor(previousColor, currentColor, 30)) {
                    pixels[x + y * width] = contour_color;
                }
                previousColor = currentColor;
            }
            if (BitmapUtils.equalsColor(src.getPixel(width - 1, y), original_fgcolor, COLOR_COMPARISON_PRECISENESS))
                pixels[width - 1 + y * width] = contour_color;
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private Bitmap skeletonByCPU(Bitmap src) {
        return new ThinningService().getSkeletonBitmap(src);
    }

    public class ThinningService {

        public Bitmap getSkeletonBitmap(Bitmap src) {
            Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
            int width = src.getWidth();
            int height = src.getHeight();
            int picsize = width * height;
            int[] pixels = new int[picsize];
            src.getPixels(pixels, 0, width, 0, 0, width, height);
            int[][] tempArray = new int[width][height];
            //prepare array
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (pixels[i * width + j] == Color.BLACK) {
                        tempArray[j][i] = 1;
                    } else {
                        tempArray[j][i] = 0;
                    }
                }
            }
            //precess
            tempArray = doZhangSuenThinning(tempArray);
            // put back to pixels
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    pixels[i * width + j] = (tempArray[j][i] == 1) ? Color.BLACK : Color.TRANSPARENT;
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        }

        public int[][] doHilditchsThinning(int[][] binaryImage) {
            int a, b;
            boolean hasChange;
            do {
                hasChange = false;
                for (int y = 1; y + 1 < binaryImage.length; y++) {
                    for (int x = 1; x + 1 < binaryImage[y].length; x++) {
                        a = getA(binaryImage, y, x);
                        b = getB(binaryImage, y, x);
                        if (binaryImage[y][x] == 1 && 2 <= b && b <= 6 && a == 1
                                && ((binaryImage[y - 1][x] * binaryImage[y][x + 1] * binaryImage[y][x - 1] == 0) || (getA(binaryImage, y - 1, x) != 1))
                                && ((binaryImage[y - 1][x] * binaryImage[y][x + 1] * binaryImage[y + 1][x] == 0) || (getA(binaryImage, y, x + 1) != 1))) {
                            binaryImage[y][x] = 0;
                            hasChange = true;
                        }
                    }
                }
            } while (hasChange);

            return binaryImage;
        }

        public int[][] doZhangSuenThinning(int[][] binaryImage) {
            int a, b;

            List<Point> pointsToChange = new LinkedList<Point>();
            boolean hasChange;

            do {

                hasChange = false;
                for (int y = 1; y + 1 < binaryImage.length; y++) {
                    for (int x = 1; x + 1 < binaryImage[y].length; x++) {
                        a = getA(binaryImage, y, x);
                        b = getB(binaryImage, y, x);
                        if (binaryImage[y][x] == 1 && 2 <= b && b <= 6 && a == 1
                                && (binaryImage[y - 1][x] * binaryImage[y][x + 1] * binaryImage[y + 1][x] == 0)
                                && (binaryImage[y][x + 1] * binaryImage[y + 1][x] * binaryImage[y][x - 1] == 0)) {
                            pointsToChange.add(new Point(x, y));
                            //binaryImage[y][x] = 0;
                            hasChange = true;
                        }
                    }
                }

                for (Point point : pointsToChange) {
                    binaryImage[point.getY()][point.getX()] = 0;
                }

                pointsToChange.clear();

                for (int y = 1; y + 1 < binaryImage.length; y++) {
                    for (int x = 1; x + 1 < binaryImage[y].length; x++) {
                        a = getA(binaryImage, y, x);
                        b = getB(binaryImage, y, x);
                        if (binaryImage[y][x] == 1 && 2 <= b && b <= 6 && a == 1
                                && (binaryImage[y - 1][x] * binaryImage[y][x + 1] * binaryImage[y][x - 1] == 0)
                                && (binaryImage[y - 1][x] * binaryImage[y + 1][x] * binaryImage[y][x - 1] == 0)) {
                            pointsToChange.add(new Point(x, y));

                            hasChange = true;
                        }
                    }
                }

                for (Point point : pointsToChange) {
                    binaryImage[point.getY()][point.getX()] = 0;
                }

                pointsToChange.clear();

            } while (hasChange);

            return binaryImage;
        }

        private class Point {

            private int x, y;

            public Point(int x, int y) {
                this.x = x;
                this.y = y;
            }

            public int getX() {
                return x;
            }

            public void setX(int x) {
                this.x = x;
            }

            public int getY() {
                return y;
            }

            public void setY(int y) {
                this.y = y;
            }
        }

        ;

        private int getA(int[][] binaryImage, int y, int x) {

            int count = 0;
            //p2 p3
            if (y - 1 >= 0 && x + 1 < binaryImage[y].length && binaryImage[y - 1][x] == 0 && binaryImage[y - 1][x + 1] == 1) {
                count++;
            }
            //p3 p4
            if (y - 1 >= 0 && x + 1 < binaryImage[y].length && binaryImage[y - 1][x + 1] == 0 && binaryImage[y][x + 1] == 1) {
                count++;
            }
            //p4 p5
            if (y + 1 < binaryImage.length && x + 1 < binaryImage[y].length && binaryImage[y][x + 1] == 0 && binaryImage[y + 1][x + 1] == 1) {
                count++;
            }
            //p5 p6
            if (y + 1 < binaryImage.length && x + 1 < binaryImage[y].length && binaryImage[y + 1][x + 1] == 0 && binaryImage[y + 1][x] == 1) {
                count++;
            }
            //p6 p7
            if (y + 1 < binaryImage.length && x - 1 >= 0 && binaryImage[y + 1][x] == 0 && binaryImage[y + 1][x - 1] == 1) {
                count++;
            }
            //p7 p8
            if (y + 1 < binaryImage.length && x - 1 >= 0 && binaryImage[y + 1][x - 1] == 0 && binaryImage[y][x - 1] == 1) {
                count++;
            }
            //p8 p9
            if (y - 1 >= 0 && x - 1 >= 0 && binaryImage[y][x - 1] == 0 && binaryImage[y - 1][x - 1] == 1) {
                count++;
            }
            //p9 p2
            if (y - 1 >= 0 && x - 1 >= 0 && binaryImage[y - 1][x - 1] == 0 && binaryImage[y - 1][x] == 1) {
                count++;
            }

            return count;
        }

        private int getB(int[][] binaryImage, int y, int x) {

            return binaryImage[y - 1][x] + binaryImage[y - 1][x + 1] + binaryImage[y][x + 1]
                    + binaryImage[y + 1][x + 1] + binaryImage[y + 1][x] + binaryImage[y + 1][x - 1]
                    + binaryImage[y][x - 1] + binaryImage[y - 1][x - 1];
        }

    }
}
