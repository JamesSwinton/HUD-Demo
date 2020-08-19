package com.zebra.jamesswinton.huddemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.symbol.zebrahud.ZebraHud;
import com.zebra.jamesswinton.huddemo.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.InvalidPreferencesFormatException;

public class MainActivity extends AppCompatActivity {

    // Debugging
    private static final String TAG = "MainActivity";

    // Constants
    private static final int SELECT_SLIDES_INTENT = 0;

    private static final String PREF_SCALE = "hud_scale";
    private static final String PREF_BRIGHTNESS = "hud_brightness";
    private static final String PREF_USE_TIMER = "hud_timer";
    private static final String PREF_TIMER_INTERVAL = "hud_timer_interval";


    // Private Variables


    // Public Variables
    private ActivityMainBinding mDataBinding;
    private ZebraHud mZebraHud = new ZebraHud();
    private List<Slide> mSlides = new ArrayList<>();
    private FileHelper mFileHelper = new FileHelper(this);
    private SlideAdapter mSlideAdapter = null;
    private SharedPreferences mSharedPrefManager = null;
    private byte[] mCachedImage = null;
    private AutoIncrementHandler autoIncrementHandler = new AutoIncrementHandler(this);

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mSharedPrefManager = PreferenceManager.getDefaultSharedPreferences(this);
        setSupportActionBar(mDataBinding.toolbarLayout.toolbar);

        // Init Adapter
        mSlideAdapter = new SlideAdapter(this, mOnSlidesChangedCallback);

        // Init ViewPager
        initViewPager();

        // Init Hud
        mZebraHud.setDisplayOn(true);
        mZebraHud.setScale(100);
        mZebraHud.setBrightness(100);
        mZebraHud.setCameraEnabled(true);
        mZebraHud.setMicrophoneEnabled(true);
        mZebraHud.setOperationMode(ZebraHud.OperationMode.NORMAL);

        // Get Existing Slides
        loadSlideImagesIfAvailable(slides -> {
            mSlides = slides;
            mSlideAdapter.setSlides(mSlides);
            if (mSlides != null && !mSlides.isEmpty()) {
                showImageOnHud(mSlides.get(mDataBinding.slideShowViewPager.getCurrentItem()));
            }
        });

        // Prevent Click when HUD isn't connected
        mDataBinding.loadingLayout.loadingView.setOnTouchListener((view, motionEvent) -> true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mZebraHud != null) {
            mZebraHud.onStart(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mZebraHud != null) {
            mZebraHud.onResume(this, mZebraHudEventsListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mZebraHud != null) {
            mZebraHud.onPause(this);
        }

        // Stop Auto Increment
        autoIncrementHandler.removeMessages(HANDLER_MESSAGE.TIMER.ordinal());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mZebraHud != null) {
            if (isFinishing()) { mZebraHud.clearDisplay(); }
            mZebraHud.onStop(this, isFinishing());
        }
    }

    /**
     * Toolbar Methods
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle Navigation Events
        switch(item.getItemId()) {
            case R.id.add_slides:
                selectFilesFromStorage();
                break;
            case R.id.clear_slides:
                CustomDialog.showCustomDialog(this, CustomDialog.DialogType.WARN,
                        "Confirm Deletion", "This action will remove all images from the device and will need to be reloaded, are you sure you want to proceed?",
                        "DELETE", (dialogInterface, i) -> clearSlidesFromStorage(),
                        "CANCEL", null);
                break;
            case R.id.settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
        } return true;
    }

    /**
     * Slide Methods
     */

    private OnSlideChangedCallback mOnSlidesChangedCallback = new OnSlideChangedCallback() {
        @Override
        public void onDelete(Slide slide) {
            clearSingleSlideFromStorage(slide);
        }

        @Override
        public void onEditText(Slide slide, String text) {
            mSlides.get(mSlides.indexOf(slide)).setImageText(text);
            mSlideAdapter.setSlides(mSlides);
            mSlideAdapter.notifyDataSetChanged();
        }
    };

    private void loadSlideImagesIfAvailable(OnSlidesLoadedCallback callback) {
        // Get Slides
        List<Slide> slides = new ArrayList<>();
        File slideDirectory = mFileHelper.getSlideDirectoryCreateIfNonExist();
        File[] slideFiles = slideDirectory.listFiles();
        if (slideDirectory.isDirectory() && slideFiles != null) {
            new Thread(() -> {
                // Get Slide file Paths & Store In Array
                for (File slide : slideFiles) {
                    // Convert File to Bitmap
                    Bitmap slideBitmap = BitmapFactory.decodeFile(slide.getAbsolutePath());

                    slides.add(new Slide(slide, slideBitmap,""));
                }

                // Order List
                if (!slides.isEmpty()) {
                    Collections.sort(slides, (slide1, slide2) ->
                            slide1.getImageFile().getName().compareTo(slide2.getImageFile().getName()));
                }

                callback.onLoaded(slides);
            }).run();

        }
    }

