package com.example.knjigomatpis.ui.conversations;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        // Inicijaliziranje socket-a
        try {
            mSocket = IO.socket(getString(R.string.base_chat_url));
            Log.d(TAG, "Socket initialized successfully");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
        }

        // Provjera je li MainActivity dostupan i ima li cached profil
        if (getActivity() instanceof MainActivity) {
            userProfile = ((MainActivity) getActivity()).cachedUserProfile;
            if (userProfile != null) {
                Log.d(TAG, "User profile loaded: " + userProfile.getId());
            } else {
                Log.w(TAG, getString(R.string.error_user_profile_null));
            }
        } else {
            Log.e(TAG,  getString(R.string.error_activity_not_mainactivity));
        }

        // Postavljanje socket listener-a
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on("user chats", onUserChats);

        // Povezivanje socket
        mSocket.connect();
        rootView = inflater.inflate(R.layout.fragment_conversations, container, false);

        recyclerViewChats = rootView.findViewById(R.id.recyclerViewChats);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicijaliziranje adapter-a s callback funkcijom za klik na chat
        adapter = new ConversationsAdapter(chatList, this::openChat);
        recyclerViewChats.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        // Traženje svih chat-ova korisnika kada je view spreman
        loadUserChats();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (mSocket != null && mSocket.connected()) {
            loadUserChats();
            return;
        }

        try {
            mSocket = IO.socket(getString(R.string.base_chat_url));
            Log.d(TAG, "Socket initialized successfully");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
            return;
        }

        if (getActivity() instanceof MainActivity) {
            userProfile = ((MainActivity) getActivity()).cachedUserProfile;
            if (userProfile != null) {
                Log.d(TAG, "User profile loaded: " + userProfile.getId());
            } else {
                Log.w(TAG, getString(R.string.error_user_profile_null));
            }
        } else {
            Log.e(TAG, getString(R.string.error_activity_not_mainactivity));
        }

        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on("user chats", onUserChats);

        mSocket.connect();

        loadUserChats();
    }

    private void loadUserChats() {
        Log.d(TAG, "loadUserChats called");

        if (userProfile != null && mSocket != null) {
            if (mSocket.connected()) {
                Log.d(TAG, "Socket connected, requesting user chats");
                JSONObject request = new JSONObject();
                try {
                    request.put("user_id", userProfile.getId());
                    mSocket.emit("get user chats", request);
                    Log.d(TAG, "Emitted 'get user chats' with user_id: " + userProfile.getId());
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating request", e);
                }
            } else {
                Log.w(TAG, getString(R.string.error_socket_not_connected));
                mSocket.connect();
            }
        } else {
            Log.w(TAG, getString(R.string.error_cannot_load_chats) + " - userProfile: " + userProfile + ", socket: " + mSocket);
        }
    }

    // Socket event listeners
    private Emitter.Listener onConnect = args -> {
        Log.d(TAG, "Socket connected");
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                loadUserChats();
            });
        }
    };

    private Emitter.Listener onDisconnect = args -> {
        Log.d(TAG, "Socket disconnected");
    };

    private Emitter.Listener onConnectError = args -> {
        Log.e(TAG, "Socket connection error: " + (args.length > 0 ? args[0].toString() : "Unknown error"));
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

                    // Postavljanje imena iz server response-a ili fallback
                    chat.setPerson2Name(chatObj.optString("other_user_name", getString(R.string.default_user_name) + otherPersonId));
                    chat.setLastMessage(chatObj.optString("last_message", ""));
                    chat.setLastMessageTime(chatObj.optString("last_message_time", ""));

                    chatList.add(chat);
                    Log.d(TAG, "Added chat: " + chat.getChat_id() + " with " + chat.getPerson2Name());
                }

                // Ažuriranje adapter-a
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Adapter updated with " + chatList.size() + " chats");

                if (chatList != null && !chatList.isEmpty()) {
                    recyclerViewChats.setVisibility(View.VISIBLE);
                    TextView noChatsView = rootView.findViewById(R.id.tvNoConversations);
                    if (noChatsView != null) {
                        noChatsView.setVisibility(View.GONE);
                    }
                } else {
                    recyclerViewChats.setVisibility(View.GONE);
                    TextView noChatsView = rootView.findViewById(R.id.tvNoConversations);
                    if (noChatsView != null) {
                        noChatsView.setVisibility(View.VISIBLE);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing chat data", e);
            }
        });
    };

    // Funkcija za otvaranje chat-a
    private void openChat(Chat chat) {
        Log.d(TAG, "Opening chat: " + chat.getChat_id() + " with " + chat.getPerson2Name());

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();

            // Kreiranje Bundle-a s podacima o chat-u
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
                Log.e(TAG, getString(R.string.error_navigation_failed) + ": "+ e.getMessage());

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
            Log.e(TAG,  getString(R.string.error_cannot_navigate));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");

        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off(Socket.EVENT_CONNECT, onConnect);
            mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.off("user chats", onUserChats);
        }
    }
}

