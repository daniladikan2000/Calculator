package com.example.javaprogect;

import android.content.Context;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import androidx.appcompat.widget.AppCompatTextView;

public class AutoResizeTextView extends AppCompatTextView {

    private static final float DEFAULT_MIN_TEXT_SIZE_PX = 20f;
    private static final float DEFAULT_MAX_TEXT_SIZE_PX = 80f;
    private static final float PRECISION_PX = 0.5f;

    private float currentMinTextSizePx;
    private float currentMaxTextSizePx;
    private final TextPaint textMeasurePaint;
    private boolean needsAdjustment = false;

    public AutoResizeTextView(Context context) {
        this(context, null);
    }

    public AutoResizeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AutoResizeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        currentMinTextSizePx = DEFAULT_MIN_TEXT_SIZE_PX;
        currentMaxTextSizePx = DEFAULT_MAX_TEXT_SIZE_PX;

        textMeasurePaint = new TextPaint();
        textMeasurePaint.set(getPaint());
    }

    public void setMinTextSizeInPixels(float minSizePx) {
        if (this.currentMinTextSizePx != minSizePx) {
            this.currentMinTextSizePx = minSizePx;
            needsAdjustment = true;
            requestLayout();
            invalidate();
        }
    }

    public void setMaxTextSizeInPixels(float maxSizePx) {
        if (this.currentMaxTextSizePx != maxSizePx) {
            this.currentMaxTextSizePx = maxSizePx;
            needsAdjustment = true;
            requestLayout();
            invalidate();
        }
    }

    public void setMinTextSizeInSp(float minSizeSp) {
        float minSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, minSizeSp, getResources().getDisplayMetrics());
        setMinTextSizeInPixels(minSizePx);
    }

    public void setMaxTextSizeInSp(float maxSizeSp) {
        float maxSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, maxSizeSp, getResources().getDisplayMetrics());
        setMaxTextSizeInPixels(maxSizePx);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        needsAdjustment = true;
        adjustTextSize();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width != oldWidth || height != oldHeight) {
            needsAdjustment = true;
            adjustTextSize();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed || needsAdjustment) {
            needsAdjustment = false;
            adjustTextSize();
        }
    }

    public void adjustTextSize() {
        int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (availableWidth <= 0 || availableHeight <= 0) {
            return;
        }

        CharSequence currentText = getText();
        if (currentText == null || currentText.length() == 0) {
            return;
        }

        textMeasurePaint.set(getPaint());

        float low = currentMinTextSizePx;
        float high = currentMaxTextSizePx;
        float bestSize = currentMinTextSizePx;

        while ((high - low) > PRECISION_PX) {
            float mid = (low + high) / 2;
            if (mid <= low) {
                break;
            }
            textMeasurePaint.setTextSize(mid);
            if (doesTextFit(currentText, textMeasurePaint, availableWidth, availableHeight)) {
                bestSize = mid;
                low = mid;
            } else {
                high = mid;
            }
        }

        textMeasurePaint.setTextSize(bestSize);
        if (Math.abs(getTextSize() - bestSize) > PRECISION_PX / 2) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, bestSize);
        }
    }

    private boolean doesTextFit(CharSequence text, TextPaint paint, int targetWidth, int targetHeight) {
        float textWidth = paint.measureText(text, 0, text.length());
        return textWidth <= targetWidth;
    }
}