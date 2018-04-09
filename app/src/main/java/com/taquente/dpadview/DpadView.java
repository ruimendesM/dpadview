package com.taquente.dpadview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class DpadView extends View {

    public static final int SECTION_BOTTOM = 0;
    public static final int SECTION_LEFT = 1;
    public static final int SECTION_TOP = 2;
    public static final int SECTION_RIGHT = 3;
    public static final int SECTION_CENTER = 4;

    private static final int DEFAULT_SECTION_COLOR = Color.BLUE;
    private static final int DEFAULT_ICON_COLOR = Color.WHITE;

    private Paint mXferPaint;
    private Paint mInnerCirclePaint;
    private Paint mDividerPaint;

    private int mDividerWidth;
    private int mDividerColor;

    // 0 - bottom, 1 - left, 2 - top, 3 - right, 4 - center
    private ColorStateList mSectionColors[] = new ColorStateList[4];
    private boolean[] mIsPressed = new boolean[5];
    private int[] mDrawableResources = new int[4];
    private float mInnerCirclePercentage;

    private float mInnerCircleRadius;

    private ColorStateList mDrawablesColor;
    private ColorStateList mInnerCircleColor;

    private OnSectionClickedListener mSectionClickedListener;

    public interface OnSectionClickedListener {

        void onSectionClicked(int section);
    }

    public DpadView(final Context context) {
        this(context, null);
    }

    public DpadView(final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DpadView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DpadView, 0, 0);
        try {
            mDividerWidth = a.getDimensionPixelSize(R.styleable.DpadView_dividerWidth, 0);
            mDividerColor = a.getColor(R.styleable.DpadView_dividerColor, Color.TRANSPARENT);
            mInnerCirclePercentage = a.getFloat(R.styleable.DpadView_innerCirclePercentage, 0.0f);
            mInnerCircleColor = a.getColorStateList(R.styleable.DpadView_innerCircleColor);
            if (mInnerCircleColor == null) {
                mInnerCircleColor = ColorStateList.valueOf(DEFAULT_ICON_COLOR);
            }
            if (a.hasValue(R.styleable.DpadView_sectionColor)) {
                for (int i = 0; i < mSectionColors.length; i++) {
                    mSectionColors[i] = a.getColorStateList(R.styleable.DpadView_sectionColor);
                }
            } else {
                mSectionColors[SECTION_BOTTOM] = a.getColorStateList(R.styleable.DpadView_bottomSectionColor);
                mSectionColors[SECTION_LEFT] = a.getColorStateList(R.styleable.DpadView_leftSectionColor);
                mSectionColors[SECTION_TOP] = a.getColorStateList(R.styleable.DpadView_topSectionColor);
                mSectionColors[SECTION_RIGHT] = a.getColorStateList(R.styleable.DpadView_rightSectionColor);
            }
            mDrawablesColor = a.getColorStateList(R.styleable.DpadView_drawablesColor);
            if (mDrawablesColor == null) {
                mDrawablesColor = ColorStateList.valueOf(DEFAULT_ICON_COLOR);
            }
            mDrawableResources[SECTION_BOTTOM] = a.getResourceId(R.styleable.DpadView_bottomDrawable, R.drawable.ic_down);
            mDrawableResources[SECTION_LEFT] = a.getResourceId(R.styleable.DpadView_leftDrawable, R.drawable.ic_left);
            mDrawableResources[SECTION_TOP] = a.getResourceId(R.styleable.DpadView_topDrawable, R.drawable.ic_up);
            mDrawableResources[SECTION_RIGHT] = a.getResourceId(R.styleable.DpadView_rightDrawable, R.drawable.ic_right);
        } finally {
            a.recycle();
        }

        mXferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInnerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDividerPaint.setStrokeWidth(mDividerWidth);
        mDividerPaint.setColor(mDividerColor);

        resetIsPressed();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        if (width > height) {
            setMeasuredDimension(height, height);
        } else {
            setMeasuredDimension(width, width);
        }
    }

    @Override
    public void draw(final Canvas canvas) {
        super.draw(canvas);

        final float halfWidth = getWidth() / 2f;
        final float halfHeight = getHeight() / 2f;

        final float radius = halfWidth > halfHeight ? halfHeight : halfWidth;
        mInnerCircleRadius = radius * Math.min(Math.abs(mInnerCirclePercentage), 1.0f);
        final float halfInnerRadius = mInnerCircleRadius / 2;

        Drawable background = getBackground();
        if (background != null) {
            background.draw(canvas);
        }

        Rect rect = new Rect();
        getDrawingRect(rect);
        RectF rectF = new RectF(rect);

        // draw arcs
        final int angleOffset = 45;
        for (int i = 0; i < mIsPressed.length - 1; i++) {
            int startAngle = angleOffset + (90 * i);
            drawArc(canvas, rectF, mSectionColors[i] != null ? mSectionColors[i] : ColorStateList.valueOf(DEFAULT_SECTION_COLOR), startAngle,
                    mIsPressed[i]);
        }

        // draw dividers (this need to be after arcs in order to be drawn above them)
        for (int i = 0; i < mIsPressed.length - 1; i++) {
            int startAngle = angleOffset + (90 * i);
            drawDivider(canvas, radius, startAngle, halfWidth, halfHeight);
        }

        // draw icons
        drawIcon(canvas, SECTION_TOP, halfWidth, halfHeight, halfInnerRadius);
        drawIcon(canvas, SECTION_BOTTOM, halfWidth, halfHeight, halfInnerRadius);
        drawIcon(canvas, SECTION_LEFT, halfWidth, halfHeight, halfInnerRadius);
        drawIcon(canvas, SECTION_RIGHT, halfWidth, halfHeight, halfInnerRadius);


        // draw innerCircle
        if (mInnerCircleRadius > 0) {
            // draw fill
            mInnerCirclePaint.setStyle(Paint.Style.FILL);
            mInnerCirclePaint.setColor(mIsPressed[SECTION_CENTER] ? mInnerCircleColor.getColorForState(new int[]{android.R.attr.state_pressed},
                    mInnerCircleColor.getDefaultColor()) : mInnerCircleColor.getDefaultColor());
            canvas.drawCircle(halfWidth, halfHeight, mInnerCircleRadius, mInnerCirclePaint);

            // draw stroke
            mInnerCirclePaint.setStyle(Paint.Style.STROKE);
            mInnerCirclePaint.setStrokeWidth(mDividerWidth);
            mInnerCirclePaint.setColor(mDividerColor);
            canvas.drawCircle(halfWidth, halfHeight, mInnerCircleRadius, mInnerCirclePaint);
        }
    }

    private void drawArc(Canvas canvas, RectF rectF, ColorStateList color, int startAngle, boolean isPressed) {
        mXferPaint.setColor(isPressed ? color.getColorForState(new int[]{android.R.attr.state_pressed}, color.getDefaultColor()) :
                color.getDefaultColor());
        canvas.drawArc(rectF, startAngle, 90, true, mXferPaint);
    }

    private void drawDivider(Canvas canvas, float radius, int startAngle, float halfWidth, float halfHeight) {
        canvas.drawLine(halfWidth, halfHeight,
                radius * (float) Math.cos(Math.toRadians(startAngle)) + halfWidth,
                radius * (float) Math.sin(Math.toRadians(startAngle)) + halfHeight,
                mDividerPaint);
    }

    private void drawIcon(Canvas canvas, int section, float halfWidth, float halfHeight, float halfInnerRadius) {
        Drawable drawableToDraw = ContextCompat.getDrawable(getContext(), mDrawableResources[section]);
        drawableToDraw.setColorFilter(mIsPressed[section] ? mDrawablesColor.getColorForState(new int[]{android.R.attr.state_pressed},
                mDrawablesColor.getDefaultColor()) : mDrawablesColor.getDefaultColor(), PorterDuff.Mode.SRC_IN);
        int halfDrawableWidth = drawableToDraw.getIntrinsicWidth() / 2;
        int halfDrawableHeight = drawableToDraw.getIntrinsicHeight() / 2;
        switch (section) {
            case SECTION_TOP:
                drawableToDraw.setBounds((int) halfWidth - halfDrawableWidth, //left
                        (int) ((halfHeight / 2) - halfInnerRadius - halfDrawableHeight), //top
                        (int) halfWidth + halfDrawableWidth,  //right
                        (int) ((halfHeight / 2) - halfInnerRadius + halfDrawableHeight)); //bottom
                break;
            case SECTION_BOTTOM:
                drawableToDraw.setBounds((int) halfWidth - halfDrawableWidth, //left
                        (int) (halfHeight + halfInnerRadius + (halfHeight / 2)) - halfDrawableHeight, //top
                        (int) halfWidth + halfDrawableWidth, //right
                        (int) (halfHeight + halfInnerRadius + (halfHeight / 2)) + halfDrawableHeight); //bottom
                break;
            case SECTION_LEFT:
                drawableToDraw.setBounds((int) ((halfWidth / 2) - halfInnerRadius - halfDrawableWidth), //left
                        (int) halfHeight - halfDrawableHeight, //top
                        (int) ((halfWidth / 2) - halfInnerRadius + halfDrawableWidth), //bottom
                        (int) halfHeight + halfDrawableHeight); //right
                break;
            case SECTION_RIGHT:
                drawableToDraw.setBounds((int) (halfWidth + halfInnerRadius + (halfWidth / 2)) - halfDrawableWidth, //left
                        (int) halfHeight - halfDrawableHeight, //top
                        (int) (halfWidth + halfInnerRadius + (halfWidth / 2)) + halfDrawableWidth, //right
                        (int) halfHeight + halfDrawableHeight); //bottom
                break;
            default:
                return;
        }
        drawableToDraw.draw(canvas);

    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int action = event.getAction();
        final float x = event.getX() - getWidth() / 2f;
        final float y = event.getY() - getHeight() / 2f;

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {

            final float radius = (float) Math.sqrt(x * x + y * y);

            if (radius > getWidth() / 2f || radius > getHeight() / 2f) {
                // click outside the drawn view
                MotionEvent cancelEvent = MotionEvent.obtain(event);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                dispatchTouchEvent(cancelEvent);
                cancelEvent.recycle();
                resetIsPressed();
                return false;
            }

            if (radius < mInnerCircleRadius) {
                // click inside of inner circle
                resetIsPressed();
                mIsPressed[SECTION_CENTER] = true;
            } else {
                // click on drawn view but outside inner circle
                float touchAngle = (float) Math.toDegrees(Math.atan2(y, x));

                if (touchAngle < 0) {
                    touchAngle += 360f;
                }

                for (int i = 0; i < mIsPressed.length - 1; i++) {
                    int startAngle = 45 + (90 * i) % 360;
                    int endAngle = (startAngle + 90) % 360;

                    if (startAngle > endAngle) {
                        if (touchAngle < startAngle && touchAngle < endAngle) {
                            touchAngle += 360;
                        }
                        endAngle += 360;
                    }

                    if (startAngle <= touchAngle && endAngle >= touchAngle) {
                        resetIsPressed();
                        mIsPressed[i] = true;
                    }
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            for (int i = 0; i < mIsPressed.length; i++) {
                if (mIsPressed[i]) {
                    if (mSectionClickedListener != null) {
                        mSectionClickedListener.onSectionClicked(i);
                    }
                    break;
                }
            }
            resetIsPressed();
        }
        this.invalidate();
        return true;
    }

    private void resetIsPressed() {
        for (int i = 0; i < mIsPressed.length; i++) {
            mIsPressed[i] = false;
        }
    }

    public void setOnSectionClickedListener(final OnSectionClickedListener sectionClickedListener) {
        mSectionClickedListener = sectionClickedListener;
    }

}
