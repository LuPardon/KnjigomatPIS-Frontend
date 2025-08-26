package com.example.knjigomatpis.ui.detailsBook;

import android.content.Context;

import com.example.knjigomatpis.R;

public class BookUtils {

    // Postojeće metode za dobivanje teksta iz ID-ja
    public static String getConditionText(Context context, Long id) {
        if (id == null) return context.getString(R.string.unknown);
        switch (id.intValue()) {
            case 1: return context.getString(R.string.condition_new);
            case 2: return context.getString(R.string.condition_good);
            case 3: return context.getString(R.string.condition_used);
            case 4: return context.getString(R.string.condition_damaged);
            default: return context.getString(R.string.unknown);
        }
    }

    public static String getVisibilityText(Context context, Long id) {
        if (id == null) return context.getString(R.string.unknown);
        switch (id.intValue()) {
            case 1: return context.getString(R.string.visibility_private);
            case 2: return context.getString(R.string.visibility_public);
            default: return context.getString(R.string.unknown);
        }
    }

    public static String getStatusText(Context context, Long id) {
        if (id == null) return context.getString(R.string.unknown);
        switch (id.intValue()) {
            case 1: return context.getString(R.string.status_available);
            case 2: return context.getString(R.string.status_borrowed);
            default: return context.getString(R.string.unknown);
        }
    }

    // Metoda za žanrove - dobivanje teksta iz ID-ja
    public static String getGenreText(Context context, Long id) {
        if (id == null) return context.getString(R.string.unknown);
        switch (id.intValue()) {
            case 1: return context.getString(R.string.genre_roman);
            case 2: return context.getString(R.string.genre_thriller);
            case 3: return context.getString(R.string.genre_horror);
            case 4: return context.getString(R.string.genre_fantasy);
            case 5: return context.getString(R.string.genre_romance);
            case 6: return context.getString(R.string.genre_historical);
            case 7: return context.getString(R.string.genre_psychology);
            case 8: return context.getString(R.string.genre_publicistic);
            case 9: return context.getString(R.string.genre_children);
            default: return context.getString(R.string.unknown);
        }
    }

    // Metode za dobivanje ID-ja iz teksta (potrebne za spinner-e)
    public static Long getConditionIdFromText(Context context, String text) {
        if (text == null) return 1L; // default

        // Provjera za lokalizirane stringove
        if (text.equals(context.getString(R.string.condition_new))) return 1L;
        if (text.equals(context.getString(R.string.condition_good))) return 2L;
        if (text.equals(context.getString(R.string.condition_used))) return 3L;
        if (text.equals(context.getString(R.string.condition_damaged))) return 4L;

        // Fallback za engleski tekst (sigurnost)
        switch (text) {
            case "New": return 1L;
            case "Good": return 2L;
            case "Used": return 3L;
            case "Damaged": return 4L;
            default: return 1L;
        }
    }

    public static Long getVisibilityIdFromText(Context context, String text) {
        if (text == null) return 1L; // default

        if (text.equals(context.getString(R.string.visibility_private))) return 1L;
        if (text.equals(context.getString(R.string.visibility_public))) return 2L;

       switch (text) {
            case "Private": return 1L;
            case "Public": return 2L;
            default: return 1L;
        }
    }

    public static Long getStatusIdFromText(Context context, String text) {
        if (text == null) return 1L; // default

        if (text.equals(context.getString(R.string.status_available))) return 1L;
        if (text.equals(context.getString(R.string.status_borrowed))) return 2L;

        switch (text) {
            case "Available": return 1L;
            case "Borrowed": return 2L;
            default: return 1L;
        }
    }

    // Metoda za žanrove - dobivanje ID-ja iz teksta
    public static Long getGenreIdFromText(Context context, String text) {
        if (text == null) return 1L; // default

        if (text.equals(context.getString(R.string.genre_roman))) return 1L;
        if (text.equals(context.getString(R.string.genre_thriller))) return 2L;
        if (text.equals(context.getString(R.string.genre_horror))) return 3L;
        if (text.equals(context.getString(R.string.genre_fantasy))) return 4L;
        if (text.equals(context.getString(R.string.genre_romance))) return 5L;
        if (text.equals(context.getString(R.string.genre_historical))) return 6L;
        if (text.equals(context.getString(R.string.genre_psychology))) return 7L;
        if (text.equals(context.getString(R.string.genre_publicistic))) return 8L;
        if (text.equals(context.getString(R.string.genre_children))) return 9L;

        switch (text) {
            case "Roman / Beletristika": return 1L;
            case "Triler / Krimić": return 2L;
            case "Horor": return 3L;
            case "Fantastika / SF": return 4L;
            case "Ljubavni / Romantika": return 5L;
            case "Povijesni / Biografije": return 6L;
            case "Psihologija / Samopomoć": return 7L;
            case "Publicistika / Eseji": return 8L;
            case "Dječje / Mladež": return 9L;
            default: return 1L;
        }
    }

    // Helper metode za dobivanje svih opcija (potrebne za spinner-e)
    public static String[] getConditionOptions(Context context) {
        return new String[] {
                context.getString(R.string.condition_new),
                context.getString(R.string.condition_good),
                context.getString(R.string.condition_used),
                context.getString(R.string.condition_damaged)
        };
    }

    public static String[] getVisibilityOptions(Context context) {
        return new String[] {
                context.getString(R.string.visibility_private),
                context.getString(R.string.visibility_public)
        };
    }

    public static String[] getStatusOptions(Context context) {
        return new String[] {
                context.getString(R.string.status_available),
                context.getString(R.string.status_borrowed)
        };
    }

    // Metoda za žanrove - sve opcije
    public static String[] getGenreOptions(Context context) {
        return new String[] {
                context.getString(R.string.genre_roman),
                context.getString(R.string.genre_thriller),
                context.getString(R.string.genre_horror),
                context.getString(R.string.genre_fantasy),
                context.getString(R.string.genre_romance),
                context.getString(R.string.genre_historical),
                context.getString(R.string.genre_psychology),
                context.getString(R.string.genre_publicistic),
                context.getString(R.string.genre_children)
        };
    }
}
