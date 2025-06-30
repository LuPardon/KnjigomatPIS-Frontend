package com.example.knjigomatpis.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Book {
    private Long book_id;
    private String user_id;
    private String title;
    private String author;
    private Long genre_id;
    private Long publication_year;
    private String publisher;
    private Long book_condition_id;
    private String book_language;
    private Long page_count;
    private String book_description;
    private String notes;
    private Long visibility_id;
    private Long book_status_id;
    private List<String> image_paths;

    // Getteri i setteri
    public Long getBookId() {
        return book_id;
    }

    public void setBookId(Long bookId) {
        this.book_id = bookId;
    }

    public String getUserId() {
        return user_id;
    }

    public void setUserId(String userId) {
        this.user_id = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Long getGenreId() {
        return genre_id;
    }

    public void setGenreId(Long genreId) {
        this.genre_id = genreId;
    }

    public Long getPublicationYear() {
        return publication_year;
    }

    public void setPublicationYear(Long publicationYear) {
        this.publication_year = publicationYear;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Long getBookConditionId() {
        return book_condition_id;
    }

    public void setBookConditionId(Long bookConditionId) {
        this.book_condition_id = bookConditionId;
    }

    public String getBookLanguage() {
        return book_language;
    }

    public void setBookLanguage(String bookLanguage) {
        this.book_language = bookLanguage;
    }

    public Long getPageCount() {
        return page_count;
    }

    public void setPageCount(Long pageCount) {
        this.page_count = pageCount;
    }

    public String getBookDescription() {
        return book_description;
    }

    public void setBookDescription(String bookDescription) {
        this.book_description = bookDescription;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getVisibilityId() {
        return visibility_id;
    }

    public void setVisibilityId(Long visibilityId) {
        this.visibility_id = visibilityId;
    }

    public Long getBookStatusId() {
        return book_status_id;
    }

    public void setBookStatusId(Long bookStatusId) {
        this.book_status_id = bookStatusId;
    }


    public List<String> getImagePaths() {

        return image_paths;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.image_paths = imagePaths;
    }
}
