package com.zebra.jamesswinton.huddemo;

import android.graphics.Bitmap;

import java.io.File;
import java.io.Serializable;

public class Slide implements Serializable {

    private File imageFile;
    private Bitmap imageBitmap;
    private String imageText;

    public Slide(File imageFile, Bitmap imageBitmap, String imageText) {
        this.imageFile = imageFile;
        this.imageText = imageText;
        this.imageBitmap = imageBitmap;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImagePath(File imagePath) {
        this.imageFile = imagePath;
    }

    public String getImageText() {
        return imageText;
    }

    public void setImageText(String imageText) {
        this.imageText = imageText;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }
}
