package com.example.knjigomatpis.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.BorrowRequest;
import com.example.knjigomatpis.models.Message;
import com.example.knjigomatpis.network.ApiClient;
import com.example.knjigomatpis.network.IApiService;
import com.example.knjigomatpis.ui.helpers.Auth0Helper;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_REGULAR_MESSAGE = 0;
    private static final int TYPE_BOOK_REQUEST = 1;
    private static final int TYPE_IMAGE_MESSAGE = 2;

    private final List<Message> messages;
    private final String currentUserId;
    private final Context context;

    public MessageAdapter(Context context, String currentUserId) {
        this.context = context;
        this.messages = new ArrayList<>();
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);

        if (message.getBorrowRequest() != null) {
            return TYPE_BOOK_REQUEST;
        } else if (message.getImagePath() != null && !message.getImagePath().isEmpty()) {
            return TYPE_IMAGE_MESSAGE;
        } else {
            return TYPE_REGULAR_MESSAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_BOOK_REQUEST:
                View requestView = inflater.inflate(R.layout.item_book_request_message, parent, false);
                return new BookRequestViewHolder(requestView);

            case TYPE_IMAGE_MESSAGE:
                View imageView = inflater.inflate(R.layout.item_image_message, parent, false);
                return new ImageMessageViewHolder(imageView);

            default:
                View messageView = inflater.inflate(R.layout.item_message_bubble, parent, false);
                return new RegularMessageViewHolder(messageView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isMyMessage = message.getSender_id().equals(currentUserId);

        if (holder instanceof RegularMessageViewHolder) {
            ((RegularMessageViewHolder) holder).bind(message, isMyMessage);
        } else if (holder instanceof ImageMessageViewHolder) {
            ((ImageMessageViewHolder) holder).bind(message, isMyMessage);
        } else if (holder instanceof BookRequestViewHolder) {
            ((BookRequestViewHolder) holder).bind(message, isMyMessage);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // Public methods za rukovanje porukama
    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessages(List<Message> newMessages) {
        int startPosition = messages.size();
        messages.addAll(newMessages);
        notifyItemRangeInserted(startPosition, newMessages.size());
    }

    public void setMessages(List<Message> messages) {
        this.messages.clear();
        this.messages.addAll(messages);
        notifyDataSetChanged();
    }

    public void clearMessages() {
        this.messages.clear();
        notifyDataSetChanged();
    }

    public void removeMessage(int position) {
        if (position >= 0 && position < messages.size()) {
            messages.remove(position);
            notifyItemRemoved(position);
        }
    }

    // Base ViewHolder class s common funkcionalnostima
    abstract static class BaseMessageViewHolder extends RecyclerView.ViewHolder {
        protected LinearLayout messageBubble;
        protected TextView messageContent;
        protected TextView messageTime;
        protected TextView senderName;
        protected View spacerLeft;
        protected View spacerRight;

        public BaseMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageBubble = itemView.findViewById(R.id.ll_message_bubble);
            messageContent = itemView.findViewById(R.id.tv_message_content);
            messageTime = itemView.findViewById(R.id.tv_message_time);
            senderName = itemView.findViewById(R.id.tv_sender_name);
            spacerLeft = itemView.findViewById(R.id.spacer_left);
            spacerRight = itemView.findViewById(R.id.spacer_right);
        }

        protected void setupMessageAppearance(Message message, boolean isMyMessage) {
            // Format i set time
            messageTime.setText(formatTime(message.getSent_at()));

            if (isMyMessage) {
                // My message - desno
                spacerLeft.setVisibility(View.VISIBLE);
                spacerRight.setVisibility(View.GONE);
                senderName.setVisibility(View.GONE);

                // pozadina za my messages
                messageBubble.setBackground(ContextCompat.getDrawable(
                        itemView.getContext(), R.drawable.message_bubble_sent));

                // White text na teal background
                messageContent.setTextColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.text_on_teal));
                messageTime.setTextColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.text_on_teal));

            } else {
                // Other's message - lijevo
                spacerLeft.setVisibility(View.GONE);
                spacerRight.setVisibility(View.VISIBLE);

                // Prikaži sender name
                senderName.setVisibility(View.VISIBLE);
                senderName.setText(message.getSender_name() != null ?
                        message.getSender_name() : itemView.getContext().getString(R.string.unknown_user));

                // Pozadina za other's messages
                messageBubble.setBackground(ContextCompat.getDrawable(
                        itemView.getContext(), R.drawable.message_bubble_received));

                // Dark text na light background
                messageContent.setTextColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.text_primary));
                messageTime.setTextColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.text_hint));
            }
        }

        public abstract void bind(Message message, boolean isMyMessage);
    }

    // ViewHolder za regular text messages
    static class RegularMessageViewHolder extends BaseMessageViewHolder {

        public RegularMessageViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void bind(Message message, boolean isMyMessage) {
            messageContent.setText(message.getContent());
            setupMessageAppearance(message, isMyMessage);
        }
    }

    // ViewHolder za image messages
    static class ImageMessageViewHolder extends BaseMessageViewHolder {
        private final ImageView messageImage;

        public ImageMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageImage = itemView.findViewById(R.id.iv_message_image);
        }

        @Override
        public void bind(Message message, boolean isMyMessage) {

            if (message.getContent() != null && !message.getContent().isEmpty()) {
                messageContent.setText(message.getContent());
                messageContent.setVisibility(View.VISIBLE);
            } else {
                messageContent.setVisibility(View.GONE);
            }

            loadImage(message.getImagePath());

            setupMessageAppearance(message, isMyMessage);
        }

        private void loadImage(String imagePath) {
            if (imagePath == null || imagePath.isEmpty()) {
                messageImage.setVisibility(View.GONE);
                return;
            }

            messageImage.setVisibility(View.VISIBLE);

            Picasso.get().load(imagePath).into(messageImage);
        }
    }

    // ViewHolder za book request messages
    static class BookRequestViewHolder extends BaseMessageViewHolder {
        private static IApiService IApiService;

        private final TextView bookTitle, bookAuthor, borrowRequestStatus, tvSenderName;
        private final Button rejectButton, acceptButton;

        public BookRequestViewHolder(@NonNull View itemView) {
            super(itemView);

            IApiService = ApiClient.getRetrofit().create(IApiService.class);

            bookTitle = itemView.findViewById(R.id.tv_book_title);
            bookAuthor = itemView.findViewById(R.id.tv_book_author);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            messageTime = itemView.findViewById(R.id.tv_message_time);
            messageContent = itemView.findViewById(R.id.tv_request_message);
            rejectButton = itemView.findViewById(R.id.btn_reject_request);
            acceptButton = itemView.findViewById(R.id.btn_accept_request);
            borrowRequestStatus = itemView.findViewById(R.id.tv_status);


        }

        @Override
        public void bind(Message message, boolean isMyMessage) {
            // Postavljanje main content
            messageTime.setText(formatTime(message.getSent_at()));
            messageContent.setText(message.getBorrowRequest().getBook_title());
            bookAuthor.setText(message.getBorrowRequest().getBook_author());
            bookTitle.setText(message.getBorrowRequest().getBook_title());
            tvSenderName.setText(message.getSender_name());

            Context ctx = itemView.getContext();
            Auth0Helper.getUserByIdAsync(message.getSender_id(), ctx).thenAccept(item->
                    tvSenderName.setText(item.getName()));
            int statusId = message.getBorrowRequest().getStatus_id();

            if (statusId == 1) {
                borrowRequestStatus.setText(ctx.getString(R.string.status_pending));
            } else if (statusId == 2) {
                rejectButton.setVisibility(View.GONE);
                acceptButton.setVisibility(View.GONE);
                borrowRequestStatus.setText(ctx.getString(R.string.status_accepted));
            } else {
                rejectButton.setVisibility(View.GONE);
                acceptButton.setVisibility(View.GONE);
                borrowRequestStatus.setText(ctx.getString(R.string.status_rejected));
            }

            if (isMyMessage) { // ako je zahtjev već prihvaćen ili odbijen
                rejectButton.setVisibility(View.GONE);
                acceptButton.setVisibility(View.GONE);
                return;
            }

            rejectButton.setOnClickListener(v -> {
                IApiService.rejectBorrowRequest(message.getBorrowRequest().getRequest_id()).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<BorrowRequest> call, Response<BorrowRequest> response) {
                        if (response.isSuccessful()) {
                            rejectButton.setVisibility(View.GONE);
                            acceptButton.setVisibility(View.GONE);
                            borrowRequestStatus.setText(ctx.getString(R.string.status_rejected));
                            Toast.makeText(ctx, ctx.getString(R.string.toast_request_rejected), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(rejectButton.getContext(), response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BorrowRequest> call, Throwable t) {
                        Toast.makeText(rejectButton.getContext(), "Failed to Reject.", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            acceptButton.setOnClickListener(v -> {
                IApiService.acceptBorrowRequest(message.getBorrowRequest().getRequest_id()).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<BorrowRequest> call, Response<BorrowRequest> response) {
                        if (response.isSuccessful()) {
                            if(response.body() == null) {
                                // nije se uspjelo, knjiga nedostupna
                            }
                            rejectButton.setVisibility(View.GONE);
                            acceptButton.setVisibility(View.GONE);
                            Toast.makeText(ctx, ctx.getString(R.string.toast_request_accepted), Toast.LENGTH_SHORT).show();
                            Toast.makeText(ctx, ctx.getString(R.string.toast_request_accepted), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BorrowRequest> call, Throwable t) {
                        Toast.makeText(acceptButton.getContext(), "Failed to accept.", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            setupMessageAppearance(message, isMyMessage);
        }
    }

    // Utility method za time formatting
    private static String formatTime(String sentAt) {
        if (sentAt == null || sentAt.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date());
        }

        try {
            ZonedDateTime dateTime = ZonedDateTime.parse(sentAt);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm");
            return dateTime.format(formatter);
        } catch (Exception e) {
            try {
                long timestamp = Long.parseLong(sentAt);
                SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd HH:mm", Locale.getDefault());
                return sdf.format(new Date(timestamp * 1000)); // Convert from seconds to milliseconds
            } catch (NumberFormatException ex) {
                SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd HH:mm", Locale.getDefault());
                return sdf.format(new Date());
            }
        }
    }
}