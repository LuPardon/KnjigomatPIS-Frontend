package com.example.knjigomatpis.adapters;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.knjigomatpis.R;
import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class BookImagePagerAdapter extends RecyclerView.Adapter<BookImagePagerAdapter.ImageViewHolder> {

    private static final String TAG = BookImagePagerAdapter.class.getSimpleName();

    private Context context;
    private List<String> imagePaths;
    private boolean isEditMode = false;
    private OnImageActionListener imageActionListener;
    private Handler mainHandler;

    public interface OnImageActionListener {
        void onImageDelete(int position, String imagePath);
        void onImageAdd();
        void onImageReplace(int position, String imagePath);
    }

    public BookImagePagerAdapter(Context context, List<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths != null ? new ArrayList<>(imagePaths) : new ArrayList<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public BookImagePagerAdapter(Context context, List<String> imagePaths, boolean isEditMode, OnImageActionListener listener) {
        this.context = context;
        this.imagePaths = imagePaths != null ? new ArrayList<>(imagePaths) : new ArrayList<>();
        this.isEditMode = isEditMode;
        this.imageActionListener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutResource = isEditMode ? R.layout.item_editable_book_image : R.layout.item_book_image;
        View view = LayoutInflater.from(context).inflate(layoutResource, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        try {
            // Prvo provjeri granice
            if (position < 0) {
                Log.e(TAG, "Invalid position: " + position);
                return;
            }

            // Provjeri je li ovo pozicija za "dodaj novu sliku" u načinu uređivanja
            if (isEditMode && position == imagePaths.size()) {
                setupAddImageView(holder);
                return;
            }

            // Provjeri je li pozicija unutar granica
            if (position >= imagePaths.size()) {
                Log.e(TAG, "Position " + position + " is out of bounds. List size: " + imagePaths.size());
                return;
            }

            Log.d(TAG, "Binding position: " + position + ", total items: " + imagePaths.size());

            String imagePath = imagePaths.get(position);
            if (imagePath == null || imagePath.isEmpty()) {
                Log.w(TAG, "Empty image path at position: " + position);
                holder.imageView.setImageResource(R.drawable.placeholder_image);
                return;
            }

            String baseUrl = context.getString(R.string.base_url);
            String imageUrl = baseUrl + imagePath;

            Log.d(TAG, "Loading image: " + imageUrl);

            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.imageView);

            // Postavi click listener za fullscreen prikaz
            holder.imageView.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < imagePaths.size()) {
                    showFullscreenDialog(adapterPosition);
                }
            });

            // Postavi edit mode buttons ako je u edit mode
            if (isEditMode) {
                setupEditModeButtons(holder, position);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onBindViewHolder at position " + position, e);
            holder.imageView.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void setupAddImageView(ImageViewHolder holder) {
        holder.imageView.setImageResource(R.drawable.ic_add_photo);
        holder.imageView.setScaleType(ImageView.ScaleType.CENTER);
        holder.imageView.setBackgroundResource(R.drawable.dashed_border_background);

        if (holder.deleteButton != null) holder.deleteButton.setVisibility(View.GONE);
        if (holder.replaceButton != null) holder.replaceButton.setVisibility(View.GONE);

        holder.imageView.setOnClickListener(v -> {
            if (imageActionListener != null) {
                imageActionListener.onImageAdd();
            }
        });
    }

    private void setupEditModeButtons(ImageViewHolder holder, int position) {
        if (holder.deleteButton != null && holder.replaceButton != null) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.replaceButton.setVisibility(View.VISIBLE);

            // DIREKTNO POZIVANJE LISTENER-A
            holder.deleteButton.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < imagePaths.size()) {

                    if (imageActionListener != null) {
                        imageActionListener.onImageDelete(adapterPosition, imagePaths.get(adapterPosition));
                    }
                }
            });

            holder.replaceButton.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (imageActionListener != null && adapterPosition != RecyclerView.NO_POSITION && adapterPosition < imagePaths.size()) {
                    imageActionListener.onImageReplace(adapterPosition, imagePaths.get(adapterPosition));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        int count;
        if (isEditMode) {
            count = imagePaths.size() + 1; // +1 za dodavanje svake slike
        } else {
            count = imagePaths.size();
        }

        Log.d(TAG, "getItemCount() returning: " + count + " (imagePaths.size(): " + imagePaths.size() + ", isEditMode: " + isEditMode + ")");
        return count;
    }

    private void showFullscreenDialog(int position) {
        if (position < 0 || position >= imagePaths.size()) {
            Log.e(TAG, "Invalid position for fullscreen: " + position);
            return;
        }

        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.image_fullscreen_dialog);

        ViewPager2 viewPager = dialog.findViewById(R.id.fullscreenViewPager);
        TextView indicatorTextView = dialog.findViewById(R.id.imageIndicatorTextView);

        FullscreenImageAdapter pagerAdapter = new FullscreenImageAdapter(context, imagePaths, dialog);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(position, false);

        updateIndicator(indicatorTextView, position);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) {
                super.onPageSelected(pos);
                updateIndicator(indicatorTextView, pos);
            }
        });

        dialog.show();
    }

    private void updateIndicator(TextView indicatorTextView, int position) {
        if (indicatorTextView != null && imagePaths.size() > 0) {
            indicatorTextView.setText((position + 1) + "/" + imagePaths.size());
        }
    }

    // Thread-safe methods for updating adapter data
    public void updateImagePaths(List<String> newImagePaths) {
        mainHandler.post(() -> {
            this.imagePaths.clear();
            if (newImagePaths != null) {
                this.imagePaths.addAll(newImagePaths);
            }
            notifyDataSetChanged();
            Log.d(TAG, "Updated image paths. New size: " + this.imagePaths.size());
        });
    }

    public void removeImage(int position) {
        if (position < 0 || position >= imagePaths.size()) {
            Log.e(TAG, "Invalid position for removal: " + position + ", size: " + imagePaths.size());
            return;
        }

        mainHandler.post(() -> {
            try {
                imagePaths.remove(position);
                notifyItemRemoved(position);
                // Only notify range change if there are items after the removed position
                if (position < imagePaths.size()) {
                    notifyItemRangeChanged(position, imagePaths.size() - position);
                }
                Log.d(TAG, "Removed image at position: " + position + ", new size: " + imagePaths.size());
            } catch (Exception e) {
                Log.e(TAG, "Error removing image at position: " + position, e);
                notifyDataSetChanged(); // Fallback to full refresh
            }
        });
    }

    public void addImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.w(TAG, "Attempted to add null or empty image path");
            return;
        }

        mainHandler.post(() -> {
            try {
                int insertPosition = imagePaths.size();
                imagePaths.add(imagePath);
                notifyItemInserted(insertPosition);
                Log.d(TAG, "Added image at position: " + insertPosition + ", new size: " + imagePaths.size());
            } catch (Exception e) {
                Log.e(TAG, "Error adding image", e);
                notifyDataSetChanged();
            }
        });
    }

    public void replaceImage(int position, String newImagePath) {
        if (position < 0 || position >= imagePaths.size()) {
            Log.e(TAG, "Invalid position for replacement: " + position + ", size: " + imagePaths.size());
            return;
        }

        if (newImagePath == null || newImagePath.isEmpty()) {
            Log.w(TAG, "Attempted to replace with null or empty image path");
            return;
        }

        mainHandler.post(() -> {
            try {
                imagePaths.set(position, newImagePath);
                notifyItemChanged(position);
                Log.d(TAG, "Replaced image at position: " + position);
            } catch (Exception e) {
                Log.e(TAG, "Error replacing image at position: " + position, e);
                notifyDataSetChanged();
            }
        });
    }

    public List<String> getImagePaths() {
        return new ArrayList<>(imagePaths);
    }

    public boolean isEmpty() {
        return imagePaths.isEmpty();
    }

    // Odvojena adapter class-a za fullscreen dialog
    private static class FullscreenImageAdapter extends RecyclerView.Adapter<FullscreenImageAdapter.FullscreenViewHolder> {
        private Context context;
        private List<String> imagePaths;
        private Dialog dialog;

        public FullscreenImageAdapter(Context context, List<String> imagePaths, Dialog dialog) {
            this.context = context;
            this.imagePaths = imagePaths;
            this.dialog = dialog;
        }

        @NonNull
        @Override
        public FullscreenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            PhotoView photoView = new PhotoView(context);
            photoView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            photoView.setBackgroundColor(android.graphics.Color.BLACK);
            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new FullscreenViewHolder(photoView);
        }

        @Override
        public void onBindViewHolder(@NonNull FullscreenViewHolder holder, int position) {
            if (position >= 0 && position < imagePaths.size()) {
                String baseUrl = context.getString(R.string.base_url);
                String fullImageUrl = baseUrl + imagePaths.get(position);

                Picasso.get()
                        .load(fullImageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(holder.photoView);

                holder.photoView.setOnClickListener(v -> {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return imagePaths.size();
        }

        static class FullscreenViewHolder extends RecyclerView.ViewHolder {
            PhotoView photoView;

            public FullscreenViewHolder(@NonNull View itemView) {
                super(itemView);
                photoView = (PhotoView) itemView;
            }
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton deleteButton;
        ImageButton replaceButton;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.bookImageView);
            deleteButton = itemView.findViewById(R.id.deleteImageButton);
            replaceButton = itemView.findViewById(R.id.replaceImageButton);
        }
    }
}