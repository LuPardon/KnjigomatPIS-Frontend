package com.example.knjigomatpis.network;

import com.auth0.android.result.UserProfile;
import com.example.knjigomatpis.models.Auth0TokenResponse;
import com.example.knjigomatpis.models.Auth0User;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface IAuth0ApiService {
    @GET("/api/v2/users/{userId}")
    Call<Auth0User> getUserById(
            @Path("userId") String userId,
            @Header("Authorization") String authToken
    );

    @POST("/oauth/token")
    @FormUrlEncoded
    Call<Auth0TokenResponse> getToken(
            @Header("Accept") String acceptHeader,
            @Field("grant_type") String grantType,
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("audience") String audience
    );
}
