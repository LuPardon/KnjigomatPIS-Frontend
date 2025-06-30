package com.example.knjigomatpis.ui.helpers;

import android.util.Log;
import android.content.Context;
import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.BookRequest;
import com.example.knjigomatpis.models.BookRequestResponse;
import com.example.knjigomatpis.network.ApiClient;
import com.example.knjigomatpis.network.IApiService;

import java.util.concurrent.CompletableFuture;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationHelper {
    private Context context;
    public NotificationHelper(Context context) {
        this.context = context;
    }

    private static final IApiService API_SERVICE = ApiClient.getRetrofit().create(IApiService.class);

    public static CompletableFuture<BookRequestResponse> sendBookRequestNotificationAsync(
            Context context, String ownerId, String requesterId, long bookId, String bookTitle, String requesterName) {

        CompletableFuture<BookRequestResponse> future = new CompletableFuture<>();

        String title = context.getString(R.string.new_book_request_title);
        String message = (requesterName + " " +  context.getString(R.string.book_request_message) + " " +  bookTitle);

        BookRequest request = new BookRequest(ownerId, requesterId, bookId, title, message);

        API_SERVICE.sendBookRequest(request).enqueue(new Callback<BookRequestResponse>() {
            @Override
            public void onResponse(Call<BookRequestResponse> call, Response<BookRequestResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("NotificationHelper", context.getString(R.string.notification_sent_success));
                    future.complete(response.body());
                } else {
                    String errorMsg = context.getString(R.string.notification_send_error) + response.code();
                    Log.e("NotificationHelper", errorMsg);
                    future.completeExceptionally(new RuntimeException(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<BookRequestResponse> call, Throwable t) {
                Log.e("NotificationHelper", context.getString(R.string.network_error) + t.getMessage());
                future.completeExceptionally(t);
            }
        });

        return future;
    }
}