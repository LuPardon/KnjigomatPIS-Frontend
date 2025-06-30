package com.example.knjigomatpis.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Auth0APIClient {
    private static Retrofit auth0Retrofit;

    private static final String BASE_URL = "https://dev-l82m10ihxsexnmhy.eu.auth0.com/";
    private static final String CLIENT_ID = "jCg7BEhst5LrESF1XgdUnupkTT7xIbQ2";
    private static final String CLIENT_SECRET = "{yourClientSecret}"; // Replace with actual secret
    private static final String AUDIENCE = "https://dev-l82m10ihxsexnmhy.eu.auth0.com/api/v2/";


    public static Retrofit getAuth0Retrofit() {
        if (auth0Retrofit == null) {

            auth0Retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return auth0Retrofit;
    }
}
