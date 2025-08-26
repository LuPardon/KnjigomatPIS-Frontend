package com.example.knjigomatpis.ui.exchangeHistory;

import android.os.Build;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.knjigomatpis.models.UserExchangeHistoryResponse;
import com.example.knjigomatpis.network.ApiClient;
import com.example.knjigomatpis.network.IApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryViewModel extends ViewModel {

    private final MutableLiveData<List<UserExchangeHistoryResponse>> originalList, filteredList;
    private final IApiService IApiService = ApiClient.getRetrofit().create(IApiService.class);
    private String currentUserId;
    private List<String> activeFilters = new ArrayList<>();
    private String currentSearchQuery = "";

    public HistoryViewModel() {
        originalList = new MutableLiveData<>();
        filteredList = new MutableLiveData<>();
    }

    public void setUserId(String userId) {
        this.currentUserId = userId;
        fetchHistory();
    }

    private void fetchHistory() {
        IApiService.getUserExchangeHistory(currentUserId).enqueue(new Callback<List<UserExchangeHistoryResponse>>() {
            @Override
            public void onResponse(Call<List<UserExchangeHistoryResponse>> call, Response<List<UserExchangeHistoryResponse>> response) {
                if (response.isSuccessful()) {
                    originalList.setValue(response.body());
                    applyAllFilters(); // Primjenjivanje svih aktivnih filtera
                } else {
                    // Handle error
                }
            }

            @Override
            public void onFailure(Call<List<UserExchangeHistoryResponse>> call, Throwable t) {
                // Handle failure
            }
        });
    }

    public void setActiveFilters(List<String> filters) {
        this.activeFilters = new ArrayList<>(filters);
        applyAllFilters();
    }

    public void resetFilters() {
        this.activeFilters.clear();
        this.currentSearchQuery = "";
        applyAllFilters();
    }

    public void FilterExchangesByQuery(String query) {
        this.currentSearchQuery = query != null ? query : "";
        applyAllFilters();
    }

    private void applyAllFilters() {
        List<UserExchangeHistoryResponse> originalData = originalList.getValue();
        if (originalData == null) {
            return;
        }

        List<UserExchangeHistoryResponse> filtered = new ArrayList<>(originalData);

        // Primjena search query filtera
        if (!currentSearchQuery.isEmpty()) {
            filtered = filterByQuery(filtered, currentSearchQuery);
        }
        filtered = applyOwnershipFilters(filtered);
        filtered = applyStatusFilters(filtered);

        filteredList.setValue(filtered);
    }

    private List<UserExchangeHistoryResponse> filterByQuery(List<UserExchangeHistoryResponse> items, String query) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return items.stream()
                    .filter(item ->
                            item.getBook().getTitle().toLowerCase().contains(query.toLowerCase()) ||
                                    item.getBook().getAuthor().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
        } else {
            // Fallback za starije verzije Androida
            List<UserExchangeHistoryResponse> result = new ArrayList<>();
            for (UserExchangeHistoryResponse item : items) {
                if (item.getBook().getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        item.getBook().getAuthor().toLowerCase().contains(query.toLowerCase())) {
                    result.add(item);
                }
            }
            return result;
        }
    }

    private List<UserExchangeHistoryResponse> applyOwnershipFilters(List<UserExchangeHistoryResponse> items) {
        boolean filterMine = activeFilters.contains("ownership_mine");
        boolean filterOthers = activeFilters.contains("ownership_others");

        // Ako niti jedan ownership filter nije aktivan, vraća sve
        if (!filterMine && !filterOthers) {
            return items;
        }

        List<UserExchangeHistoryResponse> result = new ArrayList<>();

        for (UserExchangeHistoryResponse item : items) {
            boolean isMine = item.getBorrower_id().equals(currentUserId);
            boolean isOthers = item.getLender_id().equals(currentUserId);

            if ((filterMine && isMine) || (filterOthers && isOthers)) {
                result.add(item);
            }
        }

        return result;
    }

    private List<UserExchangeHistoryResponse> applyStatusFilters(List<UserExchangeHistoryResponse> items) {
        List<String> statusFilters = new ArrayList<>();

        // Izdvajanje svih status filtera
        for (String filter : activeFilters) {
            if (filter.startsWith("exchange_status_")) {
                statusFilters.add(filter.replace("exchange_status_", ""));
            }
        }

        // Ako nema status filtera, vraća sve
        if (statusFilters.isEmpty()) {
            return items;
        }

        List<UserExchangeHistoryResponse> result = new ArrayList<>();

        for (UserExchangeHistoryResponse item : items) {
            String itemStatusString = String.valueOf(item.getStatus_ex_id());
            if (statusFilters.contains(itemStatusString)) {
                result.add(item);
            }
        }

        return result;
    }
    public void FilterExchangesByMe() {
        List<String> filters = new ArrayList<>(activeFilters);
        filters.removeIf(f -> f.startsWith("ownership_"));
        filters.add("ownership_mine");
        setActiveFilters(filters);
    }

    public void FilterExchangesByOthers() {
        List<String> filters = new ArrayList<>(activeFilters);
        filters.removeIf(f -> f.startsWith("ownership_"));
        filters.add("ownership_others");
        setActiveFilters(filters);
    }

    public MutableLiveData<List<UserExchangeHistoryResponse>> getHistory() {
        return filteredList;
    }

    public List<String> getActiveFilters() {
        return new ArrayList<>(activeFilters);
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public List<UserExchangeHistoryResponse> getOriginalList() {
        return originalList.getValue();
    }
}