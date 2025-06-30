package com.example.knjigomatpis.ui.conversations;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.auth0.android.result.UserProfile;
import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.R;
import com.example.knjigomatpis.adapters.MessageAdapter;
import com.example.knjigomatpis.models.BorrowRequest;
import com.example.knjigomatpis.models.Message;
import com.example.knjigomatpis.ui.helpers.Auth0Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ChatFragment extends Fragment {

    private EditText ETMessageToSend;
    private RecyclerView RVMessages;
    private TextView TVChatTitle;
    private Socket mSocket;
    private UserProfile userProfile;
    private MessageAdapter messageAdapter; // Referenca na adapter


    // Podaci o chat-u
    private Long bookId;
    private String ownerId;
    private String currentUserId;
    private String bookTitle;
    private Integer chatId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dohvaćanje argumenata proslijeđenih iz MainActivity ili ConversationsFragment
        if (getArguments() != null) {
            bookId = getArguments().getLong("book_id", -1);
            ownerId = getArguments().getString("owner_id");
            currentUserId = getArguments().getString("current_user_id");
            bookTitle = getArguments().getString("book_title", getString(R.string.chat_default_title));

            Auth0Helper.getUserByIdAsync(ownerId, getContext()).thenAccept(user -> {
                TVChatTitle.setText(getString(R.string.chat_with) + " " + user.getName() + " (" + user.getUserMetadata().getLocation() + ") ");
            });

            //  Provjera je li proslijeđen postojeći chat_id
            int existingChatId = getArguments().getInt("chat_id", -1);
            if (existingChatId != -1) {
                chatId = existingChatId;
                Log.d("ChatFragment", "Using existing chat_id: " + chatId);
            }
        }

        try {
            mSocket = IO.socket(getString(R.string.base_chat_url));
        } catch (URISyntaxException e) {
            Log.d("ChatFragment", "URISyntaxException: " + e.getMessage());
        }

        userProfile = ((MainActivity) getActivity()).cachedUserProfile;

        // Postavljanje socket listener-a
        mSocket.on("chat message", onNewMessage);
        mSocket.on("chat history", onChatHistory);
        mSocket.on(Socket.EVENT_CONNECT, onSocketConnect);

        mSocket.connect();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (getArguments() != null) {
            bookId = getArguments().getLong("book_id", -1);
            ownerId = getArguments().getString("owner_id");
            currentUserId = getArguments().getString("current_user_id");
            bookTitle = getArguments().getString("book_title", getString(R.string.chat_default_title));

            int existingChatId = getArguments().getInt("chat_id", -1);
            if (existingChatId != -1) {
                chatId = existingChatId;
                Log.d("ChatFragment", "Using existing chat_id: " + chatId);
            }
        }

        try {
            mSocket = IO.socket(getString(R.string.base_chat_url));
        } catch (URISyntaxException e) {
            Log.d("ChatFragment", "URISyntaxException: " + e.getMessage());
        }

        userProfile = ((MainActivity) getActivity()).cachedUserProfile;

        mSocket.on("chat message", onNewMessage);
        mSocket.on("chat history", onChatHistory);
        mSocket.on(Socket.EVENT_CONNECT, onSocketConnect);

        mSocket.connect();

        if (mSocket.connected()) {
            initializeExistingChat();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        // Ažurirani ID-jevi
        ETMessageToSend = rootView.findViewById(R.id.et_message_input);
        RVMessages = rootView.findViewById(R.id.rv_messages);
        TVChatTitle = rootView.findViewById(R.id.tv_chat_title);
        Button btnSendMessage = rootView.findViewById(R.id.btn_send_message);


        // Postavljanje adaptera za RecyclerView
        messageAdapter = new MessageAdapter(getContext(), currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Nove poruke na dnu
        RVMessages.setLayoutManager(layoutManager);
        RVMessages.setAdapter(messageAdapter);

        // Naslov chat-a s imenom korisnika
        if (TVChatTitle != null) {
            Auth0Helper.getUserByIdAsync(ownerId, getContext()).thenAccept(user -> {
                TVChatTitle.setText(getString(R.string.chat_with) + " " + user.getName() + " (" + user.getUserMetadata().getLocation() + ") ");
            });
        }

        btnSendMessage.setOnClickListener(v -> attemptSend());

        // Provjera socket konekcije prije pristupanja room-u
        if (mSocket.connected()) {
            initializeExistingChat();
        }
        return rootView;
    }

    // Inicijalizacija postojećeg chat-a
    private void initializeExistingChat() {
        if (chatId != null) {
            Log.d("ChatFragment", "Initializing existing chat: " + chatId);
            joinChatRoom();
        } else {
            Log.d("ChatFragment", "No existing chat_id, new chat will be created on first message");
        }
    }

    // Kada se socket uspješno poveže
    private Emitter.Listener onSocketConnect = args -> {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            Log.d("ChatFragment", "Socket connected, initializing chat");
            initializeExistingChat();
        });
    };


    private void attemptSend() {
        String content = ETMessageToSend.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            return;
        }

        ETMessageToSend.setText("");

        JSONObject obj = new JSONObject();
        try {
            obj.put("sender_id", currentUserId);
            obj.put("content", content);

            if (chatId != null) {
                // Chat već postoji
                obj.put("chat_id", chatId);
                Log.d("ChatFragment", "Sending message to existing chat: " + chatId);
            } else {
                // Prvi put se šalje poruka - chat će se kreirati
                obj.put("recipient_id", ownerId);
                Log.d("ChatFragment", "Sending first message, chat will be created");
            }

            mSocket.emit("chat message", obj);
        } catch (JSONException e) {
            Log.e("ChatFragment", "Error creating message JSON", e);
        }
    }

    private void joinChatRoom() {
        if (chatId != null && mSocket != null && mSocket.connected()) {
            Log.d("ChatFragment", "Joining chat room: chat_" + chatId);

            // Server očekuje samo chatId kao parametar
            mSocket.emit("join chat", chatId);

            // Odmah nakon join poziva, učitaj poruke s kratkom pauzom
            new android.os.Handler().postDelayed(() -> {
                loadChatMessages();
            }, 200); // Kratka pauza da server obradi join
        } else {
            Log.w("ChatFragment", "Cannot join room - chatId: " + chatId +
                    ", socket connected: " + (mSocket != null && mSocket.connected()));
        }
    }

    private void loadChatMessages() {
        if (chatId != null && chatId > 0 && mSocket != null && mSocket.connected()) {
            Log.d("ChatFragment", "Loading messages for chat_id: " + chatId);

            mSocket.emit("load chat messages", chatId);
            Log.d("ChatFragment", "Sent load messages request for chat_id: " + chatId);

        } else {
            Log.d("ChatFragment", "Cannot load messages - chatId: " + chatId +
                    ", socket connected: " + (mSocket != null && mSocket.connected()));
        }
    }

    private Emitter.Listener onNewMessage = args -> {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            JSONObject obj = (JSONObject) args[0];
            try {
                String sender_id = obj.has("sender_id") ? obj.getString("sender_id") : "";
                String content = obj.has("content") ? obj.getString("content") : "";
                String sent_at = obj.has("sent_at") ? obj.getString("sent_at") :
                        ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                int message_chat_id = obj.has("chat_id") ? obj.getInt("chat_id") : 0;
                int message_id = obj.has("message_id") ? obj.getInt("message_id") : 0;
                String sender_name = obj.has("sender_name") ? obj.getString("sender_name") : getString(R.string.unknown_user);

                String imagePath = obj.has("imagePath") ? obj.getString("imagePath") : "";
                JSONObject borrowRequest = obj.has("borrowRequest") ? obj.getJSONObject("borrowRequest") : null;

                if (borrowRequest != null) {
                    BorrowRequest borrowrequest = new BorrowRequest(
                            borrowRequest.getInt("request_id"),
                            borrowRequest.getInt("book_id"),
                            borrowRequest.getInt("notification_id"),
                            borrowRequest.getString("owner_id"),
                            borrowRequest.getString("requester_id"),
                            borrowRequest.getString("requested_at"),
                            borrowRequest.getString("book_title"),
                            borrowRequest.getString("book_author"),
                            borrowRequest.getString("book_image_path"),
                            borrowRequest.getInt("status_id")
                    );

                    Message message = new Message(sender_id, borrowrequest, message_id, message_chat_id, content, sent_at);

                    message.setSender_name(sender_name);
                    addMessage(message);
                } else if (!imagePath.isEmpty()) {
                    Message message = new Message(sender_id, content, sent_at, message_chat_id, message_id, imagePath);

                    message.setSender_name(sender_name);
                    addMessage(message);
                } else {
                    Message message = new Message(sender_id, content, message_chat_id, sent_at, message_id);
                    message.setSender_name(sender_name);
                    addMessage(message);
                }
            } catch (JSONException e) {
                Log.e("ChatFragment", "Error parsing message", e);
            }
        });
    };

    private Emitter.Listener onChatHistory = args -> {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                org.json.JSONArray messages = (org.json.JSONArray) args[0];

                Log.d("ChatFragment", "Received chat history with " + messages.length() + " messages");

                // Očisti postojeće poruke
                messageAdapter.clearMessages();

                // Dodaj sve poruke iz povijesti
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject obj = messages.getJSONObject(i);

                    String sender_id = obj.getString("sender_id");
                    String content = obj.getString("content");
                    String sent_at = obj.getString("sent_at");
                    int message_chat_id = obj.getInt("chat_id");
                    int message_id = obj.getInt("message_id");
                    String sender_name = obj.has("sender_name") ? obj.getString("sender_name") : getString(R.string.unknown_user);

                    String imagePath = obj.has("imagePath") ? obj.getString("imagePath") : "";
                    JSONObject borrowRequest = obj.has("borrowRequest") ? obj.getJSONObject("borrowRequest") : null;

                    if (borrowRequest != null) {
                        BorrowRequest borrowrequest = new BorrowRequest(
                                borrowRequest.optInt("request_id", 0),
                                borrowRequest.optInt("book_id", 0),
                                borrowRequest.optInt("notification_id", 0),
                                borrowRequest.optString("owner_id", ""),
                                borrowRequest.optString("requester_id", ""),
                                borrowRequest.optString("requested_at", ""),
                                borrowRequest.optString("book_title", ""),
                                borrowRequest.optString("book_author", ""),
                                borrowRequest.optString("book_image_path", ""),
                                borrowRequest.optInt("status_id", 0)
                        );

                        Message message = new Message(sender_id, borrowrequest, message_id, message_chat_id, content, sent_at);

                        message.setSender_name(sender_name);
                        addMessage(message);
                    } else if (!imagePath.isEmpty()) {
                        Message message = new Message(sender_id, content, sent_at, message_chat_id, message_id, imagePath);

                        message.setSender_name(sender_name);
                        addMessage(message);
                    } else {
                        Message message = new Message(sender_id, content, message_chat_id, sent_at, message_id);
                        message.setSender_name(sender_name);
                        addMessage(message);
                    }
                }

                Log.d("ChatFragment", "Chat history loaded and displayed: " + messages.length() + " messages");

            } catch (JSONException e) {
                Log.e("ChatFragment", "Error parsing chat history", e);
            }
        });
    };

    private void addMessage(Message message) {

        messageAdapter.addMessage(message);

        // Scroll na zadnju poruku
        RVMessages.scrollToPosition(messageAdapter.getItemCount() - 1);

        // Koristi stvarno ime umjesto "You"/"Owner"
        String senderLabel = message.getSender_id().equals(currentUserId) ? getString(R.string.you) :
                (message.getSender_name() != null ? message.getSender_name() : getString(R.string.other_user));

        Log.d("ChatFragment", "Message added to UI: " + senderLabel + ": " + message.getContent());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSocket != null) {
            if (chatId != null) {
                Log.d("ChatFragment", "Leaving chat room: " + chatId);
                mSocket.emit("leave chat", chatId);
            }
            mSocket.disconnect();
            mSocket.off("chat message", onNewMessage);
            mSocket.off("chat history", onChatHistory);
            mSocket.off(Socket.EVENT_CONNECT, onSocketConnect);

        }
    }
}