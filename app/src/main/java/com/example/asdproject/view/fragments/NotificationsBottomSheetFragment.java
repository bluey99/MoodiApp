package com.example.asdproject.view.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.asdproject.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom sheet used to display child in-app notifications.
 * Notifications are independent of task visibility.
 */
public class NotificationsBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String NOTIF_DBG = "NOTIF_DBG";
    private static final String ARG_CHILD_ID = "childId";

    private String childId;

    // ==============================
    // DISMISS CALLBACK
    // ==============================
    public interface OnNotificationsDismissed {
        void onDismissed();
    }

    private OnNotificationsDismissed dismissListener;

    public void setOnNotificationsDismissed(OnNotificationsDismissed listener) {
        this.dismissListener = listener;
    }

    // ==============================
    // FACTORY
    // ==============================
    public static NotificationsBottomSheetFragment newInstance(String childId) {
        NotificationsBottomSheetFragment fragment = new NotificationsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD_ID, childId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            childId = getArguments().getString(ARG_CHILD_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_notifications_bottom_sheet,
                container,
                false
        );

        loadNotifications(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    // ==============================
    // LOAD NOTIFICATIONS
    // ==============================
    private void loadNotifications(View root) {
        Log.d(NOTIF_DBG, "loadNotifications() childId=" + childId);

        LinearLayout layoutNotifications = root.findViewById(R.id.layoutNotifications);
        TextView txtEmpty = root.findViewById(R.id.txtEmpty);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tasks")
                .whereEqualTo("childId", childId)
                .whereEqualTo("status", "ASSIGNED")
                .get()
                .addOnSuccessListener(snapshot1 -> {

                    List<DocumentSnapshot> unseen = extractUnseen(snapshot1.getDocuments());

                    if (!unseen.isEmpty()) {
                        showNotifications(unseen, layoutNotifications, txtEmpty);
                        return;
                    }

                    // fallback for childID
                    db.collection("tasks")
                            .whereEqualTo("childID", childId)
                            .whereEqualTo("status", "ASSIGNED")
                            .get()
                            .addOnSuccessListener(snapshot2 -> {

                                List<DocumentSnapshot> unseenFallback =
                                        extractUnseen(snapshot2.getDocuments());

                                if (unseenFallback.isEmpty()) {
                                    txtEmpty.setVisibility(View.VISIBLE);
                                } else {
                                    showNotifications(unseenFallback, layoutNotifications, txtEmpty);
                                }
                            });
                });
    }

    // ==============================
    // FILTER UNSEEN (SAFE)
    // ==============================
    private List<DocumentSnapshot> extractUnseen(List<DocumentSnapshot> docs) {
        List<DocumentSnapshot> result = new ArrayList<>();

        for (DocumentSnapshot doc : docs) {
            if (!Boolean.TRUE.equals(doc.getBoolean("seenByChild"))) {
                result.add(doc);
            }
        }
        return result;
    }

    // ==============================
    // RENDER NOTIFICATIONS
    // ==============================
    private void showNotifications(
            List<DocumentSnapshot> docs,
            LinearLayout layoutNotifications,
            TextView txtEmpty) {

        txtEmpty.setVisibility(View.GONE);
        layoutNotifications.removeAllViews();

        for (DocumentSnapshot doc : docs) {

            String creatorType = doc.getString("creatorType");

            Log.d(NOTIF_DBG,
                    "Notification task id=" + doc.getId() +
                            " creatorType=" + creatorType
            );

            TextView item = new TextView(requireContext());
            item.setTextSize(15f);
            item.setPadding(0, 16, 0, 16);

            if ("PARENT".equals(creatorType)) {
                item.setText("👩 Mom added a task");
            } else if ("THERAPIST".equals(creatorType)) {
                item.setText("🧑‍⚕️ Therapist added a task");
            } else {
                item.setText("📌 New task");
            }

            layoutNotifications.addView(item);
        }

        markNotificationsAsSeen(docs);
    }

    // ==============================
    // MARK AS SEEN (NOTIFICATION-BOUND)
    // ==============================
    private void markNotificationsAsSeen(List<DocumentSnapshot> docs) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (DocumentSnapshot doc : docs) {
            db.collection("tasks")
                    .document(doc.getId())
                    .update("seenByChild", true);
        }
    }

    // ==============================
    // DISMISS HOOK
    // ==============================
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (dismissListener != null) {
            dismissListener.onDismissed();
        }
    }
}
