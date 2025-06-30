package com.example.knjigomatpis.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.Book;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private OnBookClickListener onBookClickListener;
    private List<Book> originalBookList;
    private List<Book> bookList;
    private Context context;
    private String currentUserId;
    private boolean hideMyBooks = false;




    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    public void setOnBookClickListener(OnBookClickListener listener) {
        this.onBookClickListener = listener;
    }


    public BookAdapter(Context context) {
        this.context = context;
        this.bookList = new ArrayList<>();
        this.originalBookList = new ArrayList<>();
    }

    public void setUserId(String userId) {
        this.currentUserId = userId;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_book_card, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = bookList.get(position);

        holder.title.setText( book.getTitle());
        holder.author.setText(book.getAuthor());

        // Provjeri da li je knjiga privatna i da li je trenutni korisnik vlasnik
        boolean isPrivate = book.getVisibilityId() != null && book.getVisibilityId() == 1L;
        boolean isOwner = currentUserId != null && currentUserId.equals(book.getUserId());

        if (isOwner && isPrivate) {
            // MOJA PRIVATNA KNJIGA - prikaži samo "Privatna", sakrij status
            holder.visibility.setVisibility(View.VISIBLE);
            holder.visibility.setBackgroundResource(R.drawable.status_unavailable);
            holder.visibility.setText(getVisibilityText(book.getVisibilityId()));

            // Bijeli tekst za tamnu pozadinu
            holder.visibility.setTextColor(ContextCompat.getColor(context, R.color.background_light));
            holder.status.setVisibility(View.GONE); // Sakrij status

        } else if (!isPrivate) {
            // JAVNA KNJIGA - prikaži samo status, sakrij vidljivost
            holder.status.setVisibility(View.VISIBLE);
            if (book.getBookStatusId() != null && book.getBookStatusId() == 1L) {
                holder.status.setBackgroundResource(R.drawable.status_available);
                holder.status.setText(getStatusText(book.getBookStatusId()));
            } else {
                holder.status.setBackgroundResource(R.drawable.status_unavailable);
                holder.status.setText(getStatusText(book.getBookStatusId()));

                // Postaviti bijeli tekst za tamnu pozadinu
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.black_overlay));
            }
            holder.visibility.setVisibility(View.GONE); // Sakrij vidljivost

        } else {
            // TUĐA PRIVATNA KNJIGA (ovo se teoretski neće dogoditi jer su filtrirane)
            // Ali za sigurnost, sakrij oba
            holder.status.setVisibility(View.GONE);
            holder.visibility.setVisibility(View.GONE);
        }


        String baseUrl = context.getString(R.string.base_url);
        if (book.getImagePaths() != null && !book.getImagePaths().isEmpty()) {
            String imagePath = book.getImagePaths().get(0); // prva slika
            String fullImageUrl = baseUrl + imagePath;
            Picasso.get()
                    .load(fullImageUrl)
                    .placeholder(R.drawable.placeholder_image) // prikazuje placeholder dok se slika učitava
                    .into(holder.bookImage);
        } else {
            holder.bookImage.setImageResource(R.drawable.placeholder_image);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onBookClickListener != null) {
                onBookClickListener.onBookClick(book);
            }
        });

    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView bookImage;
        TextView title, author, status, visibility;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);

            bookImage = itemView.findViewById(R.id.bookImage);
            title = itemView.findViewById(R.id.textTitle);
            author = itemView.findViewById(R.id.textAuthor);
            status = itemView.findViewById(R.id.textStatus);
            visibility = itemView.findViewById(R.id.textVisibility);
        }
    }
    private String getVisibilityText(Long id) {
        if (id == null) return context.getString(R.string.unknown);
        switch (id.intValue()) {
            case 1: return context.getString(R.string.visibility_private);
            case 2: return context.getString(R.string.visibility_public);
            default: return context.getString(R.string.unknown);
        }
    }

    private String getStatusText(Long id) {
        if (id == null) return context.getString(R.string.unknown);
        switch (id.intValue()) {
            case 1: return context.getString(R.string.status_available);
            case 2: return context.getString(R.string.status_borrowed);
            default: return context.getString(R.string.unknown);
        }
    }

    public void setBooksList(List<Book> books) {
        this.originalBookList = new ArrayList<>(books); // spremi original

        // Filtrira privatne knjige koje korisnik ne posjeduje
        List<Book> visibleBooks = new ArrayList<>();
        for (Book book : books) {
            boolean isPrivate = book.getVisibilityId() != null && book.getVisibilityId() == 1L;
            boolean isOwner = currentUserId != null && currentUserId.equals(book.getUserId());

            // LOGIKA ZA MOJE KNJIGE - ako je hideMyBooks = true, preskoči moje knjige
            if (hideMyBooks && isOwner) {
                continue;
            }
            // Prikaži knjige koje su javne ili korisnik ih posjeduje
            if (!isPrivate || isOwner) {
                visibleBooks.add(book);
            }
        }
        this.bookList = visibleBooks;
        notifyDataSetChanged();
    }

    // Inicijalno postavljanje bez filtera
    public void setBooksListInitial(List<Book> books) {
        this.originalBookList = new ArrayList<>(books);

        List<Book> visibleBooks = new ArrayList<>();
        for (Book book : books) {
            boolean isPrivate = book.getVisibilityId() != null && book.getVisibilityId() == 1L;
            boolean isOwner = currentUserId != null && currentUserId.equals(book.getUserId());

            if (hideMyBooks && isOwner) {
                continue;
            }

            if (!isPrivate || isOwner) {
                visibleBooks.add(book);
            }
        }
        this.bookList = visibleBooks;
        notifyDataSetChanged();
    }


    public void updateBooks(List<Book> books) {
        this.originalBookList = new ArrayList<>(books);

        // Filtrira privatne knjige koje korisnik ne posjeduje
        List<Book> visibleBooks = new ArrayList<>();
        for (Book book : books) {
            boolean isPrivate = book.getVisibilityId() != null && book.getVisibilityId() == 1L;
            boolean isOwner = currentUserId != null && currentUserId.equals(book.getUserId());

            if (hideMyBooks && isOwner) {
                continue;
            }

            if (!isPrivate || isOwner) {
                visibleBooks.add(book);
            }
        }

        // Postavi filtrirane knjige, ne sve knjige!
        this.bookList = visibleBooks;
        notifyDataSetChanged();
    }
