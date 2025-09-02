package com.example.knjigomatpis.ui.conversations;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.auth0.android.result.UserProfile;
import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.R;
import com.example.knjigomatpis.adapters.ConversationsAdapter;
import com.example.knjigomatpis.models.Chat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ConversationsFragment extends Fragment {

    private static final String TAG = "ConversationsFragment";

    private RecyclerView recyclerViewChats;
    private ConversationsAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();
    private Socket mSocket;
    private UserProfile userProfile;
    private View rootView;

    // Loading state komponente
    private ProgressBar progressBarLoading;
    private TextView tvNoConversations;
    private boolean isLoadingChats = false;
    private boolean isSocketInitialized = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        rootView = inflater.inflate(R.layout.fragment_conversations, container, false);

        // Inicijaliziranje views
        recyclerViewChats = rootView.findViewById(R.id.recyclerViewChats);
        progressBarLoading = rootView.findViewById(R.id.progressBarLoading);
        tvNoConversations = rootView.findViewById(R.id.tvNoConversations);

        recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicijaliziranje adaptera sa callback funkcijama za chat click
        adapter = new ConversationsAdapter(chatList, this::openChat);
        recyclerViewChats.setAdapter(adapter);

        tvNoConversations.setVisibility(View.VISIBLE);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        if (!isSocketInitialized) {
            initializeSocket();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        if (mSocket != null && mSocket.connected() && userProfile != null) {
            loadUserChats();
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
            if (mSocket != null) {
                cleanupSocket();
            }
            IO.Options opts = new IO.Options();
            opts.transports = new String[]{"websocket"};
            mSocket = IO.socket(getString(R.string.base_chat_url), opts);
            Log.d(TAG, "Socket initialized successfully");
            isSocketInitialized = true;
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
            return;
        }

        // Provjera da li je MainActivity available i da li je user profile
        if (getActivity() instanceof MainActivity) {
            userProfile = ((MainActivity) getActivity()).cachedUserProfile;
            if (userProfile != null) {
                Log.d(TAG, "User profile loaded: " + userProfile.getId());
            } else {
                Log.w(TAG, getString(R.string.error_user_profile_null));
                showError("User profile not available");
                return;
            }
        } else {
            Log.e(TAG, getString(R.string.error_activity_not_mainactivity));
            showError("Activity error");
            return;
        }

        // Postavljanje socket listeners-a
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on("user chats", onUserChats);

        // Povezivanje socket
        mSocket.connect();
    }

    private void loadUserChats() {
        Log.d(TAG, "loadUserChats called");

        if (isLoadingChats) {
            Log.d(TAG, "Already loading chats, skipping");
            return;
        }

        if (userProfile != null && mSocket != null) {
            if (mSocket.connected()) {
                showLoading(true);
                Log.d(TAG, "Socket connected, requesting user chats");
                JSONObject request = new JSONObject();
                try {
                    request.put("user_id", userProfile.getId());
                    mSocket.emit("get user chats", request);
                    Log.d(TAG, "Emitted 'get user chats' with user_id: " + userProfile.getId());
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating request", e);
                    showError("Failed to request chats");
                    showLoading(false);
                }
            } else {
                Log.w(TAG, getString(R.string.error_socket_not_connected));
                showError("Connection not available");
            }
        } else {
            Log.w(TAG, getString(R.string.error_cannot_load_chats) + " - userProfile: " + userProfile + ", socket: " + mSocket);
            showError("Cannot load chats - missing requirements");
        }
    }

    private void showLoading(boolean show) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            isLoadingChats = show;
            if (progressBarLoading != null) {
                progressBarLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (recyclerViewChats != null) {
                recyclerViewChats.setVisibility(show ? View.GONE : View.VISIBLE);
            }
//            if (tvNoConversations != null) {
//                tvNoConversations.setVisibility(View.GONE);
//            }
        });
    }

    private void showError(String errorMessage) {
        if (getActivity() == null) return;

        Log.e(TAG, "Showing error: " + errorMessage);
        getActivity().runOnUiThread(() -> {
            showLoading(false);
            if (tvNoConversations != null) {
                tvNoConversations.setVisibility(View.GONE);
            }
            if (recyclerViewChats != null) {
                recyclerViewChats.setVisibility(View.GONE);
            }
        });
    }

    private void showChats() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            showLoading(false);

            if (chatList != null && !chatList.isEmpty()) {
                // Ima razgovora - prikaži recycler view
                if (recyclerViewChats != null) {
                    recyclerViewChats.setVisibility(View.VISIBLE);
                }
                if (tvNoConversations != null) {
                    tvNoConversations.setVisibility(View.GONE);
                }
            }
            else {
                // Nema razgovora - prikaži poruku
                if (recyclerViewChats != null) {
                    recyclerViewChats.setVisibility(View.GONE);
                }
                if (tvNoConversations != null) {
                    tvNoConversations.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // Socket event listeners
    private Emitter.Listener onConnect = args -> {
        Log.d(TAG, "Socket connected");
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!isLoadingChats && (chatList == null || chatList.isEmpty())) {
                    loadUserChats();
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = args -> {
        Log.d(TAG, "Socket disconnected: " + (args.length > 0 ? args[0].toString() : "unknown reason"));

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showLoading(false);
            });
        }
    };

    private Emitter.Listener onConnectError = args -> {
        String errorMsg = args.length > 0 ? args[0].toString() : "Unknown error";
        Log.e(TAG, "Socket connection error: " + errorMsg);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showError("Connection error: " + errorMsg);
            });
        }
    };

    private Emitter.Listener onUserChats = args -> {
        Log.d(TAG, "Received 'user chats' event");

        if (getActivity() == null) {
            Log.w(TAG, getString(R.string.error_activity_null));
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                JSONArray chatsArray = (JSONArray) args[0];
                Log.d(TAG, "Received " + chatsArray.length() + " chats");

                chatList.clear();

                for (int i = 0; i < chatsArray.length(); i++) {
                    JSONObject chatObj = chatsArray.getJSONObject(i);

                    Chat chat = new Chat();
                    chat.setChat_id(chatObj.getInt("chat_id"));
                    chat.setPerson1_id(chatObj.getString("person1_id"));
                    chat.setPerson2_id(chatObj.getString("person2_id"));

                    String otherPersonId = chatObj.getString("other_person_id");
                    chat.setPerson2_id(otherPersonId);

                    // Postavljanje imena sa server response ili fallback
                    chat.setPerson2Name(chatObj.optString("other_user_name", getString(R.string.default_user_name) + otherPersonId));
                    chat.setLastMessage(chatObj.optString("last_message", ""));
                    chat.setLastMessageTime(chatObj.optString("last_message_time", ""));

                    chatList.add(chat);
                    Log.d(TAG, "Added chat: " + chat.getChat_id() + " with " + chat.getPerson2Name());
                }

                // Ažuriranje adaptera
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Adapter updated with " + chatList.size() + " chats");

                // Prikazivanje odgovarajućeg view-a
                showChats();

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing chat data", e);
                showError("Failed to load chats");
            }
        });
    };

    // Funkcija za otvaranje chat-a
    private void openChat(Chat chat) {
        Log.d(TAG, "Opening chat: " + chat.getChat_id() + " with " + chat.getPerson2Name());

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();

            // Kreiranje Bundle sa chat data
            Bundle args = new Bundle();
            args.putInt("chat_id", chat.getChat_id());

            String currentUserId = userProfile.getId();
            String otherUserId = chat.getPerson2_id();

            // Svi potrebni parametri za ChatFragment
            args.putString("owner_id", otherUserId);
            args.putString("current_user_id", currentUserId);
            args.putString("other_user_name", chat.getPerson2Name());
            args.putString("book_title", "Chat");
            args.putLong("book_id", -1L);

            Log.d(TAG, "Opening chat with args: chatId=" + chat.getChat_id() +
                    ", currentUser=" + currentUserId + ", otherUser=" + otherUserId);

            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

            try {
                navController.navigate(R.id.nav_chat_single, args);
                Log.d(TAG, "Successfully navigated to ChatFragment using NavController");
            } catch (Exception e) {
                Log.e(TAG, getString(R.string.error_navigation_failed) + ": " + e.getMessage());

                ChatFragment chatFragment = new ChatFragment();
                chatFragment.setArguments(args);

                mainActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment_content_main, chatFragment)
                        .addToBackStack(null)
                        .commit();

                Log.d(TAG, "Used FragmentTransaction as fallback");
            }
        } else {
            Log.e(TAG, getString(R.string.error_cannot_navigate));
        }
    }

    private void cleanupSocket() {
        if (mSocket != null) {
            Log.d(TAG, "Cleaning up socket");
            mSocket.disconnect();
            mSocket.off(Socket.EVENT_CONNECT, onConnect);
            mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.off("user chats", onUserChats);
            mSocket = null;
        }
        isSocketInitialized = false;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");

        cleanupSocket();
    }
}