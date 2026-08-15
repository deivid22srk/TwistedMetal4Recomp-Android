package com.deivid22srk.twistedmetal4;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Small Android-only bootstrapper. It never exposes the original disc through
 * a broad filesystem permission: the user grants one folder through SAF and
 * the app copies the required CUE/BIN files into getFilesDir()/disc.
 */
public final class MainActivity extends Activity {
    private static final int REQUEST_DISC_TREE = 7004;
    private static final String PREFS = "twistedmetal4";
    private static final String PREF_DISC = "disc_path";
    private TextView status;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (GameActivity.hasPreparedDisc(this)) {
            launchGame();
        } else {
            showPicker();
        }
    }

    private void showPicker() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int pad = (int) (getResources().getDisplayMetrics().density * 24.0f);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Twisted Metal 4 Recompiled\n\nSelecione a pasta que contém o arquivo CUE e as 23 faixas BIN da sua cópia USA Rev. 1.");
        title.setTextSize(18.0f);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("A imagem será copiada para o armazenamento privado do app.");
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = pad / 2;
        root.addView(status, statusParams);

        Button choose = new Button(this);
        choose.setText("Selecionar pasta do disco");
        choose.setOnClickListener(v -> openTreePicker());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-2, -2);
        buttonParams.topMargin = pad;
        root.addView(choose, buttonParams);
        setContentView(root);
    }

    private void openTreePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_DISC_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_DISC_TREE || resultCode != RESULT_OK || data == null)
            return;
        Uri tree = data.getData();
        if (tree == null) return;
        try {
            getContentResolver().takePersistableUriPermission(
                    tree, data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // A one-shot grant is still enough to copy the files now.
        }
        status.setText("Copiando CUE/BIN para o armazenamento privado…");
        new Thread(() -> {
            String error = null;
            try {
                String cue = copyDiscTree(this, tree);
                if (cue == null) {
                    error = "Nenhum arquivo .cue foi encontrado na pasta selecionada.";
                } else {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(PREF_DISC, cue).apply();
                }
            } catch (Exception e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            final String message = error;
            runOnUiThread(() -> {
                if (message != null) {
                    status.setText("Falha ao copiar a imagem: " + message);
                } else {
                    launchGame();
                }
            });
        }).start();
    }

    private void launchGame() {
        startActivity(new Intent(this, GameActivity.class));
    }

    static String copyDiscTree(Context context, Uri tree) throws IOException {
        File dest = new File(context.getFilesDir(), "disc");
        deleteRecursively(dest);
        if (!dest.mkdirs() && !dest.isDirectory())
            throw new IOException("não foi possível criar " + dest);
        String cue = copyDocuments(context.getContentResolver(), tree, dest);
        if (cue == null) return null;
        return cue;
    }

    private static String copyDocuments(ContentResolver resolver, Uri parentTree,
                                        File dest) throws IOException {
        String treeId = DocumentsContract.getTreeDocumentId(parentTree);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(parentTree, treeId);
        String[] columns = new String[] {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        String firstCue = null;
        try (Cursor cursor = resolver.query(children, columns, null, null, null)) {
            if (cursor == null) throw new IOException("não foi possível ler a pasta selecionada");
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                String mime = cursor.getString(2);
                if (name == null) continue;
                Uri document = DocumentsContract.buildDocumentUriUsingTree(parentTree, id);
                String lower = name.toLowerCase(Locale.ROOT);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    // Disc archives normally put all tracks at the selected root;
                    // recurse only one level for providers that wrap them.
                    File nested = new File(dest, name);
                    if (!nested.mkdirs() && !nested.isDirectory()) continue;
                    String nestedCue = copyDocuments(resolver, document, nested);
                    if (firstCue == null) firstCue = nestedCue;
                } else if (lower.endsWith(".cue") || lower.endsWith(".bin")
                        || lower.endsWith(".iso")) {
                    File out = new File(dest, name);
                    copyOne(resolver, document, out);
                    if (firstCue == null && lower.endsWith(".cue"))
                        firstCue = out.getAbsolutePath();
                }
            }
        }
        return firstCue;
    }

    private static void copyOne(ContentResolver resolver, Uri document, File out)
            throws IOException {
        try (InputStream in = resolver.openInputStream(document);
             OutputStream outStream = new FileOutputStream(out)) {
            if (in == null) throw new IOException("não foi possível abrir " + document);
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read == 0) continue;
                outStream.write(buffer, 0, read);
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (!file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
