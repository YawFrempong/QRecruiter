package com.yfrempon.qrecruiter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.SessionManager;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import java.util.ArrayList;
import java.util.Map;

//Email addresses need to be encoded since Firebase Realtime Database won't allow the following characters to be stored as keys in the database
/*
'.' -> 7702910
'#' -> 6839189
'$' -> 5073014
'[' -> 3839443
']' -> 6029018
'/' -> 2528736
 */

public class LoginActivity extends AppCompatActivity {
    EditText emailId;
    TextInputLayout password;
    TextInputEditText password_raw;
    public static Button btnSignIn;
    public static TextView tvSignUp;
    public static TextView tvForgot;
    public static SignInButton btnSignInGoogle;
    public static LoginButton btnSignInFacebook;
    public static TwitterLoginButton mTwitterBtn;
    CallbackManager mCallbackManager_Facebook;
    public static DatabaseReference databaseRef;
    public static DatabaseReference rootRef;
    public static DatabaseReference emailRef;
    public static GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 123;
    boolean facebookButtonClicked = false;
    boolean googleButtonClicked = false;
    boolean twitterButtonClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //This code must be entering before the setContentView function to make the twitter login work(according to Twitter API documentation)
        //Initialize Twitter Login
        TwitterAuthConfig mTwitterAuthConfig = new TwitterAuthConfig(getString(R.string.twitter_consumer_key), getString(R.string.twitter_consumer_secret));
        TwitterConfig twitterConfig = new TwitterConfig.Builder(this)
            .twitterAuthConfig(mTwitterAuthConfig)
            .build();
        Twitter.initialize(twitterConfig);
        SessionManager<TwitterSession> sessionManager = TwitterCore.getInstance().getSessionManager();
        
        if (sessionManager.getActiveSession() != null){
            sessionManager.clearActiveSession();
        }
        mTwitterBtn = findViewById(R.id.sign_in_button_twitter);
        
        //store activity content in variables
        setContentView(R.layout.activity_login);
        emailId = findViewById(R.id.editText);
        password = findViewById(R.id.editText2);
        password_raw = findViewById(R.id.password_raw);
        btnSignIn = findViewById(R.id.button2);
        tvSignUp = findViewById(R.id.textView);
        tvForgot = findViewById(R.id.textView2);
        
