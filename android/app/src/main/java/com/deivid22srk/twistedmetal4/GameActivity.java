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
    private TouchOverlayView touchOverlay;
    private final Handler overlayHandler = new Handler(Looper.getMainLooper());
    private final Runnable overlayVisibilityPoll = new Runnable() {
        @Override
        public void run() {
            if (touchOverlay != null) {
                touchOverlay.setVisibility(hasPhysicalGamepad()
                        ? View.GONE : View.VISIBLE);
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
        requestControllerPermissions();
        enterImmersiveFullscreen();
        if (mLayout != null) {
            touchOverlay = new TouchOverlayView(this);
            mLayout.addView(touchOverlay, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
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
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveFullscreen();
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
        if (requestCode == 7108) enterImmersiveFullscreen();
    }

    private void enterImmersiveFullscreen() {
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars()
                        | WindowInsets.Type.displayCutout());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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
        return new String[] {
                "--game", new File(root, "game.toml").getAbsolutePath(),
                "--disc", disc,
                "--bios", bios,
                "--memcard-dir", saves,
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
