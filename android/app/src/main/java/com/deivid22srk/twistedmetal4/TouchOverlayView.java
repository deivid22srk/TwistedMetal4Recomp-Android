package com.deivid22srk.twistedmetal4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * Visual touch HUD. Normal gameplay touches fall through to SDLSurface; only
 * the small settings button is consumed by this view.
 */
final class TouchOverlayView extends View {
    interface Listener {
        void onTouchSettingsPressed();
    }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;
    private boolean analog;
    private boolean hudVisible = true;
    private float opacity = 0.90f;
    private boolean settingsTouch;

    TouchOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(3.0f);
    }

    void setListener(Listener listener) { this.listener = listener; }

    void setControlMode(boolean analog) {
        this.analog = analog;
        invalidate();
    }

    void setHudVisible(boolean visible) {
        hudVisible = visible;
        invalidate();
    }

    void setHudOpacity(float opacity) {
        this.opacity = Math.max(0.35f, Math.min(1.0f, opacity));
        invalidate();
    }

    boolean isHudVisible() { return hudVisible; }

    private int alpha(int base) {
        return Math.max(10, Math.min(255, (int)(base * opacity)));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final float x = event.getX() / Math.max(1.0f, getWidth());
        final float y = event.getY() / Math.max(1.0f, getHeight());
        final boolean settingsZone = x < 0.17f && y > 0.84f;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && settingsZone) {
            settingsTouch = true;
            if (listener != null) listener.onTouchSettingsPressed();
            return true;
        }
        if (settingsTouch) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                settingsTouch = false;
            }
            return true;
        }
        // D-pad, analog, face and shoulder touches remain owned by SDL.
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;
        final float unit = Math.min(w, h);
        if (!hudVisible) {
            drawSettingsButton(canvas, w * 0.095f, h * 0.91f, unit * 0.047f);
            return;
        }
        final float padR = unit * 0.105f;
        final float cx = w * 0.095f;
        final float cy = h * 0.70f;
        final float fx = w * 0.905f;
        final float fy = h * 0.70f;

        if (analog) drawAnalogStick(canvas, cx, cy, padR * 1.12f);
        else drawDpad(canvas, cx, cy, padR);

        drawFaceButton(canvas, fx - padR * 0.92f, fy, padR * 0.72f,
                "■", Color.rgb(241, 105, 166));
        drawFaceButton(canvas, fx + padR * 0.92f, fy, padR * 0.72f,
                "○", Color.rgb(242, 92, 91));
        drawFaceButton(canvas, fx, fy - padR * 0.92f, padR * 0.72f,
                "△", Color.rgb(84, 205, 146));
        drawFaceButton(canvas, fx, fy + padR * 0.92f, padR * 0.72f,
                "×", Color.rgb(83, 154, 238));

        drawPill(canvas, w * 0.09f, h * 0.065f, w * 0.14f, h * 0.062f, "L2");
        drawPill(canvas, w * 0.09f, h * 0.155f, w * 0.14f, h * 0.062f, "L1");
        drawPill(canvas, w * 0.91f, h * 0.065f, w * 0.14f, h * 0.062f, "R2");
        drawPill(canvas, w * 0.91f, h * 0.155f, w * 0.14f, h * 0.062f, "R1");
        drawPill(canvas, w * 0.50f, h * 0.065f, w * 0.13f, h * 0.054f, "SELECT");
        drawPill(canvas, w * 0.50f, h * 0.145f, w * 0.13f, h * 0.054f, "START");
        drawSettingsButton(canvas, w * 0.095f, h * 0.91f, unit * 0.047f);
    }

    private void drawDpad(Canvas canvas, float cx, float cy, float r) {
        glow.setColor(Color.argb(alpha(72), 90, 180, 255));
        canvas.drawCircle(cx, cy, r * 1.65f, glow);
        drawCircleButton(canvas, cx, cy - r * 0.92f, r * 0.72f, "▲", Color.rgb(84, 171, 231));
        drawCircleButton(canvas, cx, cy + r * 0.92f, r * 0.72f, "▼", Color.rgb(84, 171, 231));
        drawCircleButton(canvas, cx - r * 0.92f, cy, r * 0.72f, "◀", Color.rgb(84, 171, 231));
        drawCircleButton(canvas, cx + r * 0.92f, cy, r * 0.72f, "▶", Color.rgb(84, 171, 231));
    }

    private void drawAnalogStick(Canvas canvas, float cx, float cy, float r) {
        fill.setColor(Color.argb(alpha(42), 72, 190, 255));
        canvas.drawCircle(cx, cy, r * 1.35f, fill);
        stroke.setColor(Color.argb(alpha(160), 105, 208, 255));
        stroke.setStrokeWidth(3.0f);
        canvas.drawCircle(cx, cy, r * 1.35f, stroke);
        stroke.setStrokeWidth(1.5f);
        canvas.drawCircle(cx, cy, r * 0.82f, stroke);
        fill.setColor(Color.argb(alpha(155), 94, 192, 245));
        canvas.drawCircle(cx, cy, r * 0.64f, fill);
        fill.setColor(Color.argb(alpha(185), 222, 246, 255));
        canvas.drawCircle(cx - r * 0.16f, cy - r * 0.16f, r * 0.22f, fill);
        drawLabel(canvas, cx, cy + r * 1.72f, "ANALÓGICO", r * 0.22f);
    }

    private void drawCircleButton(Canvas canvas, float x, float y, float radius,
                                  String label, int color) {
        fill.setColor(withAlpha(alpha(55), color));
        canvas.drawCircle(x, y, radius, fill);
        stroke.setColor(Color.argb(alpha(185), 240, 248, 255));
        stroke.setStrokeWidth(2.0f);
        canvas.drawCircle(x, y, radius, stroke);
        drawLabel(canvas, x, y, label, radius * 0.78f);
    }

    private void drawFaceButton(Canvas canvas, float x, float y, float radius,
                                String label, int color) {
        glow.setColor(withAlpha(alpha(52), color));
        glow.setStrokeWidth(3.0f);
        canvas.drawCircle(x, y, radius * 1.24f, glow);
        drawCircleButton(canvas, x, y, radius, label, color);
    }

    private void drawPill(Canvas canvas, float x, float y, float width, float height, String label) {
        float left = x - width * 0.5f;
        float top = y - height * 0.5f;
        float right = x + width * 0.5f;
        float bottom = y + height * 0.5f;
        float radius = height * 0.45f;
        fill.setColor(Color.argb(alpha(58), 120, 177, 225));
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, fill);
        stroke.setColor(Color.argb(alpha(190), 233, 245, 255));
        stroke.setStrokeWidth(2.0f);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, stroke);
        drawLabel(canvas, x, y, label, Math.max(11.0f, height * 0.34f));
    }

    private void drawSettingsButton(Canvas canvas, float x, float y, float radius) {
        fill.setColor(Color.argb(alpha(64), 35, 61, 86));
        canvas.drawCircle(x, y, radius, fill);
        stroke.setColor(Color.argb(alpha(180), 188, 222, 246));
        stroke.setStrokeWidth(2.0f);
        canvas.drawCircle(x, y, radius, stroke);
        drawLabel(canvas, x, y, "≡", radius * 1.25f);
    }

    private static int withAlpha(int alpha, int color) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void drawLabel(Canvas canvas, float x, float y, String label, float size) {
        text.setTextSize(size);
        text.setColor(Color.argb(alpha(225), 255, 255, 255));
        Paint.FontMetrics fm = text.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) * 0.5f;
        canvas.drawText(label, x, baseline, text);
    }
}
