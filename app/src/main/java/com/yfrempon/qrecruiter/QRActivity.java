package com.yfrempon.qrecruiter;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import android.content.DialogInterface;
import com.google.zxing.Result;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import static android.Manifest.permission.CAMERA;
import static java.lang.Character.isWhitespace;

public class QRActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    Intent intent;
    String passed_event;
    ArrayList<String> profiles;
    Map<String, Object> temp;
    ImageView btn_img;
    AnimatedVectorDrawableCompat avd;
    AnimatedVectorDrawable avd2;
    private ZXingScannerView scannerView;
    private String m_Text = "";
    private static final int REQUEST_CAMERA = 1;
    private boolean paused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        scannerView = findViewById(R.id.zxscan);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Scan QR Code");
        
        //get event and profile data passed in from the Event Activity
        intent = getIntent();
        passed_event = intent.getStringExtra("EVENT2");
        profiles = intent.getStringArrayListExtra("listOfNames");
        
        temp = new HashMap<>();
        
        //handle camera access permissions
        int currentApiVersion = Build.VERSION.SDK_INT;
        if(currentApiVersion >=  Build.VERSION_CODES.M)
        {
            if(!checkPermission())
            {
                requestPermission();
            }
        }
    }
    
    //check if camera access has already been granted
    private boolean checkPermission()
    {
        return (ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA) == PackageManager.PERMISSION_GRANTED);
    }
    
    //ask for camera access permissions
    private void requestPermission()
    {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, REQUEST_CAMERA);
    }
    
    //AnimatedVectorDrawable requires API level 22 or higher
    //start and stop camera: pause button -> play button | play button -> pause button
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void togglePlayButton(){
        paused = !paused;
        
        if(paused){
            scannerView.stopCameraPreview();
            btn_img.setImageDrawable(getResources().getDrawable(R.drawable.avd_pause_to_play));
        } else {
            scannerView.resumeCameraPreview(QRActivity.this);
            btn_img.setImageDrawable(getResources().getDrawable(R.drawable.avd_play_to_pause));
        }
        
        Drawable drawable = btn_img.getDrawable();

        if (drawable instanceof AnimatedVectorDrawableCompat) {
            avd = (AnimatedVectorDrawableCompat) drawable;
            avd.start();
        } else if (drawable instanceof AnimatedVectorDrawable) {
            avd2 = (AnimatedVectorDrawable) drawable;
            avd2.start();
        }
    }
    
    //AnimatedVectorDrawable requires API level 22 or higher
    //initialize pause/play button
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTogglePlayButton(boolean val){
        if(val){
            scannerView.stopCameraPreview();
            btn_img.setImageDrawable(getResources().getDrawable(R.drawable.avd_pause_to_play));
        } else {
            scannerView.resumeCameraPreview(QRActivity.this);
            btn_img.setImageDrawable(getResources().getDrawable(R.drawable.avd_play_to_pause));
        }
        
        paused = val;
        Drawable drawable = btn_img.getDrawable();

        if (drawable instanceof AnimatedVectorDrawableCompat) {
            avd = (AnimatedVectorDrawableCompat) drawable;
            avd.start();
        } else if (drawable instanceof AnimatedVectorDrawable) {
            avd2 = (AnimatedVectorDrawable) drawable;
            avd2.start();
        }
    }
    
    //handle camera request
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    
                    if (cameraAccepted){
                        Toast.makeText(getApplicationContext(), "Permission Granted.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied, you cannot access the camera. Allow camera access in your phone settings.", Toast.LENGTH_LONG).show();
                        Intent intToMain = new Intent(QRActivity.this, EventActivity.class);
                        startActivity(intToMain);
                    }
                }
                break;
        }
    }
    
    //show message box
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new androidx.appcompat.app.AlertDialog.Builder(QRActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    
    //triggered after a QR code is scanned
    @Override
    public void handleResult(Result result) {
        final String myResult = result.getText();
        String suggested = "";
        
        //make sure the generated URL is a LinkedIn profile
        if(myResult.contains("/in/") && myResult.contains("?")) {
            suggested = myResult.substring(myResult.indexOf("/in/") + 4, myResult.indexOf("?"));
        }
        else if(myResult.contains("/in/")){
            suggested = myResult.substring(myResult.indexOf("/in/") + 4);
        }
        else {
            Toast.makeText(getApplicationContext(), "Failed to read LinkedIn URL from QR code.", Toast.LENGTH_SHORT).show();
            setTogglePlayButton(true);
            return;
        }
        
        //parse username from URL
        if(suggested.contains("-")){
            String[] separated = suggested.split("-");
            String first_name = separated[0].substring(0, 1).toUpperCase() + separated[0].substring(1);
            String last_name = separated[1].substring(0, 1).toUpperCase() + separated[1].substring(1);
            suggested = first_name + " " + last_name;
        }
        else{
            suggested = suggested.substring(0, 1).toUpperCase() + suggested.substring(1);
        }
        
        final EditText taskEditText = new EditText(QRActivity.this);
        taskEditText.setSingleLine(true);
        taskEditText.setLines(1);
        taskEditText.setMaxLines(1);
        
        Builder builder = new Builder(this);
        builder.setTitle("Scan Result");
        builder.setMessage("Add a name:");
        builder.setView(taskEditText);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = String.valueOf(taskEditText.getText());
                if(!m_Text.isEmpty()) {
                    addToServer(passed_event, myResult, m_Text);
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        
        suggested = suggested.replaceAll("/[^A-Za-z0-9]/", "");
        suggested = suggested.replaceAll("\\s+", " ");
        suggested = suggested.trim();
        taskEditText.setText(suggested);
        taskEditText.setSelection(taskEditText.getText().length());
        final AlertDialog alert1 = builder.create();
        alert1.show();
        
        //Check if profile has already been scanned & make sure initial input text for new profile name is valid
        if(profiles.contains(taskEditText.getText().toString()) || profiles.contains(taskEditText.getText().toString().trim())  || EventActivity.name2link.containsValue(myResult) || taskEditText.getText().toString().length() > 25){
            if(EventActivity.name2link.containsValue(myResult)){
                String localKeyVal = "";
                for (Map.Entry<String,String> entry : EventActivity.name2link.entrySet()){
                    if(entry.getValue().equals(myResult)){
                        localKeyVal = entry.getKey();
                        break;
                    }
                }
                alert1.setMessage(Html.fromHtml("<font color='#FF0000'>QR Code Already Scanned -> " + localKeyVal  + "</font>"));
            }
            else if(profiles.contains(taskEditText.getText().toString())  || profiles.contains(taskEditText.getText().toString().trim())) {
                alert1.setMessage(Html.fromHtml("<font color='#FF0000'>Profile Name Already Exist</font>"));
            }
            else {
                alert1.setMessage(Html.fromHtml("<font color='#FF0000'>Character Limit: 25</font>"));
            }
            alert1.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
        else{
            alert1.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }
        
        alert1.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(final DialogInterface arg0) {
                setTogglePlayButton(true);
            }
        });
        
        //Check if profile has already been scanned & make sure edited input text for new profile name is valid
        taskEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                alert1.setMessage(Html.fromHtml("<font color='#000000'>Add a name: </font>"));
                
                if (TextUtils.isEmpty(s)) {
                    alert1.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    return;
                }
                else {
                    String alphanum = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890 ";
                    String currentVal = taskEditText.getText().toString();
                    int currentLen = currentVal.length();
                    
                    if(currentVal.contains(".")){
                        currentVal = currentVal.replace(".","");
                        taskEditText.setText(currentVal);
                        taskEditText.setSelection(taskEditText.getText().length());
                        return;
                    }
                    
                    if ((alphanum.indexOf(currentVal.charAt(currentLen - 1)) == -1) || isWhitespace(currentVal.charAt(0)) || currentVal.contains("  ")) {
                        taskEditText.setText(currentVal.substring(0, currentVal.length() - 1));
                        taskEditText.setSelection(taskEditText.getText().length());
                        return;
                    }
                    
                    if(EventActivity.name2link.containsValue(myResult) || profiles.contains(taskEditText.getText().toString()) || profiles.contains(taskEditText.getText().toString().trim()) || taskEditText.getText().toString().length() > 25){
                        if(EventActivity.name2link.containsValue(myResult)){
                            String localKeyVal = "";
                            for (Map.Entry<String,String> entry : EventActivity.name2link.entrySet()){
                                if(entry.getValue().equals(myResult)){
                                    localKeyVal = entry.getKey();
                                    break;
                                }
                            }
                            alert1.setMessage(Html.fromHtml("<font color='#FF0000'>QR Code Already Scanned -> " + localKeyVal  + "</font>"));
                        }
                        else if(profiles.contains(taskEditText.getText().toString())  || profiles.contains(taskEditText.getText().toString().trim())) {
                            alert1.setMessage(Html.fromHtml("<font color='#FF0000'>Profile Name Already Exist</font>"));
                        }
                        else {
                            alert1.setMessage(Html.fromHtml("<font color='#FF0000'>Character Limit: 25</font>"));
                        }
                        alert1.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                    else{
                        alert1.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                }
            }
        });
    }
    
    //add profile
    private void addToServer(String event, String link, String name){
        EventActivity.EventData.add(link + "~~" + name + "~~f");
        profiles.add(name);
        EventActivity.name2link.put(name, link);
        LoginActivity.rootRef.child(ListActivity.currentUserUID).child(passed_event).setValue(EventActivity.EventData);
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
    
    //get current status of the camera when the activity resumes from being inactive(check permissions, listen of pause/play button clicks)
    @Override
    public void onResume() {
        super.onResume();
        
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        
        if (currentapiVersion >= android.os.Build.VERSION_CODES.M) {
            if (checkPermission()) {
                if(scannerView == null) {
                    scannerView = new ZXingScannerView(this);
                    setContentView(scannerView);
                }
                scannerView.setResultHandler(this);
                scannerView.startCamera();
            } else {
                requestPermission();
            }
        }
        
        btn_img = findViewById(R.id.pause_btn_img);
        btn_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlayButton();
            }
        });
    }
    
    //stop camera when activity closes
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        scannerView.stopCamera();
    }
}

