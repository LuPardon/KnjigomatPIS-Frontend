package com.example.knjigomatpis.ui.detailsBook;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.services.BookService;

public class DetailsBookViewModel extends ViewModel {

    private final MutableLiveData<Book> bookLiveData = new MutableLiveData<>();

    public LiveData<Book> getBook() {
        return bookLiveData;
    }

    public void fetchBookById(long bookId) {
        BookService bookService = new BookService();
        bookService.fetchBookById(bookId, new BookService.SingleBookCallback() {
            @Override
            public void onSuccess(Book book) {
                Log.d("DetailsDebug", "Book fetched: " + book.getTitle());
                bookLiveData.postValue(book);
            }

            @Override
            public void onError(Throwable t) {
            }
        });
    }
}