    private void clearSlidesFromStorage() {
        // Remove Local List
        mSlides.clear();

        // Clear Directory
        mFileHelper.clearAppMediaStorageDirectory();

        // Clear HUD
        mZebraHud.clearDisplay();

        // Show Add Slides Message
        showMessageOnHud("No Slides Found", "Please add some slides to the application");

        // Update Adapter
        mSlideAdapter.setSlides(mSlides);
    }

    private void clearSingleSlideFromStorage(Slide slide) {
        // Remove Slide
        mSlides.remove(slide);

        // Delete Slide File
        mFileHelper.clearItemFromMediaStorageDirectory(slide);

        // Update Adapter
        mSlideAdapter.setSlides(mSlides);
    }

    @SuppressLint("InflateParams")
    private void selectFilesFromStorage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Slides"), SELECT_SLIDES_INTENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_SLIDES_INTENT) {
                if(resultData != null) { // checking empty selection
                    if(resultData.getClipData() != null) { // checking multiple selection or not
                        new Thread(() -> {
                            for(int i = 0; i < resultData.getClipData().getItemCount(); i++) {
                                // Get URI
                                Uri slideUri = resultData.getClipData().getItemAt(i).getUri();
                                try {
                                    // Save to Local File
                                    File slideFile = mFileHelper.saveUriToFile(MainActivity.this, slideUri);
                                    // Convert File to Bitmap
                                    Bitmap slideBitmap = BitmapFactory.decodeFile(slideFile.getAbsolutePath());
                                    // Store File in Array
                                    mSlides.add(new Slide(slideFile, slideBitmap, ""));
                                } catch (IOException e) {
                                    Log.e(TAG, "IOException: " + e.getMessage());
                                }
                            }

                            // Order List
                            if (!mSlides.isEmpty()) {
                                Collections.sort(mSlides, (slide1, slide2) ->
                                        slide1.getImageFile().getName().compareTo(slide2.getImageFile().getName()));
                            }

                            runOnUiThread(() -> {
                                // Update Adapter
                                mSlideAdapter.setSlides(mSlides);
                                if (mSlides != null && !mSlides.isEmpty()) {
                                    showImageOnHud(mSlides.get(mDataBinding.slideShowViewPager.getCurrentItem()));
                                }
                            });
                        }).run();
                    } else {
                        new Thread(() -> {
                            // Get URI
                            Uri slideUri = resultData.getData();
                            try {
                                // Save to Local File
                                File slideFile = mFileHelper.saveUriToFile(MainActivity.this, slideUri);
                                // Create File
                                Bitmap slideBitmap = BitmapFactory.decodeFile(slideFile.getAbsolutePath());
                                // Store File in Array
                                mSlides.add(new Slide(slideFile, slideBitmap, ""));
                            } catch (IOException e) {
                                Log.e(TAG, "IOException: " + e.getMessage());
                            }

                            // Order Slides
                            if (!mSlides.isEmpty()) {
                                Collections.sort(mSlides, (slide1, slide2) ->
                                        slide1.getImageFile().getName().compareTo(slide2.getImageFile().getName()));
                            }

                            runOnUiThread(() -> {
                                // Update Adapter
                                mSlideAdapter.setSlides(mSlides);
                                if (mSlides != null && !mSlides.isEmpty()) {
                                    showImageOnHud(mSlides.get(mDataBinding.slideShowViewPager.getCurrentItem()));
                                }
                            });
                        }).run();;

                    }
                }
            }
        }
    }

    /**
     * HUD Methods
     */

    private ZebraHud.EventListener mZebraHudEventsListener = new ZebraHud.EventListener() {
        @Override
        public void onConnected(Boolean connected) {
            Log.i(TAG, "HUD " + (connected ? "Connected" : "Disconnected"));

            if (connected) {
                // Hide Overlay
                mDataBinding.loadingLayout.loadingView.setVisibility(View.GONE);

                // Show Image On Hud
                if (mCachedImage != null) {
                    mZebraHud.showImage(BitmapFactory.decodeByteArray(mCachedImage, 0, mCachedImage.length));
                } else {
                    mZebraHud.showMessage("No Slides Found", "Please add some slides to the application");
                }

                // Set Auto Increment
                if (mSharedPrefManager.getBoolean(PREF_USE_TIMER, false) && mSlides != null && mSlides.size() > 0) {
                    autoIncrementHandler.sendEmptyMessage(HANDLER_MESSAGE.TIMER.ordinal());
                }

            } else {
                mDataBinding.loadingLayout.loadingView.setVisibility(View.VISIBLE);

                // Stop Auto Increment
                autoIncrementHandler.removeMessages(HANDLER_MESSAGE.TIMER.ordinal());
            }
        }

        @Override
        public void onImageUpdated(byte[] bytes) {
            Log.i(TAG, "ImageUpdated");

            // Store Last Image
            mCachedImage = bytes;
        }

        @Override
        public void onCameraImage(Bitmap bitmap) {
            Log.i(TAG, "Image Captured");
        }
    };

    private static class AutoIncrementHandler extends Handler {
        private final WeakReference<MainActivity> activity;

        AutoIncrementHandler(MainActivity parent) {
            activity = new WeakReference<>(parent);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Log.i(TAG, "Handler Message Received, showing next slide");

            try {
                // Show Next Slide
                if (!activity.get().isFinishing() && msg.what == HANDLER_MESSAGE.TIMER.ordinal()) {
                    activity.get().mDataBinding.slideShowViewPager.post(new Runnable() {
                        @Override
                        public void run() {
                            // Slide
                            int currentItem = activity.get().mDataBinding.slideShowViewPager.getCurrentItem();
                            int totalCount = activity.get().mSlideAdapter.getItemCount();
                            int nextItem = ++currentItem;

                            if (nextItem == totalCount) {
                                activity.get().mDataBinding.slideShowViewPager.setCurrentItem(0, false);
                            } else {
                                activity.get().mDataBinding.slideShowViewPager.setCurrentItem(nextItem, true);
                            }
                        }
                    });

                    // Get Interval
                    int intervalSecs;
                    int intervalMillis;
                    try {
                        intervalSecs = Integer.parseInt(activity.get().mSharedPrefManager.getString(PREF_TIMER_INTERVAL, "5"));
                        intervalMillis = intervalSecs * 1000;
                    } catch (ClassCastException e) {
                        intervalMillis = 5000;
                    }

                    sendEmptyMessageDelayed(HANDLER_MESSAGE.TIMER.ordinal(), intervalMillis);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private enum HANDLER_MESSAGE {
        TIMER
    }

    private void showImageOnHud(Slide slide) {
        if (mZebraHud != null && mZebraHud.isConnected()) {
            mZebraHud.showImage(slide.getImageBitmap());
        }
    }

    private void showMessageOnHud(String title, String message) {
        if (mZebraHud != null && mZebraHud.isConnected()) {
            mZebraHud.showMessage(title, message);
        }
    }

    /**
     * UI Methods
     */

    private void initViewPager() {
        // Setup View Pager
        mDataBinding.slideShowViewPager.setOffscreenPageLimit(2);
        mDataBinding.slideShowViewPager.setAdapter(mSlideAdapter);
        mDataBinding.slideShowViewPager.setPageTransformer(mPageTransformer);
        mDataBinding.slideShowViewPager.registerOnPageChangeCallback(mOnPageChangeCallback);
    }

    private ViewPager2.OnPageChangeCallback mOnPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            // Show Image In HUD
            if (mSlides != null && mSlides.size() > 0) {
                showImageOnHud(mSlides.get(position));
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
        }
    };

    private static ViewPager2.PageTransformer mPageTransformer = (page, position) -> {
        float MIN_SCALE = 0.85f;
        float MIN_ALPHA = 0.5f;
        int pageWidth = page.getWidth();
        int pageHeight = page.getHeight();

        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            page.setAlpha(0f);

        } else if (position <= 1) { // [-1,1]
            // Modify the default slide transition to shrink the page as well
            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
            float vertMargin = pageHeight * (1 - scaleFactor) / 2;
            float horzMargin = pageWidth * (1 - scaleFactor) / 2;
            if (position < 0) {
                page.setTranslationX(horzMargin - vertMargin / 2);
            } else {
                page.setTranslationX(-horzMargin + vertMargin / 2);
            }

            // Scale the page down (between MIN_SCALE and 1)
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);

            // Fade the page relative to its size.
            page.setAlpha(MIN_ALPHA +
                    (scaleFactor - MIN_SCALE) /
                            (1 - MIN_SCALE) * (1 - MIN_ALPHA));

        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            page.setAlpha(0f);
        }
    };
}
