package com.example.knjigomatpis.helpers;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.ui.detailsBook.BookUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
public class BookFiltersHelper {

        public interface FilterCallback {
            void onFiltersApplied(List<String> activeFilters, List<String> selectedYearRanges, List<String> selectedPageRanges);
            void onFiltersReset();
        }

        // Statička metoda za dohvaćanje dostupnih jezika (normaliziranih)
        public static Set<String> getAvailableLanguages(List<Book> books) {
            if (books == null) return new HashSet<>();

            // Koristi TreeSet s case-insensitive usporedbom kako bi grupiralo iste jezike
            Set<String> normalizedLanguages = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            for (Book book : books) {
                if (book.getBookLanguage() != null && !book.getBookLanguage().trim().isEmpty()) {
                    // Normaliziranje jezika - prvi znak velikim, ostali malim slovima
                    String normalizedLanguage = normalizeLanguage(book.getBookLanguage());
                    normalizedLanguages.add(normalizedLanguage);
                }
            }

            return normalizedLanguages;
        }

        // Ostale helper metode za dohvaćanje dostupnih filtera
        public static Set<Long> getAvailableGenres(List<Book> books) {
            Set<Long> genres = new HashSet<>();
            if (books == null) return genres;
            for (Book book : books) {
                if (book.getGenreId() != null) {
                    genres.add(book.getGenreId());
                }
            }
            return genres;
        }

        public static Set<String> getAvailablePublishers(List<Book> books) {
            Set<String> publishers = new HashSet<>();
            if (books == null) return publishers;
            for (Book book : books) {
                if (book.getPublisher() != null && !book.getPublisher().trim().isEmpty()) {
                    publishers.add(book.getPublisher());
                }
            }
            return publishers;
        }

        public static Set<Long> getAvailableConditions(List<Book> books) {
            Set<Long> conditions = new HashSet<>();
            if (books == null) return conditions;
            for (Book book : books) {
                if (book.getBookConditionId() != null) {
                    conditions.add(book.getBookConditionId());
                }
            }
            return conditions;
        }

        // Predefinirani range-ovi
        public static String[] getYearRanges() {
            return new String[]{"< 1850", "1850–1880", "1881–1920", "1921–1950", "1951–1970",
                    "1971–1990", "1991–2000", "2001–2010", "2011–2021", "> 2021"};
        }

        public static String[] getPageRanges() {
            return new String[]{"<100", "100–200", "201–300", "301–400", "401–500", "500+"};
        }

        // Helper metoda za normalizaciju jezika
        private static String normalizeLanguage(String language) {
            if (language == null || language.trim().isEmpty()) {
                return language;
            }

            String trimmed = language.trim();
            // Prvi znak velikom, ostali mali
            return trimmed.substring(0, 1).toUpperCase() +
                    (trimmed.length() > 1 ? trimmed.substring(1).toLowerCase() : "");
        }

        // Metoda za dodavanje checkboxova u container
        public static void addCheckBoxes(Context context, LinearLayout container, String category,
                                         Set<?> values, List<String> activeFilters) {
            for (Object value : values) {
                CheckBox checkBox = new CheckBox(context);
                String displayText = value.toString();
                String tagValue = value.toString();

                if (category.equals("condition")) {
                    Long conditionId = Long.parseLong(value.toString());
                    displayText = BookUtils.getConditionText(context, conditionId);
                    tagValue = String.valueOf(conditionId);
                }
                //  Podrška za žanrove
                else if (category.equals("genre")) {
                    Long genreId = Long.parseLong(value.toString());
                    displayText = BookUtils.getGenreText(context, genreId);
                    tagValue = String.valueOf(genreId);
                }

                // Normalizirani tag za konzistentno filtriranje
                String tagString = category.toLowerCase() + ":" + tagValue.toLowerCase();

                checkBox.setText(displayText);
                checkBox.setTag(tagString);

                // Provjera i označavanje checkbox-a ako je u activeFilters
                if (activeFilters.contains(tagString)) {
                    checkBox.setChecked(true);
                }
                container.addView(checkBox);
            }
        }

        // Metoda za dodavanje range checkbox-ova
        public static void addRangeCheckBoxes(Context context, LinearLayout container,
                                              String[] ranges, String rangeType,
                                              List<String> selectedRanges) {
            for (String range : ranges) {
                CheckBox cb = new CheckBox(context);
                cb.setText(range);
                cb.setTag(rangeType + "_range:" + range);

                // Provjera da li je ovaj range već odabran i označavanje checkbox-a
                if (selectedRanges.contains(range)) {
                    cb.setChecked(true);
                }
                container.addView(cb);
            }
        }

        // Metoda za kreiranje display teksta aktivnih filtara
        public static String createActiveFiltersDisplayText(Context context,
                                                            List<String> activeFilters,
                                                            List<String> selectedYearRanges,
                                                            List<String> selectedPageRanges) {
            // Provjera da li ima bilo kakvih filtera
            boolean hasFilters = (activeFilters != null && !activeFilters.isEmpty()) ||
                    (selectedYearRanges != null && !selectedYearRanges.isEmpty()) ||
                    (selectedPageRanges != null && !selectedPageRanges.isEmpty());

            if (!hasFilters) {
                return null; // ili prazan string
            }

            StringBuilder sb = new StringBuilder(context.getString(R.string.active_filters));
            boolean hasContent = false;

            // Obični filteri
            if (activeFilters != null && !activeFilters.isEmpty()) {
                for (int i = 0; i < activeFilters.size(); i++) {
                    String filter = activeFilters.get(i);
                    String[] parts = filter.split(":");
                    if (parts.length == 2) {
                        String category = parts[0];
                        String value = parts[1];

                        category = capitalizeFirstLetter(category);

                        if (category.equalsIgnoreCase("condition")) {
                            try {
                                Long conditionId = Long.parseLong(value);
                                value = BookUtils.getConditionText(context, conditionId);
                            } catch (NumberFormatException e) {
                                value = capitalizeFirstLetter(value);
                            }
                        } else if (category.equalsIgnoreCase("genre")) {
                            try {
                                Long genreId = Long.parseLong(value);
                                value = BookUtils.getGenreText(context, genreId);
                            } catch (NumberFormatException e) {
                                value = capitalizeFirstLetter(value);
                            }
                        } else {
                            value = capitalizeFirstLetter(value);
                        }

                        sb.append(category).append(": ").append(value);
                    } else {
                        sb.append(filter);
                    }

                    if (i < activeFilters.size() - 1) {
                        sb.append(", ");
                    }
                    hasContent = true;
                }
            }

            // Range filteri za godine
            if (selectedYearRanges != null && !selectedYearRanges.isEmpty()) {
                if (hasContent) {
                    sb.append(", ");
                }
                sb.append(context.getString(R.string.year_filter));
                for (int i = 0; i < selectedYearRanges.size(); i++) {
                    sb.append(selectedYearRanges.get(i));
                    if (i < selectedYearRanges.size() - 1) {
                        sb.append(" or ");
                    }
                }
                hasContent = true;
            }

            // Range filteri za stranice
            if (selectedPageRanges != null && !selectedPageRanges.isEmpty()) {
                if (hasContent) {
                    sb.append(", ");
                }
                sb.append(context.getString(R.string.pages_filter));
                for (int i = 0; i < selectedPageRanges.size(); i++) {
                    sb.append(selectedPageRanges.get(i));
                    if (i < selectedPageRanges.size() - 1) {
                        sb.append(context.getString(R.string.or));                    }
                }
            }

            return sb.toString();
        }

        private static String capitalizeFirstLetter(String s) {
            if (s == null || s.isEmpty()) return s;
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

