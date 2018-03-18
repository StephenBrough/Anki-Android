package com.ichi2.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import com.ichi2.anki.DialogHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import timber.log.Timber;

public class Utils {
    /**
     * Send a Message to AnkiDroidApp so that the DialogMessageHandler shows the Import apkg dialog.
     * @param path path to apkg file which will be imported
     */
    public static void sendShowImportFileDialogMsg(String path) {
        // Get the filename from the path
        File f = new File(path);
        String filename = f.getName();

        // Create a new message for DialogHandler so that we see the appropriate import dialog in DeckPickerActivity
        Message handlerMessage = Message.obtain();
        Bundle msgData = new Bundle();
        msgData.putString("importPath", path);
        handlerMessage.setData(msgData);
        if (filename.equals("collection.apkg")) {
            // Show confirmation dialog asking to confirm import with replace when file called "collection.apkg"
            handlerMessage.what = DialogHandler.MSG_SHOW_COLLECTION_IMPORT_REPLACE_DIALOG;
        } else {
            // Otherwise show confirmation dialog asking to confirm import with add
            handlerMessage.what = DialogHandler.MSG_SHOW_COLLECTION_IMPORT_ADD_DIALOG;
        }
        // Store the message in AnkiDroidApp message holder, which is loaded later in AnkiActivity.onResume
        DialogHandler.storeMessage(handlerMessage);
    }

    /**
     * Send a Message to AnkiDroidApp so that the DialogMessageHandler forces a sync
     */
    public static void sendDoSyncMsg() {
        // Create a new message for DialogHandler
        Message handlerMessage = Message.obtain();
        handlerMessage.what = DialogHandler.MSG_DO_SYNC;
        // Store the message in AnkiDroidApp message holder, which is loaded later in AnkiActivity.onResume
        DialogHandler.storeMessage(handlerMessage);
    }

    /**
     * Copy the data from the intent to a temporary file
     * @param intent intent from which to get input stream
     * @param tempPath temporary path to store the cached file
     * @return whether or not copy was successful
     */
    public static boolean copyFileToCache(Context context, Intent intent, String tempPath) {
        // Get an input stream to the data in ContentProvider
        InputStream in;
        try {
            in = context.getContentResolver().openInputStream(intent.getData());
        } catch (FileNotFoundException e) {
            Timber.e(e, "Could not open input stream to intent data");
            return false;
        }
        // Check non-null
        if (in == null) {
            return false;
        }
        // Create new output stream in temporary path
        OutputStream out;
        try {
            out = new FileOutputStream(tempPath);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Could not access destination file %s", tempPath);
            return false;
        }

        try {
            // Copy the input stream to temporary file
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
        } catch (IOException e) {
            Timber.e(e, "Could not copy file to %s", tempPath);
            return false;
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                Timber.e(e, "Error closing tempOutDir %s", tempPath);
            }
        }
        return true;
    }

    /**
     * Check if the InputStream is to a valid non-empty zip file
     * @param intent intent from which to get input stream
     * @return whether or not valid zip file
     */
    public static boolean hasValidZipFile(Context context, Intent intent) {
        // Get an input stream to the data in ContentProvider
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(intent.getData());
        } catch (FileNotFoundException e) {
            Timber.e(e, "Could not open input stream to intent data");
        }
        // Make sure it's not null
        if (in == null) {
            Timber.e("Could not open input stream to intent data");
            return false;
        }
        // Open zip input stream
        ZipInputStream zis = new ZipInputStream(in);
        boolean ok = false;
        try {
            try {
                ZipEntry ze = zis.getNextEntry();
                if (ze != null) {
                    // set ok flag to true if there are any valid entries in the zip file
                    ok = true;
                }
            } catch (IOException e) {
                // don't set ok flag
                Timber.d(e, "Error checking if provided file has a zip entry");
            }
        } finally {
            // close the input streams
            try {
                zis.close();
                in.close();
            } catch (Exception e) {
                Timber.d(e, "Error closing the InputStream");
            }
        }
        return ok;
    }


}
