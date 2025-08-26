package com.example.knjigomatpis;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import com.example.knjigomatpis.models.BookRequestResponse;
import com.example.knjigomatpis.models.UserNotification;
import com.example.knjigomatpis.ui.detailsBook.CreateBookFragment;
import com.example.knjigomatpis.ui.exchangeHistory.HistoryExchangeFragment;
import com.example.knjigomatpis.ui.helpers.LanguageHelper;
import com.example.knjigomatpis.ui.home.HomeFragment;
import com.example.knjigomatpis.ui.helpers.NotificationHelper;

import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.Callback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.example.knjigomatpis.databinding.ActivityMainBinding;
import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.ui.camera.CameraActivity;
import com.example.knjigomatpis.ui.notifications.NotificationsFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;

    public Auth0 account;
    public Credentials cachedCredentials;
    public UserProfile cachedUserProfile;

    private Book currentBook;
    private static final long REQUEST_COOLDOWN_HOURS = 24;
    private Map<Long, Long> bookRequestTimestamps = new HashMap<>(); // bookId -> timestamp
    private Set<Long> pendingRequests = new HashSet<>(); // bookId s pending zahtjevima


    public void setCurrentBook(Book book) {
        this.currentBook = book;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Postavljanje jezika prije kreiranja UI-ja
        LanguageHelper.setLocale(this, LanguageHelper.getLanguage(this));
        account = new Auth0(
                getString(R.string.com_auth0_client_id),
                getString(R.string.com_auth0_domain)
        );

        cachedCredentials = new Credentials(
                getIntent().getExtras().getString("idToken"),
                getIntent().getExtras().getString("accessToken"),
                getIntent().getExtras().getString("refreshToken"),
                getIntent().getExtras().getString("type"),
                new Date(getIntent().getExtras().getString("expiresAt")),
                getIntent().getExtras().getString("scope")
        );
        showUserProfile()
                .thenAccept(profile -> {
                    // Učitavanje stanja zahtjeva
                    loadRequestState();


                    binding = ActivityMainBinding.inflate(getLayoutInflater());
                    setContentView(binding.getRoot());

                    setSupportActionBar(binding.appBarMain.toolbar);

                    DrawerLayout drawer = binding.drawerLayout;
                    NavigationView navigationView = binding.navView;

                    mAppBarConfiguration = new AppBarConfiguration.Builder(
                            R.id.nav_home, R.id.nav_myBooks, R.id.nav_history, R.id.nav_profile,R.id.nav_conversations, R.id.nav_notifications)
                            .setOpenableLayout(drawer)
                            .build();

                    navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                    NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
                    NavigationUI.setupWithNavController(navigationView, navController);

                    navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                        configureFabForDestination(destination.getId());
                    });
                })
                .exceptionally(throwable -> {
                    return null;
                });


    }

    // Metoda za pravilno rukovanje promjenom jezika
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.updateBaseContextLocale(newBase));
    }
    @Nullable
    @Override
    public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);


    }

    // Metoda za čišćenje starih zahtjeva (periodički)
    private void cleanupOldRequests() {
        long currentTime = System.currentTimeMillis();
        long cooldownPeriod = REQUEST_COOLDOWN_HOURS * 60 * 60 * 1000;

        Iterator<Map.Entry<Long, Long>> iterator = bookRequestTimestamps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            if (currentTime - entry.getValue() >= cooldownPeriod) {
                iterator.remove();
            }
        }

        saveRequestState();
    }

    // Metoda za resetiranje zahtjeva (ako je potrebno)
    public void resetBookRequest(long bookId) {
        pendingRequests.remove(bookId);
        bookRequestTimestamps.remove(bookId);
        saveRequestState();
    }

    // Metoda za označavanje da je zahtjev odbačen/prihvaćen (poziv kad se dobije odgovor)
    public void markRequestResolved(long bookId) {
        pendingRequests.remove(bookId);
        saveRequestState();
    }
    private void configureFabForDestination(int destinationId) {
        if (destinationId == R.id.nav_home) {
            configureFabForHome();
        } else if (destinationId == R.id.nav_myBooks) {
            configureFabForMyBooks();
        } else if (destinationId == R.id.nav_details_book) {
            configureFabForDetails();
        } else if (destinationId == R.id.nav_profile) {
            configureFabForProfile();
        } else if (destinationId == R.id.nav_conversations) {
            configureFabForConversations();
        } else if (destinationId == R.id.nav_chat_single) {
            configureFabForSingleChat();
        } else if (destinationId == R.id.nav_history) {
            configureFabForHistory();
        } else if (destinationId == R.id.nav_notifications) {
            configureFabForNotifications();
        } else {
            hideFab();
        }
    }

    private void configureFabForHome() {
        binding.appBarMain.fab.setVisibility(View.VISIBLE);
        binding.appBarMain.fab.setImageResource(android.R.drawable.ic_search_category_default);
        binding.appBarMain.fab.setOnClickListener(view -> {
            Log.d("MainActivity", "FAB clicked for Home");

            Fragment navHostFragment  = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            if (navHostFragment != null) {
                Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
                Log.d("MainActivity", "Current fragment: " + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));

                if (currentFragment instanceof HomeFragment) {
                    Log.d("MainActivity", "Calling openSearch on HomeFragment");
                    ((HomeFragment) currentFragment).openSearch();
                } else {
                    Log.e("MainActivity", "Current fragment is not HomeFragment! It's: " +
                            (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                }
            }
        });
    }

    private void configureFabForMyBooks() {
        binding.appBarMain.fab.setVisibility(View.VISIBLE);
        binding.appBarMain.fab.setImageResource(android.R.drawable.ic_input_add);
        binding.appBarMain.fab.setOnClickListener(view -> {

            showAddBookConfirmationDialog();
        });
    }

    // Alert za potvrdu dodavanja knjige
    private void showAddBookConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_add_book_title))
                .setMessage(getString(R.string.dialog_add_book_message))
                .setPositiveButton(getString(R.string.btn_add_book), (d, which) -> {
                    navController.navigate(R.id.action_create_book);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    private void configureFabForDetails() {
        binding.appBarMain.fab.setVisibility(View.VISIBLE);
        binding.appBarMain.fab.setImageResource(android.R.drawable.ic_menu_edit);
        binding.appBarMain.fab.setOnClickListener(null);
    }

    public void setupDetailsBookFab(Book book) {
        if (book != null && cachedUserProfile != null) {
            this.currentBook = book;
            String currentUserId = cachedUserProfile.getId();

            if (book.getUserId().equals(currentUserId)) {
                setupEditFab();
            } else {
                // Provjera statusa knjige prije postavljanja chat FAB-a
                if (isBookUnavailable(book)) {
                    setupUnavailableBookFab();
                } else {
                    setupChatFab();
                }
            }
        }
    }
    // Provjera je li knjiga dostupna
    private boolean isBookUnavailable(Book book) {
        return book.getBookStatusId() == 2; // ID za "zauzeto"
    }

    // Postavljanje FAB-a za nedostupne knjige
    private void setupUnavailableBookFab() {
        binding.appBarMain.fab.setImageResource(R.drawable.ic_lock);
        binding.appBarMain.fab.setOnClickListener(view -> {
            showUnavailableBookDialog();
        });
    }

    // Prikaz dialoga za nedostupne knjige
    private void showUnavailableBookDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_book_unavailable_title))
                .setMessage(getString(R.string.dialog_book_unavailable_message, currentBook.getTitle()))
                .setPositiveButton(getString(R.string.btn_ok), null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    private void setupEditFab() {
        binding.appBarMain.fab.setImageResource(android.R.drawable.ic_menu_edit);
        binding.appBarMain.fab.setOnClickListener(view -> {
            if (currentBook != null) {

                showEditBookConfirmationDialog();
            } else {
                showSnackBar(getString(R.string.no_book_selected));
            }
        });
    }

    // Alert za potvrdu uređivanja knjige
    private void showEditBookConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_edit_book_title))
                .setMessage(getString(R.string.dialog_edit_book_message, currentBook.getTitle()))
                .setPositiveButton(getString(R.string.btn_edit), (d, which) -> {
                    Bundle args = new Bundle();
                    args.putLong("bookId", currentBook.getBookId());
                    navController.navigate(R.id.action_edit_book, args);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    public void openEditBookFragment(String bookId) {
        Bundle args = new Bundle();
        args.putString("bookId", bookId);
        navController.navigate(R.id.action_edit_book, args);
    }

    CreateBookFragment fragment = CreateBookFragment.newInstance();

    private void setupChatFab() {
        binding.appBarMain.fab.setImageResource(R.drawable.message);
        binding.appBarMain.fab.setOnClickListener(view -> {
            if (currentBook != null && cachedUserProfile != null) {
                // Provjera može li poslati zahtjev
                if (canSendBookRequest(currentBook.getBookId())) {
                    showBookRequestConfirmationDialog();
                } else {
                    // Alert kad ne može poslati zahtjev
                    showRequestLimitDialog();
                }
            } else {
                showSnackBar(getString(R.string.chat_start_failed));
            }
        });
    }

    // Metoda za provjeru može li poslati zahtjev
    private boolean canSendBookRequest(long bookId) {
        // Provjera je li zahtjev pending
        if (pendingRequests.contains(bookId)) {
            return false;
        }
        // Provjera je li prošlo dovoljno vremena od zadnjeg zahtjeva
        Long lastRequestTime = bookRequestTimestamps.get(bookId);
        if (lastRequestTime != null) {
            long currentTime = System.currentTimeMillis();
            long timeDifference = currentTime - lastRequestTime;
            long cooldownPeriod = REQUEST_COOLDOWN_HOURS * 60 * 60 * 1000; // 24h u ms

            return timeDifference >= cooldownPeriod;
        }

        return true; // Nije bilo zahtjeva, može poslati
    }

    // Metoda za prikaz poruke o ograničenju
    private void showRequestLimitDialog() {
        Long lastRequestTime = bookRequestTimestamps.get(currentBook.getBookId());
        String message;

        if (pendingRequests.contains(currentBook.getBookId())) {
            message = getString(R.string.dialog_request_pending_message);
        } else if (lastRequestTime != null) {
            long remainingTime = calculateRemainingCooldown(lastRequestTime);
            message = getString(R.string.dialog_request_cooldown_message, formatRemainingTime(remainingTime));
        } else {
            message = getString(R.string.dialog_request_limit_generic);
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_request_limit_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.btn_ok), null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    // Pomoćne metode za računanje vremena
    private long calculateRemainingCooldown(long lastRequestTime) {
        long currentTime = System.currentTimeMillis();
        long cooldownPeriod = REQUEST_COOLDOWN_HOURS * 60 * 60 * 1000;
        long elapsed = currentTime - lastRequestTime;
        return Math.max(0, cooldownPeriod - elapsed);
    }

    private String formatRemainingTime(long remainingTimeMs) {
        long hours = remainingTimeMs / (60 * 60 * 1000);
        long minutes = (remainingTimeMs % (60 * 60 * 1000)) / (60 * 1000);

        if (hours > 0) {
            return hours + "h " + minutes + "min";
        } else {
            return minutes + "min";
        }
    }

    // Alert za potvrdu slanja zahtjeva za knjigu
    private void showBookRequestConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_book_request_title))
                .setMessage(getString(R.string.dialog_book_request_message, currentBook.getTitle()))
                .setPositiveButton(getString(R.string.btn_send_request), (d, which) -> {
                    showBookRequestProgressDialog();
                    sendBookRequestNotification();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    // Progress dialog za slanje zahtjeva
    private AlertDialog progressDialog;

    private void showBookRequestProgressDialog() {
        progressDialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_sending_request_title))
                .setMessage(getString(R.string.dialog_sending_request_message))
                .setCancelable(false)
                .create();

        styleDialog(progressDialog);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void sendBookRequestNotification() {
        if (currentBook != null && cachedUserProfile != null) {
            long bookId = currentBook.getBookId();

            // Dodavanje u pending zahtjeve
            pendingRequests.add(bookId);

            // Zapis vremena zahtjeva
            bookRequestTimestamps.put(bookId, System.currentTimeMillis());

            String ownerId = currentBook.getUserId();
            String bookTitle = currentBook.getTitle();
            String requesterName = cachedUserProfile.getName();

            NotificationHelper.sendBookRequestNotificationAsync(
                    this,
                    ownerId,
                    cachedUserProfile.getId(),
                    currentBook.getBookId(),
                    bookTitle,
                    requesterName
            ).thenAccept(response -> {
                runOnUiThread(() -> {
                    dismissProgressDialog();
                    // Ukloni iz pending zahtjeva jer je uspješno poslan
                    pendingRequests.remove(bookId);
                    showBookRequestSuccessDialog(response);

                    // Spremi stanje u SharedPreferences
                    saveRequestState();
                });
            }).exceptionally(throwable -> {
                runOnUiThread(() -> {
                    dismissProgressDialog();
                    // Ukloni iz pending zahtjeva jer je failed
                    pendingRequests.remove(bookId);
                    showBookRequestErrorDialog(throwable);

                    // Ukloni timestamp jer zahtjev nije uspješno poslan
                    bookRequestTimestamps.remove(bookId);
                    saveRequestState();
                });
                return null;
            });
        }
    }
    // Metode za spremanje/učitavanje stanja u SharedPreferences
    private void saveRequestState() {
        SharedPreferences prefs = getSharedPreferences("BookRequests", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Spremi timestamps
        for (Map.Entry<Long, Long> entry : bookRequestTimestamps.entrySet()) {
            editor.putLong("timestamp_" + entry.getKey(), entry.getValue());
        }

        // Spremi pending requests (kao JSON string ili comma-separated)
        String pendingRequestsString = TextUtils.join(",", pendingRequests);
        editor.putString("pending_requests", pendingRequestsString);

        editor.apply();
    }

    private void loadRequestState() {
        SharedPreferences prefs = getSharedPreferences("BookRequests", MODE_PRIVATE);

        // Učitavanje pending requests
        String pendingRequestsString = prefs.getString("pending_requests", "");
        if (!pendingRequestsString.isEmpty()) {
            String[] pendingArray = pendingRequestsString.split(",");
            for (String bookIdStr : pendingArray) {
                try {
                    pendingRequests.add(Long.parseLong(bookIdStr.trim()));
                } catch (NumberFormatException e) {
                    // Ignorira neispravne vrijednosti
                }
            }
        }
        // Učitavanje timestamps - iteriranje kroz sve keys
        Map<String, ?> allPrefs = prefs.getAll();
        for (String key : allPrefs.keySet()) {
            if (key.startsWith("timestamp_")) {
                try {
                    long bookId = Long.parseLong(key.substring("timestamp_".length()));
                    long timestamp = prefs.getLong(key, 0);
                    bookRequestTimestamps.put(bookId, timestamp);
                } catch (NumberFormatException e) {
                    // Ignorira neispravne keys
                }
            }
        }
    }
    // Success dialog za uspješan zahtjev
    private void showBookRequestSuccessDialog(BookRequestResponse response) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_request_success_title))
                .setMessage(getString(R.string.dialog_request_success_message))
                .setPositiveButton(getString(R.string.btn_start_chat), (d, which) -> {
                    navigateToChat(response);
                })
                .setNegativeButton(getString(R.string.btn_later), null)
                .create();

        styleSuccessDialog(dialog);
        dialog.show();
    }

    // Error dialog za neuspješan zahtjev
    private void showBookRequestErrorDialog(Throwable throwable) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_request_error_title))
                .setMessage(getString(R.string.dialog_request_error_message, throwable.getMessage()))
                .setPositiveButton(getString(R.string.btn_retry), (d, which) -> {
                    showBookRequestConfirmationDialog();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleErrorDialog(dialog);
        dialog.show();
    }

    private void navigateToChat(BookRequestResponse response) {
        Bundle args = new Bundle();
        args.putLong("book_id", currentBook.getBookId());
        args.putString("owner_id", currentBook.getUserId());
        args.putString("current_user_id", cachedUserProfile.getId());
        args.putString("book_title", currentBook.getTitle());
        args.putInt("chat_id", response.getChatId());

        navController.navigate(R.id.action_details_to_chat, args);
    }

    public void navigateToChatFromNotification(UserNotification notification) {
        Bundle args = new Bundle();
        args.putLong("book_id", notification.getBook_id());
        args.putString("owner_id", notification.getRequester_id());
        args.putString("current_user_id", cachedUserProfile.getId());
        args.putString("book_title", notification.getBook_title());
        args.putInt("chat_id", notification.getChat_id());

        navController.navigate(R.id.action_notifications_to_chat, args);

    }

    private void configureFabForProfile() {
        binding.appBarMain.fab.setVisibility(View.VISIBLE);
        binding.appBarMain.fab.setImageResource(android.R.drawable.ic_menu_edit);
        binding.appBarMain.fab.setOnClickListener(view -> {
            // Alert za potvrdu uređivanja profila
            showEditProfileConfirmationDialog();
        });
    }

    // Alert za potvrdu uređivanja profila
    private void showEditProfileConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_edit_profile_title))
                .setMessage(getString(R.string.dialog_edit_profile_message))
                .setPositiveButton(getString(R.string.btn_edit_profile), (d, which) -> {
                    navController.navigate(R.id.action_profile_to_editProfile);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    private void configureFabForConversations() {
        binding.appBarMain.fab.setVisibility(View.INVISIBLE);
        binding.appBarMain.fab.setImageResource(android.R.drawable.ic_menu_edit);
        binding.appBarMain.fab.setOnClickListener(view -> {
            Snackbar.make(view, getString(R.string.edit_conversations), Snackbar.LENGTH_SHORT)
                    .setAction("Edit", null)
                    .setAnchorView(R.id.fab).show();
        });
    }

    private void configureFabForSingleChat() {
        binding.appBarMain.fab.setVisibility(View.INVISIBLE);
        binding.appBarMain.fab.setImageResource(android.R.drawable.ic_menu_share);
        binding.appBarMain.fab.setOnClickListener(view -> {
            Snackbar.make(view, getString(R.string.share_conversation), Snackbar.LENGTH_SHORT)
                    .setAction("Share", null)
                    .setAnchorView(R.id.fab).show();
        });
    }

    private void configureFabForHistory() {
        binding.appBarMain.fab.setVisibility(View.VISIBLE);
        binding.appBarMain.fab.setImageResource(android.R.drawable.ic_menu_sort_by_size);
        binding.appBarMain.fab.setOnClickListener(view -> {

            // Pozivanje metode iz fragmenta za otvaranje filtera
            Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            if (navHostFragment != null) {
                Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
                Log.d("MainActivity", "Current fragment: " + (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));

                if (currentFragment instanceof HistoryExchangeFragment) {
                    Log.d("MainActivity", "Calling openFilterDialog on HistoryExchangeFragment");
                    ((HistoryExchangeFragment) currentFragment).openFilterDialog();
                } else {
                    Log.e("MainActivity", "Current fragment is not HistoryExchangeFragment! It's: " +
                            (currentFragment != null ? currentFragment.getClass().getSimpleName() : "null"));
                }
            } else {
                Log.e("MainActivity", "NavHostFragment is null!");
            }
        });
    }

    private void configureFabForNotifications() {
        binding.appBarMain.fab.setVisibility(View.INVISIBLE);
        binding.appBarMain.fab.setImageResource(android.R.drawable.ic_popup_sync);
        binding.appBarMain.fab.setOnClickListener(view -> {
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);

            if (currentFragment instanceof NotificationsFragment) {
                showMarkAllNotificationsDialog((NotificationsFragment) currentFragment);
            }
        });
    }

    // Alert za označavanje svih notifikacija kao pročitane
    private void showMarkAllNotificationsDialog(NotificationsFragment notifFragment) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_mark_notifications_title))
                .setMessage(getString(R.string.dialog_mark_notifications_message))
                .setPositiveButton(getString(R.string.btn_mark_all), (d, which) -> {
                    notifFragment.markAllAsRead();
                    Snackbar.make(binding.getRoot(), getString(R.string.notifications_marked), Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.fab).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    private void hideFab() {
        binding.appBarMain.fab.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            // Alert za potvrdu odjave
            showLogoutConfirmationDialog();
            return true;
        }
        if (item.getItemId() == R.id.action_settings) {
            showLanguageSelectionDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Metoda za prikaz dijaloga za odabir jezika
    private void showLanguageSelectionDialog() {
        String currentLanguage = LanguageHelper.getLanguage(this);
        String[] languages = {"Hrvatski", "English"};
        String[] languageCodes = {"hr", "en"};

        int selectedIndex = currentLanguage.equals("hr") ? 0 : 1;

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_language_title))
                .setSingleChoiceItems(languages, selectedIndex, (d, which) -> {
                    String selectedLanguage = languageCodes[which];
                    if (!selectedLanguage.equals(currentLanguage)) {
                        changeLanguage(selectedLanguage);
                    }
                    d.dismiss();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    // Metoda za promjenu jezika i restart aktivnosti
    private void changeLanguage(String languageCode) {
        LanguageHelper.setLanguage(this, languageCode);

        // Kratak toast da je jezik promijenjen
        showSnackBar(getString(R.string.language_changed));

        // Restart aktivnost da se promjena jezika primjeni
        recreate();
    }
    // Alert za potvrdu odjave
    private void showLogoutConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_logout_title))
                .setMessage(getString(R.string.dialog_logout_message))
                .setPositiveButton(getString(R.string.btn_logout), (d, which) -> {
                    logout();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void logout() {
        WebAuthProvider.logout(account)
                .withScheme(getString(R.string.com_auth0_scheme))
                .start(this, new Callback<Void, AuthenticationException>() {
                    @Override
                    public void onSuccess(Void payload) {
                        cachedCredentials = null;
                        cachedUserProfile = null;

                        Intent mIntent = new Intent(MainActivity.this, StartActivity.class);
                        startActivity(mIntent);
                        finish();
                    }

                    @Override
                    public void onFailure(AuthenticationException exception) {
                        showLogoutErrorDialog(exception);
                    }
                });
    }

    // Error dialog za neuspješnu odjavu
    private void showLogoutErrorDialog(AuthenticationException exception) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle(getString(R.string.dialog_logout_error_title))
                .setMessage(getString(R.string.dialog_logout_error_message, exception.getMessage()))
                .setPositiveButton(getString(R.string.btn_retry), (d, which) -> {
                    showLogoutConfirmationDialog();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        styleErrorDialog(dialog);
        dialog.show();
    }

    private CompletableFuture<UserProfile> showUserProfile() {
        AuthenticationAPIClient client = new AuthenticationAPIClient(account);
        CompletableFuture<UserProfile> future = new CompletableFuture<>();

        client.userInfo(cachedCredentials.getAccessToken()).start(new Callback<UserProfile, AuthenticationException>() {
            @Override
            public void onFailure(AuthenticationException exception) {
                showSnackBar(getString(R.string.generic_failure) + exception.getCode());
                future.completeExceptionally(exception);
            }

            @Override
            public void onSuccess(UserProfile profile) {
                cachedUserProfile = profile;
                future.complete(profile);
            }
        });

        return future;
    }


    public void showSnackBar(String text) {
        Snackbar.make(
                binding.getRoot(),
                text,
                Snackbar.LENGTH_LONG
        ).show();
    }

    // Stilovi za dialoge
    private void styleDialog(AlertDialog dialog) {
        // Osnovni stil za neutralne dialoge
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }

    private void styleSuccessDialog(AlertDialog dialog) {
        // Stil za uspješne dialoge (zelenkasti ton)
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_success_background);
    }

    private void styleErrorDialog(AlertDialog dialog) {
        // Stil za error dialoge (crvenkasti ton)
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_error_background);
    }
}





