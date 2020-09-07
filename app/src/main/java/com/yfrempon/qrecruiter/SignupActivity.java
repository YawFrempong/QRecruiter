package com.yfrempon.qrecruiter;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import java.util.ArrayList;

public class SignupActivity extends AppCompatActivity {
    EditText emailId;
    TextInputLayout password;
    Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Sign Up");
        
        //store activity content in variables
        emailId = findViewById(R.id.editText);
        password = findViewById(R.id.editText2);
        btnSignUp = findViewById(R.id.button2);
        
        //handle sign-up for a Firebase account(make sure email and password are valid, encode illegal characters, verify account doesn't already exist)
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = emailId.getText().toString();
                final String pwd = password.getEditText().getText().toString();
                closeKeyboard();
                if(email.isEmpty() && pwd.isEmpty()){
                    Toast.makeText(SignupActivity.this,"Fields Are Empty!",Toast.LENGTH_SHORT).show();
                }
                else if(email.isEmpty()){
                    Toast.makeText(SignupActivity.this,"Please provide an email to sign up.",Toast.LENGTH_SHORT).show();
                    emailId.requestFocus();
                }
                else if(pwd.isEmpty()){
                    Toast.makeText(SignupActivity.this,"Please provide a password to sign up.",Toast.LENGTH_SHORT).show();
                    password.requestFocus();
                }
                else if(!(email.isEmpty() && pwd.isEmpty())){
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()){
                                try {
                                    throw task.getException();
                                } catch(FirebaseAuthWeakPasswordException e) {
                                    Toast.makeText(SignupActivity.this,"Password must be at least 6 characters.",Toast.LENGTH_SHORT).show();
                                } catch(FirebaseAuthInvalidCredentialsException e) {
                                    Toast.makeText(SignupActivity.this,"Invalid email.",Toast.LENGTH_SHORT).show();
                                } catch(FirebaseAuthUserCollisionException e) {
                                    Toast.makeText(SignupActivity.this,"Account already exist.",Toast.LENGTH_SHORT).show();
                                } catch(Exception e) {
                                    Toast.makeText(SignupActivity.this,"Failed to create account.",Toast.LENGTH_SHORT).show();
                                }
                            }
                            else {
                                ArrayList<String> init_arr = new ArrayList<>();
                                init_arr.add("link~~name~~starred");
                                LoginActivity.rootRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("default_event").setValue(init_arr)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            String clean_email = FirebaseAuth.getInstance().getCurrentUser().getEmail().replaceAll("\\.","7702910").replaceAll("\\#","6839189").replaceAll("\\$","5073014").replaceAll("\\[","3839443").replaceAll("\\]","6029018").replaceAll("/","2528736");
                                            LoginActivity.emailRef.child(clean_email).setValue("Firebase");
                                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                            emailId.getText().clear();
                                            password.getEditText().getText().clear();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            emailId.getText().clear();
                                            password.getEditText().getText().clear();
                                            Toast.makeText(SignupActivity.this,"Error occurred during account creation.",Toast.LENGTH_SHORT).show();
                                        }
                                    });
                            }
                        }
                    });
                }
                else {
                    Toast.makeText(SignupActivity.this,"Error occurred!",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    //custom function for closing the keyboard
    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if(view != null){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    //back button on toolbar pressed
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
