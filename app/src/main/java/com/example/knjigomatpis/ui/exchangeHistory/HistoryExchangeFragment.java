package com.example.knjigomatpis.ui.exchangeHistory;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.auth0.android.result.UserProfile;
import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.R;
import com.example.knjigomatpis.adapters.ExchangeHistoryAdapter;
import com.example.knjigomatpis.databinding.FragmentHistoryExchangeBinding;
import com.example.knjigomatpis.models.UserExchangeHistoryResponse;
import com.example.knjigomatpis.ui.helpers.HistoryFiltersDialogHelper;

import java.util.List;

public class HistoryExchangeFragment extends Fragment implements HistoryFiltersDialogHelper.HistoryFilterCallback {

    private static final String TAG = "HistoryExchangeFragment";
    private FragmentHistoryExchangeBinding binding;
    private HistoryViewModel viewModel;
    private UserProfile userProfile;
    private ExchangeHistoryAdapter adapter;
    private SearchView searchView;
    private String currentQuery = "";

    public static HistoryExchangeFragment newInstance() {
        return new HistoryExchangeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            userProfile = mainActivity.cachedUserProfile;

            if (userProfile != null) {
                Log.d(TAG, "User profile loaded: " + userProfile.getId());
            } else {
                Log.w(TAG, "User profile is null");
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHistoryExchangeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        searchView = binding.searchView;

        setupRecyclerView();
        setupSearchView();
        setupUserData();

        return root;
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.historyItemsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Provjera da li je userProfile dostupan
        if (userProfile == null) {
            Log.e(TAG, "UserProfile is null, cannot setup adapter properly");
        }

        // Observiranje promjena u podacima
        viewModel.getHistory().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty()) {
                Log.d(TAG, "Received " + items.size() + " history items");

                if (adapter == null) {
                    adapter = new ExchangeHistoryAdapter(items, this::onHistoryItemClick, userProfile);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.updateData(items);
                }

                // Prikaži recycler view, sakrij empty poruku
                binding.historyItemsRecyclerView.setVisibility(View.VISIBLE);
                binding.noHistoryContainer.setVisibility(View.GONE);

            } else {
                Log.w(TAG, "Received null or empty history items");

                // Sakrij recycler view, prikaži empty poruku
                binding.historyItemsRecyclerView.setVisibility(View.GONE);
                binding.noHistoryContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupUserData() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();

            if (mainActivity.cachedUserProfile != null) {
                String userId = mainActivity.cachedUserProfile.getId();
                Log.d(TAG, "Setting up data for user: " + userId);

                viewModel.setUserId(userId);
            } else {
                Log.e(TAG, "User profile not available in MainActivity");
            }
        }
    }

    private void showFilterDialog() {
        List<UserExchangeHistoryResponse> originalData = viewModel.getOriginalList();
        if (originalData == null || originalData.isEmpty()) {
            Log.w(TAG, "No data available for filtering");
            return;
        }

        HistoryFiltersDialogHelper.showFilterDialog(
                requireContext(),
                originalData,
                viewModel.getCurrentUserId(),
                viewModel.getActiveFilters(),
                this
        );
    }

    @Override
    public void onFiltersApplied(List<String> activeFilters) {
        Log.d(TAG, "Filters applied: " + activeFilters.toString());
        viewModel.setActiveFilters(activeFilters);
    }

    @Override
    public void onFiltersReset() {
        Log.d(TAG, "Filters reset");
        viewModel.resetFilters();

        // Također resetiraj search query
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
        }
        currentQuery = "";
    }

    @Override
    public void onResume() {
        super.onResume();
        // Osvježavanje podataka kada se fragment vraća u fokus
        if (viewModel != null && userProfile != null) {
            viewModel.setUserId(userProfile.getId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }

    private void onHistoryItemClick(UserExchangeHistoryResponse historyItem) {
        Log.d(TAG, "History item clicked: " + historyItem.getBook().getTitle());

        // Implementirati navigaciju na detalje razmjene
        // Na primjer:
        // Navigation.findNavController(requireView()).navigate(
        //     HistoryExchangeFragmentDirections.actionHistoryToExchangeDetails(historyItem.getExchange_id())
        // );

        // Ili otvaranje dijaloga s detaljima
        // showExchangeDetailsDialog(historyItem);

        // Ili navigacija na chat
        // if (getActivity() instanceof MainActivity) {
        //     ((MainActivity) getActivity()).navigateToChatFromExchange(historyItem);
        // }
    }

    public void openFilterDialog() {
        showFilterDialog(); // Poziva postojeću privatnu metodu
    }
    private void setupSearchView() {
        if (searchView != null) {
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    viewModel.FilterExchangesByQuery(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentQuery = newText;
                    return false;
                }
            });

            // Search view konfiguracija
            binding.searchView.setIconifiedByDefault(true);
            binding.searchView.setIconified(true);

            View searchEditText = binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchEditText instanceof EditText) {
                ((EditText) searchEditText).setHint(getString(R.string.search_hint));
            }

            // Handle search view click
            binding.searchView.setOnClickListener(v -> {
                binding.searchView.setIconified(false);
                if (searchEditText != null) {
                    searchEditText.requestFocus();
                    InputMethodManager imm = (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }
    }
}