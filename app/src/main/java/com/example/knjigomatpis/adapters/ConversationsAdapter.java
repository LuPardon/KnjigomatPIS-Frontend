package com.example.knjigomatpis.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.knjigomatpis.R;
import com.example.knjigomatpis.models.Chat;

import java.util.List;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private OnChatClickListener onChatClickListener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ConversationsAdapter(List<Chat> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.onChatClickListener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        holder.bind(chat);

        holder.itemView.setOnClickListener(v -> {
            if (onChatClickListener != null) {
                onChatClickListener.onChatClick(chat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUserName;
        private TextView tvLastMessage;
        private TextView tvLastMessageTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
        }

        public void bind(Chat chat) {
            // Prikaži ime drugog korisnika ili ID ako ime nije dostupno
            String displayName = chat.getPerson2Name() != null && !chat.getPerson2Name().isEmpty()
                    ? chat.getPerson2Name()
                    : itemView.getContext().getString(R.string.placeholder_user) + chat.getPerson2_id();

            tvUserName.setText(displayName);

            // Prikaži zadnju poruku ili placeholder
            String lastMessage = chat.getLastMessage() != null && !chat.getLastMessage().isEmpty()
                    ? chat.getLastMessage()
                    : itemView.getContext().getString(R.string.no_messages_yet);
            tvLastMessage.setText(lastMessage);

            // Prikaži vrijeme zadnje poruke
            String lastTime = chat.getLastMessageTime() != null && !chat.getLastMessageTime().isEmpty()
                    ? formatTime(chat.getLastMessageTime())
                    : "";
            tvLastMessageTime.setText(lastTime);
        }

        private String formatTime(String timestamp) {
            // Formatiranje vremena
            if (timestamp.length() > 10) {
                return timestamp.substring(0, 10); // Samo datum
            }
            return timestamp;
        }
    }
}