package com.example.knjigomatpis.ui.detailsBook;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.R;
import com.example.knjigomatpis.adapters.BookImagePagerAdapter;
import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.services.BookService;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class CreateBookFragment extends Fragment implements BookImagePagerAdapter.OnImageActionListener {

    private static final String TAG = "CreateBookFragment";
    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int CAMERA_REQUEST = 1002;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private BookImagePagerAdapter imageAdapter;
    private BookService bookService;
    private List<String> tempImagePaths;

    // UI elementi
    private ViewPager2 imageViewPager;
    private ProgressBar progressBar;
    private EditText addTitle;
    private EditText addAuthor;
    private Spinner spinnerGenre;
    private EditText addPublicationYear;
    private EditText addPublisher;
    private EditText addLanguage;
    private EditText addPageCount;
    private EditText addDescription;
    private EditText addNotes;
    private Spinner spinnerCondition;
    private Spinner spinnerVisibility;
    private Spinner spinnerStatus;
    private Button btnAddBook;
    private Button buttonCancel;

    // Image handling
    private int pendingImagePosition = -1;
    private Uri tempImageUri;
    private final ArrayList<File> selectedImages;

    public CreateBookFragment() {
        selectedImages = new ArrayList<>();
    }

    public static CreateBookFragment newInstance() {
        return new CreateBookFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookService = new BookService();
        tempImagePaths = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_book, container, false);

        initializeViews(view);
        setupSpinners();
        setupImageViewPager();

        return view;
    }

    private void initializeViews(View view) {
        imageViewPager = view.findViewById(R.id.imageViewPager);
        progressBar = view.findViewById(R.id.progressBar);
        addTitle = view.findViewById(R.id.addTitle);
        addAuthor = view.findViewById(R.id.addAuthor);
        spinnerGenre = view.findViewById(R.id.spinnerGenre);
        addPublicationYear = view.findViewById(R.id.addPublicationYear);
        addPublisher = view.findViewById(R.id.addPublisher);
        addLanguage = view.findViewById(R.id.addLanguage);
        addPageCount = view.findViewById(R.id.addPageCount);
        addDescription = view.findViewById(R.id.addDescription);
        addNotes = view.findViewById(R.id.addNotes);
        spinnerCondition = view.findViewById(R.id.spinnerCondition);
        spinnerVisibility = view.findViewById(R.id.spinnerVisibility);
        spinnerStatus = view.findViewById(R.id.spinnerStatus);
        btnAddBook = view.findViewById(R.id.btnAddBook);
        buttonCancel = view.findViewById(R.id.buttonCancel);

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnAddBook.setOnClickListener(v -> showCreateBookConfirmation());
        buttonCancel.setOnClickListener(v -> showCancelConfirmation());
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

    private void setupImageViewPager() {
        imageAdapter = new BookImagePagerAdapter(requireContext(), tempImagePaths, true, this);
        imageViewPager.setAdapter(imageAdapter);
    }

    // Image Action Listener implementacija
    @Override
    public void onImageAdd() {
        pendingImagePosition = -1;
        showImagePickerDialog();
    }

    @Override
    public void onImageReplace(int position, String imagePath) {
        pendingImagePosition = position;
        showImagePickerDialog();
    }

    @Override
    public void onImageDelete(int position, String imagePath) {
        showStyledConfirmationDialog(
                getString(R.string.dialog_delete_image_title),
                getString(R.string.dialog_delete_image_message),
                getString(R.string.btn_delete),
                getString(R.string.btn_cancel),
                () -> performImageDelete(position)
        );
    }

    private void performImageDelete(int position) {
        if (position >= 0 && position < tempImagePaths.size()) {
            tempImagePaths.remove(position);
            imageAdapter.removeImage(position);
            showStyledSnackbar(getString(R.string.message_image_deleted_success), true);
            Log.d(TAG, "Image removed from position: " + position);
        }
    }

    private void showImagePickerDialog() {
        String[] options = {getString(R.string.camera), getString(R.string.gallery)};

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.select_image_source))
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else {
                        openGallery();
                    }
                })
                .create();

        styleAlertDialog(dialog);
        dialog.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                tempImageUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getApplicationContext().getPackageName() + ".provider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                showErrorAlert(
                        getString(R.string.dialog_error_title),
                        getString(R.string.error_creating_image_file)
                );
            }
        } else {
            showErrorAlert(
                    getString(R.string.dialog_error_title),
                    getString(R.string.camera_not_available)
            );
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = null;

            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                imageUri = data.getData();
            } else if (requestCode == CAMERA_REQUEST) {
                imageUri = tempImageUri;
            }

            if (imageUri != null) {
                handleImageSelection(imageUri);
            } else {
                showErrorAlert(
                        getString(R.string.dialog_error_title),
                        getString(R.string.error_getting_image)
                );
            }
        }
    }

    private void handleImageSelection(Uri imageUri) {
        try {
            String uriString = imageUri.toString();

            if (pendingImagePosition == -1) {
                // Dodavanje nove slike
                tempImagePaths.add(uriString);
                imageAdapter.addImage(uriString);
                addImageToBookImages(null, 0);
            } else {
                // Zamjena postojeće slike
                tempImagePaths.set(pendingImagePosition, uriString);
                imageAdapter.replaceImage(pendingImagePosition, uriString);
            }

            showStyledSnackbar(getString(R.string.image_added), true);
            Log.d(TAG, "Image selection handled successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error handling image selection", e);
            showErrorAlert(
                    getString(R.string.dialog_error_title),
                    getString(R.string.error_adding_image)
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showPermissionDeniedDialog();
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

    private boolean validateRequiredFields() {
        List<String> errors = new ArrayList<>();
        boolean isValid = true;

        // Resetiranje svih postojećih grešaka
        clearFieldErrors();

        // 1. NASLOV - obavezan
        String title = addTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            errors.add(getString(R.string.validation_title_required));
            addTitle.setError(getString(R.string.validation_title_required));
            if (isValid) addTitle.requestFocus();
            isValid = false;
        } else if (title.length() < 2) {
            errors.add(getString(R.string.validation_title_min_length));
            addTitle.setError(getString(R.string.validation_title_min_length));
            if (isValid) addTitle.requestFocus();
            isValid = false;
        } else if (title.length() > 200) {
            errors.add(getString(R.string.validation_title_max_length));
            addTitle.setError("Naslov može imati maksimalno 200 znakova");
            if (isValid) addTitle.requestFocus();
            isValid = false;
        }

        // 2. AUTOR - obavezan
        String author = addAuthor.getText().toString().trim();
        if (TextUtils.isEmpty(author)) {
            errors.add(getString(R.string.validation_author_required));
            addAuthor.setError(getString(R.string.validation_author_required));
            if (isValid) addAuthor.requestFocus();
            isValid = false;
        } else if (author.length() < 2) {
            errors.add(getString(R.string.validation_author_min_length));
            addAuthor.setError("Autor mora imati barem 2 znakova");
            if (isValid) addAuthor.requestFocus();
            isValid = false;
        } else if (author.length() > 100) {
            errors.add(getString(R.string.validation_author_max_length));
            addAuthor.setError("Autor može imati maksimalno 100 znakova");
            if (isValid) addAuthor.requestFocus();
            isValid = false;
        }

        // 3. GODINA IZDANJA - obavezna
        String yearText = addPublicationYear.getText().toString().trim();
        if (TextUtils.isEmpty(yearText)) {
            errors.add(getString(R.string.validation_year_required));
            addPublicationYear.setError("Godina izdanja je obavezna");
            if (isValid) addPublicationYear.requestFocus();
            isValid = false;
        } else {
            try {
                long year = Long.parseLong(yearText);
                if (year < 1000 || year > 2025) {
                    errors.add(getString(R.string.validation_year_range));
                    addPublicationYear.setError(getString(R.string.validation_year_range));
                    if (isValid) addPublicationYear.requestFocus();
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                errors.add(getString(R.string.validation_year_number));
                addPublicationYear.setError(getString(R.string.validation_year_number));
                if (isValid) addPublicationYear.requestFocus();
                isValid = false;
            }
        }

        // 4. IZDAVAČ - obavezan
        String publisher = addPublisher.getText().toString().trim();
        if (TextUtils.isEmpty(publisher)) {
            errors.add(getString(R.string.validation_publisher_required));
            addPublisher.setError("Izdavač je obavezan");
            if (isValid) addPublisher.requestFocus();
            isValid = false;
        } else if (publisher.length() < 2) {
            errors.add(getString(R.string.validation_publisher_min_length));
            addPublisher.setError("Izdavač mora imati barem 2 znakova");
            if (isValid) addPublisher.requestFocus();
            isValid = false;
        }

        // 5. JEZIK - obavezan
        String language = addLanguage.getText().toString().trim();
        if (TextUtils.isEmpty(language)) {
            errors.add(getString(R.string.validation_language_required));
            addLanguage.setError("Jezik je obavezan");
            if (isValid) addLanguage.requestFocus();
            isValid = false;
        } else if (language.length() < 2) {
            errors.add(getString(R.string.validation_language_min_length));
            addLanguage.setError("Jezik mora imati barem 2 znakova");
            if (isValid) addLanguage.requestFocus();
            isValid = false;
        }

        // 6. BROJ STRANICA - obavezan
        String pagesText = addPageCount.getText().toString().trim();
        if (TextUtils.isEmpty(pagesText)) {
            errors.add(getString(R.string.validation_pages_required));
            addPageCount.setError("Broj stranica je obavezan");
            if (isValid) addPageCount.requestFocus();
            isValid = false;
        } else {
            try {
                long pages = Long.parseLong(pagesText);
                if (pages <= 0) {
                    errors.add("• " + getString(R.string.validation_pages_positive));
                    addPageCount.setError(getString(R.string.validation_pages_positive));
                    if (isValid) addPageCount.requestFocus();
                    isValid = false;
                } else if (pages > 10000) {
                    errors.add(getString(R.string.validation_pages_max));
                    addPageCount.setError("Knjiga ne može imati više od 10,000 stranica");
                    if (isValid) addPageCount.requestFocus();
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                errors.add("• " + getString(R.string.validation_pages_number));
                addPageCount.setError(getString(R.string.validation_pages_number));
                if (isValid) addPageCount.requestFocus();
                isValid = false;
            }
        }

        // OPCIONALNE VALIDACIJE (description i notes nisu obavezni)
        // Provjera duljine opisa ako je unešena
        String description = addDescription.getText().toString().trim();
        if (!TextUtils.isEmpty(description) && description.length() > 1000) {
            errors.add(getString(R.string.validation_description_max));
            addDescription.setError("Opis može imati maksimalno 1000 znakova");
            if (isValid) addDescription.requestFocus();
            isValid = false;
        }

        // Provjera duljine bilješki ako su unešene
        String notes = addNotes.getText().toString().trim();
        if (!TextUtils.isEmpty(notes) && notes.length() > 500) {
            errors.add(getString(R.string.validation_notes_max));
            addNotes.setError("Bilješke mogu imati maksimalno 500 znakova");
            if (isValid) addNotes.requestFocus();
            isValid = false;
        }

        // Prikaz svih grešaka odjednom
        if (!isValid) {
            showValidationErrorDialog(errors);
        }

        return isValid;
    }

    // Metoda za čišćenje grešaka
    private void clearFieldErrors() {
        addTitle.setError(null);
        addAuthor.setError(null);
        addPublicationYear.setError(null);
        addPublisher.setError(null);
        addLanguage.setError(null);
        addPageCount.setError(null);
        addDescription.setError(null);
        addNotes.setError(null);
    }

    // Dialog za prikazivanje grešaka validacije
    private void showValidationErrorDialog(List<String> errors) {
        StringBuilder message = new StringBuilder();

        for (String error : errors) {
            message.append(error).append("\n");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_validation_title))
                .setMessage(getString(R.string.dialog_validation_message) + "\n" + message.toString())
                .setPositiveButton(getString(R.string.btn_ok), (d, which) -> {
                    // Fokus na prvo polje s greškom
                    focusFirstErrorField();
                })
                .setCancelable(false)
                .create();

        // Stiliziranje kao error dialog
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_color));
                positiveButton.setTypeface(null, Typeface.BOLD);
                positiveButton.setTextSize(16);
            }
        });

        dialog.show();
    }

    // Metoda za fokusiranje na prvo polje s greškom
    private void focusFirstErrorField() {
        if (addTitle.getError() != null) {
            addTitle.requestFocus();
        } else if (addAuthor.getError() != null) {
            addAuthor.requestFocus();
        } else if (addPublicationYear.getError() != null) {
            addPublicationYear.requestFocus();
        } else if (addPublisher.getError() != null) {
            addPublisher.requestFocus();
        } else if (addLanguage.getError() != null) {
            addLanguage.requestFocus();
        } else if (addPageCount.getError() != null) {
            addPageCount.requestFocus();
        } else if (addDescription.getError() != null) {
            addDescription.requestFocus();
        } else if (addNotes.getError() != null) {
            addNotes.requestFocus();
        }
    }

    // Metoda za kreiranje knjige
    private void showCreateBookConfirmation() {
        // Provjera validacije
        if (!validateRequiredFields()) {
            return; // Ne nastavlja ako validacija ne prolazi
        }

        // Ako nema slika, upozori korisnika
        if (tempImagePaths.isEmpty()) {
            showNoImagesWarningDialog();
            return;
        }

        // Prikazivanje sažetka prije kreiranja
        showBookSummaryDialog();
    }

    // Dialog upozorenja ako nema slika
    private void showNoImagesWarningDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_no_images_title))
                .setMessage(getString(R.string.dialog_no_images_message))
                .setPositiveButton(getString(R.string.btn_add_images), (d, which) -> showImagePickerDialog())
                .setNegativeButton(getString(R.string.btn_create_without_images), (d, which) -> showBookSummaryDialog())
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color));
                negativeButton.setTypeface(null, Typeface.BOLD);
            }
        });

        dialog.show();
    }

    // Dialog sa sažetkom knjige prije kreiranja
    private void showBookSummaryDialog() {
        StringBuilder summary = new StringBuilder();
        summary.append(getString(R.string.summary_title, addTitle.getText().toString().trim())).append("\n");
        summary.append(getString(R.string.summary_author, addAuthor.getText().toString().trim())).append("\n");
        summary.append(getString(R.string.summary_year, addPublicationYear.getText().toString().trim())).append("\n");
        summary.append(getString(R.string.summary_publisher, addPublisher.getText().toString().trim())).append("\n");
        summary.append(getString(R.string.summary_language, addLanguage.getText().toString().trim())).append("\n");
        summary.append(getString(R.string.summary_pages, addPageCount.getText().toString().trim())).append("\n");
        summary.append(getString(R.string.summary_genre, spinnerGenre.getSelectedItem().toString())).append("\n");
        summary.append(getString(R.string.summary_condition, spinnerCondition.getSelectedItem().toString())).append("\n");
        summary.append(getString(R.string.summary_visibility, spinnerVisibility.getSelectedItem().toString())).append("\n");
        summary.append(getString(R.string.summary_status, spinnerStatus.getSelectedItem().toString())).append("\n");
        summary.append(getString(R.string.summary_images, tempImagePaths.size()));

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_summary_title))
                .setMessage(getString(R.string.dialog_summary_message) + "\n\n" + summary.toString())
                .setPositiveButton(requireContext().getString(R.string.create_book), (d, which) -> performBookCreation())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                positiveButton.setTypeface(null, Typeface.BOLD);
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_color));
            }
        });

        dialog.show();
    }

    // Confirmation dialog za odustajanje
    private void showCancelConfirmation() {
        if (hasUnsavedData()) {
            showStyledConfirmationDialog(
                    getString(R.string.dialog_discard_changes_title),
                    getString(R.string.dialog_discard_changes_message),
                    getString(R.string.btn_discard),
                    getString(R.string.btn_cancel),
                    this::navigateBack
            );
        } else {
            navigateBack();
        }
    }

    // Provjera ima li korisnik unos podataka
    private boolean hasUnsavedData() {
        return !TextUtils.isEmpty(addTitle.getText().toString().trim()) ||
                !TextUtils.isEmpty(addAuthor.getText().toString().trim()) ||
                !TextUtils.isEmpty(addPublicationYear.getText().toString().trim()) ||
                !TextUtils.isEmpty(addPublisher.getText().toString().trim()) ||
                !TextUtils.isEmpty(addLanguage.getText().toString().trim()) ||
                !TextUtils.isEmpty(addPageCount.getText().toString().trim()) ||
                !TextUtils.isEmpty(addDescription.getText().toString().trim()) ||
                !TextUtils.isEmpty(addNotes.getText().toString().trim()) ||
                !tempImagePaths.isEmpty();
    }

    private void performBookCreation() {
        // Dohvaćanje userId iz MainActivity
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity == null || mainActivity.cachedUserProfile == null) {
            showErrorAlert(
                    getString(R.string.dialog_error_title),
                    getString(R.string.user_not_authenticated)
            );
            return;
        }

        String userId = mainActivity.cachedUserProfile.getId();

        // Kreiranje Book objekta
        Book newBook = createBookFromInputs();

        // Prvo popuni selectedImages iz tempImagePaths
        selectedImages.clear(); // Očisti postojeće

        for (String imagePath : tempImagePaths) {
            try {
                Uri imageUri = Uri.parse(imagePath);
                File imageFile = createFileFromUri(imageUri);
                if (imageFile != null) {
                    selectedImages.add(imageFile);
                    Log.d(TAG, "Added image file: " + imageFile.getName());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing image: " + imagePath, e);
            }
        }

        // Priprema slika - sada selectedImages ima sadržaj
        MultipartBody.Part[] surveyImagesParts = new MultipartBody.Part[selectedImages.size()];
        for (int i = 0; i < selectedImages.size(); i++) {
            File file = selectedImages.get(i);
            surveyImagesParts[i] = createMultipartBody(file);
            Log.d(TAG, "Created multipart for: " + file.getName());
        }

        Log.d(TAG, "Sending " + surveyImagesParts.length + " images to backend");

        showProgress(true);

        bookService.createBook(newBook, userId, surveyImagesParts, new BookService.CreateBookCallback() {
            @Override
            public void onSuccess(Book createdBook) {
                runOnUiThread(() -> handleBookCreationSuccess(createdBook));
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> handleBookCreationError(t));
            }
        });
    }

    private Book createBookFromInputs() {
        Book newBook = new Book();
        newBook.setTitle(addTitle.getText().toString().trim());
        newBook.setAuthor(addAuthor.getText().toString().trim());

        // Parsiranje brojčanih vrijednosti
        String yearText = addPublicationYear.getText().toString().trim();
        if (!TextUtils.isEmpty(yearText)) {
            try {
                newBook.setPublicationYear(Long.parseLong(yearText));
            } catch (NumberFormatException e) {
                newBook.setPublicationYear(null);
            }
        }

        String pagesText = addPageCount.getText().toString().trim();
        if (!TextUtils.isEmpty(pagesText)) {
            try {
                newBook.setPageCount(Long.parseLong(pagesText));
            } catch (NumberFormatException e) {
                newBook.setPageCount(null);
            }
        }

        newBook.setPublisher(addPublisher.getText().toString().trim());
        newBook.setBookLanguage(addLanguage.getText().toString().trim());
        newBook.setBookDescription(addDescription.getText().toString().trim());
        newBook.setNotes(addNotes.getText().toString().trim());

        // Postavljanje ID-jeva iz spinnera
        newBook.setBookConditionId(BookUtils.getConditionIdFromText(requireContext(),
                spinnerCondition.getSelectedItem().toString()));
        newBook.setGenreId(BookUtils.getGenreIdFromText(requireContext(),
                spinnerGenre.getSelectedItem().toString()));
        newBook.setVisibilityId(BookUtils.getVisibilityIdFromText(requireContext(),
                spinnerVisibility.getSelectedItem().toString()));
        newBook.setBookStatusId(BookUtils.getStatusIdFromText(requireContext(),
                spinnerStatus.getSelectedItem().toString()));

        newBook.setImagePaths(new ArrayList<>());

        return newBook;
    }

    private void handleBookCreationSuccess(Book createdBook) {
        showProgress(false);
        showStyledSnackbar(getString(R.string.message_book_created_success), true);

        // Automatska navigacija natrag nakon kratke pauze
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Book created successfully: " + createdBook.getTitle());
            navigateBack();
        }, 1500); // 1.5 sekunde pauze
    }

    private void handleBookCreationError(Throwable t) {
        showProgress(false);
        String errorMessage = getString(R.string.error_creating_book, t.getMessage());

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_book_creation_error_title))
                .setMessage(getString(R.string.message_book_creation_error, t.getMessage()))
                .setPositiveButton(getString(R.string.btn_retry), (d, which) -> showCreateBookConfirmation())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleErrorDialog(dialog);
        dialog.show();

        Log.e(TAG, "Error creating book", t);
    }

    private void addImageToBookImages(Book createdBook, int index) {
        if (index >= tempImagePaths.size()) {
            return;
        }

        String uriString = tempImagePaths.get(index);
        Uri imageUri = Uri.parse(uriString);

        try {
            File imageFile = createFileFromUri(imageUri);
            if (imageFile == null) {
                addImageToBookImages(createdBook, index + 1);
                return;
            }
            selectedImages.add(imageFile);
        } catch (Exception e) {
            Log.e(TAG, "Error processing image " + (index + 1), e);
            addImageToBookImages(createdBook, index + 1);
        }
    }

    private File createFileFromUri(Uri uri) {
        try (InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;

            File tempFile = File.createTempFile("upload_image", ".jpg",
                    requireActivity().getCacheDir());

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
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

    private MultipartBody.Part createMultipartBody(File imageFile) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        return MultipartBody.Part.createFormData("images", imageFile.getName(), requestFile);
    }

    // STYLING UTILITY metode
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

    private void showErrorAlert(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.btn_ok), null)
                .create();

        styleErrorDialog(dialog);
        dialog.show();
    }

    private void styleAlertDialog(AlertDialog dialog) {
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color));
                positiveButton.setTypeface(null, Typeface.BOLD);
            }

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_color));
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

            View snackbarView = snackbar.getView();
            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (textView != null) {
                textView.setTextColor(Color.WHITE);
                textView.setTypeface(null, Typeface.BOLD);
            }

            snackbar.show();
        }
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

    private void navigateBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
}
