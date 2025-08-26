package com.example.knjigomatpis.ui.notifications;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.knjigomatpis.MainActivity;
import com.example.knjigomatpis.adapters.NotificationAdapter;
import com.example.knjigomatpis.databinding.FragmentNotificationsBinding;
import com.example.knjigomatpis.models.UserNotification;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private NotificationsViewModel viewModel;


    public static NotificationsFragment newInstance() {
        return new NotificationsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null && mainActivity.cachedUserProfile != null) {
            String userId = mainActivity.cachedUserProfile.getId();
            viewModel.setUserId(userId);
        }

        RecyclerView recyclerView = binding.notificationsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications != null && !notifications.isEmpty()) {
                NotificationAdapter adapter = new NotificationAdapter(notifications, this::onNotificationClick);
                recyclerView.setAdapter(adapter);
                recyclerView.setVisibility(View.VISIBLE);
                binding.tvNoNotifications.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                binding.tvNoNotifications.setVisibility(View.VISIBLE);
            }
        });

        return binding.getRoot();
    }

    public void refreshNotifications() {
        if (viewModel != null) {
            viewModel.fetchNotifications();
        }
    }

    public void markAllAsRead() {
        if (viewModel != null) {
            viewModel.markAllAsRead(); // Koristi currentUserId iz ViewModel-a
        }
    }
    private void onNotificationClick(UserNotification notification) {

        // Označava notifikaciju kao pročitanu kad se klikne
        if (!notification.isRead()) {
            viewModel.markAsRead(notification.getNotification_id());
        }
        if(notification.getChat_id() == -1 || notification.getChat_id() == 0) return;

        ((MainActivity) getActivity()).navigateToChatFromNotification(notification);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
