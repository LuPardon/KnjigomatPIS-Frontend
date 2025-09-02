package com.example.knjigomatpis.helpers;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;
public class LanguageHelper {
        private static final String PREF_NAME = "language_preferences";
        private static final String KEY_LANGUAGE = "selected_language";
        private static final String LANGUAGE_CROATIAN = "hr";
        private static final String LANGUAGE_ENGLISH = "en";

        public static void setLanguage(Context context, String languageCode) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();

            setLocale(context, languageCode);
        }

        public static String getLanguage(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return prefs.getString(KEY_LANGUAGE, LANGUAGE_CROATIAN);
        }

        public static void setLocale(Context context, String languageCode) {
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);

            Resources resources = context.getResources();
            Configuration config = resources.getConfiguration();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLocale(locale);
            } else {
                config.locale = locale;
            }

            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }

        public static Context updateBaseContextLocale(Context context) {
            String language = getLanguage(context);
            Locale locale = new Locale(language);
            Locale.setDefault(locale);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return updateResourcesLocale(context, locale);
            }

            return updateResourcesLocaleLegacy(context, locale);
        }

        private static Context updateResourcesLocale(Context context, Locale locale) {
            Configuration configuration = context.getResources().getConfiguration();
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        }

        private static Context updateResourcesLocaleLegacy(Context context, Locale locale) {
            Resources resources = context.getResources();
            Configuration configuration = resources.getConfiguration();
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }

        public static String getNextLanguage(Context context) {
            String currentLanguage = getLanguage(context);
            return currentLanguage.equals(LANGUAGE_CROATIAN) ? LANGUAGE_ENGLISH : LANGUAGE_CROATIAN;
        }

        public static String getLanguageDisplayName(String languageCode) {
            switch (languageCode) {
                case LANGUAGE_CROATIAN:
                    return "Hrvatski";
                case LANGUAGE_ENGLISH:
                    return "English";
                default:
                    return "Hrvatski";
            }
        }
    }

