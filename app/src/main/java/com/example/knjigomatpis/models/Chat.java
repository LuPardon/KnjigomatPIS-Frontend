package com.example.knjigomatpis.models;

public class Chat {
    private int chat_id;
    private String person1_id;
    private String person2_id;

    private String lastMessage;
    private String lastMessageTime;
    private String person2Name;

    public Chat() {
    }

    public Chat(int chat_id, String person1_id, String person2_id) {
        this.chat_id = chat_id;
        this.person1_id = person1_id;
        this.person2_id = person2_id;
    }

    // Getteri i setteri za osnovne podatke
    public int getChat_id() {
        return chat_id;
    }

    public void setChat_id(int chat_id) {
        this.chat_id = chat_id;
    }

    public String getPerson1_id() {
        return person1_id;
    }

    public void setPerson1_id(String person1_id) {
        this.person1_id = person1_id;
    }

    public String getPerson2_id() {
        return person2_id;
    }

    public void setPerson2_id(String person2_id) {
        this.person2_id = person2_id;
    }

    // Getteri i setteri za dodatne podatke
    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getPerson2Name() {
        return person2Name;
    }

    public void setPerson2Name(String person2Name) {
        this.person2Name = person2Name;
    }

}