public void filterBooks(String query, List<String> filters, List<String> selectedYearRanges, List<String> selectedPageRanges) {
    List<Book> filteredList = new ArrayList<>();

    // Grupiranje filtera po kategoriji
    Map<String, List<String>> filtersByCategory = new HashMap<>();
    for (String filter : filters) {
        if (filter == null) continue;
        String[] parts = filter.split(":", 2);
        if (parts.length < 2) continue;
        String category = parts[0];
        String value = parts[1];
        filtersByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(value);
    }

    for (Book book : originalBookList) {
        // PRVA PROVJERA - privatnost knjige
        boolean isPrivate = book.getVisibilityId() != null && book.getVisibilityId() == 1L;
        boolean isOwner = currentUserId != null && currentUserId.equals(book.getUserId());

        if (hideMyBooks && isOwner) {
            continue; // Preskoči moje knjige
        }

        // Ako je knjiga privatna i korisnik nije vlasnik, preskoči je
        if (isPrivate && !isOwner) {
            continue;
        }

        boolean matchesSearch = query == null || query.isEmpty() ||
                book.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                book.getAuthor().toLowerCase().contains(query.toLowerCase());

        if (!matchesSearch) {
            continue;
        }

        boolean matchesFilters = true;

        // PROVJERI OBIČNE FILTERE
        for (Map.Entry<String, List<String>> entry : filtersByCategory.entrySet()) {
            String category = entry.getKey();
            List<String> values = entry.getValue();

            boolean matchesCategory = false;

            switch (category) {
                case "genre":
                    if (book.getGenreId() != null) {
                        matchesCategory = values.stream()
                                .anyMatch(v -> {
                                    try {
                                        return book.getGenreId().equals(Long.parseLong(v));
                                    } catch (NumberFormatException e) {
                                        return false;
                                    }
                                });
                    }
                    break;

                case "publisher":
                    if (book.getPublisher() != null) {
                        matchesCategory = values.stream()
                                .anyMatch(v -> book.getPublisher().equalsIgnoreCase(v));
                    }
                    break;

                case "condition":
                    if (book.getBookConditionId() != null) {
                        matchesCategory = values.stream()
                                .anyMatch(v -> {
                                    try {
                                        return book.getBookConditionId().equals(Long.parseLong(v));
                                    } catch (NumberFormatException e) {
                                        return false;
                                    }
                                });
                    }
                    break;

                case "language":
                    if (book.getBookLanguage() != null) {
                        matchesCategory = values.stream()
                                .anyMatch(v -> book.getBookLanguage().equalsIgnoreCase(v));
                    }
                    break;

                default:
                    matchesCategory = true; // Nepoznata kategorija - zanemari filter
                    break;
            }

            if (!matchesCategory) {
                matchesFilters = false;
                break;
            }
        }

        // PROVJERA RANGE FILTERE NEZAVISNO
        if (matchesFilters && !selectedYearRanges.isEmpty()) {
            if (book.getPublicationYear() != null) {
                matchesFilters = isInSelectedYearRange(book.getPublicationYear(), selectedYearRanges);
            } else {
                matchesFilters = false; // Nema godina, a traže se godine
            }
        }

        if (matchesFilters && !selectedPageRanges.isEmpty()) {
            if (book.getPageCount() != null) {
                matchesFilters = isInSelectedPageRange(book.getPageCount(), selectedPageRanges);
            } else {
                matchesFilters = false; // Nema stranice, a traže se stranice
            }
        }

        if (matchesFilters) {
            filteredList.add(book);
        }
    }
    this.bookList = filteredList;
    notifyDataSetChanged();
}
    public void resetBooks() {
        // Ne može samo kopirati originalBookList, nego mora filtrirati
        List<Book> visibleBooks = new ArrayList<>();
        for (Book book : originalBookList) {
            boolean isPrivate = book.getVisibilityId() != null && book.getVisibilityId() == 1L;
            boolean isOwner = currentUserId != null && currentUserId.equals(book.getUserId());

            if (hideMyBooks && isOwner) {
                continue;
            }

            if (!isPrivate || isOwner) {
                visibleBooks.add(book);
            }
        }

        this.bookList = visibleBooks;
        notifyDataSetChanged();
    }

    // Metode za provjeru raspona godina i stranica
    private boolean isInSelectedYearRange(Long year, List<String> selectedRanges) {
        for (String range : selectedRanges) {
            switch (range) {
                case "< 1850":
                    if (year < 1850) return true;
                    break;
                case "1850–1880":
                    if (year >= 1850 && year <= 1880) return true;
                    break;
                case "1881–1920":
                    if (year >= 1881 && year <= 1920) return true;
                    break;
                case "1921–1950":
                    if (year >= 1921 && year <= 1950) return true;
                    break;
                case "1951–1970":
                    if (year >= 1951 && year <= 1970) return true;
                    break;
                case "1971–1990":
                    if (year >= 1971 && year <= 1990) return true;
                    break;
                case "1991–2000":
                    if (year >= 1991 && year <= 2000) return true;
                    break;
                case "2001–2010":
                    if (year >= 2001 && year <= 2010) return true;
                    break;
                case "2011–2021":
                    if (year >= 2011 && year <= 2021) return true;
                    break;
                case "> 2021":
                    if (year > 2021) return true;
                    break;
            }
        }
        return false;
    }

    private boolean isInSelectedPageRange(Long pages, List<String> selectedRanges) {
        for (String range : selectedRanges) {
            switch (range) {
                case "<100":
                    if (pages < 100) return true;
                    break;
                case "100–200":
                    if (pages >= 100 && pages <= 200) return true;
                    break;
                case "201–300":
                    if (pages >= 201 && pages <= 300) return true;
                    break;
                case "301–400":
                    if (pages >= 301 && pages <= 400) return true;
                    break;
                case "401–500":
                    if (pages >= 401 && pages <= 500) return true;
                    break;
                case "500+":
                    if (pages > 500) return true;
                    break;
            }
        }
        return false;
    }
}
