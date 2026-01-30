package com.juancavr6.regibot.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;

import com.google.mediapipe.tasks.components.containers.Detection;
import com.juancavr6.regibot.ml.ModelHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager for the debug overlay.
 * Handles creation, visibility, and updates of the debug overlay view.
 */
public class DebugOverlayManager {

    private static DebugOverlayManager instance;

    private final Context context;
    private final WindowManager windowManager;
    private final Handler mainHandler;

    private DebugOverlayView debugOverlayView;
    private boolean isShowing = false;
    private boolean isEnabled = false;

    private DebugOverlayManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized DebugOverlayManager getInstance(Context context) {
        if (instance == null) {
            instance = new DebugOverlayManager(context);
        }
        return instance;
    }

    /**
     * Enable or disable the debug overlay feature.
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (!enabled && isShowing) {
            hide();
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Show the debug overlay on screen.
     */
    public void show() {
        if (!isEnabled || isShowing) return;

        mainHandler.post(() -> {
            if (debugOverlayView == null) {
                debugOverlayView = new DebugOverlayView(context);
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;

            try {
                windowManager.addView(debugOverlayView, params);
                isShowing = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Hide the debug overlay.
     */
    public void hide() {
        if (!isShowing || debugOverlayView == null) return;

        mainHandler.post(() -> {
            try {
                windowManager.removeView(debugOverlayView);
                isShowing = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Toggle the debug overlay visibility.
     */
    public boolean toggle() {
        if (isShowing) {
            hide();
        } else {
            setEnabled(true);
            show();
        }
        return isShowing;
    }

    /**
     * Clear all debug drawings.
     */
    public void clearAll() {
        if (debugOverlayView != null) {
            mainHandler.post(() -> debugOverlayView.clearDetections());
        }
    }

    /**
     * Update the classifier result display.
     */
    public void updateClassifier(ModelHandler.Classifier classifier) {
        if (!isEnabled || !isShowing || debugOverlayView == null || classifier == null) return;

        String className = classifier.getClassName(0);
        float score = classifier.getScore(0);

        mainHandler.post(() -> debugOverlayView.setClassifierResult(className, score));
    }

    /**
     * Update detections from the map detector model.
     */
    public void updateMapDetector(ModelHandler.Detector detector) {
        updateDetector(detector, DebugOverlayView.MODEL_MAP);
    }

    /**
     * Update detections from the encounter detector model.
     */
    public void updateEncounterDetector(ModelHandler.Detector detector) {
        updateDetector(detector, DebugOverlayView.MODEL_ENCOUNTER);
    }

    /**
     * Update detections from the clickable detector model.
     */
    public void updateClickableDetector(ModelHandler.Detector detector) {
        updateDetector(detector, DebugOverlayView.MODEL_CLICKABLE);
    }

    /**
     * Update detections from a detector model with the specified model type.
     */
    private void updateDetector(ModelHandler.Detector detector, String modelType) {
        if (!isEnabled || !isShowing || debugOverlayView == null || detector == null) return;

        List<Detection> detections = detector.getDetectionList();
        if (detections == null || detections.isEmpty()) {
            // Clear detections for this model
            mainHandler.post(() -> debugOverlayView.setDetectionsForModel(
                    modelType, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
            return;
        }

        List<RectF> boxes = new ArrayList<>();
        List<String> classNames = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();

        for (int i = 0; i < detections.size(); i++) {
            RectF box = detector.getBoundingBox(i);
            String className = detector.getClassName(i);
            float score = detector.getScore(i);

            if (box != null && className != null) {
                boxes.add(box);
                classNames.add(className);
                confidences.add(score);
            }
        }

        mainHandler.post(() -> debugOverlayView.setDetectionsForModel(modelType, boxes, classNames, confidences));
    }

    /**
     * Update predictor visualization (shows target point and trajectory info).
     */
    public void updatePredictor(float[] pokeballCoords, RectF targetBox, float deltaY, float duration) {
        if (!isEnabled || !isShowing || debugOverlayView == null) return;

        // Create a small box around the predicted target point
        float targetX = targetBox != null ? targetBox.centerX() : pokeballCoords[0];
        float targetY = pokeballCoords[1] + deltaY;

        List<RectF> boxes = new ArrayList<>();
        List<String> classNames = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();

        // Target point indicator
        RectF targetPoint = new RectF(targetX - 20, targetY - 20, targetX + 20, targetY + 20);
        boxes.add(targetPoint);
        classNames.add(String.format("dY:%.0f d:%.0fms", deltaY, duration));
        confidences.add(1.0f);

        mainHandler.post(() -> debugOverlayView.setDetectionsForModel(
                DebugOverlayView.MODEL_PREDICTOR, boxes, classNames, confidences));
    }

    /**
     * Check if the overlay is currently visible.
     */
    public boolean isShowing() {
        return isShowing;
    }

    /**
     * Destroy the manager and clean up resources.
     */
    public void destroy() {
        hide();
        debugOverlayView = null;
        instance = null;
    }
}
