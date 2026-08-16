package com.deivid22srk.twistedmetal4;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/** SDL3 host for the generated PSX runtime. */
public final class GameActivity extends SDLActivity {
    private static final String PREFS = "twistedmetal4";
    private static final String PREF_DISC = "disc_path";
    private static final String PREF_FULLSCREEN = "ui_fullscreen";
    private static final String PREF_HUD = "ui_hud";
    private static final String PREF_ANALOG = "ui_analog";
    private static final String PREF_AUTO_HIDE = "ui_auto_hide";
    private static final String PREF_OPACITY = "ui_opacity";

    private static native void nativeSetAndroidTouchMode(int mode);
    private static native void nativeSetAndroidFullscreen(int enabled);

    private TouchOverlayView touchOverlay;
    private SettingsDrawerView settingsDrawer;
    private boolean fullscreenPreference = true;
    private boolean hudPreference = true;
    private boolean analogPreference;
    private boolean autoHidePreference = true;
    private float hudOpacity = 0.90f;
    private final Handler overlayHandler = new Handler(Looper.getMainLooper());
    private final Runnable overlayVisibilityPoll = new Runnable() {
        @Override
        public void run() {
            if (touchOverlay != null) {
                boolean hide = autoHidePreference && hasPhysicalGamepad();
                touchOverlay.setVisibility(hide ? View.GONE : View.VISIBLE);
            }
            overlayHandler.postDelayed(this, 500L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        try {
            prepareRuntimeFiles(this);
        } catch (IOException e) {
            throw new RuntimeException("Unable to stage runtime assets", e);
        }
        super.onCreate(state);
        loadUiPreferences();
        requestControllerPermissions();
        applyFullscreenSystemUi(fullscreenPreference);
        if (mLayout != null) {
            touchOverlay = new TouchOverlayView(this);
            touchOverlay.setControlMode(analogPreference);
            touchOverlay.setHudVisible(hudPreference);
            touchOverlay.setHudOpacity(hudOpacity);
            touchOverlay.setListener(new TouchOverlayView.Listener() {
                @Override
                public void onTouchSettingsPressed() { openSettingsDrawer(); }
            });
            mLayout.addView(touchOverlay, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            settingsDrawer = new SettingsDrawerView(this);
            settingsDrawer.setState(fullscreenPreference, hudPreference,
                    analogPreference, autoHidePreference, hudOpacity);
            settingsDrawer.setListener(new SettingsDrawerView.Listener() {
                @Override
                public void onDrawerFullscreenChanged(boolean enabled) {
                    fullscreenPreference = enabled;
                    saveUiPreferences();
                    applyFullscreen(enabled);
                }

                @Override
                public void onDrawerHudChanged(boolean enabled) {
                    hudPreference = enabled;
                    if (touchOverlay != null) touchOverlay.setHudVisible(enabled);
                    saveUiPreferences();
                }

                @Override
                public void onDrawerControlModeChanged(boolean analog) {
                    analogPreference = analog;
                    if (touchOverlay != null) touchOverlay.setControlMode(analog);
                    nativeSetAndroidTouchMode(analog ? 1 : 0);
                    saveUiPreferences();
                }

                @Override
                public void onDrawerAutoHideChanged(boolean enabled) {
                    autoHidePreference = enabled;
                    saveUiPreferences();
                    overlayVisibilityPoll.run();
                }

                @Override
                public void onDrawerOpacityChanged(float opacity) {
                    hudOpacity = opacity;
                    if (touchOverlay != null) touchOverlay.setHudOpacity(opacity);
                    saveUiPreferences();
                }

                @Override
                public void onDrawerClosed() { }
            });
            mLayout.addView(settingsDrawer, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            nativeSetAndroidTouchMode(analogPreference ? 1 : 0);
            overlayVisibilityPoll.run();
        }
    }

    private boolean hasPhysicalGamepad() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null) continue;
            int sources = device.getSources();
            if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                    || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        overlayHandler.removeCallbacks(overlayVisibilityPoll);
        touchOverlay = null;
        settingsDrawer = null;
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyFullscreenSystemUi(fullscreenPreference);
    }

    @Override
    public void onBackPressed() {
        if (settingsDrawer != null && settingsDrawer.isOpen()) {
            settingsDrawer.close();
            return;
        }
        super.onBackPressed();
    }

    private void loadUiPreferences() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        fullscreenPreference = p.getBoolean(PREF_FULLSCREEN, true);
        hudPreference = p.getBoolean(PREF_HUD, true);
        analogPreference = p.getBoolean(PREF_ANALOG, false);
        autoHidePreference = p.getBoolean(PREF_AUTO_HIDE, true);
        hudOpacity = p.getFloat(PREF_OPACITY, 0.90f);
    }

    private void saveUiPreferences() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(PREF_FULLSCREEN, fullscreenPreference)
                .putBoolean(PREF_HUD, hudPreference)
                .putBoolean(PREF_ANALOG, analogPreference)
                .putBoolean(PREF_AUTO_HIDE, autoHidePreference)
                .putFloat(PREF_OPACITY, hudOpacity)
                .apply();
    }

