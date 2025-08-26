package com.example.knjigomatpis.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.auth0.android.result.UserProfile;
import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.Auth0User;
import com.example.knjigomatpis.models.UserExchangeHistoryResponse;
import com.example.knjigomatpis.ui.helpers.Auth0Helper;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExchangeHistoryAdapter extends RecyclerView.Adapter<ExchangeHistoryAdapter.ExchangeHistoryViewHolder> {

    private List<UserExchangeHistoryResponse> items;
    private final OnHistoryItemClickListener clickListener;
    private final UserProfile currentUserProfile;
    private final Map<String, String> userNameCache;
    private static final String TAG = "ExchangeHistoryAdapter";

    public interface OnHistoryItemClickListener {
        void OnHistoryItemClick(UserExchangeHistoryResponse historyItem);
    }

    public ExchangeHistoryAdapter(List<UserExchangeHistoryResponse> items,
                                  OnHistoryItemClickListener clickListener,
                                  UserProfile profile) {
        this.items = items;
        this.clickListener = clickListener;
        this.currentUserProfile = profile;
        this.userNameCache = new ConcurrentHashMap<>();
    }

    public void updateData(List<UserExchangeHistoryResponse> newItems) {
        if (newItems == null) {
            newItems = new java.util.ArrayList<>();
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new HistoryDiffCallback(this.items, newItems));
        this.items = newItems;
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ExchangeHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exchange_history, parent, false);
        return new ExchangeHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExchangeHistoryViewHolder holder, int position) {
        if (position >= items.size()) return;

        UserExchangeHistoryResponse item = items.get(position);

        setBookData(holder, item);

        setBookImage(holder, item);

        setUserData(holder, item);

        setExchangeStatus(holder, item);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.OnHistoryItemClick(item);
            }
        });
    }

    private void setBookData(ExchangeHistoryViewHolder holder, UserExchangeHistoryResponse item) {
        if (item.getBook() != null) {
            holder.tv_book_title.setText(item.getBook().getTitle() != null ?
                    item.getBook().getTitle() : "Unknown Title");
            holder.tv_book_author.setText(item.getBook().getAuthor() != null ?
                    item.getBook().getAuthor() : "Unknown Author");
        }

        String period = formatTime(item.getStart_date()) + " - " + formatTime(item.getEnd_date());
        holder.tv_exchange_period.setText(period);
    }

    private void setBookImage(ExchangeHistoryViewHolder holder, UserExchangeHistoryResponse item) {
        if (item.getBook() != null &&
                item.getBook().getImagePaths() != null &&
                !item.getBook().getImagePaths().isEmpty()) {

            String baseUrl = holder.itemView.getContext().getString(R.string.base_url);
            String imageUrl = baseUrl + item.getBook().getImagePaths().get(0);

            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .fit()
                    .centerCrop()
                    .into(holder.iv_book_cover);
        } else {
            holder.iv_book_cover.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void setUserData(ExchangeHistoryViewHolder holder, UserExchangeHistoryResponse item) {
        String currentUserId = currentUserProfile != null ? currentUserProfile.getId() : "";

        // Postavljanje lender data
        setUserRole(holder.tv_exchange_lender, item.getLender_id(), currentUserId,
                holder.itemView.getContext().getString(R.string.lender));

        // Postavljanje borrower data
        setUserRole(holder.tv_exchange_borrower, item.getBorrower_id(), currentUserId,
                holder.itemView.getContext().getString(R.string.borrower));
    }

    private void setUserRole(TextView textView, String userId, String currentUserId, String rolePrefix) {
        if (userId.equals(currentUserId)) {
            String displayName = getCurrentUserDisplayName();
            textView.setText(rolePrefix + " " + displayName);
        } else {
            String cachedName = userNameCache.get(userId);
            if (cachedName != null) {
                textView.setText(rolePrefix + " " + cachedName);
            } else {
                textView.setText(rolePrefix + " ...");
                fetchAndCacheUserName(userId, textView, rolePrefix);
            }
        }
    }

    private void fetchAndCacheUserName(String userId, TextView textView, String rolePrefix) {
        Auth0Helper.getUserByIdAsync(userId, textView.getContext())
                .thenAccept(user -> {
                    String displayName = extractUserDisplayName(user);
                    userNameCache.put(userId, displayName);
                    textView.setText(rolePrefix + " " + displayName);
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error fetching user data for " + userId, throwable);
                    String fallbackName = "N/A";
                    userNameCache.put(userId, fallbackName);
                    textView.setText(rolePrefix + " " + fallbackName);
                    return null;
                });
    }

    private String extractUserDisplayName(Auth0User user) {
        if (user == null || user.getUserMetadata() == null) {
            return "N/A";
        }

        String firstName = user.getUserMetadata().getFirstName();
        String lastName = user.getUserMetadata().getLastName();

        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            name.append(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(lastName.trim());
        }

        return name.length() > 0 ? name.toString() : "N/A";
    }

    private String getCurrentUserDisplayName() {
        if (currentUserProfile == null) {
            return "N/A";
        }

        // Pokušaj drugačijeg izvora za ime korisnika
        String firstName = extractFromMetadata("firstName");
        String lastName = extractFromMetadata("lastName");

        if (firstName != null || lastName != null) {
            StringBuilder name = new StringBuilder();
            if (firstName != null) name.append(firstName);
            if (lastName != null) {
                if (name.length() > 0) name.append(" ");
                name.append(lastName);
            }
            return name.toString();
        }

        // Fallback opcije
        if (currentUserProfile.getName() != null) {
            return currentUserProfile.getName();
        }
        if (currentUserProfile.getNickname() != null) {
            return currentUserProfile.getNickname();
        }
        if (currentUserProfile.getEmail() != null) {
            return currentUserProfile.getEmail().split("@")[0];
        }

        return "N/A";
    }

    private String extractFromMetadata(String key) {
        // Pokušaj user metadata
        Map<String, Object> userMetadata = currentUserProfile.getUserMetadata();
        if (userMetadata != null) {
            Object value = userMetadata.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return (String) value;
            }
            // Pokušaj alternate key format
            value = userMetadata.get(key.replace("Name", "_name"));
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return (String) value;
            }
        }

        // Pokušaj app metadata
        Map<String, Object> appMetadata = currentUserProfile.getAppMetadata();
        if (appMetadata != null) {
            Object value = appMetadata.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return (String) value;
            }
        }

        return null;
    }

    private void setExchangeStatus(ExchangeHistoryViewHolder holder, UserExchangeHistoryResponse item) {
        int statusColor;
        String statusText;

        switch (item.getStatus_ex_id()) {
            case 1:
                statusText = holder.itemView.getContext().getString(R.string.exchange_completed);
                statusColor = holder.itemView.getResources().getColor(R.color.primary_color);
                break;
            case 2:
                statusText = holder.itemView.getContext().getString(R.string.exchange_cancelled);
                statusColor = holder.itemView.getResources().getColor(R.color.error_color);
                break;
            case 3:
                statusText = holder.itemView.getContext().getString(R.string.exchange_in_progress);
                statusColor = holder.itemView.getResources().getColor(R.color.warning_color);
                break;
            default:
                statusText = "Unknown";
                statusColor = holder.itemView.getResources().getColor(android.R.color.darker_gray);
                break;
        }

        holder.tv_exchange_status.setText(statusText);
        holder.tv_exchange_status.setBackgroundColor(statusColor);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ExchangeHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tv_book_title, tv_book_author, tv_exchange_status,
                tv_exchange_period, tv_exchange_borrower, tv_exchange_lender;
        ImageView iv_book_cover;

        public ExchangeHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_book_title = itemView.findViewById(R.id.tv_book_title);
            tv_book_author = itemView.findViewById(R.id.tv_book_author);
            tv_exchange_status = itemView.findViewById(R.id.tv_exchange_status);
            tv_exchange_period = itemView.findViewById(R.id.tv_exchange_period);
            tv_exchange_lender = itemView.findViewById(R.id.tv_exchange_lender);
            tv_exchange_borrower = itemView.findViewById(R.id.tv_exchange_borrower);
            iv_book_cover = itemView.findViewById(R.id.iv_book_cover);
        }
    }

    // DiffUtil callback za efikasno ažuriranje liste
    private static class HistoryDiffCallback extends DiffUtil.Callback {
        private final List<UserExchangeHistoryResponse> oldList;
        private final List<UserExchangeHistoryResponse> newList;

        public HistoryDiffCallback(List<UserExchangeHistoryResponse> oldList,
                                   List<UserExchangeHistoryResponse> newList) {
            this.oldList = oldList != null ? oldList : new java.util.ArrayList<>();
            this.newList = newList != null ? newList : new java.util.ArrayList<>();
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            UserExchangeHistoryResponse oldItem = oldList.get(oldItemPosition);
            UserExchangeHistoryResponse newItem = newList.get(newItemPosition);
            return oldItem.getHistory_id() == newItem.getHistory_id();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            UserExchangeHistoryResponse oldItem = oldList.get(oldItemPosition);
            UserExchangeHistoryResponse newItem = newList.get(newItemPosition);

            // Usporedba relevantnih polja za update
            return oldItem.getStatus_ex_id() == newItem.getStatus_ex_id() &&
                    java.util.Objects.equals(oldItem.getStart_date(), newItem.getStart_date()) &&
                    java.util.Objects.equals(oldItem.getEnd_date(), newItem.getEnd_date());
        }
    }

    private static String formatTime(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return "";
        }

        try {
            // Pokušaj parsiranja ISO formata ako je dostupno
            ZonedDateTime dateTime = ZonedDateTime.parse(dateString);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm");
            return dateTime.format(formatter);
        } catch (Exception e) {
            try {
                // Pokušaj parsiranja UNIX timestamp ako nije ISO format
                long timestamp = Long.parseLong(dateString);
                SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd HH:mm", Locale.getDefault());
                return sdf.format(new Date(timestamp * 1000));
            } catch (NumberFormatException ex){
                return "";
            }
        }
    }
}