package com.example.knjigomatpis.ui.detailsBook;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.adapters.BookImagePagerAdapter;
import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.services.BookService;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class EditBookFragment extends Fragment implements BookImagePagerAdapter.OnImageActionListener {

    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private static final String TAG = "EditBookFragment";
    private static final String ARG_BOOK_ID = "bookId";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int CAMERA_REQUEST = 1002;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // State variables
    private DetailsBookViewModel mViewModel;
    private Book currentBook;
    private BookImagePagerAdapter imageAdapter;
    private BookService bookService;

    private int pendingImagePosition = -1;
    private String pendingOriginalImagePath = null;
    private Uri tempImageUri;

    // UI komponente
    private ViewPager2 imageViewPager;
    private ProgressBar progressBar;

    // Book info polja
    private EditText editTitle, editAuthor, editPublicationYear;
    private EditText editPublisher, editLanguage, editPageCount;
    private EditText editDescription, editNotes;
    private Spinner spinnerCondition, spinnerVisibility, spinnerStatus, spinnerGenre;
    private Button buttonSave, buttonCancel, buttonDelete;

    public static EditBookFragment newInstance(long bookId) {
        EditBookFragment fragment = new EditBookFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_BOOK_ID, bookId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookService = new BookService();
    }

    //  Registracija launchera
    private void setupActivityLaunchers() {
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSuccess -> {
                    if (isSuccess && tempImageUri != null) {
                        processImageResult(tempImageUri);
                    } else {
                        showToast("Error taking photo");
                        resetPendingOperation();
                    }
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        processImageResult(uri);
                    } else {
                        showToast("Error selecting image");
                        resetPendingOperation();
                    }
                }
        );

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        showPermissionDeniedDialog();
                        resetPendingOperation();
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_book, container, false);

        setupActivityLaunchers();
        initializeViews(view);
        setupSpinners();
        setupViewModel();

        return view;
    }

    private void initializeViews(View view) {
        // Image components
        imageViewPager = view.findViewById(R.id.imageViewPager);

        // Book info fields
        editTitle = view.findViewById(R.id.editTitle);
        editAuthor = view.findViewById(R.id.editAuthor);
        editPublicationYear = view.findViewById(R.id.editPublicationYear);
        editPublisher = view.findViewById(R.id.editPublisher);
        editLanguage = view.findViewById(R.id.editLanguage);
        editPageCount = view.findViewById(R.id.editPageCount);
        editDescription = view.findViewById(R.id.editDescription);
        editNotes = view.findViewById(R.id.editNotes);

        // Spinners
        spinnerCondition = view.findViewById(R.id.spinnerCondition);
        spinnerVisibility = view.findViewById(R.id.spinnerVisibility);
        spinnerStatus = view.findViewById(R.id.spinnerStatus);
        spinnerGenre = view.findViewById(R.id.spinnerGenre);

        // Buttons
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonCancel = view.findViewById(R.id.buttonCancel);
        buttonDelete = view.findViewById(R.id.buttonDelete);

        setupClickListeners();
    }

    private void setupClickListeners() {
        buttonSave.setOnClickListener(v -> saveBook());
        buttonCancel.setOnClickListener(v -> navigateBack());
        buttonDelete.setOnClickListener(v -> deleteBook());
    }

    private void deleteBook() {
        if (currentBook == null) {
            showToast(getString(R.string.error_no_book_data));
            return;
        }

        // Potvrda brisanja knjige
        showStyledConfirmationDialog(
                getString(R.string.dialog_delete_book_title),
                getString(R.string.dialog_delete_book_message, currentBook.getTitle()),
                getString(R.string.btn_delete),
                getString(R.string.btn_cancel),
                () -> performBookDelete()
        );
    }
    private void performBookDelete() {
        showProgress(true);
        bookService.deleteBook(currentBook.getBookId(), new BookService.DeleteBookCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> handleBookDeleteSuccess());
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> handleBookDeleteError(t));
            }
        });
    }

    // SUCCESS DELETE HANDLER - samo obavijest
    private void handleBookDeleteSuccess() {
        showProgress(false);

        showSuccessNotification(getString(R.string.message_book_deleted_success));

        Log.d(TAG, getString(R.string.debug_book_deleted, currentBook.getTitle()));

        // Automatski navigiraj nazad nakon kratke pauze
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getActivity() != null) {
                NavController navController = Navigation.findNavController(requireView());
                navController.navigate(R.id.nav_myBooks);
            }
        }, 1500); // 1.5 sekunde da korisnik vidi obavijest
    }

    private void showSuccessNotification(String message) {

        if (getView() != null) {
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
            snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_color));

            View snackbarView = snackbar.getView();
            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (textView != null) {
                textView.setTextColor(Color.WHITE);
                textView.setTypeface(null, Typeface.BOLD);
            }

            snackbar.show();
        }}

    private void handleBookDeleteError(Throwable t) {
        showProgress(false);
        String errorMsg = getString(R.string.error_deleting_book, t.getMessage());

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_book_delete_error_title))
                .setMessage(getString(R.string.message_book_delete_error, t.getMessage()))
                .setPositiveButton(getString(R.string.btn_retry), (d, which) -> deleteBook())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleErrorDialog(dialog);
        dialog.show();

        Log.e(TAG, "Error deleting book", t);
    }

    private void setupSpinners() {
        setupSpinner(spinnerCondition, BookUtils.getConditionOptions(requireContext()));
        setupSpinner(spinnerVisibility, BookUtils.getVisibilityOptions(requireContext()));
        setupSpinner(spinnerStatus, BookUtils.getStatusOptions(requireContext()));
        setupSpinner(spinnerGenre, BookUtils.getGenreOptions(requireContext()));
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupViewModel() {
        mViewModel = new ViewModelProvider(this).get(DetailsBookViewModel.class);

        long bookId = getBookIdFromArguments();
        if (bookId == -1) {
            handleError(getString(R.string.invalid_book_id), true);
            return;
        }

        mViewModel.fetchBookById(bookId);
        observeBookData();
    }

    private long getBookIdFromArguments() {
        Bundle args = getArguments();
        return args != null ? args.getLong(ARG_BOOK_ID, -1) : -1;
    }

    private void observeBookData() {
        mViewModel.getBook().observe(getViewLifecycleOwner(), book -> {
            if (book != null) {
                currentBook = book;
                populateFields(book);
                Log.d(TAG, getString(R.string.debug_book_loaded, book.getTitle()));
            } else {
                handleError(getString(R.string.error_loading_book_data), false);
            }
        });
    }

    private void populateFields(Book book) {
        setupImagePager(book);
        populateTextFields(book);
        setSpinnerSelections(book);
    }

    private void setupImagePager(Book book) {
        imageAdapter = new BookImagePagerAdapter(requireContext(),
                book.getImagePaths(), true, this);
        imageViewPager.setAdapter(imageAdapter);
    }

    private void populateTextFields(Book book) {
        editTitle.setText(book.getTitle());
        editAuthor.setText(book.getAuthor());
        editPublicationYear.setText(String.valueOf(book.getPublicationYear()));
        editPublisher.setText(book.getPublisher());
        editLanguage.setText(book.getBookLanguage());
        editPageCount.setText(String.valueOf(book.getPageCount()));
        editDescription.setText(book.getBookDescription());
        editNotes.setText(book.getNotes());
    }

    private void setSpinnerSelections(Book book) {
        setSpinnerSelection(spinnerCondition,
                BookUtils.getConditionText(requireContext(), book.getBookConditionId()));
        setSpinnerSelection(spinnerVisibility,
                BookUtils.getVisibilityText(requireContext(), book.getVisibilityId()));
        setSpinnerSelection(spinnerStatus,
                BookUtils.getStatusText(requireContext(), book.getBookStatusId()));
        setSpinnerSelection(spinnerGenre,
                BookUtils.getGenreText(requireContext(), book.getGenreId()));
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    // Image Action Listener implementacija
    @Override
    public void onImageAdd() {
        resetPendingOperation();
        showImagePickerDialog();
    }

    @Override
    public void onImageReplace(int position, String imagePath) {
        pendingImagePosition = position;
        pendingOriginalImagePath = imagePath;
        showImagePickerDialog();
    }

    // Potvrda za brisanje slike (Alert)
    @Override
    public void onImageDelete(int position, String imagePath) {
        if (currentBook == null) return;

        showStyledConfirmationDialog(
                getString(R.string.dialog_delete_image_title),
                getString(R.string.dialog_delete_image_message),
                getString(R.string.btn_delete),
                getString(R.string.btn_cancel),
                () -> performImageDelete(position, imagePath)
        );
    }

    private void performImageDelete(int position, String imagePath) {
        Log.d(TAG, getString(R.string.debug_deleting_image_position, position));

        showProgress(true);
        bookService.deleteBookImage(currentBook.getBookId(), imagePath,
                new BookService.ImageDeleteCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> handleImageDeleteSuccess(position));
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> handleImageDeleteError(t));
                    }
                });
    }

    private void handleImageDeleteSuccess(int position) {
        showProgress(false);

        if (isValidPosition(position, currentBook.getImagePaths().size())) {
            currentBook.getImagePaths().remove(position);
            if (imageAdapter != null) {
                imageAdapter.removeImage(position);
            }

            showStyledSnackbar(getString(R.string.message_image_deleted_success), true);
            Log.d(TAG, "Image deleted successfully");
        }
    }

    private void handleImageDeleteError(Throwable t) {
        showProgress(false);
        String errorMsg = "Error deleting image: " + t.getMessage();

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_image_delete_error_title))
                .setMessage(getString(R.string.message_image_delete_error, t.getMessage()))
                .setPositiveButton(getString(R.string.btn_ok), null)
                .create();

        styleErrorDialog(dialog);
        dialog.show();

        Log.e(TAG, errorMsg, t);
    }

    private void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        tempImageUri = createImageUri();
        if (tempImageUri != null) {
            takePhotoLauncher.launch(tempImageUri);
        } else {
            showToast("Error creating image file");
            resetPendingOperation();
        }
    }

    // Metoda za kreiranje URI-ja
    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "photo_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        return requireActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }
    private void processImageResult(Uri imageUri) {
        if (isReplaceOperation()) {
            editImage(imageUri, pendingOriginalImagePath);
        } else {
            uploadImage(imageUri);
        }
    }

    private boolean isReplaceOperation() {
        return pendingImagePosition != -1 && pendingOriginalImagePath != null;
    }

    private void uploadImage(Uri imageUri) {
        if (currentBook == null) return;

        File imageFile = createFileFromUri(imageUri);
        if (imageFile == null) {
            showToast("Error processing image");
            resetPendingOperation();
            return;
        }

        MultipartBody.Part body = createMultipartBody(imageFile);
        showProgress(true);

        bookService.uploadBookImage(currentBook.getBookId(), body,
                new BookService.ImageUploadCallback() {
                    @Override
                    public void onSuccess(BookService.ImageUploadResponse response) {
                        runOnUiThread(() -> handleImageUploadSuccess(response));
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> handleImageUploadError(t));
                    }
                });
    }

    private void editImage(Uri imageUri, String originalImagePath) {
        if (currentBook == null) return;

        File imageFile = createFileFromUri(imageUri);
        if (imageFile == null) {
            showToast("Error processing image");
            resetPendingOperation();
            return;
        }

        MultipartBody.Part body = createMultipartBody(imageFile);
        showProgress(true);

        bookService.editBookImage(originalImagePath, body,
                new BookService.ImageUploadCallback() {
                    @Override
                    public void onSuccess(BookService.ImageUploadResponse response) {
                        runOnUiThread(() -> handleImageReplaceSuccess(response));
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> handleImageReplaceError(t));
                    }
                });
    }

    private MultipartBody.Part createMultipartBody(File imageFile) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        return MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);
    }

    private void handleImageUploadSuccess(BookService.ImageUploadResponse response) {
        showProgress(false);
        String imagePath = response.getImagePath();

        currentBook.getImagePaths().add(imagePath);
        if (imageAdapter != null) {
            imageAdapter.addImage(imagePath);
        }

        showStyledSnackbar(getString(R.string.image_uploaded_successfully), true);
        resetPendingOperation();
        Log.d(TAG, getString(R.string.debug_image_upload_success, imagePath));
    }

    private void handleImageUploadError(Throwable t) {
        showProgress(false);
        String errorMsg = getString(R.string.error_uploading_image, t.getMessage());

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_image_upload_error_title))
                .setMessage(getString(R.string.message_image_upload_error, t.getMessage()))
                .setPositiveButton(getString(R.string.btn_ok), null)
                .create();

        styleErrorDialog(dialog);
        dialog.show();
        resetPendingOperation();
        Log.e(TAG, errorMsg, t);
    }

    private void handleImageReplaceSuccess(BookService.ImageUploadResponse response) {
        showProgress(false);
        String newImagePath = response.getImagePath();

        if (isValidPosition(pendingImagePosition, currentBook.getImagePaths().size())) {
            currentBook.getImagePaths().set(pendingImagePosition, newImagePath);
            if (imageAdapter != null) {
                imageAdapter.replaceImage(pendingImagePosition, newImagePath);
            }

            showStyledSnackbar(getString(R.string.image_replaced_successfully), true);
        }

        resetPendingOperation();
        Log.d(TAG, getString(R.string.debug_image_replace_success, newImagePath));
    }

    private void handleImageReplaceError(Throwable t) {
        showProgress(false);
        String errorMsg = getString(R.string.error_replacing_image, t.getMessage());

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_image_replace_error_title))
                .setMessage(getString(R.string.message_image_replace_error, t.getMessage()))
                .setPositiveButton(getString(R.string.btn_ok), null)
                .create();

        styleErrorDialog(dialog);
        dialog.show();
        resetPendingOperation();
        Log.e(TAG, errorMsg, t);
    }

    private File createFileFromUri(Uri uri) {
        try (InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;

            File tempFile = File.createTempFile("upload_image", ".jpg",
                    requireActivity().getCacheDir());

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192]; // Povećan buffer za bolju performansu
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Error creating file from URI", e);
            return null;
        }
    }

    // ALERT-ovi za dozvole
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                showPermissionDeniedDialog();
                resetPendingOperation();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_permission_required_title))
                .setMessage(getString(R.string.dialog_permission_required_message))
                .setPositiveButton(getString(R.string.btn_settings), (d, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleAlertDialog(dialog);
        dialog.show();
    }

    private void saveBook() {
        if (currentBook == null) {
            showToast(getString(R.string.error_no_book_data));
            return;
        }

        if (!validateRequiredFields()) return;

        showStyledConfirmationDialog(
                getString(R.string.dialog_save_changes_title),
                getString(R.string.dialog_save_changes_message),
                getString(R.string.btn_save),
                getString(R.string.btn_cancel),
                () -> performBookSave()
        );
    }

    private void performBookSave() {
        updateBookFromFields();

        showProgress(true);
        bookService.updateBook(currentBook, new BookService.UpdateBookCallback() {
            @Override
            public void onSuccess(Book updatedBook) {
                runOnUiThread(() -> handleBookUpdateSuccess(updatedBook));
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> handleBookUpdateError(t));
            }
        });
    }

    // Metoda za provjeru promjena
    private boolean hasUnsavedChanges() {
        if (currentBook == null) return false;

        return !currentBook.getTitle().equals(editTitle.getText().toString().trim()) ||
                !currentBook.getAuthor().equals(editAuthor.getText().toString().trim()) ||
                currentBook.getPublicationYear() != Long.parseLong(editPublicationYear.getText().toString().trim()) ||
                !currentBook.getPublisher().equals(editPublisher.getText().toString().trim()) ||
                !currentBook.getBookLanguage().equals(editLanguage.getText().toString().trim()) ||
                currentBook.getPageCount() != Long.parseLong(editPageCount.getText().toString().trim()) ||
                !currentBook.getBookDescription().equals(editDescription.getText().toString().trim()) ||
                !currentBook.getNotes().equals(editNotes.getText().toString().trim());
    }

    // Validacija s ALERT-ovima
    private boolean validateRequiredFields() {
        StringBuilder errors = new StringBuilder();

        if (TextUtils.isEmpty(editTitle.getText().toString().trim())) {
            errors.append(getString(R.string.validation_title_required));
        }
        if (TextUtils.isEmpty(editAuthor.getText().toString().trim())) {
            errors.append(getString(R.string.validation_author_required));
        }

        try {
            long year = Long.parseLong(editPublicationYear.getText().toString().trim());
            if (year < 1000 || year > 2025) {
                errors.append(getString(R.string.validation_year_range));
            }
        } catch (NumberFormatException e) {
            errors.append(getString(R.string.validation_year_number));
        }

        try {
            long pages = Long.parseLong(editPageCount.getText().toString().trim());
            if (pages <= 0) {
                errors.append(getString(R.string.validation_pages_positive));
            }
        } catch (NumberFormatException e) {
            errors.append(getString(R.string.validation_pages_number));
        }

        if (errors.length() > 0) {
            AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                    .setTitle(getString(R.string.dialog_validation_error_title))
                    .setMessage(getString(R.string.message_validation_errors, errors.toString()))
                    .setPositiveButton(getString(R.string.btn_ok), null)
                    .create();

            styleErrorDialog(dialog);
            dialog.show();
            return false;
        }

        return true;
    }


    private void updateBookFromFields() {
        currentBook.setTitle(editTitle.getText().toString().trim());
        currentBook.setAuthor(editAuthor.getText().toString().trim());
        currentBook.setPublicationYear(Long.parseLong(editPublicationYear.getText().toString().trim()));
        currentBook.setPublisher(editPublisher.getText().toString().trim());
        currentBook.setBookLanguage(editLanguage.getText().toString().trim());
        currentBook.setPageCount(Long.parseLong(editPageCount.getText().toString().trim()));
        currentBook.setBookDescription(editDescription.getText().toString().trim());
        currentBook.setNotes(editNotes.getText().toString().trim());

        // Ažuriranje ID-jeva iz Spinner-a
        currentBook.setBookConditionId(BookUtils.getConditionIdFromText(requireContext(),
                spinnerCondition.getSelectedItem().toString()));
        currentBook.setVisibilityId(BookUtils.getVisibilityIdFromText(requireContext(),
                spinnerVisibility.getSelectedItem().toString()));
        currentBook.setBookStatusId(BookUtils.getStatusIdFromText(requireContext(),
                spinnerStatus.getSelectedItem().toString()));
        currentBook.setGenreId(BookUtils.getGenreIdFromText(requireContext(),
                spinnerGenre.getSelectedItem().toString()));
    }

    // SUCCESS UPDATE HANDLER - samo obavijest
    private void handleBookUpdateSuccess(Book updatedBook) {
        showProgress(false);

        showSuccessNotification(getString(R.string.message_book_updated_success));

        Log.d(TAG, getString(R.string.debug_book_updated, updatedBook.getTitle()));

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            navigateBack();
        }, 1500);
    }

    // Mrežne greške
    private void handleBookUpdateError(Throwable t) {
        showProgress(false);
        String errorMsg = getString(R.string.error_updating_book, t.getMessage());


        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_book_update_error_title))
                .setMessage(getString(R.string.message_book_update_error, t.getMessage()))
                .setPositiveButton(getString(R.string.btn_retry), (d, which) -> saveBook())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleErrorDialog(dialog);
        dialog.show();

        Log.e(TAG, "Error updating book", t);
    }

    // UTILITY metode za ALERT-ove
    private void showStyledConfirmationDialog(String title, String message, String positiveText, String negativeText, Runnable onConfirm) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, (d, which) -> onConfirm.run())
                .setNegativeButton(negativeText, null)
                .setCancelable(false)
                .create();

        styleAlertDialog(dialog);
        dialog.show();
    }

    private void styleAlertDialog(AlertDialog dialog) {
        dialog.setOnShowListener(d -> {

            // Positive button styling
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color));
                positiveButton.setTypeface(null, Typeface.BOLD);
            }

            // Negative button styling
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_color));
            }
        });
    }

    private void styleSuccessDialog(AlertDialog dialog) {
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_color));
                positiveButton.setTypeface(null, Typeface.BOLD);
            }
        });
    }

    private void styleErrorDialog(AlertDialog dialog) {
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_color));
                positiveButton.setTypeface(null, Typeface.BOLD);
            }

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_color));
            }
        });
    }

    private void showStyledSnackbar(String message, boolean isSuccess) {
        if (getView() != null) {
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT);

            if (isSuccess) {
                snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_color));
            } else {
                snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_color));
            }

            // Style the text
            View snackbarView = snackbar.getView();
            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (textView != null) {
                textView.setTextColor(Color.WHITE);
                textView.setTypeface(null, Typeface.BOLD);
            }

            snackbar.show();
        }
    }


    // Utility metode
    private void resetPendingOperation() {
        pendingImagePosition = -1;
        pendingOriginalImagePath = null;
    }

    private boolean isValidPosition(int position, int size) {
        return position >= 0 && position < size;
    }

    private void runOnUiThread(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void handleError(String message, boolean finish) {
        Log.e(TAG, message);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_error_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.btn_ok), (d, which) -> {
                    if (finish) {
                        navigateBack();
                    }
                })
                .create();

        styleAlertDialog(dialog);
        dialog.show();
    }

    private void navigateBack() {
        if (hasUnsavedChanges()) {
            showStyledConfirmationDialog(
                    getString(R.string.dialog_discard_changes_title),
                    getString(R.string.dialog_discard_changes_message),
                    getString(R.string.btn_discard),
                    getString(R.string.btn_cancel),
                    () -> {
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    }
            );
        } else {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }
}