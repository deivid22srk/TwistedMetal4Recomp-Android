package com.deivid22srk.twistedmetal4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** In-game settings drawer. It is visible only while the user is configuring the overlay. */
final class SettingsDrawerView extends View {
    interface Listener {
        void onDrawerFullscreenChanged(boolean enabled);
        void onDrawerHudChanged(boolean enabled);
        void onDrawerControlModeChanged(boolean analog);
        void onDrawerAutoHideChanged(boolean enabled);
        void onDrawerOpacityChanged(float opacity);
        void onDrawerClosed();
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accent = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private Listener listener;
    private boolean open;
    private boolean fullscreen;
    private boolean hudVisible = true;
    private boolean analog;
    private boolean autoHide = true;
    private float opacity = 0.90f;
    private int activeRow = -1;

    SettingsDrawerView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        setVisibility(GONE);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        text.setColor(Color.WHITE);
        accent.setColor(Color.rgb(82, 190, 255));
    }

    void setListener(Listener listener) { this.listener = listener; }

    void setState(boolean fullscreen, boolean hudVisible, boolean analog,
                  boolean autoHide, float opacity) {
        this.fullscreen = fullscreen;
        this.hudVisible = hudVisible;
        this.analog = analog;
        this.autoHide = autoHide;
        this.opacity = Math.max(0.35f, Math.min(1.0f, opacity));
        invalidate();
    }

    boolean isOpen() { return open; }

    void open() {
        open = true;
        activeRow = -1;
        setVisibility(VISIBLE);
        bringToFront();
        invalidate();
    }

    void close() {
        if (!open) return;
        open = false;
        activeRow = -1;
        setVisibility(GONE);
        if (listener != null) listener.onDrawerClosed();
    }

    private float dp(float value) { return value * density; }

    private float panelWidth() {
        return Math.min(getWidth() * 0.82f, dp(395.0f));
    }

