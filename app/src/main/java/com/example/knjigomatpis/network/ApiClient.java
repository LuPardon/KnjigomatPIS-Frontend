package com.example.knjigomatpis.network;


import android.util.Log;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String EMULATOR_BASE_URL = "http://10.0.2.2:3000/api/"; // za emulator
//    private static final String LOCAL_NETWORK_BASE_URL = "http://192.168.5.25:3000/api/"; // FAKS moja adresa  IP-ja
    private static final String LOCAL_NETWORK_BASE_URL = "http://192.168.100.178:3000/api/"; // POSAO moja adresa IP-ja
//    private static final String LOCAL_NETWORK_BASE_URL = "http://192.168.1.23:3000/api/"; // Višnjevac
    private static Retrofit retrofit;

    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            String baseUrl;
            if (isRunningOnEmulator()) {
                baseUrl = EMULATOR_BASE_URL;
            } else {
                baseUrl = LOCAL_NETWORK_BASE_URL;
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Log.d("ApiClient", "Korišteni base URL: " + baseUrl);
        }
        return retrofit;
    }


    private static boolean isRunningOnEmulator() {
        String product = android.os.Build.PRODUCT;
        return product != null && (product.contains("sdk") || product.contains("emulator"));
    }
}
