package com.example.knjigomatpis.models;


    public class BookRequestMessage extends Message {
        public static final String TYPE_BOOK_REQUEST = "book_request";
        public static final String TYPE_REQUEST_ACCEPTED = "request_accepted";
        public static final String TYPE_REQUEST_REJECTED = "request_rejected";

        public static final String STATUS_PENDING = "pending";
        public static final String STATUS_ACCEPTED = "accepted";
        public static final String STATUS_REJECTED = "rejected";

        private String message_type;
        private Long book_id;
        private String book_title;
        private String book_author;
        private String request_status;
        private Long request_id;

        // Konstruktor za običnu poruku (nasljeđuje od Message)
        public BookRequestMessage(String sender_id, String content, int chat_id, String sent_at, int message_id) {
            super(sender_id, content, chat_id, sent_at, message_id);
            this.message_type = "text";
        }

        // Konstruktor za zahtjev za knjigu
        public BookRequestMessage(String sender_id, String content, int chat_id, String sent_at, int message_id,
                                  String message_type, Long book_id, String book_title, String book_author,
                                  String request_status, Long request_id) {
            super(sender_id, content, chat_id, sent_at, message_id);
            this.message_type = message_type;
            this.book_id = book_id;
            this.book_title = book_title;
            this.book_author = book_author;
            this.request_status = request_status;
            this.request_id = request_id;
        }

        // Getteri i setteri
        public String getMessage_type() {
            return message_type;
        }

        public void setMessage_type(String message_type) {
            this.message_type = message_type;
        }

        public Long getBook_id() {
            return book_id;
        }

        public void setBook_id(Long book_id) {
            this.book_id = book_id;
        }

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

        public String getRequest_status() {
            return request_status;
        }

        public void setRequest_status(String request_status) {
            this.request_status = request_status;
        }

        public Long getRequest_id() {
            return request_id;
        }

        public void setRequest_id(Long request_id) {
            this.request_id = request_id;
        }

        // Helper metode
        public boolean isBookRequest() {
            return TYPE_BOOK_REQUEST.equals(message_type);
        }

        public boolean isRequestResponse() {
            return TYPE_REQUEST_ACCEPTED.equals(message_type) || TYPE_REQUEST_REJECTED.equals(message_type);
        }

        public boolean isPending() {
            return STATUS_PENDING.equals(request_status);
        }

        public boolean isAccepted() {
            return STATUS_ACCEPTED.equals(request_status);
        }

        public boolean isRejected() {
            return STATUS_REJECTED.equals(request_status);
        }
    }