    private float headerHeight() { return dp(78.0f); }
    private float rowHeight() { return dp(68.0f); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!open) return;
        final float panel = panelWidth();
        final float h = getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(145, 0, 0, 0));
        canvas.drawRect(panel, 0, getWidth(), h, paint);

        paint.setColor(Color.rgb(17, 24, 39));
        rect.set(0, 0, panel, h);
        canvas.drawRoundRect(rect, dp(18), dp(18), paint);
        paint.setColor(Color.rgb(26, 38, 58));
        canvas.drawRect(0, 0, panel, headerHeight(), paint);

        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setTextSize(dp(21));
        text.setColor(Color.WHITE);
        canvas.drawText("CONTROLES", dp(24), dp(34), text);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        text.setTextSize(dp(12));
        text.setColor(Color.rgb(165, 190, 215));
        canvas.drawText("TWISTED METAL 4  •  ANDROID", dp(24), dp(57), text);

        text.setTextSize(dp(26));
        text.setColor(Color.rgb(195, 215, 235));
        canvas.drawText("×", panel - dp(38), dp(44), text);

        drawRow(canvas, 0, "Tela cheia", fullscreen ? "Ativada" : "Desativada", fullscreen);
        drawRow(canvas, 1, "HUD touch", hudVisible ? "Visível" : "Oculto", hudVisible);
        drawRow(canvas, 2, "Estilo de controle", analog ? "Analógico" : "D-pad", analog);
        drawRow(canvas, 3, "Ocultar com gamepad", autoHide ? "Automático" : "Sempre visível", autoHide);
        drawOpacityRow(canvas, 4);

        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        text.setTextSize(dp(11));
        text.setColor(Color.rgb(130, 151, 177));
        canvas.drawText("As opções são salvas automaticamente", dp(24), h - dp(28), text);
    }

    private void drawRow(Canvas canvas, int row, String title, String value, boolean selected) {
        float top = headerHeight() + dp(10) + row * rowHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? Color.rgb(28, 55, 78) : Color.rgb(22, 32, 49));
        rect.set(dp(12), top, panelWidth() - dp(12), top + rowHeight() - dp(6));
        canvas.drawRoundRect(rect, dp(12), dp(12), paint);

        paint.setColor(selected ? Color.rgb(82, 190, 255) : Color.rgb(75, 94, 118));
        canvas.drawCircle(dp(34), top + dp(27), dp(7), paint);

        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setTextSize(dp(14));
        text.setColor(Color.WHITE);
        canvas.drawText(title, dp(54), top + dp(25), text);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        text.setTextSize(dp(12));
        text.setColor(Color.rgb(165, 190, 215));
        canvas.drawText(value, dp(54), top + dp(45), text);

        drawSwitch(canvas, panelWidth() - dp(55), top + dp(27), selected);
    }

    private void drawOpacityRow(Canvas canvas, int row) {
        float top = headerHeight() + dp(10) + row * rowHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(22, 32, 49));
        rect.set(dp(12), top, panelWidth() - dp(12), top + rowHeight() + dp(4));
        canvas.drawRoundRect(rect, dp(12), dp(12), paint);

        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setTextSize(dp(14));
        text.setColor(Color.WHITE);
        canvas.drawText("Opacidade do HUD", dp(24), top + dp(25), text);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        text.setTextSize(dp(12));
        text.setColor(Color.rgb(165, 190, 215));
        canvas.drawText((int)(opacity * 100) + "%", panelWidth() - dp(57), top + dp(25), text);

        float left = dp(24);
        float right = panelWidth() - dp(24);
        float y = top + dp(49);
        paint.setColor(Color.rgb(63, 83, 108));
        rect.set(left, y - dp(3), right, y + dp(3));
        canvas.drawRoundRect(rect, dp(3), dp(3), paint);
        paint.setColor(Color.rgb(82, 190, 255));
        rect.set(left, y - dp(3), left + (right - left) * opacity, y + dp(3));
        canvas.drawRoundRect(rect, dp(3), dp(3), paint);
        canvas.drawCircle(left + (right - left) * opacity, y, dp(9), accent);
    }

    private void drawSwitch(Canvas canvas, float x, float y, boolean on) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(on ? Color.rgb(34, 142, 202) : Color.rgb(58, 73, 94));
        rect.set(x - dp(22), y - dp(12), x + dp(22), y + dp(12));
        canvas.drawRoundRect(rect, dp(12), dp(12), paint);
        paint.setColor(on ? Color.WHITE : Color.rgb(150, 164, 181));
        canvas.drawCircle(x + (on ? dp(10) : -dp(10)), y, dp(8), paint);
    }

    private int rowAt(float y) {
        float start = headerHeight() + dp(10);
        int row = (int)((y - start) / rowHeight());
        return row >= 0 && row <= 4 ? row : -1;
    }

    private void applyRow(int row, float x) {
        if (row == 0) {
            fullscreen = !fullscreen;
            if (listener != null) listener.onDrawerFullscreenChanged(fullscreen);
        } else if (row == 1) {
            hudVisible = !hudVisible;
            if (listener != null) listener.onDrawerHudChanged(hudVisible);
        } else if (row == 2) {
            analog = !analog;
            if (listener != null) listener.onDrawerControlModeChanged(analog);
        } else if (row == 3) {
            autoHide = !autoHide;
            if (listener != null) listener.onDrawerAutoHideChanged(autoHide);
        } else if (row == 4) {
            float left = dp(24);
            float right = panelWidth() - dp(24);
            opacity = Math.max(0.35f, Math.min(1.0f, (x - left) / (right - left)));
            if (listener != null) listener.onDrawerOpacityChanged(opacity);
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!open) return false;
        final float x = event.getX();
        final float y = event.getY();
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            activeRow = rowAt(y);
            if (y >= headerHeight() && activeRow == 4) applyRow(activeRow, x);
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (activeRow == 4) applyRow(activeRow, x);
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (x > panelWidth() - dp(82) && y < headerHeight()) {
                close();
            } else if (x > panelWidth()) {
                close();
            } else if (activeRow >= 0 && activeRow != 4) {
                applyRow(activeRow, x);
            }
            activeRow = -1;
            return true;
        }
        return true;
    }
}
