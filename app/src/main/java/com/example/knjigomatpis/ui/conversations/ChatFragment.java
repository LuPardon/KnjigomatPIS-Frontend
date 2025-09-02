package com.example.knjigomatpis.ui.conversations;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.example.knjigomatpis.helpers.Auth0Helper;

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
    private ProgressBar progressBarChatLoading;
    private TextView TVChatError;
    private Socket mSocket;
    private UserProfile userProfile;
    private MessageAdapter messageAdapter; // Adapter reference


    // Chat data
    private Long bookId;
    private String ownerId;
    private String currentUserId;
    private String bookTitle;
    private Integer chatId;

    // Loading slučajevi
    private boolean isLoadingMessages = false;
    private boolean isSocketConnected = false;
    private boolean isSocketInitialized = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        // Dohvaća argumente koji su prošli do ovog fragmenta iz MainActivity ili ConversationsFragment
        if (getArguments() != null) {
            bookId = getArguments().getLong("book_id", -1);
            ownerId = getArguments().getString("owner_id");
            currentUserId = getArguments().getString("current_user_id");
            bookTitle = getArguments().getString("book_title", getString(R.string.chat_default_title));

            // Provjera da li postojeći chat_id je proslijeđen
            int existingChatId = getArguments().getInt("chat_id", -1);
            if (existingChatId != -1) {
                chatId = existingChatId;
                Log.d("ChatFragment", "Using existing chat_id: " + chatId);
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        // Inicijaliziranje views-a
        ETMessageToSend = rootView.findViewById(R.id.et_message_input);
        RVMessages = rootView.findViewById(R.id.rv_messages);
        TVChatTitle = rootView.findViewById(R.id.tv_chat_title);
        progressBarChatLoading = rootView.findViewById(R.id.progressBarChatLoading);
        TVChatError = rootView.findViewById(R.id.tvChatError);
        Button btnSendMessage = rootView.findViewById(R.id.btn_send_message);

        // Postavljanje Adaptera za RecyclerView
        messageAdapter = new MessageAdapter(getContext(), currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Nove poruke na dnu
        RVMessages.setLayoutManager(layoutManager);
        RVMessages.setAdapter(messageAdapter);

        // Postavljanje chat title sa user name
        if (TVChatTitle != null) {
            Auth0Helper.getUserByIdAsync(ownerId, getContext()).thenAccept(user -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        TVChatTitle.setText(getString(R.string.chat_with) + " " + user.getName() + " (" + user.getUserMetadata().getLocation() + ") ");
                    });
                }
            });
        }

        btnSendMessage.setOnClickListener(v -> attemptSend());
        return rootView;
    }

    @Override
    public void onViewCreated( View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        // Inicijaliziranje socket-a samo jednom
        if (!isSocketInitialized) {
            initializeSocket();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        // Učitavanje poruka samo ako je socket spreman
        if (mSocket != null && mSocket.connected() && userProfile != null) {
            initializeExistingChat();
        } else if (mSocket != null && !mSocket.connected()) {
            Log.d(TAG, "Socket not connected, connecting...");
            mSocket.connect();
        }
    }

    private void initializeSocket() {
        if (isSocketInitialized && mSocket != null) {
            Log.d(TAG, "Socket already initialized, skipping");
            return;
        }

        try {
            // Zatvaranje postojećeg socket-a ako postoji
            if (mSocket != null) {
                cleanupSocket();
            }

            IO.Options opts = new IO.Options();
            opts.transports = new String[]{"websocket"};  // samo websocket
            mSocket = IO.socket(getString(R.string.base_chat_url), opts);

            Log.d(TAG, "Socket initialized successfully");
            isSocketInitialized = true;
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
            showError("Socket initialization failed");
            return;
        }

        // Provjera da li je MainActivity dostupan i da li je user profile
        if (getActivity() instanceof MainActivity) {
            userProfile = ((MainActivity) getActivity()).cachedUserProfile;
            if (userProfile != null) {
                Log.d(TAG, "User profile loaded: " + userProfile.getId());
            } else {
                Log.w(TAG, "User profile is null");
                showError("User profile not available");
                return;
            }
        } else {
            Log.e(TAG, "Activity is not MainActivity");
            showError("Activity error");
            return;
        }

        // Postavljanje socket listener-a
        mSocket.on("chat message", onNewMessage);
        mSocket.on("chat history", onChatHistory);
        mSocket.on(Socket.EVENT_CONNECT, onSocketConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onSocketDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onSocketError);

        // Povezivanje socket-a
        mSocket.connect();
    }
    private void showLoading(boolean show) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            isLoadingMessages = show;
            if (progressBarChatLoading != null) {
                progressBarChatLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (RVMessages != null) {
                RVMessages.setVisibility(show ? View.GONE : View.VISIBLE);
            }
            if (TVChatError != null) {
                TVChatError.setVisibility(View.GONE);
            }
        });
    }

    private void showError(String errorMessage) {
        if (getActivity() == null) return;

        Log.e("ChatFragment", "Showing error: " + errorMessage);
        getActivity().runOnUiThread(() -> {
            showLoading(true);
        });
    }
    private void showMessages() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            showLoading(false);
            if (RVMessages != null) {
                RVMessages.setVisibility(View.VISIBLE);
            }
            if (TVChatError != null) {
                TVChatError.setVisibility(View.GONE);
            }
        });
    }

    // Inicijaliziranje postojećeg chat-a
    private void initializeExistingChat() {
        if (chatId != null) {
            Log.d("ChatFragment", "Initializing existing chat: " + chatId);
            showLoading(true);
            joinChatRoom();
        } else {
            Log.d("ChatFragment", "No existing chat_id, new chat will be created on first message");
            showMessages(); // Prikazivanje praznog chat-a spremnog za nove poruke

        }
    }

    // Kad je socket uspješno povezan
    private Emitter.Listener onSocketConnect = args -> {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            Log.d("ChatFragment", "Socket connected, initializing chat");
            isSocketConnected = true;
            // Inicijaliziranje chat-a samo ako nisu već inicijalizirane poruke
            if (!isLoadingMessages) {
                initializeExistingChat();
            }        });
    };

    // Kad je socket disconnect-an
    private Emitter.Listener onSocketDisconnect = args -> {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            Log.d("ChatFragment", "Socket disconnected");
            isSocketConnected = false;
            if (isLoadingMessages) {
                showError("Connection lost");
            }
        });
    };

    // Kad je socket connection pogrešna
    private Emitter.Listener onSocketError = args -> {
        if (getActivity() == null) return;

        String errorMsg = args.length > 0 ? args[0].toString() : "Unknown error";
        getActivity().runOnUiThread(() -> {
            Log.e("ChatFragment", "Socket error: " + errorMsg);
            isSocketConnected = false;
            showError("Connection failed");
        });
    };

    private void attemptSend() {
        if (!isSocketConnected || mSocket == null || !mSocket.connected()) {
            Log.w(TAG, "Cannot send message - socket not connected");
            showError("Not connected. Please wait...");
            return;
        }

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
                // Prva poruka - chat će biti kreiran
                obj.put("recipient_id", ownerId);
                Log.d("ChatFragment", "Sending first message, chat will be created");
            }

            mSocket.emit("chat message", obj);
        } catch (JSONException e) {
            Log.e("ChatFragment", "Error creating message JSON", e);
            showError("Failed to send message");

        }
    }

    private void joinChatRoom() {
        if (chatId != null && mSocket != null && mSocket.connected()) {
            Log.d("ChatFragment", "Joining chat room: chat_" + chatId);

            // Server očekuje samo chatId kao parametar
            mSocket.emit("join chat", chatId);

            // Load messages sa kratkom odgovdom nakon join poziva
            new android.os.Handler().postDelayed(() -> {
                loadChatMessages();
            }, 500); // Pauza za server da procesuira join
        } else {
            Log.w("ChatFragment", "Cannot join room - chatId: " + chatId +
                    ", socket connected: " + (mSocket != null && mSocket.connected()));
            if (!isSocketConnected) {
                showError("Not connected to server");
            }
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
            if (!isSocketConnected) {
                showError("Cannot load messages - not connected");
            } else {
                showMessages(); // Prikazivanje praznog chat-a
            }
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
                // Postavljanje chat_id ako je nova poruka kreirala chat
                if (chatId == null && message_chat_id > 0) {
                    chatId = message_chat_id;
                    Log.d(TAG, "Chat ID set to: " + chatId);
                    joinChatRoom(); // Pridruživanje room-u novog chat-a
                }

                // Postavljanje starih poruka vidljivima kad se dobivaju nove poruke
                showMessages();

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

                // Dodavanje svih poruka iz povijesti razgovora
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

                // Prikazivanje poruka kad je povijest očitana
                showMessages();

            } catch (JSONException e) {
                Log.e("ChatFragment", "Error parsing chat history", e);
            }
        });
    };

    private void addMessage(Message message) {
        if (messageAdapter != null) {
            messageAdapter.addMessage(message);

            // Scroll-anje na zadnju poruku
            if (RVMessages != null) {
                RVMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
            }

            // Korištenje pravog imena umjesto "You"/"Other User"
            String senderLabel = message.getSender_id().equals(currentUserId) ? getString(R.string.you) :
                    (message.getSender_name() != null ? message.getSender_name() : getString(R.string.other_user));

            Log.d("ChatFragment", "Message added to UI: " + senderLabel + ": " + message.getContent());
        }
    }

    // Funkcija za čišćenje socket-a (ista kao u ConversationsFragment)
    private void cleanupSocket() {
        if (mSocket != null) {
            Log.d(TAG, "Cleaning up socket");
            mSocket.disconnect();
            mSocket.off("chat message", onNewMessage);
            mSocket.off("chat history", onChatHistory);
            mSocket.off(Socket.EVENT_CONNECT, onSocketConnect);
            mSocket.off(Socket.EVENT_DISCONNECT, onSocketDisconnect);
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onSocketError);
            mSocket = null;
        }
        isSocketInitialized = false;
        isSocketConnected = false;
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
            mSocket.off(Socket.EVENT_DISCONNECT, onSocketDisconnect);
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onSocketError);
        }
    }
}