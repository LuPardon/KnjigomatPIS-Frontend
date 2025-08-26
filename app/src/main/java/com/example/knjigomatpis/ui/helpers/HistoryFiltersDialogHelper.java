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
import com.example.knjigomatpis.models.UserExchangeHistoryResponse;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class HistoryFiltersDialogHelper {
    private static final String TAG = "HistoryFiltersDialogHelper";

    public interface HistoryFilterCallback {
        void onFiltersApplied(List<String> activeFilters);
        void onFiltersReset();
    }

    public static void showFilterDialog(Context context,
                                        List<UserExchangeHistoryResponse> historyItems,
                                        String currentUserId,
                                        List<String> currentActiveFilters,
                                        HistoryFilterCallback callback) {

        View dialogView = LayoutInflater.from(context).inflate(R.layout.filter_history_bottom_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(dialogView);

        setupBottomSheetBehavior(dialog, dialogView, context);

        LinearLayout ownershipLayout = dialogView.findViewById(R.id.ownershipCheckboxContainer);
        LinearLayout exchangeStatusLayout = dialogView.findViewById(R.id.exchangeStatusCheckboxContainer);

        try {
            // Dodavanje ownership filtera (Moja/Od nekog drugog)
            addOwnershipCheckBoxes(context, ownershipLayout, currentActiveFilters);

            // Dodavanje exchange status filtera
            addExchangeStatusCheckBoxes(context, exchangeStatusLayout, historyItems, currentActiveFilters);

        } catch (Exception e) {
            showErrorAlert(context,
                    context.getString(R.string.dialog_error_title),
                    context.getString(R.string.error_resetting_filters));
            dialog.dismiss();
            return;
        }

        // Postavljanje buttons-a
        Button applyButton = dialogView.findViewById(R.id.applyFiltersBtn);
        Button resetButton = dialogView.findViewById(R.id.resetFiltersBtn);

        applyButton.setOnClickListener(v -> {
            try {
                List<String> newActiveFilters = new ArrayList<>();

                collectFilters(ownershipLayout, newActiveFilters);
                collectFilters(exchangeStatusLayout, newActiveFilters);

                callback.onFiltersApplied(newActiveFilters);
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
                            context.getString(R.string.error_loading_filters));
                }
            });
        });

        dialog.show();
    }

    private static void addOwnershipCheckBoxes(Context context, LinearLayout container, List<String> currentActiveFilters) {
        // Očisti postojeće checkbox-ove
        container.removeAllViews();

        // Definiranje ownership opcija
        String[] ownershipOptions = {
                "ownership_mine",     // Moja knjiga (ja sam borrower)
                "ownership_others"    // Od nekog drugog (ja sam lender)
        };

        String[] ownershipLabels = {
                context.getString(R.string.ownership_label_mine),
                context.getString(R.string.ownership_label_others)
        };

        for (int i = 0; i < ownershipOptions.length; i++) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setText(ownershipLabels[i]);
            checkBox.setTag(ownershipOptions[i]);
            checkBox.setTextSize(14);
            checkBox.setPadding(8, 4, 8, 4);

            if (currentActiveFilters.contains(ownershipOptions[i])) {
                checkBox.setChecked(true);
            }

            container.addView(checkBox);
        }
    }

    private static void addExchangeStatusCheckBoxes(Context context, LinearLayout container,
                                                    List<UserExchangeHistoryResponse> historyItems,
                                                    List<String> currentActiveFilters) {
        container.removeAllViews();

        // Dohvati unikatne exchange statuses iz history items
        List<String> availableStatuses = getAvailableExchangeStatuses(historyItems);

        for (String status : availableStatuses) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setText(getExchangeStatusDisplayName(context, status));
            checkBox.setTag("exchange_status_" + status);
            checkBox.setTextSize(14);
            checkBox.setPadding(8, 4, 8, 4);

            // Provjera jesu li trenutni filteri aktivni
            if (currentActiveFilters.contains("exchange_status_" + status)) {
                checkBox.setChecked(true);
            }

            container.addView(checkBox);
        }
    }

    private static List<String> getAvailableExchangeStatuses(List<UserExchangeHistoryResponse> historyItems) {
        List<String> statuses = new ArrayList<>();
        for (UserExchangeHistoryResponse item : historyItems) {
            int statusId = item.getStatus_ex_id();
            String statusIdString = String.valueOf(statusId);

            if (statusId != -1 && !statuses.contains(statusIdString)) {
                statuses.add(statusIdString);
            }
        }
        return statuses;
    }

    private static String getExchangeStatusDisplayName(Context context, String status) {
        // Mapiranje statusa na user-friendly nazive
        switch (status.toLowerCase()) {
            case "1":
                return context.getString(R.string.exchange_completed);
            case "2":
                return context.getString(R.string.exchange_cancelled);
            case "3":
                return context.getString(R.string.exchange_in_progress);
            default:
                return status;
        }
    }

    private static void setupBottomSheetBehavior(BottomSheetDialog dialog, View dialogView, Context context) {
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            // Maksimalna visina (60% ekrana)
            int maxHeight = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.6);
            bottomSheet.getLayoutParams().height = maxHeight;
            bottomSheet.requestLayout();

            // Konfiguracija ponašanja
            behavior.setPeekHeight(maxHeight);
            behavior.setSkipCollapsed(false);
            behavior.setDraggable(false);
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            behavior.setFitToContents(true);
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
    private static void showResetFiltersConfirmation(Context context, Runnable onConfirm) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.reset_filters_title))
                .setMessage(context.getString(R.string.reset_filters_message))
                .setPositiveButton(context.getString(R.string.reset_button), (d, which) -> onConfirm.run())
                .setNegativeButton(context.getString(R.string.cancel_button), null)
                .setCancelable(false)
                .create();
        styleAlertDialog(context, dialog);
        dialog.show();
    }

    private static void showErrorAlert(Context context, String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.ok_button), null)
                .create();

        styleErrorDialog(context, dialog);
        dialog.show();
    }

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
        });
    }
}