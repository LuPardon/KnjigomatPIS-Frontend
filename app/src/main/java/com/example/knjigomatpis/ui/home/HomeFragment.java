package com.example.knjigomatpis.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.models.Book;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.adapters.BookAdapter;
import com.example.knjigomatpis.databinding.FragmentHomeBinding;
import com.example.knjigomatpis.helpers.BookFiltersDialogHelper;
import com.example.knjigomatpis.helpers.BookFiltersHelper;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private BookAdapter adapter;
    private HomeViewModel homeViewModel;
    private String currentUserId;
    private final List<String> activeFilters = new ArrayList<>();
    List<String> selectedYearRanges = new ArrayList<>();
    List<String> selectedPageRanges = new ArrayList<>();
    private String currentQuery = "";


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null && mainActivity.cachedUserProfile != null) {
            currentUserId = mainActivity.cachedUserProfile.getId();
        }

        adapter = new BookAdapter(getContext());


        if (currentUserId != null) {
            adapter.setUserId(currentUserId);
            homeViewModel.fetchBooks(currentUserId);
        }

        binding.bookRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        binding.bookRecyclerView.setAdapter(adapter);

        // PRIJE fetchBooks()
        observeViewModel();

        adapter.setOnBookClickListener(book -> {
            Bundle bundle = new Bundle();
            bundle.putLong("bookId", book.getBookId());
            NavHostFragment.findNavController(HomeFragment.this)
                    .navigate(R.id.nav_details_book, bundle);
        });

        binding.imageButton.setOnClickListener(v -> {
            List<Book> currentBooks = homeViewModel.getBooks().getValue();
            if (currentBooks == null) return;

            BookFiltersDialogHelper.showFilterDialog(
                    getContext(),
                    currentBooks,
                    activeFilters,
                    selectedYearRanges,
                    selectedPageRanges,
                    new BookFiltersHelper.FilterCallback() {
                        @Override
                        public void onFiltersApplied(List<String> newActiveFilters,
                                                     List<String> newSelectedYearRanges,
                                                     List<String> newSelectedPageRanges) {

                            activeFilters.clear();
                            activeFilters.addAll(newActiveFilters);

                            selectedYearRanges.clear();
                            selectedYearRanges.addAll(newSelectedYearRanges);

                            selectedPageRanges.clear();
                            selectedPageRanges.addAll(newSelectedPageRanges);

                            // Primjena filtera
                            adapter.filterBooks(currentQuery, activeFilters, selectedYearRanges, selectedPageRanges);
                            updateActiveFiltersDisplay();
                        }

                        @Override
                        public void onFiltersReset() {
                            currentQuery = "";
                            activeFilters.clear();
                            selectedYearRanges.clear();
                            selectedPageRanges.clear();
                            adapter.resetBooks();
                            updateActiveFiltersDisplay();
                        }
                    }
            );
        });

        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                // Za search pozivanje s praznim range listama
                adapter.filterBooks(currentQuery, activeFilters, selectedYearRanges, selectedPageRanges);
                return true;
            }
        });

        // Drži SearchView zatvoren na početku
        binding.searchView.setIconifiedByDefault(true);
        binding.searchView.setIconified(true);

        // Pronađi EditText unutar SearchView-a
        View searchEditText = binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchEditText instanceof EditText) {
            ((EditText) searchEditText).setHint(getString(R.string.search_hint));
        }

        // Klik na cijeli SearchView
        binding.searchView.setOnClickListener(v -> {
            binding.searchView.setIconified(false); // Otvori search
            if (searchEditText != null) {
                searchEditText.requestFocus(); // Fokus na tekstualni unos
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
        return root;
    }

    private void updateActiveFiltersDisplay() {
        String displayText = BookFiltersHelper.createActiveFiltersDisplayText(
                getContext(),
                activeFilters,
                selectedYearRanges,
                selectedPageRanges);

        if (displayText == null || displayText.isEmpty()) {
            binding.activeFiltersTextView.setVisibility(View.GONE);
        } else {
            binding.activeFiltersTextView.setVisibility(View.VISIBLE);
            binding.activeFiltersTextView.setText(displayText);
        }
    }
    private void observeViewModel() {
        homeViewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            // PROVJERA DA LI JE USER ID POSTAVLJEN PRIJE POZIVA
            if (currentUserId != null) {
                adapter.setUserId(currentUserId);
            } else {
                // Ako još nema currentUserId, pokušaj ponovno dohvatiti
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null && mainActivity.cachedUserProfile != null) {
                    currentUserId = mainActivity.cachedUserProfile.getId();
                    adapter.setUserId(currentUserId);
                }
            }
            adapter.setBooksListInitial(books);

            reapplyCurrentFilters();
        });

        homeViewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> {
        });

        homeViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
            }
        });
    }
    private void reapplyCurrentFilters() {
        // Provjera ima li aktivnih filtera
        boolean hasActiveFilters = !activeFilters.isEmpty() ||
                !selectedYearRanges.isEmpty() ||
                !selectedPageRanges.isEmpty() ||
                !currentQuery.isEmpty();

        if (hasActiveFilters) {
            // Primjena postojećih filtera
            adapter.filterBooks(currentQuery, activeFilters, selectedYearRanges, selectedPageRanges);
            updateActiveFiltersDisplay();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reapplyCurrentFilters();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    public void openSearch() {
        if (binding == null || binding.searchView == null) return;

        // Toggle logika - otvori/zatvori ovisno o stanju
        if (binding.searchView.isIconified()) {
            // Zatvoren - otvori
            binding.searchView.setIconified(false);
            binding.searchView.onActionViewExpanded(); // eksplicitno expandiranje

            binding.searchView.requestFocus();

            View searchEditText = binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchEditText != null) {
                searchEditText.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    searchEditText.postDelayed(() -> {
                        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                    }, 100);
                }
            }
        } else {
            // Otvoren - provjeri je li prazan
            String query = binding.searchView.getQuery().toString().trim();
            if (query.isEmpty()) {
                // Prazan - zatvori
                binding.searchView.setIconified(true);
                binding.searchView.onActionViewCollapsed();
                binding.searchView.clearFocus();

                // Sakrivanje tipkovnice
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && getActivity() != null && getActivity().getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }
            }else {
                // Ima tekst - samo sakrij tipkovnicu i ukloni fokus
                binding.searchView.clearFocus();

                // Sakrivanje tipkovnice
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && getActivity() != null && getActivity().getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }
            }
        }
    }
}