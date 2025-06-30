package com.example.knjigomatpis.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import java.util.ArrayList;

import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.services.BookService;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<List<Book>> privateBooks;
    private final MutableLiveData<List<Book>> allUserBooks;
    private final MutableLiveData<String> error;
    private final MutableLiveData<Integer> borrowedCount;
    private final MutableLiveData<Integer> availableCount;
    private BookService bookService;

    public ProfileViewModel() {
        privateBooks = new MutableLiveData<>();
        allUserBooks = new MutableLiveData<>();
        error = new MutableLiveData<>();
        borrowedCount = new MutableLiveData<>();
        availableCount = new MutableLiveData<>();

        bookService = new BookService();
    }

    public LiveData<List<Book>> fetchPrivateBooks(String userId) {
        loadPrivateBooks(userId);
        return privateBooks;
    }

    public LiveData<List<Book>> fetchAllUserBooks(String userId) {
        loadAllUserBooks(userId);
        return allUserBooks;
    }
    public LiveData<String> getError() {
        return error;
    }
    public LiveData<Integer> getBorrowedCount() {
        return borrowedCount;
    }
    public LiveData<Integer> getAvailableCount() {
        return availableCount;
    }
    private void loadPrivateBooks(String userId) {

        // Koristi BookService.BookCallback
        bookService.fetchBooksByUserId(userId, new BookService.BookCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                List<Book> userPrivateBooks = new ArrayList<>();

                // Filtriranje samo privatnih knjiga (visibility_id = 1)
                for (Book book : books) {
                    if (book.getVisibilityId() != null && book.getVisibilityId() == 1L) {
                        userPrivateBooks.add(book);
                    }
                }

                privateBooks.postValue(userPrivateBooks);
            }

            @Override
            public void onError(Throwable t) {
                error.postValue(t.getMessage());
                privateBooks.postValue(new ArrayList<>());
            }
        });
    }
    private void loadAllUserBooks(String userId) {
        bookService.fetchBooksByUserId(userId, new BookService.BookCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                allUserBooks.postValue(books);
                // Izračunavanje statistike odmah nakon dohvaćanja
                calculateStatistics(books);
            }
            @Override
            public void onError(Throwable t) {
                error.postValue(t.getMessage());
                allUserBooks.postValue(new ArrayList<>());
                // Resetiranje statistike na 0 u slučaju greške
                borrowedCount.postValue(0);
                availableCount.postValue(0);
            }
        });
    }
    private void calculateStatistics(List<Book> books) {
        int borrowed = 0;
        int available = 0;

        for (Book book : books) {
            if (book.getBookStatusId() != null && book.getBookStatusId() == 2L) {
                borrowed++; // Posuđene knjige
            } else if (book.getBookStatusId() != null && book.getBookStatusId() == 1L) {
                available++; // Dostupne knjige
            }
        }

        borrowedCount.postValue(borrowed);
        availableCount.postValue(available);
    }

    public LiveData<List<Book>> getPublicBooks() {
        MutableLiveData<List<Book>> publicBooks = new MutableLiveData<>();

        bookService.fetchAllBooks(new BookService.BookCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                List<Book> publicBooksList = new ArrayList<>();

                // Filtriranje samo javnih knjiga (visibility_id = 2)
                for (Book book : books) {
                    if (book.getVisibilityId() != null && book.getVisibilityId() == 2L) {
                        publicBooksList.add(book);
                    }
                }

                publicBooks.postValue(publicBooksList);
            }

            @Override
            public void onError(Throwable t) {
                publicBooks.postValue(new ArrayList<>());
            }
        });

        return publicBooks;
    }
}