package com.example.knjigomatpis.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class UserNotification {
    private int notification_id;
    private int chat_id;
    private int book_id;
    private String book_title;
    private String requester_id;
    private String user_id;
    private String title;
    private String message;
    private String type;
    private int is_read;
    private String created_at;
    private int related_book_id;
    private String from_user_id;

    public UserNotification() {}

    public UserNotification(String user_id, String title, String message, String type,
                            int related_book_id, String from_user_id) {
        this.user_id = user_id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.is_read = 0;
        this.related_book_id = related_book_id;
        this.from_user_id = from_user_id;
    }

    public int getBook_id() {
        return book_id;
    }

    public void setBook_id(int book_id) {
        this.book_id = book_id;
    }

    public String getBook_title() {
        return book_title;
    }

    public void setBook_title(String book_title) {
        this.book_title = book_title;
    }

    public String getRequester_id() {
        return requester_id;
    }

    public void setRequester_id(String requester_id) {
        this.requester_id = requester_id;
    }

    public int getChat_id() {
        return chat_id;
    }

    public void setChat_id(int chat_id) {
        this.chat_id = chat_id;
    }

    // Getteri i setteri
    public int getNotification_id() { return notification_id; }
    public void setNotification_id(int notification_id) { this.notification_id = notification_id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return is_read == 1; }
    public void setRead(boolean is_read) { this.is_read = is_read ? 1 : 0; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getRelated_book_id() { return related_book_id; }
    public void setRelated_book_id(int related_book_id) { this.related_book_id = related_book_id; }

    public String getFrom_user_id() { return from_user_id; }
    public void setFrom_user_id(String from_user_id) { this.from_user_id = from_user_id; }

    // Metoda za formatiranje datuma
    public String getFormattedCreatedAt() {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // jer Z znači UTC

        try {
            Date date = isoFormat.parse(created_at);

            // Lokalni prikaz: 10.05.2025. 23:27
            SimpleDateFormat localFormat = new SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault());
            localFormat.setTimeZone(TimeZone.getDefault());
            return localFormat.format(date);

        } catch (ParseException e) {
            e.printStackTrace();
            return created_at; // fallback ako pukne parsiranje
        }
    }

    // Konstante za tipove notifikacija
    public static class NotificationType {
        public static final String REQUEST_RECEIVED = "REQUEST_RECEIVED";
        public static final String REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
        public static final String REQUEST_REJECTED = "REQUEST_REJECTED";
        public static final String BOOK_AVAILABLE = "BOOK_AVAILABLE";
        public static final String CHAT_MESSAGE = "CHAT_MESSAGE";
    }
}