package com.example.knjigomatpis.ui.helpers;

import android.content.Context;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.Auth0TokenResponse;
import com.example.knjigomatpis.models.Auth0User;
import com.example.knjigomatpis.network.Auth0APIClient;
import com.example.knjigomatpis.network.IAuth0ApiService;

import java.util.concurrent.CompletableFuture;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Auth0Helper {
    private static final IAuth0ApiService apiService = Auth0APIClient.getAuth0Retrofit().create(IAuth0ApiService.class);

    private static final String CLIENT_ID = "jCg7BEhst5LrESF1XgdUnupkTT7xIbQ2";
    private static final String CLIENT_SECRET = "dFHqnIp7-oBBzMIsh2FAxG_ikCafjJPn3A7Js6YZNGvDy1uHgi4mVs48GMSJViFA";
    private static final String AUDIENCE = "https://dev-l82m10ihxsexnmhy.eu.auth0.com/api/v2/";

    public static CompletableFuture<Auth0User> getUserByIdAsync(String userId, Context context) {
        CompletableFuture<Auth0User> future = new CompletableFuture<>();

        getAccessTokenAsString(context).thenAccept(token -> {
            String authHeader = "Bearer " + token;
            Call<Auth0User> call = apiService.getUserById(userId, authHeader);

            call.enqueue(new Callback<Auth0User>() {
                @Override
                public void onResponse(Call<Auth0User> call, Response<Auth0User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        future.complete(response.body());
                        /* primjer šta vraća
                        * {
  "created_at": "2025-05-07T07:17:21.771Z",
  "email": "ana.horvat@example.com",
  "email_verified": true,
  "identities": [
    {
      "user_id": "681b0901af7b2aa6b0e4db58",
      "provider": "auth0",
      "connection": "Username-Password-Authentication",
      "isSocial": false
    }
  ],
  "name": "Ana Horvat",
  "nickname": "ana.horvat",
  "picture": "https://s.gravatar.com/avatar/48b96004cf98ca88dad7d625f41f0083?s=480&r=pg&d=https%3A%2F%2Fcdn.auth0.com%2Favatars%2Fan.png",
  "updated_at": "2025-06-18T23:39:55.365Z",
  "user_id": "auth0|681b0901af7b2aa6b0e4db58",
  "user_metadata": {
    "firstName": "Ana",
    "lastName": "Horvat",
    "location": "Virovitica4"
  },
  "last_ip": "193.198.57.189",
  "last_login": "2025-06-18T23:39:55.364Z",
  "logins_count": 18
}
                        * */
                    } else {
                        String errorMsg = context.getString(R.string.error_failed_get_user) + response.code() + " - " + response.message();
                        // Log the error body for debugging
                        if (response.errorBody() != null) {
                            try {
                                errorMsg += " - " + response.errorBody().string();
                            } catch (Exception e) {
                                errorMsg += " - " + context.getString(R.string.error_could_not_read_error_body);
                            }
                        }
                        future.completeExceptionally(new RuntimeException(errorMsg));
                    }
                }

                @Override
                public void onFailure(Call<Auth0User> call, Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }).exceptionally(throwable -> {
            future.completeExceptionally(new RuntimeException(context.getString(R.string.error_failed_get_access_token) + throwable.getMessage(), throwable));
            return null;
        });

        return future;
    }

    public static CompletableFuture<Auth0TokenResponse> getAccessToken(Context context) {
        CompletableFuture<Auth0TokenResponse> future = new CompletableFuture<>();

        // Remove the Content-Type parameter from the call
        Call<Auth0TokenResponse> call = apiService.getToken(
                "application/json",
                "client_credentials",
                CLIENT_ID,
                CLIENT_SECRET,
                AUDIENCE
        );

        call.enqueue(new Callback<Auth0TokenResponse>() {
            @Override
            public void onResponse(Call<Auth0TokenResponse> call, Response<Auth0TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    future.complete(response.body());
                } else {
                    String errorMsg = context.getString(R.string.error_token_request_failed) + response.code() + " - " + response.message();
                    // Log the error body for debugging
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg += " - " + context.getString(R.string.error_could_not_read_error_body);
                        }
                    }
                    future.completeExceptionally(new RuntimeException(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<Auth0TokenResponse> call, Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private static CompletableFuture<String> getAccessTokenAsString(Context context) {
        return getAccessToken(context)
                .thenApply(Auth0TokenResponse::getAccess_token);
    }
}
