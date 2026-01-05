package com.example.asdproject.model;


import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class AuthRepository {

    private final FirebaseAuth auth;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
    }

    public void login(String email,
                      String password,
                      @NonNull OnCompleteListener<AuthResult> listener) {

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }
}
