package com.example.knjigomatpis.adapters;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.UserNotification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<UserNotification> notifications;
    private final OnNotificationClickListener clickListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(UserNotification notification);
    }

    public NotificationAdapter(List<UserNotification> notifications, OnNotificationClickListener clickListener) {
        this.notifications = notifications;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        UserNotification notification = notifications.get(position);
        holder.titleText.setText(notification.getTitle());
        holder.messageText.setText(notification.getMessage());
        holder.dateText.setText(notification.getFormattedCreatedAt());

        if (!notification.isRead()) {
            holder.itemView.setBackgroundResource(R.color.unread_notification_background);
            holder.titleText.setTypeface(null, Typeface.BOLD);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
            holder.titleText.setTypeface(null, Typeface.NORMAL);
        }

        // Dodavanje click listener-a
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, messageText, dateText;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.notificationTitle);
            messageText = itemView.findViewById(R.id.notificationMessage);
            dateText = itemView.findViewById(R.id.notificationDate);
        }
    }
}

