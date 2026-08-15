package com.deivid22srk.twistedmetal4;

import android.content.Context;
import android.os.Bundle;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** SDL3 host for the generated PSX runtime. */
public final class GameActivity extends SDLActivity {
    private static final String PREFS = "twistedmetal4";
    private static final String PREF_DISC = "disc_path";

    @Override
    protected void onCreate(Bundle state) {
        try {
            prepareRuntimeFiles(this);
        } catch (IOException e) {
            throw new RuntimeException("Unable to stage runtime assets", e);
        }
        super.onCreate(state);
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
        return new String[] {
                "--game", new File(root, "game.toml").getAbsolutePath(),
                "--disc", disc,
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
