package com.example.knjigomatpis.models;

public class Message {
    private String sender_id;
    private String content;
    private String sent_at;
    private int chat_id;
    private int message_id;
    private String imagePath;
    private BorrowRequest borrowRequest;
    private String sender_name;

    public Message(String sender_id, BorrowRequest borrowRequest, int message_id, int chat_id, String content, String sent_at) {
        this.sender_id = sender_id;
        this.borrowRequest = borrowRequest;
        this.message_id = message_id;
        this.chat_id = chat_id;
        this.content = content;
        this.sent_at = sent_at;
    }

    public Message(String sender_id, String content, int chat_id, String sent_at, int message_id) {
        this.sender_id = sender_id;
        this.content = content;
        this.sent_at = sent_at;
        this.chat_id = chat_id;
        this.message_id = message_id;
    }

    public Message(String sender_id, String content, int chat_id) {
        this.sender_id = sender_id;
        this.content = content;
        this.chat_id = chat_id;
    }

    public Message(String sender_id, String content, String sent_at, int chat_id, int message_id, String imagePath) {
        this.sender_id = sender_id;
        this.content = content;
        this.sent_at = sent_at;
        this.chat_id = chat_id;
        this.message_id = message_id;
        this.imagePath = imagePath;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public BorrowRequest getBorrowRequest() {
        return borrowRequest;
    }

    public void setBorrowRequest(BorrowRequest borrowRequest) {
        this.borrowRequest = borrowRequest;
    }
    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSent_at() {
        return sent_at;
    }

    public void setSent_at(String sent_at) {
        this.sent_at = sent_at;
    }

    public int getChat_id() {
        return chat_id;
    }

    public void setChat_id(int chat_id) {
        this.chat_id = chat_id;
    }

    public int getMessage_id() {
        return message_id;
    }

    public void setMessage_id(int message_id) {
        this.message_id = message_id;
    }

    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
    }
}
