package com.example.expensetracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

public class AnimatedPieChartView extends View {

    private List<PieSlice> slices = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF bounds = new RectF();
    private float total = 0;
    private float animationProgress = 0f;
    private float rotationAngle = 0f;
    private static final long ANIMATION_DURATION = 1000; // 1 second animation
    private ValueAnimator animator;
    private GestureDetector gestureDetector;

    public static class PieSlice {
        String category;
        float value;
        int color;

        public PieSlice(String category, float value, int color) {
            this.category = category;
            this.value = value;
            this.color = color;
        }
    }

    public AnimatedPieChartView(Context context) {
        super(context);
        init();
    }

    public AnimatedPieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedPieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setColor(0xFFFFFFFF); // White text
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Set up gesture detector for rotation
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // Calculate rotation from finger movement
                float centerX = getWidth() / 2f;
                float centerY = getHeight() / 2f;

                // Calculate angles from center to touch points
                float angle1 = (float) Math.toDegrees(Math.atan2(e1.getY() - centerY, e1.getX() - centerX));
                float angle2 = (float) Math.toDegrees(Math.atan2(e2.getY() - centerY, e2.getX() - centerX));

                // Add the difference to our rotation
                rotationAngle += (angle2 - angle1);
                invalidate();
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    public void setSlices(List<PieSlice> slices) {
        this.slices = slices;
        total = 0;
        for (PieSlice slice : slices) {
            total += slice.value;
        }

        // Reset and start animation
        animationProgress = 0f;
        startAnimation();
    }

    private void startAnimation() {
        // Cancel any running animation
        if (animator != null) {
            animator.cancel();
        }

        // Create new animator from 0 to 1
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIMATION_DURATION);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animationProgress = (float) animation.getAnimatedValue();
                invalidate(); // Redraw the view
            }
        });

        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slices.isEmpty() || total == 0) {
            return;
        }

        // Calculate dimensions
        int width = getWidth();
        int height = getHeight();
        int padding = 20;
        int size = Math.min(width, height) - padding * 2;
        float radius = size / 2f;

        // Center the pie chart
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Set the bounds for the pie chart
        bounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        float startAngle = rotationAngle; // Apply rotation to starting angle

        // Draw each slice with animation
        for (PieSlice slice : slices) {
            float sweepAngle = (slice.value / total) * 360 * animationProgress;
            paint.setColor(slice.color);
            paint.setStyle(Paint.Style.FILL);

            // Draw main slice
            canvas.drawArc(bounds, startAngle, sweepAngle, true, paint);

            // Draw 3D effect (side shadow)
            paint.setColor(darkenColor(slice.color, 0.7f));
            paint.setStyle(Paint.Style.FILL);

            // Create a smaller arc for the 3D effect
            RectF innerBounds = new RectF(
                    bounds.left + 15,
                    bounds.top + 15,
                    bounds.right - 15,
                    bounds.bottom - 15
            );
            canvas.drawArc(innerBounds, startAngle, sweepAngle, true, paint);

            // Draw a thin separator between slices
            paint.setColor(0xFF333333); // Dark separator color
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(bounds, startAngle, sweepAngle, true, paint);

            // Only draw percentage text when animation is mostly complete (>80%)
            if (animationProgress > 0.8f) {
                // Calculate position for percentage text
                float midAngle = startAngle + (sweepAngle / 2);
                float percentageRadius = radius * 0.7f; // Position text at 70% of the radius
                float textX = centerX + (float) Math.cos(Math.toRadians(midAngle)) * percentageRadius;
                float textY = centerY + (float) Math.sin(Math.toRadians(midAngle)) * percentageRadius;

                // Only draw text for slices that have enough angle to be visible
                if (sweepAngle > 5) {
                    // Draw percentage text with fade-in effect
                    float textAlpha = Math.min(1f, (animationProgress - 0.8f) * 5f); // Fade in text
                    textPaint.setAlpha((int) (255 * textAlpha));

                    // Draw percentage text
                    float percentage = (slice.value / total) * 100;
                    String percentText = String.format("%.0f%%", percentage);
                    canvas.drawText(percentText, textX, textY + 8, textPaint); // +8 for vertical alignment
                }
            }

            startAngle += sweepAngle;
        }
    }

    // Helper method to darken a color for the 3D effect
    private int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xff;
        int r = Math.round(((color >> 16) & 0xff) * factor);
        int g = Math.round(((color >> 8) & 0xff) * factor);
        int b = Math.round((color & 0xff) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Add animation to highlight a slice on double tap
    public void highlightSlice(int position) {
        // Create a pulsing animation for the specified slice
        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(500);
        pulseAnimator.setRepeatCount(1);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new DecelerateInterpolator());

        final int slicePosition = position;
        pulseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Will be implemented in a future update
                invalidate();
            }
        });

        pulseAnimator.start();
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Clean up animator to prevent memory leaks
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }
}