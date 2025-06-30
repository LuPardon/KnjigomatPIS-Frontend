

package com.example.knjigomatpis.ui.myBooks;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.R;
import com.example.knjigomatpis.adapters.BookAdapter;
import com.example.knjigomatpis.databinding.FragmentMyBooksBinding;
import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.ui.helpers.BookFiltersDialogHelper;
import com.example.knjigomatpis.ui.helpers.BookFiltersHelper;

import java.util.ArrayList;
import java.util.List;

public class MyBooksFragment extends Fragment implements BookFiltersHelper.FilterCallback {

    private FragmentMyBooksBinding binding;
    private BookAdapter adapter;
    private MyBooksViewModel myBooksViewModel;
    private String currentUserId;

    // Filter varijable
    private final List<String> activeFilters = new ArrayList<>();
    private final List<String> selectedYearRanges = new ArrayList<>();
    private final List<String> selectedPageRanges = new ArrayList<>();
    private String currentQuery = "";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        currentUserId = ((MainActivity)getActivity()).cachedUserProfile.getId();

        myBooksViewModel = new ViewModelProvider(this).get(MyBooksViewModel.class);
        myBooksViewModel.fetchBooksByUserId(currentUserId);

        binding = FragmentMyBooksBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null && mainActivity.cachedUserProfile != null) {
            currentUserId = mainActivity.cachedUserProfile.getId();
        }

        setupRecyclerView();
        setupClickListeners();
        setupSearchView();
        updateActiveFiltersDisplay();

        observeViewModel();

        return root;
    }

    private void setupRecyclerView() {
        adapter = new BookAdapter(getContext());

        if (currentUserId != null) {
            adapter.setUserId(currentUserId);
        }

        binding.bookRecyclerView2.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );
        binding.bookRecyclerView2.setAdapter(adapter);

        adapter.setOnBookClickListener(book -> {
            Bundle bundle = new Bundle();
            bundle.putLong("bookId", book.getBookId());
            NavHostFragment.findNavController(MyBooksFragment.this)
                    .navigate(R.id.nav_details_book, bundle);
        });
    }

    private void setupClickListeners() {
        // Filter button
        binding.imageButton.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        List<com.example.knjigomatpis.models.Book> currentBooks = myBooksViewModel.getBooks().getValue();
        if (currentBooks == null) {
            Toast.makeText(getContext(), getString(R.string.no_books_available_filtering), Toast.LENGTH_SHORT).show();
            return;
        }

        BookFiltersDialogHelper.showFilterDialog(
                getContext(),
                currentBooks,
                activeFilters,
                selectedYearRanges,
                selectedPageRanges,
                this
        );
    }

    private void setupSearchView() {
        if (binding.searchView != null) {
            binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentQuery = newText;
                    // Primijeni pretragu SAMO ako ima knjiga
                    List<Book> currentBooks = myBooksViewModel.getBooks().getValue();
                    if (currentBooks != null && !currentBooks.isEmpty()) {
                        adapter.filterBooks(currentQuery, activeFilters, selectedYearRanges, selectedPageRanges);
                    }
                    return true;
                }
            });

            binding.searchView.setIconifiedByDefault(true);
            binding.searchView.setIconified(true);

            View searchEditText = binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchEditText instanceof EditText) {
                ((EditText) searchEditText).setHint(getString(R.string.search_hint));
            }

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

    private void updateActiveFiltersDisplay() {
        String displayText = BookFiltersHelper.createActiveFiltersDisplayText(
                requireContext(),
                activeFilters,
                selectedYearRanges,
                selectedPageRanges
        );

        if (displayText == null || displayText.isEmpty()) {
            if (binding.activeFiltersTextView != null) {
                binding.activeFiltersTextView.setVisibility(View.GONE);
            }
        } else {
            if (binding.activeFiltersTextView != null) {
                binding.activeFiltersTextView.setVisibility(View.VISIBLE);
                binding.activeFiltersTextView.setText(displayText);
            }
        }
    }

    private void observeViewModel() {
        myBooksViewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            if (currentUserId != null) {
                adapter.setUserId(currentUserId);
            }
            adapter.setBooksList(books);

            if (books != null && !books.isEmpty()) {
                adapter.setBooksList(books);
                binding.bookRecyclerView2.setVisibility(View.VISIBLE);
                binding.tvNoMyBooks.setVisibility(View.GONE);

                // Primijeni trenutne filtere nakon što se podaci učitaju
                adapter.filterBooks(currentQuery, activeFilters, selectedYearRanges, selectedPageRanges);
            } else {
                binding.bookRecyclerView2.setVisibility(View.GONE);
                binding.tvNoMyBooks.setVisibility(View.VISIBLE);
            }
        });

        myBooksViewModel.getError().observe(getViewLifecycleOwner(), errorMessage -> {
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public void onFiltersApplied(List<String> newActiveFilters,
                                 List<String> newSelectedYearRanges,
                                 List<String> newSelectedPageRanges) {
        // Update filter lists
        activeFilters.clear();
        activeFilters.addAll(newActiveFilters);

        selectedYearRanges.clear();
        selectedYearRanges.addAll(newSelectedYearRanges);

        selectedPageRanges.clear();
        selectedPageRanges.addAll(newSelectedPageRanges);

        // Apply filters to adapter SAMO ako ima knjiga
        List<Book> currentBooks = myBooksViewModel.getBooks().getValue();
        if (currentBooks != null && !currentBooks.isEmpty()) {
            adapter.filterBooks(currentQuery, activeFilters, selectedYearRanges, selectedPageRanges);
        }
        updateActiveFiltersDisplay();
    }

    @Override
    public void onFiltersReset() {
        // Clear all filters
        currentQuery = "";
        activeFilters.clear();
        selectedYearRanges.clear();
        selectedPageRanges.clear();

        adapter.resetBooks();
        updateActiveFiltersDisplay();

        if (binding.searchView != null) {
            binding.searchView.setQuery("", false);
            binding.searchView.clearFocus();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

