package com.zebra.jamesswinton.huddemo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class FileHelperBackground extends AsyncTask<Uri, Void, String> {

    // Debugging
    private static final String TAG = "FileHelper";

    // Constants


    // private Variables
    private static WeakReference<Context> mContext;
    private FileHelperCallback fileHelperCallback;

    // Public Variables


    public FileHelperBackground(WeakReference<Context> context, Uri imageUri, FileHelperCallback fileHelperCallback) {
        mContext = context;
        this.fileHelperCallback = fileHelperCallback;
        this.execute(imageUri);
    }

    /**
     * Async Methods
     */

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected String doInBackground(Uri... imageUri) {
        // Validate Context
        if (mContext.get() != null) {
            // Set Context
            Context context = mContext.get();

            // Get content Resolver
            ContentResolver uriContentResolver = context.getContentResolver();

            // Open Input Stream from Content Resolver
            InputStream contentResolverInputStream = null;
            try {
                contentResolverInputStream = uriContentResolver.openInputStream(imageUri[0]);
            } catch (FileNotFoundException e) {
                fileHelperCallback.onError(e);
                e.printStackTrace();
            }

            // Get Name
            String fileName = getFileNameFromUri(context, imageUri[0]);

            // Create File
            File slideFile = new File(getSlideDirectoryCreateIfNonExist(context)
                    + File.separator + fileName);

            // Create OutputStream from File
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(slideFile);
            } catch (FileNotFoundException e) {
                fileHelperCallback.onError(e);
                e.printStackTrace();
            }

            // Copy file from InputStream to Output Stream
            byte[] buffer = new byte[1024];
            int len;
            while (true) {
                try {
                    assert contentResolverInputStream != null;
                    if ((len = contentResolverInputStream.read(buffer)) == -1) break;
                    fileOutputStream.write(buffer, 0, len);
                } catch (IOException e) {
                    fileHelperCallback.onError(e);
                    e.printStackTrace();
                }
            }

            return fileName;
        }

        return null;
    }

    @Override
    protected void onPostExecute(String fileName) {
        super.onPostExecute(fileName);
        if (fileName != null) {
            fileHelperCallback.onFileSaved(fileName);
        } else {
            fileHelperCallback.onError(new Exception("No valid context, aborting operation"));
        }
    }

    /**
     * Interface
     */

    public interface FileHelperCallback {
        void onFileSaved(String fileName);
        void onError(Exception e);
    }

    /**
     * Support Methods
     */

    @NonNull
    private File getSlideDirectoryCreateIfNonExist(Context context) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                + File.separator + Constants.SLIDE_FOLDER_NAME);
        if (!file.mkdirs() && !file.exists()) {
            Log.e(TAG, "Directory not created");
        } return file;
    }

    public String getFileNameFromUri(Context context, Uri uri) {
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
