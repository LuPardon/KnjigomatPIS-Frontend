package com.example.knjigomatpis.services;

import android.util.Log;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.network.ApiClient;
import com.example.knjigomatpis.network.IApiService;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookService {

    private final IApiService IApiService;
    public BookService() {
        this.IApiService = ApiClient.getRetrofit().create(IApiService.class);
    }

    public void fetchAllBooks(BookCallback callback) {
        Call<List<Book>> call = IApiService.getAllBooks();
        call.enqueue(new Callback<List<Book>>() {
            @Override
            public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Throwable("Neuspješan odgovor sa servera"));
                }
            }

            @Override
            public void onFailure(Call<List<Book>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void fetchBooksByUserId(String userId, BookCallback callback) {
        Call<List<Book>> call = IApiService.getBooksByUserId(userId);
        call.enqueue(new Callback<List<Book>>() {
            @Override
            public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Throwable("Neuspješan odgovor sa servera"));
                }
            }

            @Override
            public void onFailure(Call<List<Book>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }
    public void fetchOtherUsersBooksByUserId(String userId, BookCallback callback) {
        Call<List<Book>> call = IApiService.getOtherUsersBooksByUserId(userId);
        call.enqueue(new Callback<List<Book>>() {
            @Override
            public void onResponse(Call<List<Book>> call, Response<List<Book>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Throwable("Neuspješan odgovor sa servera"));
                }
            }

            @Override
            public void onFailure(Call<List<Book>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }


    public void updateBook(Book book, UpdateBookCallback callback) {
        Call<Book> call = IApiService.updateBook(book.getBookId(), book);
        call.enqueue(new Callback<Book>() {
            @Override
            public void onResponse(Call<Book> call, Response<Book> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("BookService", "Book updated successfully: " + response.body().getTitle());
                    callback.onSuccess(response.body());
                } else {
                    Log.e("BookService", "Update failed with code: " + response.code());
                    callback.onError(new Throwable("Error updating book: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Book> call, Throwable t) {
                Log.e("BookService", "Update request failed", t);
                callback.onError(t);
            }
        });
    }

    public void uploadBookImage(Long bookId, MultipartBody.Part imagePart, ImageUploadCallback callback) {
        Log.d("BookService", "Starting image upload for bookId: " + bookId);
        Log.d("BookService", "MultipartBody.Part: " + (imagePart != null ? imagePart.headers() : "null"));

        Call<ImageUploadResponse> call = IApiService.uploadBookImage(imagePart, bookId);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ImageUploadResponse> call, Response<ImageUploadResponse> response) {
                Log.d("BookService", "Upload response code: " + response.code());
                Log.d("BookService", "Upload response message: " + response.message());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("BookService", "Upload successful, imagePath: " + response.body().getImagePath());
                    callback.onSuccess(response.body());
                } else {
                    Log.e("BookService", "Upload failed with code: " + response.code());
                    Log.e("BookService", "Error Body: " + (response.errorBody() != null ? response.errorBody().toString() : "null"));
                    callback.onError(new Throwable("Failed to upload image: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ImageUploadResponse> call, Throwable t) {
                Log.e("BookService", "Upload request failed", t);
                callback.onError(t);
            }
        });
    }
    public void editBookImage(String originaImagePath, MultipartBody.Part imagePart, ImageUploadCallback callback) {
        Log.d("BookService", "Updating image with path: " + originaImagePath);
        Log.d("BookService", "MultipartBody.Part: " + (imagePart != null ? imagePart.headers() : "null"));

        Call<ImageUploadResponse> call = IApiService.editBookImage(imagePart, originaImagePath);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ImageUploadResponse> call, Response<ImageUploadResponse> response) {
                Log.d("BookService", "Upload response code: " + response.code());
                Log.d("BookService", "Upload response message: " + response.message());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("BookService", "Upload successful, imagePath: " + response.body().getImagePath());
                    callback.onSuccess(response.body());
                } else {
                    Log.e("BookService", "Upload failed with code: " + response.code());
                    Log.e("BookService", "Error Body: " + (response.errorBody() != null ? response.errorBody().toString() : "null"));
                    callback.onError(new Throwable("Failed to upload image: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ImageUploadResponse> call, Throwable t) {
                Log.e("BookService", "Upload request failed", t);
                callback.onError(t);
            }
        });
    }
    public interface CreateBookCallback {
        void onSuccess(Book createdBook);
        void onError(Throwable t);
    }

    // Implementacija za stvaranje knjige putem API-ja
    public void createBook(Book book, String userId, MultipartBody.Part[] body, CreateBookCallback callback) {
        book.setUserId(userId);
        Call<Book> call = IApiService.createBook(book, body);
        call.enqueue(new Callback<Book>() {
            @Override
            public void onResponse(Call<Book> call, Response<Book> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("BookService", "User ID: " + response.body().getUserId());
                    Log.d("BookService", "Book created successfully: " + response.body().getTitle());
                    callback.onSuccess(response.body());
                } else {
                    Log.e("BookService", "Create failed with code: " + response.code());
                    callback.onError(new Throwable(" Failed to create book:" + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Book> call, Throwable t) {
                Log.e("BookService", "Create request failed", t);
                callback.onError(t);
            }
        });
    }

    public void deleteBookImage(Long bookId, String imagePath, ImageDeleteCallback callback) {
        Call<Void> call = IApiService.deleteBookImage(bookId, imagePath);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Throwable("Failed to delete image: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void deleteBook(Long bookId, DeleteBookCallback callback) {
        Log.d("BookService", "Starting book deletion for bookId: " + bookId);

        Call<Void> call = IApiService.deleteBook(bookId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("BookService", "Delete response code: " + response.code());
                Log.d("BookService", "Delete response message: " + response.message());

                if (response.isSuccessful()) {
                    Log.d("BookService", "Book deleted successfully");
                    callback.onSuccess();
                } else {
                    Log.e("BookService", "Delete failed with code: " + response.code());
                    Log.e("BookService", "Error Body: " + (response.errorBody() != null ? response.errorBody().toString() : "null"));
                    callback.onError(new Throwable("Failed to delete book: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("BookService", "Delete request failed", t);
                callback.onError(t);
            }
        });
    }



    // Callback interface za brisanje knjige
    public interface DeleteBookCallback {
        void onSuccess();
        void onError(Throwable t);
    }

    public interface ImageUploadCallback {
        void onSuccess(ImageUploadResponse response);
        void onError(Throwable t);
    }

    public interface ImageDeleteCallback {
        void onSuccess();
        void onError(Throwable t);
    }

    public static class ImageUploadResponse {
        private String imagePath;
        private String message;

        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    ///

    // Callback interface za update knjigu
    public interface UpdateBookCallback {
        void onSuccess(Book updatedBook);
        void onError(Throwable t);
    }
    // Callback interface za sve knjige
    public interface BookCallback {
        void onSuccess(List<Book> books);
        void onError(Throwable t);
    }
    public void fetchBookById(Long bookId, SingleBookCallback callback) {
        Call<Book> call = IApiService.getBookById(bookId);
        call.enqueue(new Callback<Book>() {
            @Override
            public void onResponse(Call<Book> call, Response<Book> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Throwable("Unsuccessful response"));
                }
                Log.d("DetailsDebug", "API Response: " + response.code() +
                        ", Body: " + (response.body() != null ? response.body().getTitle() : "null"));
            }

            @Override
            public void onFailure(Call<Book> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    // Callback interface za pojedinačnu knjigu
    public interface SingleBookCallback {
        void onSuccess(Book book);
        void onError(Throwable t);
    }

}
