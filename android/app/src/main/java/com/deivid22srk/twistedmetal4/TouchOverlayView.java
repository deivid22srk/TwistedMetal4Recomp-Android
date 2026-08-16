package com.deivid22srk.twistedmetal4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * Visual-only touch controller. SDL keeps ownership of touch events; this view
 * returns false from dispatchTouchEvent so the native finger-region mapper
 * below the overlay continues to receive every finger event.
 */
final class TouchOverlayView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    TouchOverlayView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(Color.argb(48, 255, 255, 255));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(Color.argb(155, 255, 255, 255));
        text.setColor(Color.argb(205, 255, 255, 255));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        setAlpha(0.90f);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Do not become the touch target; SDLSurface/native SDL must receive it.
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;
        final float unit = Math.min(w, h);
        final float padR = unit * 0.105f;
        final float cx = w * 0.095f;
        final float cy = h * 0.70f;
        final float fx = w * 0.905f;
        final float fy = h * 0.70f;

        // The side rails are outside the 4:3 game image on a 20:9 phone.
        drawCircleButton(canvas, cx, cy - padR * 0.92f, padR * 0.72f, "▲");
        drawCircleButton(canvas, cx, cy + padR * 0.92f, padR * 0.72f, "▼");
        drawCircleButton(canvas, cx - padR * 0.92f, cy, padR * 0.72f, "◀");
        drawCircleButton(canvas, cx + padR * 0.92f, cy, padR * 0.72f, "▶");

        drawCircleButton(canvas, fx - padR * 0.92f, fy, padR * 0.72f, "■");
        drawCircleButton(canvas, fx + padR * 0.92f, fy, padR * 0.72f, "○");
        drawCircleButton(canvas, fx, fy - padR * 0.92f, padR * 0.72f, "△");
        drawCircleButton(canvas, fx, fy + padR * 0.92f, padR * 0.72f, "×");

        // Shoulder pairs are stacked in the side rails, not over the game HUD.
        drawPill(canvas, w * 0.09f, h * 0.065f, w * 0.14f, h * 0.062f, "L2");
        drawPill(canvas, w * 0.09f, h * 0.155f, w * 0.14f, h * 0.062f, "L1");
        drawPill(canvas, w * 0.91f, h * 0.065f, w * 0.14f, h * 0.062f, "R2");
        drawPill(canvas, w * 0.91f, h * 0.155f, w * 0.14f, h * 0.062f, "R1");

        // Utility buttons live in a narrow top-center strip, away from the HUD.
        drawPill(canvas, w * 0.50f, h * 0.065f, w * 0.13f, h * 0.054f, "SELECT");
        drawPill(canvas, w * 0.50f, h * 0.145f, w * 0.13f, h * 0.054f, "START");
    }

    private void drawCircleButton(Canvas canvas, float x, float y, float radius, String label) {
        canvas.drawCircle(x, y, radius, fill);
        canvas.drawCircle(x, y, radius, stroke);
        text.setTextSize(radius * 0.78f);
        Paint.FontMetrics fm = text.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) * 0.5f;
        canvas.drawText(label, x, baseline, text);
    }

    private void drawPill(Canvas canvas, float x, float y, float width, float height, String label) {
        float left = x - width * 0.5f;
        float top = y - height * 0.5f;
        float right = x + width * 0.5f;
        float bottom = y + height * 0.5f;
        float radius = height * 0.45f;
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, fill);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, stroke);
        text.setTextSize(Math.max(11.0f, height * 0.34f));
        Paint.FontMetrics fm = text.getFontMetrics();
        float baseline = y - (fm.ascent + fm.descent) * 0.5f;
        canvas.drawText(label, x, baseline, text);
    }
}
