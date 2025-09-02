package com.example.knjigomatpis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.Callback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.example.knjigomatpis.databinding.ActivityStartBinding;
import com.example.knjigomatpis.helpers.LanguageHelper;
import com.google.android.material.snackbar.Snackbar;

public class StartActivity extends AppCompatActivity {
    private Auth0 account;
    private ActivityStartBinding binding;
    private Credentials cachedCredentials;
    private UserProfile cachedUserProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Postavljanje jezika prije kreiranja UI-ja
        LanguageHelper.setLocale(this, LanguageHelper.getLanguage(this));

        // Sakrivanje ACTION BAR prije setContentView()
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        account = new Auth0(getString(R.string.com_auth0_client_id), getString(R.string.com_auth0_domain));


        binding = ActivityStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        EdgeToEdge.enable(this);  // omogućuje prikaz preko cijelog ekrana

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.authButton.setOnClickListener(v -> loginWithBrowser());
        setupLanguageButton();
    }
    // Metoda za pravilno rukovanje promjenom jezika
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.updateBaseContextLocale(newBase));
    }

    // Metoda za postavljanje language button-a
    private void setupLanguageButton() {
        ImageButton languageButton = findViewById(R.id.languageButton);
        if (languageButton != null) {
            updateLanguageButtonIcon(languageButton);
            languageButton.setOnClickListener(v -> toggleLanguage());
        }
    }

    // Metoda za ažuriranje ikone language button-a
    private void updateLanguageButtonIcon(ImageButton button) {
        String currentLanguage = LanguageHelper.getLanguage(this);
        if (currentLanguage.equals("hr")) {
            button.setImageResource(R.drawable.ic_flag_hr);
        } else {
            button.setImageResource(R.drawable.ic_flag_en);
        }
    }
    // Metoda za promjenu jezika
    private void toggleLanguage() {
        String currentLanguage = LanguageHelper.getLanguage(this);
        String newLanguage = currentLanguage.equals("hr") ? "en" : "hr";

        LanguageHelper.setLanguage(this, newLanguage);

        // Kratki toast da se vidi promjena
        String message = newLanguage.equals("hr") ? "Jezik promijenjen na hrvatski" : "Language changed to English";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Restart aktivnost da se promjena jezika primjeni
        recreate();
    }

    private void updateUI() {
        //obavijest za uspjeh
        showSnackBar(getString(R.string.login_success));

        Intent mIntent = new Intent(StartActivity.this, MainActivity.class);
        Bundle mBundle = new Bundle();

        //  postavljanje podataka o korisniku za prosljeđivanje
        mBundle.putString("idToken", cachedCredentials.getIdToken() + "");
        mBundle.putString("accessToken", cachedCredentials.getAccessToken() + "");
        mBundle.putString("refreshToken", cachedCredentials.getRefreshToken() + "");
        mBundle.putString("type", cachedCredentials.getType() + "");
        mBundle.putString("expiresAt", cachedCredentials.getExpiresAt().toString() + "");
        mBundle.putString("scope", cachedCredentials.getScope() + "");
        mIntent.putExtras(mBundle);

        // Pokretanje MainActivity-ja
        startActivity(mIntent);

        // Završavanje StartActivity-ja da se ne može vratiti back gumbom
        finish();
    }


    private void loginWithBrowser() {
        // slanje zahtjeva na auth0
        WebAuthProvider.login(account).withScheme(getString(R.string.com_auth0_scheme)).withScope("openid profile email read:current_user update:current_user_metadata").withAudience("https://" + getString(R.string.com_auth0_domain) + "/api/v2/")
                .start(this, new Callback<Credentials, AuthenticationException>() {
                    @Override
                    public void onFailure(AuthenticationException exception) {
                        showSnackBar(getString(R.string.login_failure) + exception.getCode());
                    }

                    @Override
                    public void onSuccess(Credentials credentials) {
                        cachedCredentials = credentials;
                        if (!credentials.getUser().isEmailVerified()) {
                            showSnackBar(getString(R.string.email_not_verified));
                        } else {
                            showSnackBar(getString(R.string.access_token_success) + credentials.getAccessToken());
                            updateUI();
                        }
                    }
                });
    }

    private void showSnackBar(String text) {
        // funkcija za snackbar, obavijest dolje
        Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG).show();
    }
}