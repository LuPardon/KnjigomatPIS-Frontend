package com.example.knjigomatpis.models;

public class BookRequestResponse {
    private String message;
    private int messageId;
    private int borrowRequestId;
    private int chatId;
    private int id;
    private String otherUserId;

    public BookRequestResponse(String message, int messageId, int borrowRequestId, int chatId, int id, String otherUserId) {
        this.message = message;
        this.messageId = messageId;
        this.borrowRequestId = borrowRequestId;
        this.chatId = chatId;
        this.id = id;
        this.otherUserId = otherUserId;
    }

    public BookRequestResponse() {
    }
    public BookRequestResponse(BookRequestResponse original) {
        this.message = original.message;
        this.messageId = original.messageId;
        this.borrowRequestId = original.borrowRequestId;
        this.chatId = original.chatId;
        this.id = original.id;
        this.otherUserId = original.otherUserId;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getBorrowRequestId() {
        return borrowRequestId;
    }

    public void setBorrowRequestId(int borrowRequestId) {
        this.borrowRequestId = borrowRequestId;
    }

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }
}
