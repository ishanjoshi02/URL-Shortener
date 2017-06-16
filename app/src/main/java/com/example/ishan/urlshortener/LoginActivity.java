package com.example.ishan.urlshortener;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private final static String TAG = "ClipboardManagerLogin";
    
    private FirebaseAuth mAuth;
    private EditText editTextEmail, editTextPassword;
    Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        mButton = (Button) findViewById(R.id.sign_in_button);
        
        mButton.setOnClickListener(mClickListener);

    }

    private View.OnClickListener mClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = v.getId();
                    switch(id) {
                        case R.id.sign_in_button: {
                            final String email = editTextEmail.getText().toString();
                            final String password = editTextPassword.getText().toString();

                            if (email != null && password != null) {

                                mAuth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                } else {
                                                    mAuth.createUserWithEmailAndPassword(email, password)
                                                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                                    if (task.isSuccessful()) {
                                                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                                    } else {
                                                                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                                                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                                                                Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        });

                            }

                        }
                    }
                }
            };

}
