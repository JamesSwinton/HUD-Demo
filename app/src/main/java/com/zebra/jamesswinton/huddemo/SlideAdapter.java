package com.zebra.jamesswinton.huddemo;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;
import java.util.List;

public class SlideAdapter extends RecyclerView.Adapter {

    // Debugging
    private static final String TAG = "SlideAdapter";

    // Constants
    private static final int NO_SLIDES = 0;
    private static final int SLIDE_SHOW = 1;

    // Public Variables


    // Private Variables
    private Context context;
    private List<Slide> mSlides = null;
    private OnSlideChangedCallback mOnSlideChangedCallback;

    public SlideAdapter(Context context, OnSlideChangedCallback onSlideChangedCallback) {
        this.context = context;
        this.mOnSlideChangedCallback = onSlideChangedCallback;
    }


    /**
     * RecyclerView Methods
     */

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder view = null;
        if (viewType == NO_SLIDES) {
            view = new NoSlidesViewHolder(LayoutInflater.from(
                    parent.getContext()).inflate(R.layout.adapter_no_slide, parent, false));
        } else if (viewType == SLIDE_SHOW) {
            view = new SlideViewHolder(LayoutInflater.from(
                    parent.getContext()).inflate(R.layout.adapter_slide_constraint, parent, false));
        }
        return view;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NoSlidesViewHolder) {
            Log.i(TAG, "Showing No Slides View Holder");
        } else if (holder instanceof SlideViewHolder) {

            // Get Slide
            Slide currentSlide = mSlides.get(position);
            SlideViewHolder viewHolder = (SlideViewHolder) holder;

            // Set Image
            Glide.with(context)
                    .load(currentSlide.getImageBitmap())
                    .into(viewHolder.slideImage);

            // Set Title & Position
            viewHolder.slidePosition.setText(String.valueOf(position + 1));
            viewHolder.slideTitle.setText(currentSlide.getImageFile().getName());

            // Set Text
            if (!currentSlide.getImageText().isEmpty()) {
                viewHolder.slideText.setVisibility(View.VISIBLE);
                viewHolder.slideText.setText(currentSlide.getImageText());
            } else {
                viewHolder.slideText.setVisibility(View.GONE);
            }

            // Show Additional Options
            viewHolder.slideOverflow.setOnClickListener(v -> showOverflowMenu(viewHolder, position,
                    currentSlide));
        }
    }

    private void showOverflowMenu(SlideViewHolder viewHolder, int position, Slide currentSlide) {
        PopupMenu popup = new PopupMenu(viewHolder.slideImage.getContext(), viewHolder.slideOverflow);
        popup.inflate(R.menu.slide_overflow_menu);

        // Set Movement Methods Enabled / Disabled
        if (position == 0) {
            popup.getMenu().findItem(R.id.move_up).setEnabled(false);
            popup.getMenu().findItem(R.id.move_down).setEnabled(true);
        } else if (position == mSlides.size() - 1) {
            popup.getMenu().findItem(R.id.move_up).setEnabled(true);
            popup.getMenu().findItem(R.id.move_down).setEnabled(false);
        } else {
            popup.getMenu().findItem(R.id.move_up).setEnabled(true);
            popup.getMenu().findItem(R.id.move_down).setEnabled(true);
        }

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.move_up:
                    Log.i(TAG, "Moving Slide from: " + position + " to: " + (position - 1));

                    Collections.swap(mSlides, position, position - 1);
                    notifyItemMoved(position, position - 1);
                    notifyDataSetChanged();
                    return true;
                case R.id.move_down:
                    Log.i(TAG, "Moving Slide from: " + position + " to: " + (position + 1));

                    Collections.swap(mSlides, position, position + 1);
                    notifyItemMoved(position, position + 1);
                    notifyDataSetChanged();
                    return true;
                case R.id.set_position:
                    // TODO: Show Position Selection Dialog
                    return true;
                case R.id.edit_text:
                    showEditTextDialog(viewHolder, position);
                    return true;
                case R.id.delete_slide:
                    mOnSlideChangedCallback.onDelete(currentSlide);
                    return true;
                default:
                    return false;
            }
        });

        //displaying the popup
        popup.show();
    }

    private void showEditTextDialog(SlideViewHolder holder, int position) {
        final EditText input = new EditText(holder.slideOverflow.getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        new MaterialAlertDialogBuilder(holder.slideOverflow.getContext())
                .setTitle("Edit Slide Text")
                .setView(input)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    mOnSlideChangedCallback.onEditText(mSlides.get(position), input.getText() == null ? "" : input.getText().toString());
                    dialog.dismiss();
                })
                .setNegativeButton("CANCEL", null)
                .create()
                .show();
    }

    @Override
    public int getItemCount() {
        return mSlides == null || mSlides.isEmpty() ? 1 : mSlides.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mSlides == null || mSlides.isEmpty() ? NO_SLIDES : SLIDE_SHOW;
    }

    /**
     * Data Methods
     */

    public void setSlides(List<Slide> slides) {
        this.mSlides = slides;
        notifyDataSetChanged();
    }

    /**
     * ViewHolders
     */

    private class SlideViewHolder extends RecyclerView.ViewHolder {

        public ImageView slideImage;
        public TextView slideText;
        public TextView slidePosition;
        public TextView slideTitle;
        public ImageView slideOverflow;

        private SlideViewHolder(@NonNull View view) {
            super(view);
            slideImage = view.findViewById(R.id.slide_image);
            slideText = view.findViewById(R.id.slide_text);
            slidePosition = view.findViewById(R.id.slide_position);
            slideTitle = view.findViewById(R.id.slide_title);
            slideOverflow = view.findViewById(R.id.slide_overflow);
        }
    }

    private class NoSlidesViewHolder extends RecyclerView.ViewHolder {
        private NoSlidesViewHolder(@NonNull View view) {
            super(view);
        }
    }
}
