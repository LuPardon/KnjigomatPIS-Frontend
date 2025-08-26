package com.example.knjigomatpis.ui.notifications;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.UserNotification;
import com.example.knjigomatpis.network.ApiClient;
import com.example.knjigomatpis.network.IApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsViewModel extends ViewModel {

    private final MutableLiveData<List<UserNotification>> notifications = new MutableLiveData<>();
    private final IApiService IApiService = ApiClient.getRetrofit().create(IApiService.class);
    private Context context;
    private String currentUserId;

    public NotificationsViewModel() {
    }

    public void setUserId(String userId) {
        this.currentUserId = userId;
        fetchNotifications();
    }

    public void fetchNotifications() {
        if (currentUserId == null) {
            if (context != null) {
                Log.e("ViewModel", context.getString(R.string.user_id_not_set));
            } else {
                    Log.e("ViewModel", "User ID nije postavljen!");
                }
            return;
        }
        IApiService.getNotifications(currentUserId).enqueue(new Callback<List<UserNotification>>() {
            @Override
            public void onResponse(Call<List<UserNotification>> call, Response<List<UserNotification>> response) {
                if (response.isSuccessful()) {
                    if (context != null) {
                        Log.d("ViewModel", context.getString(R.string.notifications_fetch_success, response.body().size()));
                    } else {
                        Log.d("ViewModel", "Uspješan dohvat notifikacija: " + response.body().size());
                    }
                    notifications.setValue(response.body());
                } else {
                    if (context != null) {
                        Log.e("ViewModel", context.getString(R.string.notifications_fetch_failed, response.code()));
                    } else {
                        Log.e("ViewModel", "Neuspješan odgovor: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<UserNotification>> call, Throwable t) {
                if (context != null) {
                    Log.e("ViewModel", context.getString(R.string.api_call_error, t.getMessage()));
                } else {
                    Log.e("ViewModel", "Greška prilikom API poziva: " + t.getMessage());
                }
                t.printStackTrace();
            }
        });
    }
    public void fetchNotifications(String userId) {
        this.currentUserId = userId;
        fetchNotifications();
    }

    public void markAsRead(int notificationId) {
        IApiService.markNotificationAsRead(notificationId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (context != null) {
                        Log.d("ViewModel", context.getString(R.string.notification_marked_read));
                    } else {
                        Log.d("ViewModel", "Notifikacija označena kao pročitana");
                    }
                    fetchNotifications(); // Osvježavanje liste
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (context != null) {
                    Log.e("ViewModel", context.getString(R.string.mark_read_error, t.getMessage()));
                } else {
                    Log.e("ViewModel", "Greška označavanja kao pročitano: " + t.getMessage());
                }
            }
        });
    }

    public void markAllAsRead() {
        if (currentUserId == null) {
            if (context != null) {
                Log.e("ViewModel", context.getString(R.string.user_id_not_set));
            } else {
                Log.e("ViewModel", "User ID nije postavljen!");
            }
            return;
        }

        IApiService.markAllNotificationsAsRead(currentUserId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (context != null) {
                        Log.d("ViewModel", context.getString(R.string.all_notifications_marked_read));
                    } else {
                        Log.d("ViewModel", "Sve notifikacije označene kao pročitane");
                    }
                    fetchNotifications();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (context != null) {
                    Log.e("ViewModel", context.getString(R.string.mark_all_read_error, t.getMessage()));
                } else {
                    Log.e("ViewModel", "Greška označavanja svih kao pročitano: " + t.getMessage());
                }            }
        });
    }

    public LiveData<List<UserNotification>> getNotifications() {
        return notifications;
    }
}
