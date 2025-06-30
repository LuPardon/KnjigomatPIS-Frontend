package com.example.knjigomatpis.network;

import com.example.knjigomatpis.models.BorrowRequest;
import com.example.knjigomatpis.models.BookRequest;
import com.example.knjigomatpis.models.BookRequestResponse;
import com.example.knjigomatpis.models.UserExchangeHistoryResponse;
import com.example.knjigomatpis.models.UserNotification;
import com.example.knjigomatpis.models.Book;
import com.example.knjigomatpis.services.BookService;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface IApiService {
    // Postojeće metode za knjige
    @GET("books")
    Call<List<Book>> getAllBooks();
    @GET("books/{bookId}")
    Call<Book> getBookById(@Path("bookId") Long bookId);

    @GET("books/user/{userId}")
    Call<List<Book>> getBooksByUserId(@Path("userId") String userId);

    @GET("books/user/other/{userId}")
    Call<List<Book>> getOtherUsersBooksByUserId(@Path("userId") String userId);
    @PUT("books/{bookId}")
    Call<Book> updateBook(@Path("bookId") Long bookId, @Body Book book);

    @Multipart
    @POST("books")
    Call<Book> createBook(
            @Part("book") Book book,
            @Part MultipartBody.Part[] images);
    @Multipart
    @POST("bookImages")
    Call<BookService.ImageUploadResponse> uploadBookImage(
            @Part MultipartBody.Part image,
            @Query("bookId") Long bookId
    );

    @Multipart
    @PUT("bookImages")
    Call<BookService.ImageUploadResponse> editBookImage(
            @Part MultipartBody.Part image,
            @Query("originaImagePath") String originaImagePath
    );

    @DELETE("bookImages")
    Call<Void> deleteBookImage(
            @Query("bookId") Long bookId,
            @Query("imagePath") String imagePath
    );

    @DELETE("books/{id}")
    Call<Void> deleteBook(@Path("id") Long bookId);


    // Metode za API pozive
    @GET("notifications/{userId}")
    Call<List<UserNotification>> getNotifications(@Path("userId") String userId);

    @POST("notifications")
    Call<BookRequestResponse> sendBookRequest(@Body BookRequest request);

    @PUT("notifications/read/{notificationId}")
    Call<Void> markNotificationAsRead(@Path("notificationId") int notificationId);

    @PUT("notifications/read-all/{userId}")
    Call<Void> markAllNotificationsAsRead(@Path("userId") String userId);

    @POST("borrowRequests")
    Call<BorrowRequest> postBorrowRequest(@Body BorrowRequest borrowRequest);

    @POST("borrowRequests/{id}/accept")
    Call<BorrowRequest> acceptBorrowRequest(@Path("id") int id);

    @POST("borrowRequests/{id}/reject")
    Call<BorrowRequest> rejectBorrowRequest(@Path("id") int id);

    @GET("exchangeHistory/user/{userId}")
    Call<List<UserExchangeHistoryResponse>> getUserExchangeHistory(@Path("userId") String userId);
}

