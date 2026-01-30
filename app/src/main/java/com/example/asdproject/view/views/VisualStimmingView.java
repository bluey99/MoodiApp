package com.example.asdproject.view.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.example.asdproject.R;

import java.util.ArrayList;
import java.util.List;

/**
 * VisualStimmingView
 *
 * Calm visual stimming with gentle touch responsiveness.
 * Touch creates a soft attraction — never snapping or dragging.
 */
public class VisualStimmingView extends View {

    private static class Shape {
        float x, y;
        float radius;
        float vx, vy;
        int alpha;
    }

    private final List<Shape> shapes = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ValueAnimator animator;

    // user modifiers
    private float speedMultiplier = 1f;
    private float sizeMultiplier  = 1f;

    // touch state
    private boolean isTouching = false;
    private float touchX, touchY;

    public VisualStimmingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // ---------- PUBLIC API ----------

    public void setSpeedMultiplier(float multiplier) {
        speedMultiplier = multiplier;
    }

    public void setSizeMultiplier(float multiplier) {
        sizeMultiplier = multiplier;
    }

    // ---------- INIT ----------

    private void init() {
        setClickable(true);
        setFocusable(true);

        paint.setColor(0xFF68C4C5);
        paint.setStyle(Paint.Style.FILL);

        buildShapes();

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> invalidate());
        animator.start();
    }

    // ---------- SHAPES ----------

    private void buildShapes() {
        shapes.clear();

        // foreground
        addLayer(6, 110, 0.18f, 0.14f, 80);
        // mid
        addLayer(5, 180, 0.10f, 0.08f, 45);
        // background
        addLayer(4, 260, 0.05f, 0.04f, 25);
    }

    private void addLayer(int count, float radius, float vx, float vy, int alpha) {
        for (int i = 0; i < count; i++) {
            Shape s = new Shape();
            s.radius = radius;
            s.vx = (i % 2 == 0) ? vx : -vx;
            s.vy = (i % 3 == 0) ? vy : -vy;
            s.alpha = alpha;
            shapes.add(s);
        }
    }

    // ---------- LAYOUT ----------

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float cx = w / 2f;
        float cy = h / 2f;
        float spread = Math.min(w, h) / 3f;

        for (int i = 0; i < shapes.size(); i++) {
            Shape s = shapes.get(i);
            float angle = (float) (2 * Math.PI * i / shapes.size());
            s.x = cx + (float) Math.cos(angle) * spread;
            s.y = cy + (float) Math.sin(angle) * spread;
        }
    }

    // ---------- TOUCH ----------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                isTouching = true;
                touchX = event.getX();
                touchY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    // ---------- DRAW ----------

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        for (Shape s : shapes) {
            paint.setAlpha(s.alpha);

            //  attraction
            if (isTouching) {
                float dx = touchX - s.x;
                float dy = touchY - s.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;

                float speedFactor = speedMultiplier;

                // Attraction force
                float force = 0.04f * speedFactor;
                s.vx += (dx / dist) * force;
                s.vy += (dy / dist) * force;

                // --- DAMPING when close ---
                if (dist < 80f) {
                    // soften movement as we arrive
                    s.vx *= 0.85f;
                    s.vy *= 0.85f;
                }

                // --- VERY CLOSE: gentle "stick" ---
                if (dist < 20f) {
                    s.vx *= 0.6f;
                    s.vy *= 0.6f;
                }
            }




            // apply motion
            s.x += s.vx * speedMultiplier;
            s.y += s.vy * speedMultiplier;

            // soft wrap
            if (s.x + s.radius < 0) s.x = w + s.radius;
            if (s.x - s.radius > w) s.x = -s.radius;
            if (s.y + s.radius < 0) s.y = h + s.radius;
            if (s.y - s.radius > h) s.y = -s.radius;

            canvas.drawCircle(
                    s.x,
                    s.y,
                    s.radius * sizeMultiplier,
                    paint
            );
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }
}
