
package com.example.knjigomatpis.ui.myBooks;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.services.BookService;

import java.util.List;

public class MyBooksViewModel extends ViewModel {

    private final MutableLiveData<List<Book>> booksLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final BookService bookService;

    public MyBooksViewModel() {
        bookService = new BookService();
    }

    public LiveData<List<Book>> getBooks() {
        return booksLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoadingState() {
        return isLoading;
    }

    public void fetchBooksByUserId(String userId) {
        isLoading.setValue(true);
        bookService.fetchBooksByUserId(userId, new BookService.BookCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                isLoading.postValue(false);
                booksLiveData.setValue(books);
            }

            @Override
            public void onError(Throwable t) {
                isLoading.postValue(false);
                errorLiveData.postValue("Greška: " + t.getMessage());
            }
        });
    }
}
