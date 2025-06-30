package com.example.knjigomatpis.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.Callback;
import com.auth0.android.management.ManagementException;
import com.auth0.android.management.UsersAPIClient;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private EditProfileViewModel mViewModel;
    private View view;
    private EditText editFirstName, editLastName, editLocation;
    private Button btnSave;
    private ImageButton btnChangePassword;
    private Auth0 account;
    private Credentials cachedCredentials;
    private UserProfile cachedUserProfile;
    private TextView tvEmail;
    private ImageView ivProfilePicture;

    public static EditProfileFragment newInstance() {
        return new EditProfileFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        editFirstName = view.findViewById(R.id.editFirstName);
        editLastName = view.findViewById(R.id.editLastName);
        editLocation = view.findViewById(R.id.editLocation);
        btnSave = view.findViewById(R.id.btnSaveProfile);
        btnChangePassword = view.findViewById(R.id.imageBtnResetPassword);
        tvEmail = view.findViewById(R.id.tvEmail);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);

        // Popunjavanje ako dobijemo podatke kao argumente
        Bundle args = getArguments();
        if (args != null) {}

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        account = ((MainActivity) getActivity()).account;
        cachedCredentials = ((MainActivity) getActivity()).cachedCredentials;
        cachedUserProfile = ((MainActivity) getActivity()).cachedUserProfile;

        editFirstName.setText(cachedUserProfile.getUserMetadata().get("firstName") == null ? "" : cachedUserProfile.getUserMetadata().get("firstName").toString());
        editLastName.setText(cachedUserProfile.getUserMetadata().get("lastName") == null ? "" : cachedUserProfile.getUserMetadata().get("lastName").toString());
        editLocation.setText(cachedUserProfile.getUserMetadata().get("location") == null ? "" : cachedUserProfile.getUserMetadata().get("location").toString());
        tvEmail.setText(cachedUserProfile.getEmail());

        Picasso.get()
                .load(((MainActivity) getActivity()).cachedUserProfile.getPictureURL())
                .fit()
                .centerCrop()
                .into(ivProfilePicture);

        btnSave.setOnClickListener(v -> {
            showSaveChangesDialog();
        });

        btnChangePassword.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        mViewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
    }

    private void showSaveChangesDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.btn_save))
                .setMessage(getString(R.string.dialog_save_message))
                .setPositiveButton(getString(R.string.btn_save), (dialog, which) -> {
                    patchUserMetadata();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showChangePasswordDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_password_title))
                .setMessage(getString(R.string.dialog_password_message))
                .setPositiveButton(getString(R.string.btn_send), (dialog, which) -> {
                    resetPassword();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void resetPassword() {
        AuthenticationAPIClient client = new AuthenticationAPIClient(account);
        client.resetPassword(cachedUserProfile.getEmail(), "Username-Password-Authentication")
                .start(new Callback<Void, AuthenticationException>() {
                    @Override
                    public void onFailure(AuthenticationException exception) {
                        ((MainActivity)getActivity()).showSnackBar(getString(R.string.failure_message) + exception.getMessage());
                    }
                    @Override
                    public void onSuccess(Void result) {
                        ((MainActivity)getActivity()).showSnackBar((getString(R.string.request_sent_email)));
                    }
                });
    }
    private void patchUserMetadata() {// za update korisnika

        // dohvaćanje podataka iz main activity-ja
        UsersAPIClient usersClient = new UsersAPIClient(account, cachedCredentials.getAccessToken());
        Map<String, String> metadata = new HashMap<>();

        metadata.put("firstName", editFirstName.getText().toString());
        metadata.put("lastName", editLastName.getText().toString());
        metadata.put("location", editLocation.getText().toString());

        // primjer slanja zahtjeva za aužuriranje podataka o korisniku
        usersClient
                .updateMetadata(cachedUserProfile.getId(), metadata)
                .start(new Callback<UserProfile, ManagementException>() {
                    @Override
                    public void onFailure(ManagementException exception) {
                        ((MainActivity) getActivity()).showSnackBar(getString(R.string.failure_message) + exception.getCode());
                    }

                    @Override
                    public void onSuccess(UserProfile profile) {
                        ((MainActivity) getActivity()).cachedUserProfile = profile;

                        ((MainActivity)getActivity()).showSnackBar(getString(R.string.successful));
                        Navigation.findNavController(view).navigate(R.id.action_open_profile_after_update);
                    }
                });
    }
}