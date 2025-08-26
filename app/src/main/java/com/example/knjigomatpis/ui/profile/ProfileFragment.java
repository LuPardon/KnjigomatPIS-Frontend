package com.example.knjigomatpis.ui.profile;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.android.callback.Callback;
import com.auth0.android.management.ManagementException;
import com.auth0.android.management.UsersAPIClient;
import com.auth0.android.result.UserProfile;
import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.R;
import com.example.knjigomatpis.adapters.BookAdapter;
import com.example.knjigomatpis.databinding.FragmentProfileBinding;
import com.example.knjigomatpis.models.Book;
import com.squareup.picasso.Picasso;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    ProfileViewModel profileViewModel;
    private BookAdapter privateBookAdapter;

    //view types trebaju za private collections
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupUserProfile();
        setupPrivateCollection();
        getUserMetadata();

        return root;
    }
    private void setupUserProfile() {
        Picasso.get()
                .load(((MainActivity) getActivity()).cachedUserProfile.getPictureURL())
                .fit()
                .centerCrop()
                .into(binding.ivProfilePicture);

        binding.tvName.setText(((MainActivity) getActivity()).cachedUserProfile.getName());
        binding.tvEmail.setText(((MainActivity) getActivity()).cachedUserProfile.getEmail());

        // Postavljanje početne vrijednosti lokacije
        updateLocationDisplay();

    }
    private void updateLocationDisplay() {
        String location = getString(R.string.location_default);

        if (((MainActivity) getActivity()).cachedUserProfile.getUserMetadata() != null) {
            String userLocation = (String) ((MainActivity) getActivity()).cachedUserProfile.getUserMetadata().get("location");
            if (userLocation != null && !userLocation.trim().isEmpty()) {
                location = userLocation;
            }
        }
        binding.tvLocation.setText(location);
    }

    private void setupPrivateCollection() {
        // Postavljanje RecyclerView za privatne knjige
        privateBookAdapter = new BookAdapter(getContext());

        String currentUserId = ((MainActivity)getActivity()).cachedUserProfile.getId();
        privateBookAdapter.setUserId(currentUserId);

        binding.recyclerViewPrivateBooks.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        binding.recyclerViewPrivateBooks.setAdapter(privateBookAdapter);

        privateBookAdapter.setOnBookClickListener(book -> {
            Bundle bundle = new Bundle();
            bundle.putLong("bookId", book.getBookId());
            NavHostFragment.findNavController(ProfileFragment.this)
                    .navigate(R.id.nav_details_book, bundle);
        });

        // Observe private books
        profileViewModel.fetchPrivateBooks(currentUserId).observe(getViewLifecycleOwner(), books -> {
            if (books != null && !books.isEmpty()) {
                privateBookAdapter.updateBooks(books);

                binding.recyclerViewPrivateBooks.setVisibility(View.VISIBLE);
                binding.tvNoPrivateBooks.setVisibility(View.GONE);
            } else {
                binding.recyclerViewPrivateBooks.setVisibility(View.GONE);
                binding.tvNoPrivateBooks.setVisibility(View.VISIBLE);
            }
        });

        profileViewModel.fetchAllUserBooks(currentUserId).observe(getViewLifecycleOwner(), allBooks -> {
        });

        // Observe statistike direktno iz ViewModela
        profileViewModel.getBorrowedCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                binding.tvBorrowedCount.setText(String.valueOf(count));
            }
        });

        profileViewModel.getAvailableCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                binding.tvAvailableCount.setText(String.valueOf(count));
            }
        });

        // Observe errors
        profileViewModel.getError().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getUserMetadata() {
        UsersAPIClient usersClient = new UsersAPIClient(
                ((MainActivity)getActivity()).account, ((MainActivity)getActivity()).cachedCredentials.getAccessToken());

        usersClient.getProfile(((MainActivity)getActivity()).cachedUserProfile.getId())
                .start(new Callback<UserProfile, ManagementException>() {
                    @Override
                    public void onFailure(ManagementException exception) {
                        ((MainActivity)getActivity()).showSnackBar(getString(R.string.failure_message) + exception.getCode());
                    }

                    @Override
                    public void onSuccess(UserProfile userProfile) {
                        ((MainActivity)getActivity()).cachedUserProfile = userProfile;
                        // Ažuriranje UI nakon dohvaćanja novih podataka
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                updateLocationDisplay();
                            });
                        }
                    }
                });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}