package com.example.expensetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {

    private List<PieSlice> slices = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF bounds = new RectF();
    private float total = 0;

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

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setColor(0xFFFFFFFF); // White text
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public void setSlices(List<PieSlice> slices) {
        this.slices = slices;
        total = 0;
        for (PieSlice slice : slices) {
            total += slice.value;
        }
        invalidate();
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

        float startAngle = 0;

        // Draw each slice
        for (PieSlice slice : slices) {
            float sweepAngle = (slice.value / total) * 360;
            paint.setColor(slice.color);
            paint.setStyle(Paint.Style.FILL);

            canvas.drawArc(bounds, startAngle, sweepAngle, true, paint);

            // Draw a thin separator between slices
            paint.setColor(0xFF333333); // Dark separator color
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(bounds, startAngle, sweepAngle, true, paint);

            // Calculate position for percentage text
            float midAngle = startAngle + (sweepAngle / 2);
            float percentageRadius = radius * 0.7f; // Position text at 70% of the radius
            float textX = centerX + (float) Math.cos(Math.toRadians(midAngle)) * percentageRadius;
            float textY = centerY + (float) Math.sin(Math.toRadians(midAngle)) * percentageRadius;

            // Draw percentage text
            float percentage = (slice.value / total) * 100;
            String percentText = String.format("%.0f%%", percentage);
            canvas.drawText(percentText, textX, textY + 8, textPaint); // +8 for vertical alignment

            startAngle += sweepAngle;
        }
    }
}