        //initialize database reference and enable persistence for offline mode(update database when reconnecting if internet connect was lost)
        if(databaseRef == null){
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);
            databaseRef = database.getReference();
        }
        rootRef = databaseRef.child("users");
        emailRef = databaseRef.child("emails");
        
        //initialize Google login
        createGoogleSignIn();
        btnSignInGoogle = findViewById(R.id.sign_in_button_google);
        btnSignInGoogle.setSize(SignInButton.SIZE_WIDE);
        
        //initialize Facebook login
        FacebookSdk.getApplicationContext();
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        mCallbackManager_Facebook = CallbackManager.Factory.create();
        btnSignInFacebook = findViewById(R.id.sign_in_button_facebook);
        btnSignInFacebook.setReadPermissions("email","public_profile");
        
        //make sure all accounts are logged out when app loads
        mGoogleSignInClient.signOut();
        LoginManager.getInstance().logOut();
        FirebaseAuth.getInstance().signOut();
        
        //login through Firebase Authentication
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailId.getText().toString();
                String pwd = password.getEditText().getText().toString();
                closeKeyboard();
                
                //handle login through Firebase Authentication 
                if(email.isEmpty() && pwd.isEmpty()){
                    Toast.makeText(LoginActivity.this,"Fields Are Empty!",Toast.LENGTH_SHORT).show();
                }
                else if(email.isEmpty()){
                    Toast.makeText(LoginActivity.this,"Please provide an email to login.",Toast.LENGTH_SHORT).show();
                    emailId.requestFocus();
                }
                else if(pwd.isEmpty()){
                    Toast.makeText(LoginActivity.this,"Please provide a password to login.",Toast.LENGTH_SHORT).show();
                    password.requestFocus();
                }
                else if(!(email.isEmpty() && pwd.isEmpty())){
                    btnSignIn.setEnabled(false);
                    tvSignUp.setEnabled(false);
                    tvForgot.setEnabled(false);
                    btnSignInGoogle.setEnabled(false);
                    btnSignInFacebook.setEnabled(false);
                    mTwitterBtn.setEnabled(false);
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pwd).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()){
                                btnSignIn.setEnabled(true);
                                tvSignUp.setEnabled(true);
                                tvForgot.setEnabled(true);
                                btnSignInGoogle.setEnabled(true);
                                btnSignInFacebook.setEnabled(true);
                                mTwitterBtn.setEnabled(true);
                                Toast.makeText(LoginActivity.this,"Incorrect Email and/or Password.",Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Intent intToHome = new Intent(LoginActivity.this, ListActivity.class);
                                startActivity(intToHome);
                                emailId.getText().clear();
                                password.getEditText().getText().clear();
                            }
                        }
                    });
                }
                else {
                    btnSignIn.setEnabled(true);
                    tvSignUp.setEnabled(true);
                    tvForgot.setEnabled(true);
                    btnSignInGoogle.setEnabled(true);
                    btnSignInFacebook.setEnabled(true);
                    mTwitterBtn.setEnabled(true);
                    Toast.makeText(LoginActivity.this,"Error Occurred During Login!",Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        //sign-up button clicked
        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intSignUp = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intSignUp);
                emailId.getText().clear();
                password.getEditText().getText().clear();
            }
        });
        
        //forgot password button clicked(3rd party accounts must reset password through their provider -> Facebook, Google, Twitter)
        tvForgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotDialog();
            }
        });
        
        //sign-in with Google button clicked
        btnSignInGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleButtonClicked = true;
                btnSignIn.setEnabled(false);
                tvSignUp.setEnabled(false);
                tvForgot.setEnabled(false);
                btnSignInGoogle.setEnabled(false);
                btnSignInFacebook.setEnabled(false);
                mTwitterBtn.setEnabled(false);
                signInGoogle();
            }
        });
        
        //sign-in with Facebook button clicked
        btnSignInFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookButtonClicked = true;
                btnSignIn.setEnabled(false);
                tvSignUp.setEnabled(false);
                tvForgot.setEnabled(false);
                btnSignInGoogle.setEnabled(false);
                btnSignInFacebook.setEnabled(false);
                mTwitterBtn.setEnabled(false);
            }
        });
        
        //sign-in with Twitter button clicked
        mTwitterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                twitterButtonClicked = true;
                btnSignIn.setEnabled(false);
                tvSignUp.setEnabled(false);
                tvForgot.setEnabled(false);
                btnSignInGoogle.setEnabled(false);
                btnSignInFacebook.setEnabled(false);
                mTwitterBtn.setEnabled(false);
            }
        });
        
        //Facebook callback function
        btnSignInFacebook.registerCallback(mCallbackManager_Facebook, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                signInFacebook(loginResult.getAccessToken());
            }
            @Override
            public void onCancel() {
                btnSignIn.setEnabled(true);
                tvSignUp.setEnabled(true);
                tvForgot.setEnabled(true);
                btnSignInGoogle.setEnabled(true);
                btnSignInFacebook.setEnabled(true);
                mTwitterBtn.setEnabled(true);
            }
            @Override
            public void onError(FacebookException error) {
                Toast.makeText(LoginActivity.this,error.getMessage(),Toast.LENGTH_SHORT).show();
                btnSignIn.setEnabled(true);
                tvSignUp.setEnabled(true);
                tvForgot.setEnabled(true);
                btnSignInGoogle.setEnabled(true);
                btnSignInFacebook.setEnabled(true);
                mTwitterBtn.setEnabled(true);
            }
        });
        
        //Twitter callback function
        mTwitterBtn.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                signInToFirebaseWithTwitterSession(result.data);
            }

            @Override
            public void failure(TwitterException exception) {
                btnSignIn.setEnabled(true);
                tvSignUp.setEnabled(true);
                tvForgot.setEnabled(true);
                btnSignInGoogle.setEnabled(true);
                btnSignInFacebook.setEnabled(true);
                mTwitterBtn.setEnabled(true);
            }
        });
    }
    
    //restart login UI when the login activity is resumed
    @Override
    protected void onResume(){
        super.onResume();
        password.setPasswordVisibilityToggleEnabled(false);
        password.setPasswordVisibilityToggleEnabled(true);
        emailId.setFocusable(false);
        password.setFocusable(false);
        password_raw.setFocusable(false);
        mGoogleSignInClient.signOut();
        LoginManager.getInstance().logOut();
        FirebaseAuth.getInstance().signOut();

        emailId.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                emailId.setFocusableInTouchMode(true);
                return false;
            }
        });
        password.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                password.setFocusableInTouchMode(true);
                return false;
            }
        });
        password_raw.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                password_raw.setFocusableInTouchMode(true);
                return false;
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
    
    //handle text input for provided email during password recovery
    private void showForgotDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recover Password");
        LinearLayout linearLayout = new LinearLayout(this);
        final EditText emailEt = new EditText(this);
        emailEt.setHint("Email");
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEt.setMinEms(16);
        linearLayout.addView(emailEt);
        linearLayout.setPadding(10,10,10,10);
        builder.setView(linearLayout);

        builder.setPositiveButton("Send Recovery Email", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String forgot_email = emailEt.getText().toString().trim();
                beginRecovery(forgot_email);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        emailEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });
    }
    
    //handle recovery(encode illegal characters, verify that the provided email exist in our database, and make sure it is not a 3rd party account)
    private void beginRecovery(final String emailInput){
        final String cleanEmailInput = emailInput.replaceAll("\\.","7702910").replaceAll("\\#","6839189").replaceAll("\\$","5073014").replaceAll("\\[","3839443").replaceAll("\\]","6029018").replaceAll("/","2528736");
        emailRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> emailList;
                emailList = (Map<String, Object>) dataSnapshot.getValue();
                
                if(emailList == null){
                    beginRecovery2(emailInput);
                } else {
                    if(emailList.containsKey(cleanEmailInput)){
                        String email_provider = emailList.get(cleanEmailInput).toString();
                        
                        if(email_provider.equals("Firebase")){
                            beginRecovery2(emailInput);
                        } else {
                            Toast.makeText(LoginActivity.this,"This email is linked to a 3rd party provider(Facebook, Google, or Twitter). Request a password reset through them.",Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        beginRecovery2(emailInput);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }
    
    //send recovery info to provided email
    private void beginRecovery2(String emailInput){
        FirebaseAuth.getInstance().sendPasswordResetEmail(emailInput).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Email sent.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(LoginActivity.this, "Email failed to send.", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(LoginActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    //start Google sign-in process
    private void createGoogleSignIn(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    
    //go to google sign-in page
    private void signInGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
   
    //attempt to login with an existing google account
    //Note 1: Google accounts have priority over Firebase accounts that have been created with a gmail address
    //Note 2: We keep track of emails to prevent Google accounts from overriding Firebase accounts that have been created with a gmail address
    private void firebaseAuthWithGoogle(String idToken, final String account_email) {
        final AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        emailRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> emailList;
                emailList = (Map<String, Object>) dataSnapshot.getValue();
                String clean_email = account_email.replaceAll("\\.","7702910").replaceAll("\\#","6839189").replaceAll("\\$","5073014").replaceAll("\\[","3839443").replaceAll("\\]","6029018").replaceAll("/","2528736");
                if(emailList == null){
                    firebaseAuthWithGoogle2(credential);
                } else {
                    if(emailList.containsKey(clean_email)){
                        String email_provider = emailList.get(clean_email).toString();
                        
                        if(email_provider.equals("Google")){
                            firebaseAuthWithGoogle2(credential);
                        } else {
                            Toast.makeText(LoginActivity.this,"Failed to Sign-In with Google: An account already exists with the same email address but different sign-in credentials. Sign in using a provider associated with this email address.",Toast.LENGTH_SHORT).show();
                            mGoogleSignInClient.signOut();
                            btnSignIn.setEnabled(true);
                            tvSignUp.setEnabled(true);
                            tvForgot.setEnabled(true);
                            btnSignInGoogle.setEnabled(true);
                            btnSignInFacebook.setEnabled(true);
                            mTwitterBtn.setEnabled(true);
                        }
                    } else {
                        firebaseAuthWithGoogle2(credential);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                btnSignIn.setEnabled(true);
                tvSignUp.setEnabled(true);
                tvForgot.setEnabled(true);
                btnSignInGoogle.setEnabled(true);
                btnSignInFacebook.setEnabled(true);
                mTwitterBtn.setEnabled(true);
            }
        });
    }
    
    //attempt to login with a new google account
    private void firebaseAuthWithGoogle2(AuthCredential x){
        FirebaseAuth.getInstance().signInWithCredential(x)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Failed to Sign-In with Google: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        mGoogleSignInClient.signOut();
                        btnSignIn.setEnabled(true);
                        tvSignUp.setEnabled(true);
                        tvForgot.setEnabled(true);
                        btnSignInGoogle.setEnabled(true);
                        btnSignInFacebook.setEnabled(true);
                        mTwitterBtn.setEnabled(true);
                    } else {
                        rootRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.getValue() == null){
                                    initUIDinDatabase();
                                    addToEmailList(FirebaseAuth.getInstance().getCurrentUser().getEmail(),"Google");
                                } else {
                                    Intent intToHome = new Intent(LoginActivity.this, ListActivity.class);
                                    startActivity(intToHome);
                                    emailId.getText().clear();
                                    password.getEditText().getText().clear();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(LoginActivity.this,error.getMessage(),Toast.LENGTH_SHORT).show();
                                btnSignIn.setEnabled(true);
                                tvSignUp.setEnabled(true);
                                tvForgot.setEnabled(true);
                                btnSignInGoogle.setEnabled(true);
                                btnSignInFacebook.setEnabled(true);
                                mTwitterBtn.setEnabled(true);
                            }
                        });
                    }

                }
            });
    }
    
    //attempt to login with facebook account
    private void signInFacebook(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(!task.isSuccessful()){
                    Toast.makeText(LoginActivity.this, "Failed to Sign-In with Facebook: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    LoginManager.getInstance().logOut();
                    btnSignIn.setEnabled(true);
                    tvSignUp.setEnabled(true);
                    tvForgot.setEnabled(true);
                    btnSignInGoogle.setEnabled(true);
                    btnSignInFacebook.setEnabled(true);
                    mTwitterBtn.setEnabled(true);
                } else {
                    rootRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getValue() == null){
                                initUIDinDatabase();
                                addToEmailList(FirebaseAuth.getInstance().getCurrentUser().getEmail(),"Facebook");
                            } else {
                                Intent intToHome = new Intent(LoginActivity.this, ListActivity.class);
                                startActivity(intToHome);
                                emailId.getText().clear();
                                password.getEditText().getText().clear();
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(LoginActivity.this,error.getMessage(),Toast.LENGTH_SHORT).show();
                            btnSignIn.setEnabled(true);
                            tvSignUp.setEnabled(true);
                            tvForgot.setEnabled(true);
                            btnSignInGoogle.setEnabled(true);
                            btnSignInFacebook.setEnabled(true);
                            mTwitterBtn.setEnabled(true);
                        }
                    });

                }
            }
        });
    }
    
    //attempt to login with twitter account
    private void signInToFirebaseWithTwitterSession(TwitterSession session){
        AuthCredential credential = TwitterAuthProvider.getCredential(session.getAuthToken().token, session.getAuthToken().secret);
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()){
                        Toast.makeText(LoginActivity.this, "Failed to Sign-In with Twitter: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        SessionManager<TwitterSession> sessionManager = TwitterCore.getInstance().getSessionManager();
                        
                        if (sessionManager.getActiveSession() != null){
                            sessionManager.clearActiveSession();
                        }
                        btnSignIn.setEnabled(true);
                        tvSignUp.setEnabled(true);
                        tvForgot.setEnabled(true);
                        btnSignInGoogle.setEnabled(true);
                        btnSignInFacebook.setEnabled(true);
                        mTwitterBtn.setEnabled(true);
                    } else {
                        rootRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.getValue() == null){
                                    initUIDinDatabase();
                                    addToEmailList(FirebaseAuth.getInstance().getCurrentUser().getEmail(),"Twitter");
                                } else {
                                    Intent intToHome = new Intent(LoginActivity.this, ListActivity.class);
                                    startActivity(intToHome);
                                    emailId.getText().clear();
                                    password.getEditText().getText().clear();
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(LoginActivity.this,error.getMessage(),Toast.LENGTH_SHORT).show();
                                btnSignIn.setEnabled(true);
                                tvSignUp.setEnabled(true);
                                tvForgot.setEnabled(true);
                                btnSignInGoogle.setEnabled(true);
                                btnSignInFacebook.setEnabled(true);
                                mTwitterBtn.setEnabled(true);
                            }
                        });
                    }
                }
            });
    }
    
    //generate a key(unique ID) in the database to store logged-in user's data
    private void initUIDinDatabase(){
        ArrayList<String> init_arr = new ArrayList<>();
        init_arr.add("link~~name~~starred");
        rootRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("default_event").setValue(init_arr)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Intent intToHome = new Intent(LoginActivity.this, ListActivity.class);
                    startActivity(intToHome);
                    emailId.getText().clear();
                    password.getEditText().getText().clear();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(LoginActivity.this,"Error occurred during account initialization.",Toast.LENGTH_SHORT).show();
                    btnSignIn.setEnabled(true);
                    tvSignUp.setEnabled(true);
                    tvForgot.setEnabled(true);
                    btnSignInGoogle.setEnabled(true);
                    btnSignInFacebook.setEnabled(true);
                    mTwitterBtn.setEnabled(true);
                }
            });
    }
    
    //keep track of email addresses and email providers for our users to prevent duplicate accounts and account overriding
    private void addToEmailList(String passed_email, String domain){
        String clean_email = passed_email.replaceAll("\\.","7702910").replaceAll("\\#","6839189").replaceAll("\\$","5073014").replaceAll("\\[","3839443").replaceAll("\\]","6029018").replaceAll("/","2528736");
        emailRef.child(clean_email).setValue(domain).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(LoginActivity.this,"Error occurred during account initialization. Failed to add email to database",Toast.LENGTH_SHORT).show();
                btnSignIn.setEnabled(true);
                tvSignUp.setEnabled(true);
                tvForgot.setEnabled(true);
                btnSignInGoogle.setEnabled(true);
                btnSignInFacebook.setEnabled(true);
                mTwitterBtn.setEnabled(true);
            }
        });
    }
    
    //handle login event when returning from (Google, Facebook, Twitter) custom login activity
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(facebookButtonClicked) {
            facebookButtonClicked = false;
            mCallbackManager_Facebook.onActivityResult(requestCode, resultCode, data);
            super.onActivityResult(requestCode, resultCode, data);
        }
        else if(twitterButtonClicked){
            twitterButtonClicked = false;
            super.onActivityResult(requestCode, resultCode, data);
            mTwitterBtn.onActivityResult(requestCode, resultCode, data);
        }
        else if(googleButtonClicked){
            googleButtonClicked = false;
            super.onActivityResult(requestCode, resultCode, data);
            
            if (requestCode == RC_SIGN_IN) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken(), account.getEmail());
                } catch (ApiException e) {
                    btnSignIn.setEnabled(true);
                    tvSignUp.setEnabled(true);
                    tvForgot.setEnabled(true);
                    btnSignInGoogle.setEnabled(true);
                    btnSignInFacebook.setEnabled(true);
                    mTwitterBtn.setEnabled(true);
                }
            }
        }
    }
}
