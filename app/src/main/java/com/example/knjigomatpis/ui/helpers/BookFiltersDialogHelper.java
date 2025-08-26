package com.example.knjigomatpis.ui.helpers;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.Book;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class BookFiltersDialogHelper {
    private static final String TAG = "BookFiltersDialogHelper";

    // Prima filtriranu listu knjiga
    public static void showFilterDialog(Context context,
                                        List<Book> visibleBooks,
                                        List<String> currentActiveFilters,
                                        List<String> currentSelectedYearRanges,
                                        List<String> currentSelectedPageRanges,
                                        BookFiltersHelper.FilterCallback callback) {

        View dialogView = LayoutInflater.from(context).inflate(R.layout.filter_bottom_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(dialogView);

        setupBottomSheetBehavior(dialog, dialogView, context);

        LinearLayout genreLayout = dialogView.findViewById(R.id.genreCheckboxContainer);
        LinearLayout languageLayout = dialogView.findViewById(R.id.languageCheckboxContainer);
        LinearLayout publisherLayout = dialogView.findViewById(R.id.publisherCheckboxContainer);
        LinearLayout conditionLayout = dialogView.findViewById(R.id.conditionCheckboxContainer);
        LinearLayout yearRangeContainer = dialogView.findViewById(R.id.yearRangeContainer);
        LinearLayout pageRangeContainer = dialogView.findViewById(R.id.pageRangeContainer);

        try {
            // Add range checkboxes - koristi sve knjige za range računanje
            BookFiltersHelper.addRangeCheckBoxes(context, yearRangeContainer,
                    BookFiltersHelper.getYearRanges(),
                    "year", currentSelectedYearRanges);

            BookFiltersHelper.addRangeCheckBoxes(context, pageRangeContainer,
                    BookFiltersHelper.getPageRanges(),
                    "page", currentSelectedPageRanges);

            // Koristi visibleBooks umjesto allBooks za filter opcije
            BookFiltersHelper.addCheckBoxes(context, genreLayout, "genre",
                    BookFiltersHelper.getAvailableGenres(visibleBooks),
                    currentActiveFilters);

            BookFiltersHelper.addCheckBoxes(context, languageLayout, "language",
                    BookFiltersHelper.getAvailableLanguages(visibleBooks),
                    currentActiveFilters);

            BookFiltersHelper.addCheckBoxes(context, publisherLayout, "publisher",
                    BookFiltersHelper.getAvailablePublishers(visibleBooks),
                    currentActiveFilters);

            BookFiltersHelper.addCheckBoxes(context, conditionLayout, "condition",
                    BookFiltersHelper.getAvailableConditions(visibleBooks),
                    currentActiveFilters);

        } catch (Exception e) {
            showErrorAlert(context,
                    context.getString(R.string.dialog_error_title),
                    context.getString(R.string.error_loading_filters));
            dialog.dismiss();
            return;
        }

        // Setup buttons
        Button applyButton = dialogView.findViewById(R.id.applyFiltersBtn);
        Button resetButton = dialogView.findViewById(R.id.resetFiltersBtn);

        applyButton.setOnClickListener(v -> {
            try {
                List<String> newActiveFilters = new ArrayList<>();
                List<String> newSelectedYearRanges = new ArrayList<>();
                List<String> newSelectedPageRanges = new ArrayList<>();

                // Pokupi range filtere
                collectRangeFilters(yearRangeContainer, newSelectedYearRanges);
                collectRangeFilters(pageRangeContainer, newSelectedPageRanges);

                // Pokupi ostale filtere
                LinearLayout[] allFilterContainers = {
                        genreLayout, languageLayout, publisherLayout, conditionLayout
                };

                for (LinearLayout container : allFilterContainers) {
                    collectFilters(container, newActiveFilters);
                }

                // Direktno primijeni filtere bez alert dialoga
                callback.onFiltersApplied(newActiveFilters, newSelectedYearRanges, newSelectedPageRanges);
                dialog.dismiss();

            } catch (Exception e) {
                showErrorAlert(context,
                        context.getString(R.string.dialog_error_title),
                        context.getString(R.string.error_applying_filters));
            }
        });

        resetButton.setOnClickListener(v -> {
            showResetFiltersConfirmation(context, () -> {
                try {
                    callback.onFiltersReset();
                    dialog.dismiss();
                } catch (Exception e) {
                    showErrorAlert(context,
                            context.getString(R.string.dialog_error_title),
                            context.getString(R.string.error_resetting_filters));
                }
            });
        });

        dialog.setOnDismissListener(d -> {
        });

        dialog.show();
    }

    private static void setupBottomSheetBehavior(BottomSheetDialog dialog, View dialogView, Context context) {
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            // Maksimalna visina (60% ekrana)
            int maxHeight = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.6);
            bottomSheet.getLayoutParams().height = maxHeight;
            bottomSheet.requestLayout();

            behavior.setPeekHeight(maxHeight);
            behavior.setSkipCollapsed(false);
            behavior.setDraggable(false);
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            behavior.setFitToContents(true);
        }
    }

    private static void collectRangeFilters(LinearLayout container, List<String> selectedRanges) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof CheckBox && ((CheckBox) child).isChecked()) {
                selectedRanges.add(((CheckBox) child).getText().toString());
            }
        }
    }

    private static void collectFilters(LinearLayout container, List<String> activeFilters) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) child;
                if (checkBox.isChecked()) {
                    activeFilters.add((String) checkBox.getTag());
                }
            }
        }
    }

    // ALERT dialog metode
    private static void showResetFiltersConfirmation(Context context, Runnable onConfirm) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setTitle(context.getString(R.string.dialog_reset_filters_title))
                .setMessage(context.getString(R.string.message_reset_filters_confirmation))
                .setPositiveButton(context.getString(R.string.btn_reset), (d, which) -> onConfirm.run())
                .setNegativeButton(context.getString(R.string.btn_cancel), null)
                .setCancelable(false)
                .create();

        styleAlertDialog(context, dialog);
        dialog.show();
    }

    private static void showErrorAlert(Context context, String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.btn_ok), null)
                .create();

        styleErrorDialog(context, dialog);
        dialog.show();
    }

    // STYLING UTILITY metode
    private static void styleAlertDialog(Context context, AlertDialog dialog) {
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(context, R.color.primary_color));
                positiveButton.setTypeface(null, Typeface.BOLD);
            }

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                negativeButton.setTextColor(ContextCompat.getColor(context, R.color.secondary_color));
            }
        });
    }

    private static void styleErrorDialog(Context context, AlertDialog dialog) {
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(context, R.color.error_color));
                positiveButton.setTypeface(null, Typeface.BOLD);
            }

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                negativeButton.setTextColor(ContextCompat.getColor(context, R.color.secondary_color));
            }
        });
    }
}