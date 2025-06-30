package com.example.knjigomatpis.models;

public class BookRequest {
    private String bookOwnerId, requesterId;
    private String title;
    private String message;
    private long bookId;


    public BookRequest(String bookOwnerId, String requesterId, long bookId, String title, String message) {
        this.bookOwnerId = bookOwnerId;
        this.title = title;
        this.message = message;
        this.bookId = bookId;
        this.requesterId = requesterId;
    }

    // Getteri i setteri

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public long getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getBookOwnerId() {
        return bookOwnerId;
    }

    public void setBookOwnerId(String bookOwnerId) {
        this.bookOwnerId = bookOwnerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