    private void openSettingsDrawer() {
        if (settingsDrawer == null) return;
        settingsDrawer.setState(fullscreenPreference, hudPreference,
                analogPreference, autoHidePreference, hudOpacity);
        settingsDrawer.open();
    }

    private void applyFullscreen(boolean enabled) {
        applyFullscreenSystemUi(enabled);
        nativeSetAndroidFullscreen(enabled ? 1 : 0);
        SDLActivity.setWindowStyle(enabled);
    }

    private void requestControllerPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        ArrayList<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), 7108);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 7108) applyFullscreenSystemUi(fullscreenPreference);
    }

    private void applyFullscreenSystemUi(boolean enabled) {
        Window window = getWindow();
        if (enabled) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(!enabled);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                int bars = WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars()
                        | WindowInsets.Type.displayCutout();
                if (enabled) {
                    controller.hide(bars);
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else {
                    controller.show(bars);
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_DEFAULT);
                }
            }
        } else if (enabled) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    protected String[] getLibraries() {
        // Keep the order required by SDLActivity: SDL3 first, application second.
        return new String[] { "SDL3", "main" };
    }

    @Override
    protected String getMainFunction() {
        return "SDL_main";
    }

    @Override
    protected String[] getArguments() {
        String disc = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(PREF_DISC, "");
        String root = getFilesDir().getAbsolutePath();
        String bios = new File(root, "bios/openbios.bin").getAbsolutePath();
        String saves = new File(root, "saves").getAbsolutePath();
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean initialFullscreen = p.getBoolean(PREF_FULLSCREEN, true);
        boolean initialAnalog = p.getBoolean(PREF_ANALOG, false);
        return new String[] {
                "--game", new File(root, "game.toml").getAbsolutePath(),
                "--disc", disc,
                "--bios", bios,
                "--memcard-dir", saves,
                "--android-fullscreen", initialFullscreen ? "1" : "0",
                "--android-touch-mode", initialAnalog ? "analog" : "dpad",
                "--no-launcher",
                "--renderer", "opengl"
        };
    }

    static boolean hasPreparedDisc(Context context) {
        String path = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(PREF_DISC, "");
        return path != null && path.toLowerCase().endsWith(".cue")
                && new File(path).isFile()
                && new File(context.getFilesDir(), "game.toml").isFile();
    }

    private static void prepareRuntimeFiles(Context context) throws IOException {
        File root = context.getFilesDir();
        copyAssetIfMissing(context, "game.toml", new File(root, "game.toml"));
        File bios = new File(root, "bios");
        if (!bios.exists() && !bios.mkdirs())
            throw new IOException("cannot create " + bios);
        copyAssetIfMissing(context, "bios/openbios.bin",
                new File(bios, "openbios.bin"));
        copyAssetIfMissing(context, "bios/OpenBIOS.LICENSE",
                new File(bios, "OpenBIOS.LICENSE"));
    }

    private static void copyAssetIfMissing(Context context, String asset,
                                           File destination) throws IOException {
        if (destination.isFile()) return;
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
            throw new IOException("cannot create " + parent);
        try (InputStream in = context.getAssets().open(asset);
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read == 0) continue;
                out.write(buffer, 0, read);
            }
        }
    }
}
