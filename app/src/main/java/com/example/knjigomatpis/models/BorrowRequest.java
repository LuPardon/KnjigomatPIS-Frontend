package com.example.knjigomatpis.models;

public class BorrowRequest {
    private int request_id;
    private int book_id;
    private String requester_id;
    private String owner_id;
    private int status_id;
    private int notification_id;
    private String requested_at;
    private String book_title;
    private String book_author;
    private String book_image_path;

    public String getBook_title() {
        return book_title;
    }

    public void setBook_title(String book_title) {
        this.book_title = book_title;
    }

    public String getBook_author() {
        return book_author;
    }

    public void setBook_author(String book_author) {
        this.book_author = book_author;
    }

    public String getBook_image_path() {
        return book_image_path;
    }

    public void setBook_image_path(String book_image_path) {
        this.book_image_path = book_image_path;
    }

    public BorrowRequest(int request_id, int book_id, int notification_id, String owner_id, String requester_id, String requested_at, String book_title, String book_author, String book_image_path, int status_id) {
        this.book_id = book_id;
        this.notification_id = notification_id;
        this.owner_id = owner_id;
        this.requester_id = requester_id;
        this.requested_at = requested_at;
        this.book_title = book_title;
        this.book_author = book_author;
        this.book_image_path = book_image_path;
        this.status_id = status_id;
        this.request_id = request_id;

    }

    public int getRequest_id() {
        return request_id;
    }

    public void setRequest_id(int request_id) {
        this.request_id = request_id;
    }

    public BorrowRequest(int book_id, int notification_id, String owner_id, String requester_id, String requested_at, int status_id) {
        this.book_id = book_id;
        this.notification_id = notification_id;
        this.owner_id = owner_id;
        this.requester_id = requester_id;
        this.requested_at = requested_at;
        this.status_id = status_id;
    }
    public int getBook_id() {
        return book_id;
    }

    public void setBook_id(int book_id) {
        this.book_id = book_id;
    }

    public int getNotification_id() {
        return notification_id;
    }

    public void setNotification_id(int notification_id) {
        this.notification_id = notification_id;
    }

    public String getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(String owner_id) {
        this.owner_id = owner_id;
    }

    public String getRequester_id() {
        return requester_id;
    }

    public void setRequester_id(String requester_id) {
        this.requester_id = requester_id;
    }

    public String getRequested_at() {
        return requested_at;
    }

    public void setRequested_at(String requested_at) {
        this.requested_at = requested_at;
    }

    public int getStatus_id() {
        return status_id;
    }

    public void setStatus_id(int status_id) {
        this.status_id = status_id;
    }
}
