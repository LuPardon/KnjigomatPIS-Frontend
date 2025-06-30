package com.example.knjigomatpis.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.services.BookService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<Book>> booksLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final BookService bookService;
    public HomeViewModel() {
        bookService = new BookService();
    }

    public LiveData<List<Book>> getBooks() {
        return booksLiveData;
    }

    public LiveData<Boolean> getLoadingState() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void fetchBooks(String userId) {
        isLoading.setValue(true);
        bookService.fetchOtherUsersBooksByUserId(userId, new BookService.BookCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                isLoading.postValue(false);
                booksLiveData.setValue(books);
            }

            @Override
            public void onError(Throwable t) {
                isLoading.postValue(false);
            }
        });
    }
}
