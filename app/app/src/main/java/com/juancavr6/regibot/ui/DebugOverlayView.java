package com.juancavr6.regibot.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom view that draws debug bounding boxes for each ML model detection.
 * Each model has a distinct color for easy identification.
 */
public class DebugOverlayView extends View {

    // Model colors
    public static final int COLOR_CLASSIFIER = Color.parseColor("#FF9800");  // Orange
    public static final int COLOR_MAP_DETECTOR = Color.parseColor("#4CAF50");  // Green
    public static final int COLOR_ENCOUNTER_DETECTOR = Color.parseColor("#2196F3");  // Blue
    public static final int COLOR_CLICKABLE_DETECTOR = Color.parseColor("#9C27B0");  // Purple
    public static final int COLOR_PREDICTOR = Color.parseColor("#F44336");  // Red

    // Model identifiers
    public static final String MODEL_CLASSIFIER = "classifier";
    public static final String MODEL_MAP = "map";
    public static final String MODEL_ENCOUNTER = "encounter";
    public static final String MODEL_CLICKABLE = "clickable";
    public static final String MODEL_PREDICTOR = "predictor";

    private final Paint boxPaint;
    private final Paint textPaint;
    private final Paint textBackgroundPaint;

    private final List<DebugDetection> detections = new ArrayList<>();
    private String classifierLabel = null;

    public DebugOverlayView(Context context) {
        super(context);

        // Box paint setup
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setAntiAlias(true);

        // Text paint setup
        textPaint = new Paint();
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);

        // Text background paint
        textBackgroundPaint = new Paint();
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setColor(Color.parseColor("#99000000"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw classifier label at top
        if (classifierLabel != null) {
            textPaint.setColor(COLOR_CLASSIFIER);
            float textWidth = textPaint.measureText(classifierLabel);
            float x = (getWidth() - textWidth) / 2f;
            float y = 80f;

            // Background
            canvas.drawRect(x - 10, y - 35, x + textWidth + 10, y + 10, textBackgroundPaint);
            // Text
            canvas.drawText(classifierLabel, x, y, textPaint);
        }

        // Draw all detection boxes
        synchronized (detections) {
            for (DebugDetection detection : detections) {
                int color = getColorForModel(detection.modelType);
                boxPaint.setColor(color);
                textPaint.setColor(color);

                // Draw bounding box
                canvas.drawRect(detection.boundingBox, boxPaint);

                // Draw label with background
                String label = String.format("%s: %s (%.0f%%)",
                        detection.modelType, detection.className, detection.confidence * 100);
                float textWidth = textPaint.measureText(label);
                float textX = detection.boundingBox.left;
                float textY = detection.boundingBox.top - 10;

                // Ensure text stays on screen
                if (textY < 40) {
                    textY = detection.boundingBox.bottom + 35;
                }
                if (textX + textWidth > getWidth()) {
                    textX = getWidth() - textWidth - 10;
                }

                // Background
                canvas.drawRect(textX - 5, textY - 30, textX + textWidth + 5, textY + 5, textBackgroundPaint);
                // Text
                canvas.drawText(label, textX, textY, textPaint);
            }
        }
    }

    private int getColorForModel(String modelType) {
        switch (modelType) {
            case MODEL_CLASSIFIER:
                return COLOR_CLASSIFIER;
            case MODEL_MAP:
                return COLOR_MAP_DETECTOR;
            case MODEL_ENCOUNTER:
                return COLOR_ENCOUNTER_DETECTOR;
            case MODEL_CLICKABLE:
                return COLOR_CLICKABLE_DETECTOR;
            case MODEL_PREDICTOR:
                return COLOR_PREDICTOR;
            default:
                return Color.WHITE;
        }
    }

    /**
     * Update the classifier result displayed at the top of the screen.
     */
    public void setClassifierResult(String className, float confidence) {
        this.classifierLabel = String.format("Screen: %s (%.0f%%)", className, confidence * 100);
        postInvalidate();
    }

    /**
     * Clear all detections and redraw.
     */
    public void clearDetections() {
        synchronized (detections) {
            detections.clear();
        }
        classifierLabel = null;
        postInvalidate();
    }

    /**
     * Add a detection to be drawn on the overlay.
     */
    public void addDetection(String modelType, RectF boundingBox, String className, float confidence) {
        synchronized (detections) {
            detections.add(new DebugDetection(modelType, boundingBox, className, confidence));
        }
        postInvalidate();
    }

    /**
     * Set all detections for a specific model type (replaces previous detections of same type).
     */
    public void setDetectionsForModel(String modelType, List<RectF> boxes, List<String> classNames, List<Float> confidences) {
        synchronized (detections) {
            // Remove existing detections for this model
            detections.removeIf(d -> d.modelType.equals(modelType));

            // Add new detections
            for (int i = 0; i < boxes.size(); i++) {
                detections.add(new DebugDetection(
                        modelType,
                        boxes.get(i),
                        classNames.get(i),
                        confidences.get(i)
                ));
            }
        }
        postInvalidate();
    }

    /**
     * Data class to hold detection information.
     */
    private static class DebugDetection {
        final String modelType;
        final RectF boundingBox;
        final String className;
        final float confidence;

        DebugDetection(String modelType, RectF boundingBox, String className, float confidence) {
            this.modelType = modelType;
            this.boundingBox = boundingBox;
            this.className = className;
            this.confidence = confidence;
        }
    }
}
