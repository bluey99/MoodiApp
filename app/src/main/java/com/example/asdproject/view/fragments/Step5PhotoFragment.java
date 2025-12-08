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
 * Step 5 of the child emotion-logging flow.
 * The child takes a photo of their face to document the feeling.
 * The captured image is uploaded to Firebase Storage and the download URL
 * is returned to the hosting activity.
 */
public class Step5PhotoFragment extends Fragment {

    /** Callback to the hosting activity with the uploaded photo URL. */
    public interface Listener {
        void onPhotoCaptured(String photoUrl);
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

    // Launcher for camera preview (returns a Bitmap)
    private final ActivityResultLauncher<Void> takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicturePreview(),
                    this::handleCameraResult
            );

    // Launcher for runtime CAMERA permission
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            launchCamera();
                        } else {
                            Toast.makeText(getContext(),
                                    "Camera permission is required to take a photo",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    /** Factory method to create this fragment with the childId argument. */
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
            throw new IllegalStateException("Parent must implement Step5PhotoFragment.Listener");
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

        // Initial state
        imgPreview.setVisibility(View.GONE);
        btnRetake.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        btnContinue.setEnabled(false);
        btnContinue.setAlpha(0.5f);

        // Tap to start camera
        cameraCard.setOnClickListener(v -> checkPermissionAndOpenCamera());
        imgPreview.setOnClickListener(v -> checkPermissionAndOpenCamera());
        btnRetake.setOnClickListener(v -> checkPermissionAndOpenCamera());

        btnContinue.setOnClickListener(v -> {
            if (uploadedPhotoUrl == null) {
                Toast.makeText(getContext(),
                        "Please wait for the upload to finish",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) {
                listener.onPhotoCaptured(uploadedPhotoUrl);
            }
        });

        return view;
    }

    /** Ensures CAMERA permission is granted before launching the camera. */
    private void checkPermissionAndOpenCamera() {
        if (getContext() == null) return;

        if (ContextCompat.checkSelfPermission(getContext(), CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(CAMERA);
        }
    }

    /** Launches the camera using TakePicturePreview (returns a Bitmap only). */
    private void launchCamera() {
        takePictureLauncher.launch(null);
    }

    /** Called when a Bitmap result is returned from the camera. */
    private void handleCameraResult(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        // Update UI to show preview
        cameraCard.setVisibility(View.GONE);
        imgPreview.setVisibility(View.VISIBLE);
        imgPreview.setImageBitmap(bitmap);
        btnRetake.setVisibility(View.VISIBLE);

        // Start upload to Firebase Storage
        uploadPhotoToFirebase(bitmap);
    }

    /** Uploads the captured Bitmap to Firebase Storage and stores the download URL. */
    private void uploadPhotoToFirebase(Bitmap bitmap) {
        if (childId == null || getContext() == null) {
            Toast.makeText(getContext(),
                    "Missing child information",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnContinue.setEnabled(false);
        btnContinue.setAlpha(0.5f);

        // Convert bitmap to JPEG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();

        // children/{childId}/photos/{timestamp}.jpg
        String fileName = System.currentTimeMillis() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("children")
                .child(childId)
                .child("photos")
                .child(fileName);

        ref.putBytes(data)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            uploadedPhotoUrl = uri.toString();
                            progressBar.setVisibility(View.GONE);
                            btnContinue.setEnabled(true);
                            btnContinue.setAlpha(1f);
                        }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                            "Upload failed. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }
}
