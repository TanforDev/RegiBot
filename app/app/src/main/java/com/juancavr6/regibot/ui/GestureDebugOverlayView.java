package com.juancavr6.regibot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Custom view that draws debug visualizations for gestures (taps, swipes, throws).
 * Gestures fade out over time for better visualization.
 */
public class GestureDebugOverlayView extends View {

    // Gesture colors
    public static final int COLOR_TAP = Color.parseColor("#FFEB3B");       // Yellow
    public static final int COLOR_SWIPE = Color.parseColor("#00BCD4");     // Cyan
    public static final int COLOR_THROW = Color.parseColor("#E91E63");     // Pink
    public static final int COLOR_HOLD = Color.parseColor("#FF5722");      // Deep Orange

    // Gesture types
    public static final String GESTURE_TAP = "tap";
    public static final String GESTURE_SWIPE = "swipe";
    public static final String GESTURE_THROW = "throw";
    public static final String GESTURE_HOLD = "hold";

    private static final long GESTURE_LIFETIME_MS = 2000; // Gestures fade after 2 seconds
    private static final float TAP_RADIUS = 30f;
    private static final float POINT_RADIUS = 8f;

    private final Paint linePaint;
    private final Paint circlePaint;
    private final Paint textPaint;

    private final List<DebugGesture> gestures = new ArrayList<>();

    public GestureDebugOverlayView(Context context) {
        super(context);

        // Line paint for paths
        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // Circle paint for points
        circlePaint = new Paint();
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAntiAlias(true);

        // Text paint for labels
        textPaint = new Paint();
        textPaint.setTextSize(28f);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long currentTime = System.currentTimeMillis();
        boolean needsRedraw = false;

        synchronized (gestures) {
            // Remove expired gestures
            Iterator<DebugGesture> iterator = gestures.iterator();
            while (iterator.hasNext()) {
                DebugGesture gesture = iterator.next();
                if (currentTime - gesture.timestamp > GESTURE_LIFETIME_MS) {
                    iterator.remove();
                }
            }

            // Draw remaining gestures
            for (DebugGesture gesture : gestures) {
                float alpha = 1.0f - ((float)(currentTime - gesture.timestamp) / GESTURE_LIFETIME_MS);
                alpha = Math.max(0, Math.min(1, alpha));

                int color = getColorForGesture(gesture.type);
                int alphaInt = (int)(alpha * 255);

                drawGesture(canvas, gesture, color, alphaInt);
                needsRedraw = true;
            }
        }

        // Schedule redraw if there are active gestures
        if (needsRedraw) {
            postInvalidateDelayed(16); // ~60fps
        }
    }

    private void drawGesture(Canvas canvas, DebugGesture gesture, int color, int alpha) {
        switch (gesture.type) {
            case GESTURE_TAP:
                drawTap(canvas, gesture, color, alpha);
                break;
            case GESTURE_HOLD:
                drawHold(canvas, gesture, color, alpha);
                break;
            case GESTURE_SWIPE:
            case GESTURE_THROW:
                drawPath(canvas, gesture, color, alpha);
                break;
        }
    }

    private void drawTap(Canvas canvas, DebugGesture gesture, int color, int alpha) {
        circlePaint.setColor(color);
        circlePaint.setAlpha(alpha);

        // Draw outer circle
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(4f);
        canvas.drawCircle(gesture.startX, gesture.startY, TAP_RADIUS, circlePaint);

        // Draw inner filled circle
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAlpha(alpha / 2);
        canvas.drawCircle(gesture.startX, gesture.startY, TAP_RADIUS * 0.6f, circlePaint);

        // Draw crosshair
        linePaint.setColor(color);
        linePaint.setAlpha(alpha);
        linePaint.setStrokeWidth(2f);
        canvas.drawLine(gesture.startX - TAP_RADIUS, gesture.startY,
                       gesture.startX + TAP_RADIUS, gesture.startY, linePaint);
        canvas.drawLine(gesture.startX, gesture.startY - TAP_RADIUS,
                       gesture.startX, gesture.startY + TAP_RADIUS, linePaint);

        // Draw label
        textPaint.setColor(color);
        textPaint.setAlpha(alpha);
        canvas.drawText("TAP", gesture.startX + TAP_RADIUS + 10, gesture.startY + 10, textPaint);
    }

    private void drawHold(Canvas canvas, DebugGesture gesture, int color, int alpha) {
        circlePaint.setColor(color);
        circlePaint.setAlpha(alpha / 3);
        circlePaint.setStyle(Paint.Style.FILL);

        // Draw pulsing circles for hold
        float radius = TAP_RADIUS * 1.5f;
        canvas.drawCircle(gesture.startX, gesture.startY, radius, circlePaint);

        circlePaint.setAlpha(alpha / 2);
        canvas.drawCircle(gesture.startX, gesture.startY, radius * 0.7f, circlePaint);

        circlePaint.setAlpha(alpha);
        canvas.drawCircle(gesture.startX, gesture.startY, radius * 0.4f, circlePaint);

        // Draw label with duration
        textPaint.setColor(color);
        textPaint.setAlpha(alpha);
        String label = String.format("HOLD %dms", gesture.duration);
        canvas.drawText(label, gesture.startX + radius + 10, gesture.startY + 10, textPaint);
    }

