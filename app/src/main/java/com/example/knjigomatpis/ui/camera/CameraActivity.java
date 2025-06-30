package com.example.knjigomatpis.ui.camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.knjigomatpis.R;

import java.io.ByteArrayOutputStream;

public class CameraActivity extends AppCompatActivity {
    private ImageView imageView;
    private Uri photoUri;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestGalleryPermissionLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        imageView = findViewById(R.id.imageView);
        Button btnChooseImage = findViewById(R.id.btnChooseImage);
        Button btnSaveImage = findViewById(R.id.btnSaveImage);


        takePhotoLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
            if (isSuccess && photoUri != null) {
                imageView.setImageURI(photoUri);
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                imageView.setImageURI(uri);
            }
        });

        requestCameraPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show();
                    }
                });

        requestGalleryPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        Toast.makeText(this, getString(R.string.storage_permission_denied), Toast.LENGTH_SHORT).show();
                    }
                });

        btnChooseImage.setOnClickListener(v -> showImagePickerDialog());
        btnSaveImage.setOnClickListener(v -> {
            Bitmap bmp = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 85, stream);
            byte[] byteArray = stream.toByteArray();

        });
    }

    private void showImagePickerDialog() {
        String[] options = {
                getString(R.string.take_photo),
                getString(R.string.choose_from_gallery)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_image_source))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkAndRequestCameraPermission();
                    } else {
                        checkAndRequestGalleryPermission();
                    }
                })
                .show();
    }


    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkAndRequestGalleryPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*");
        } else {
            requestGalleryPermissionLauncher.launch(permission);
        }
    }

    private void launchCamera() {
        photoUri = createImageUri();
        if (photoUri != null) {
            takePhotoLauncher.launch(photoUri);
        }
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "photo_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}
