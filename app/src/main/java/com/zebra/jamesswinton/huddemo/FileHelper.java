package com.zebra.jamesswinton.huddemo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileHelper {

    // Debugging
    private static final String TAG = "FileHelper";

    // Constants


    // Private Variables
    private Context context;

    // Public Variables


    public FileHelper(Context context) {
        this.context = context;
    }

    @NonNull
    public File getSlideDirectoryCreateIfNonExist() {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                + File.separator + Constants.SLIDE_FOLDER_NAME);
        if (!file.mkdirs() && !file.exists()) {
            Log.e(TAG, "Directory not created");
        } return file;
    }

    public void clearAppMediaStorageDirectory() {
        File slidesDirectory = getSlideDirectoryCreateIfNonExist();
        if (slidesDirectory.isDirectory()) {
            String[] children = slidesDirectory.list();
            for (int i = 0; i < children.length; i++) {
                File slide = new File(slidesDirectory, children[i]);
                if (slide.delete()) {
                    Log.i(TAG, "Deleted file: " + slide.getName() + " from: "
                            + slide.getAbsolutePath());
                } else {
                    Log.e(TAG, "Unable to delete file: " + slide.getName() + " from: "
                            + slide.getAbsolutePath());
                }
            }
        }
    }

    public boolean clearItemFromMediaStorageDirectory(Slide slide) {
        File slideToDelete = slide.getImageFile();
        return slideToDelete.delete();
    }

    public File saveUriToFile(Context context, Uri slideUri) throws IOException {
        // Get content Resolver
        ContentResolver uriContentResolver = context.getContentResolver();

        // Open Input Stream from Content Resolver
        InputStream contentResolverInputStream = uriContentResolver.openInputStream(slideUri);

        // Create File
        File slideFile = new File(getSlideDirectoryCreateIfNonExist() + File.separator +
                getFileNameFromUri(slideUri));

        // Create OutputStream from File
        FileOutputStream fileOutputStream = new FileOutputStream(slideFile);

        // Copy file from InputStream to Output Stream
        byte[] buffer = new byte[1024];
        int len;
        while ((len = contentResolverInputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, len);
        }

        // Return File
        return slideFile;
    }

    public String getFileNameFromUri(Uri uri) {
        // Init Cursor & MetaDataHolder
        String fileName;
        Cursor cursor = null;

        try {
            // Query URI With Cursor
            cursor = context.getContentResolver().query(uri, null, null, null,
                    null);

            // Add Default Name as Current Time + Mime Type
            fileName = System.currentTimeMillis() + context.getContentResolver().getType(uri);

            // Move cursor
            if (cursor != null && cursor.moveToFirst()) {

                // Get display name
                fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return fileName;
    }
}