    private void drawPath(Canvas canvas, DebugGesture gesture, int color, int alpha) {
        linePaint.setColor(color);
        linePaint.setAlpha(alpha);
        linePaint.setStrokeWidth(6f);

        // Draw the path line
        Path path = new Path();
        path.moveTo(gesture.startX, gesture.startY);
        path.lineTo(gesture.endX, gesture.endY);
        canvas.drawPath(path, linePaint);

        // Draw start point (larger)
        circlePaint.setColor(color);
        circlePaint.setAlpha(alpha);
        circlePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(gesture.startX, gesture.startY, POINT_RADIUS * 1.5f, circlePaint);

        // Draw end point with arrow-like indicator
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(4f);
        canvas.drawCircle(gesture.endX, gesture.endY, POINT_RADIUS * 2f, circlePaint);
        circlePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(gesture.endX, gesture.endY, POINT_RADIUS, circlePaint);

        // Draw arrowhead at end
        drawArrowhead(canvas, gesture.startX, gesture.startY, gesture.endX, gesture.endY, color, alpha);

        // Draw label with duration
        textPaint.setColor(color);
        textPaint.setAlpha(alpha);
        String label = gesture.type.equals(GESTURE_THROW)
            ? String.format("THROW %dms", gesture.duration)
            : String.format("SWIPE %dms", gesture.duration);
        float labelX = (gesture.startX + gesture.endX) / 2 + 20;
        float labelY = (gesture.startY + gesture.endY) / 2;
        canvas.drawText(label, labelX, labelY, textPaint);
    }

    private void drawArrowhead(Canvas canvas, float startX, float startY, float endX, float endY, int color, int alpha) {
        float angle = (float) Math.atan2(endY - startY, endX - startX);
        float arrowLength = 30f;
        float arrowAngle = (float) Math.toRadians(25);

        float x1 = endX - arrowLength * (float) Math.cos(angle - arrowAngle);
        float y1 = endY - arrowLength * (float) Math.sin(angle - arrowAngle);
        float x2 = endX - arrowLength * (float) Math.cos(angle + arrowAngle);
        float y2 = endY - arrowLength * (float) Math.sin(angle + arrowAngle);

        linePaint.setColor(color);
        linePaint.setAlpha(alpha);
        linePaint.setStrokeWidth(4f);
        canvas.drawLine(endX, endY, x1, y1, linePaint);
        canvas.drawLine(endX, endY, x2, y2, linePaint);
    }

    private int getColorForGesture(String type) {
        switch (type) {
            case GESTURE_TAP:
                return COLOR_TAP;
            case GESTURE_SWIPE:
                return COLOR_SWIPE;
            case GESTURE_THROW:
                return COLOR_THROW;
            case GESTURE_HOLD:
                return COLOR_HOLD;
            default:
                return Color.WHITE;
        }
    }

    /**
     * Add a tap gesture visualization.
     */
    public void addTap(float x, float y) {
        synchronized (gestures) {
            gestures.add(new DebugGesture(GESTURE_TAP, x, y, x, y, 10));
        }
        postInvalidate();
    }

    /**
     * Add a hold gesture visualization.
     */
    public void addHold(float x, float y, long durationMs) {
        synchronized (gestures) {
            gestures.add(new DebugGesture(GESTURE_HOLD, x, y, x, y, durationMs));
        }
        postInvalidate();
    }

    /**
     * Add a swipe gesture visualization.
     */
    public void addSwipe(float startX, float startY, float endX, float endY, long durationMs) {
        synchronized (gestures) {
            gestures.add(new DebugGesture(GESTURE_SWIPE, startX, startY, endX, endY, durationMs));
        }
        postInvalidate();
    }

    /**
     * Add a throw gesture visualization.
     */
    public void addThrow(float startX, float startY, float endX, float endY, long durationMs) {
        synchronized (gestures) {
            gestures.add(new DebugGesture(GESTURE_THROW, startX, startY, endX, endY, durationMs));
        }
        postInvalidate();
    }

    /**
     * Clear all gestures.
     */
    public void clearGestures() {
        synchronized (gestures) {
            gestures.clear();
        }
        postInvalidate();
    }

    /**
     * Data class to hold gesture information.
     */
    private static class DebugGesture {
        final String type;
        final float startX, startY;
        final float endX, endY;
        final long duration;
        final long timestamp;

        DebugGesture(String type, float startX, float startY, float endX, float endY, long duration) {
            this.type = type;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.duration = duration;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
