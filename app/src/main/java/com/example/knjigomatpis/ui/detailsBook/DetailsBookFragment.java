package com.example.knjigomatpis.ui.detailsBook;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.adapters.BookImagePagerAdapter;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.Book;

import java.util.ArrayList;
import java.util.List;

public class DetailsBookFragment extends Fragment {

    private DetailsBookViewModel mViewModel;

    public static DetailsBookFragment newInstance() {
        return new DetailsBookFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details_book, container, false);

        mViewModel = new ViewModelProvider(this).get(DetailsBookViewModel.class);

        long bookId = getArguments() != null ? getArguments().getLong("bookId") : -1;
        Log.d("DetailsDebug", getString(R.string.debug_received_book_id, bookId));

        if (bookId != -1) {
            mViewModel.fetchBookById(bookId);
        } else {
            Log.e("DetailsDebug", getString(R.string.debug_invalid_book_id));
            return view; // Vrati prazan view ako nema bookId
        }

        // Promatranje promjena podataka knjige
        mViewModel.getBook().observe(getViewLifecycleOwner(), book -> {
            Log.d("DetailsDebug", getString(R.string.debug_observer_triggered) +
                    (book != null ? book.getTitle() : "null"));

            if (book != null) {
                List<String> imagePaths = book.getImagePaths();
                Log.d("DetailsDebug", getString(R.string.debug_image_paths_count) +
                        (imagePaths != null ? imagePaths.size() : "null"));

                if (imagePaths != null) {
                    for (int i = 0; i < imagePaths.size(); i++) {
                        Log.d("DetailsDebug", getString(R.string.debug_image_path, i, imagePaths.get(i)));
                    }
                }

                ViewPager2 viewPager = view.findViewById(R.id.imageViewPager);

                //  Provjera ima li slika prije postavljanja adapter-a
                if (imagePaths != null && !imagePaths.isEmpty()) {
                    BookImagePagerAdapter imageAdapter = new BookImagePagerAdapter(requireContext(), imagePaths);
                    viewPager.setOffscreenPageLimit(imagePaths.size());
                    viewPager.setAdapter(imageAdapter);
                    viewPager.setVisibility(View.VISIBLE);
                } else {
                    // Postavljanje praznog adapter-a ako nema slika
                    BookImagePagerAdapter emptyAdapter = new BookImagePagerAdapter(requireContext(), new ArrayList<>());
                    viewPager.setAdapter(emptyAdapter);
                    viewPager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);
                    Log.d("DetailsDebug", getString(R.string.debug_no_images_found));
                }

                // Debug za ViewPager2:
                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        Log.d("DetailsDebug", getString(R.string.debug_viewpager_page_changed, position));
                    }

                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                        super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                        Log.d("DetailsDebug", getString(R.string.debug_viewpager_scrolled, position, positionOffset));
                    }
                });

                // Poziva MainActivity da postavi FAB
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setupDetailsBookFab(book);
                }

                TextView textTitle = view.findViewById(R.id.textTitle);
                TextView textAuthor = view.findViewById(R.id.textAuthor);
                TextView textGenre = view.findViewById(R.id.textGenre);
                TextView textPublicationYear = view.findViewById(R.id.textPublicationYear);
                TextView textPublisher = view.findViewById(R.id.textPublisher);
                TextView textLanguage = view.findViewById(R.id.textLanguage);
                TextView textPageCount = view.findViewById(R.id.textPageCount);
                TextView textDescription = view.findViewById(R.id.textDescription);
                TextView textNotes = view.findViewById(R.id.textNotes);
                TextView textCondition = view.findViewById(R.id.textCondition);
                TextView textVisibility = view.findViewById(R.id.textVisibility);
                TextView textStatus = view.findViewById(R.id.textStatus);

                textTitle.setText(book.getTitle());
                textAuthor.setText(book.getAuthor());
                textGenre.setText(BookUtils.getGenreText(requireContext(), book.getGenreId()));
                textPublicationYear.setText(String.valueOf(book.getPublicationYear()));
                textPublisher.setText(book.getPublisher());
                textLanguage.setText(book.getBookLanguage());
                textPageCount.setText(String.valueOf(book.getPageCount()));
                textDescription.setText(book.getBookDescription());
                textNotes.setText(book.getNotes());
                textCondition.setText(BookUtils.getConditionText(requireContext(), book.getBookConditionId()));
                textVisibility.setText(BookUtils.getVisibilityText(requireContext(), book.getVisibilityId()));

                String statusText = BookUtils.getStatusText(requireContext(), book.getBookStatusId());
                textStatus.setText(statusText);

                // Stiliziranje ovisno o statusu
                if (book.getBookStatusId() == 2) { // zauzeto
                    textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.unavailable_color));
                    Drawable lockIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_lock_idle_lock);
                    textStatus.setCompoundDrawablesWithIntrinsicBounds(lockIcon, null, null, null);
                    textStatus.setCompoundDrawablePadding(8);
                } else { // dostupno
                    textStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
                    textStatus.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
            } else {
                Log.d("DetailsDebug", "Observer triggered. Book: " + book);
            }
        });

        return view;
    }
                private boolean isBookUnavailable(Book book) {
                    return book.getBookStatusId() == 2; //  "zauzeto"
                }
}