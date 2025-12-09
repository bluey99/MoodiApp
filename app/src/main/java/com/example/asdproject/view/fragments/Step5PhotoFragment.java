package com.example.asdproject.view.fragments;

import static android.Manifest.permission.CAMERA;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.asdproject.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;

/**
 * Step 5 – Optional photo capture.
 * Child may take a picture, or skip directly.
 */
public class Step5PhotoFragment extends Fragment {

    public interface Listener {
        void onPhotoCaptured(String photoUrl); // may be null
    }

    private static final String ARG_CHILD_ID = "childId";

    private Listener listener;
    private String childId;

    // UI
    private View cameraCard;
    private ImageView imgPreview;
    private Button btnRetake;
    private Button btnContinue;
    private ProgressBar progressBar;

    // State
    private String uploadedPhotoUrl;

    // Camera launcher
    private final ActivityResultLauncher<Void> takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicturePreview(),
                    this::handleCameraResult
            );

    // Permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            launchCamera();
                        } else {
                            Toast.makeText(getContext(),
                                    "Camera permission denied",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    /** Factory */
    public static Step5PhotoFragment newInstance(String childId) {
        Step5PhotoFragment fragment = new Step5PhotoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD_ID, childId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new IllegalStateException("Parent must implement Listener");
        }
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step5_photo, container, false);

        cameraCard = view.findViewById(R.id.cameraCard);
        imgPreview = view.findViewById(R.id.imgPhotoPreview);
        btnRetake = view.findViewById(R.id.btnRetakePhoto);
        btnContinue = view.findViewById(R.id.btnPhotoContinue);
        progressBar = view.findViewById(R.id.photoUploadProgress);

        // Initial UI state
        imgPreview.setVisibility(View.GONE);
        btnRetake.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        btnContinue.setText("Skip");   // default state → no photo taken yet


        // Continue is always enabled (photo is optional)
        btnContinue.setAlpha(1f);


        // Click events
        cameraCard.setOnClickListener(v -> checkPermissionAndOpenCamera());
        imgPreview.setOnClickListener(v -> checkPermissionAndOpenCamera());
        btnRetake.setOnClickListener(v -> checkPermissionAndOpenCamera());

        btnContinue.setOnClickListener(v -> {
            if (listener != null) {
                // may be null if user skipped photo
                listener.onPhotoCaptured(uploadedPhotoUrl);
            }
        });

        return view;
    }

    /** CAMERA permission logic */
    private void checkPermissionAndOpenCamera() {
        if (getContext() == null) return;

        if (ContextCompat.checkSelfPermission(getContext(), CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(CAMERA);
        }
    }

    private void launchCamera() {
        takePictureLauncher.launch(null);
    }

    /** Camera returned a bitmap */
    private void handleCameraResult(Bitmap bitmap) {
        if (bitmap == null) return;

        cameraCard.setVisibility(View.GONE);
        imgPreview.setVisibility(View.VISIBLE);
        imgPreview.setImageBitmap(bitmap);
        btnRetake.setVisibility(View.VISIBLE);

        uploadPhotoToFirebase(bitmap);
    }

    /** Upload captured photo */
    private void uploadPhotoToFirebase(Bitmap bitmap) {
        if (childId == null || getContext() == null) {
            Toast.makeText(getContext(), "Missing child information", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Convert → JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();

        String fileName = System.currentTimeMillis() + ".jpg";

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("children")
                .child(childId)
                .child("photos")
                .child(fileName);

        ref.putBytes(data)
                .addOnSuccessListener(task ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            uploadedPhotoUrl = uri.toString();
                            progressBar.setVisibility(View.GONE);

                            // Highlight button now that photo exists
                            btnContinue.setAlpha(1f);
                        })
                )
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                });
    }
}
