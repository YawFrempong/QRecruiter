package com.yfrempon.qrecruiter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.twitter.sdk.android.core.SessionManager;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.lang.Character.isWhitespace;

public class ListActivity extends AppCompatActivity {

    public static ListView mListView;
    private String m_Text = "";
    public static ArrayList<String> profiles;
    private ArrayList<String> empty;
    public static String currentUserUID;
    public static ListViewAdapter adapter;
    private SearchView searchView;
    private MenuItem searchItem;
    public static boolean deleteSelected = false;
    public static boolean editSelected = false;
    private ActionMode mActionMode;
    public static List<String> userSelection = new ArrayList<>();
    private boolean onBackButtonPressed;
    private boolean onLogoutPressed;
    private boolean search_active_flag;
    private boolean search_inactive_flag;
    private boolean search_skip_flag;
    public static Map<String, Object> finalDocumentData;
    public static int listview_position = 0;
    public static int listview_index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Events");
        setContentView(R.layout.activity_list);
        //enable login buttons again
        LoginActivity.btnSignIn.setEnabled(true);
        LoginActivity.tvSignUp.setEnabled(true);
        LoginActivity.tvForgot.setEnabled(true);
        LoginActivity.btnSignInGoogle.setEnabled(true);
        LoginActivity.btnSignInFacebook.setEnabled(true);
        LoginActivity.mTwitterBtn.setEnabled(true);

        mListView = findViewById(R.id.eventListView);
        profiles = new ArrayList<>();
        empty = new ArrayList<>();
        finalDocumentData = new HashMap<>();
        currentUserUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        onBackButtonPressed = false;
        onLogoutPressed = false;
        search_active_flag = false;
        search_inactive_flag = false;
        search_skip_flag = false;
        empty.add("");
        populateListView();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = ((TextView)view.findViewById(R.id.txtName)).getText().toString();
                Intent intToEvent = new Intent(ListActivity.this, EventActivity.class);
                intToEvent.putExtra("EVENT", item);
                startActivity(intToEvent);
                searchItem.collapseActionView();
                if(mActionMode != null){
                    mActionMode.finish();
                }
            }
        });
    }
    @Override
    protected void onResume(){
        super.onResume();
        MyApplication.setContext(this);
    }
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu, menu);
            mode.setTitle("Delete");
            return true;
        }
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch(item.getItemId()){
                case R.id.action_delete:
                    deleteSelected = false;
                    adapter = new ListViewAdapter(profiles,ListActivity.this);
                    mListView.setAdapter(adapter);
                    deleteProfile(userSelection);
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            deleteSelected = false;
            mListView.setAdapter(adapter);
            mActionMode = null;
            userSelection.clear();
        }
    };
    private ActionMode.Callback mActionModeCallback2 = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu_2, menu);
            mode.setTitle("Rename");
            return true;
        }
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            editSelected = false;
            mListView.setAdapter(adapter);
            mActionMode = null;
        }
    };
    private void populateListViewLocal() {
        profiles.clear();
        for (Map.Entry<String, Object> entry : finalDocumentData.entrySet()) {
            String key = entry.getKey();
            if(!key.equals("default_event")){
                profiles.add(key);
            }
        }
        loadListView();
    }
    private void populateListView() {
        profiles.clear();
        FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                finalDocumentData = (Map<String, Object>) dataSnapshot.getValue();
                for (Map.Entry<String, Object> entry : finalDocumentData.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals("default_event")) {
                        profiles.add(key);
                    }
                }
                loadListView();
            }
            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }
    private void loadListView(){
        adapter = new ListViewAdapter(profiles, ListActivity.this);
        mListView.setAdapter(adapter);
    }
    public static void loadListViewStatic(){
        listview_index = mListView.getFirstVisiblePosition();
        View v = mListView.getChildAt(0);
        listview_position = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
        adapter = new ListViewAdapter(profiles, MyApplication.getAppContext());
        mListView.setAdapter(adapter);
        mListView.setSelectionFromTop(listview_index, listview_position);
    }
    private void showAddItemDialog(Context c) {
        final EditText taskEditText = new EditText(c);
        taskEditText.setSingleLine(true);
        taskEditText.setLines(1);
        taskEditText.setMaxLines(1);
        final AlertDialog dialog = new AlertDialog.Builder(c)
                .setTitle("Add an event")
                .setMessage("Put a new event on the list: ")
                .setView(taskEditText)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        m_Text = String.valueOf(taskEditText.getText());
                        if(!m_Text.isEmpty()){
                            addToServer(m_Text);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        taskEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                dialog.setMessage(Html.fromHtml("<font color='#000000'>Put a new event on the list: </font>"));
                if (TextUtils.isEmpty(s)) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
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
                    if ((alphanum.indexOf(currentVal.charAt(currentLen - 1)) == -1) || isWhitespace(currentVal.charAt(0)) ||currentVal.contains("  ")) {
                        taskEditText.setText(currentVal.substring(0, currentVal.length() - 1));
                        taskEditText.setSelection(taskEditText.getText().length());
                        return;
                    }
                    if(profiles.contains(taskEditText.getText().toString()) || profiles.contains(taskEditText.getText().toString().trim()) || taskEditText.getText().toString().length() > 25){
                        if(profiles.contains(taskEditText.getText().toString()) || profiles.contains(taskEditText.getText().toString().trim())) {
                            dialog.setMessage(Html.fromHtml("<font color='#FF0000'>Event Already Exist</font>"));
                        }
                        else {
                            dialog.setMessage(Html.fromHtml("<font color='#FF0000'>Character Limit: 25</font>"));
                        }
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                    else{
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                }
            }
        });
    }
    private void showDeleteAccountDialog(Context c) {
        final AlertDialog dialog = new AlertDialog.Builder(c)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? All your data will be erased.")
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String curr_email = FirebaseAuth.getInstance().getCurrentUser().getEmail().replaceAll("\\.","7702910").replaceAll("\\#","6839189").replaceAll("\\$","5073014").replaceAll("\\[","3839443").replaceAll("\\]","6029018").replaceAll("/","2528736");
                    LoginActivity.rootRef.child(currentUserUID).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            LoginActivity.emailRef.child(curr_email).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    FirebaseAuth.getInstance().getCurrentUser().delete();
                                    Intent intToMain = new Intent(ListActivity.this, LoginActivity.class);
                                    startActivity(intToMain);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
        dialog.show();
    }
    private void addToServer(String event){
        ArrayList<String> temp_val = new ArrayList<>();
        temp_val.add("link~~name~~starred");
        finalDocumentData.put(event, temp_val);
        LoginActivity.rootRef.child(currentUserUID).child(event).setValue(temp_val);
        populateListViewLocal();
    }
    private void deleteProfile(List<String> events) {
        for (String event : events) {
            finalDocumentData.remove(event);
        }
        if(events.size() == 1){
            LoginActivity.rootRef.child(currentUserUID).child(events.get(0)).removeValue();
        }
        else{
            LoginActivity.rootRef.child(currentUserUID).setValue(finalDocumentData);
        }
        populateListViewLocal();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        if(search_skip_flag){
            inflater.inflate(R.menu.example_menu_2, menu);
            search_skip_flag = false;
        }
        else if(search_active_flag && search_inactive_flag){
            search_skip_flag = true;
            inflater.inflate(R.menu.example_menu_plain, menu);
        } else {
            inflater.inflate(R.menu.example_menu_2, menu);
        }
        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewDetachedFromWindow(View arg0) {
                search_inactive_flag = true;
                invalidateOptionsMenu();
            }

            @Override
            public void onViewAttachedToWindow(View arg0) {
                search_active_flag = true;
                invalidateOptionsMenu();
            }
        });
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.action_menu_item_1_sub_1:
                sortList(0);
                return true;
            case R.id.action_menu_item_1_sub_2:
                sortList(1);
                return true;
            case R.id.action_menu_item_1_point_5:
                showAddItemDialog(ListActivity.this);
                return true;
            case R.id.action_menu_item_2:
                if(mActionMode != null){
                    return false;
                }
                mActionMode = startSupportActionMode(mActionModeCallback2);
                editSelected = true;
                mListView.setAdapter(adapter);
                return true;
            case R.id.action_menu_item_2_point_5:
                if(mActionMode != null){
                    return false;
                }
                mActionMode = startSupportActionMode(mActionModeCallback);
                deleteSelected = true;
                mListView.setAdapter(adapter);
                return true;
            case  R.id.action_menu_item_3:
                onLogoutPressed = true;
                Intent intToMain = new Intent(ListActivity.this, LoginActivity.class);
                startActivity(intToMain);
                searchItem.collapseActionView();
                return true;
            case R.id.action_menu_item_4:
                showDeleteAccountDialog(ListActivity.this);
        }
        return super.onOptionsItemSelected(item);
    }
    public void sortList(int value){
        if(value == 0){
            ArrayList<Object> temp = (ArrayList<Object>)profiles.clone();
            ArrayList<String> temp_final = (ArrayList<String>)(ArrayList<?>)(sort_merge(temp, 0, temp.size()-1));
            profiles = new ArrayList<>(temp_final);
            adapter = new ListViewAdapter(temp_final,this);
            mListView.setAdapter(adapter);
        }
        else if(value == 1){
            ArrayList<Object> temp = (ArrayList<Object>)profiles.clone();
            ArrayList<String> temp_final = (ArrayList<String>)(ArrayList<?>)(sort_merge2(temp, 0, temp.size()-1));
            profiles = new ArrayList<>(temp_final);
            adapter = new ListViewAdapter(temp_final,this);
            mListView.setAdapter(adapter);
        }
    }
    public static void merge(ArrayList<Object> arr, int l, int m, int r) {
        int n1 = m - l + 1;
        int n2 = r - m;

        String L[] = new String[n1];
        String R[] = new String[n2];

        for (int i = 0; i < n1; ++i)
            L[i] = arr.get(l + i).toString();
        for (int j = 0; j < n2; ++j)
            R[j] = arr.get(m + 1 + j).toString();

        int i = 0, j = 0;
        int k = l;
        while (i < n1 && j < n2) {
            if (((L[i].toLowerCase()).compareTo(R[j].toLowerCase())) < 0) {
                arr.set(k , L[i]);
                i++;
            }
            else if(((L[i].toLowerCase()).compareTo(R[j].toLowerCase())) > 0) {
                arr.set(k , R[j]);
                j++;
            }
            else{
                int min_length = L[i].length();
                boolean r_lesser = false;
                if(L[i].length() > R[j].length()){
                    min_length = R[i].length();
                    r_lesser = true;
                }
                innerloop:
                for(int z = 0; z < min_length; z++){
                    if(((int)L[i].charAt(z)) < ((int)R[j].charAt(z))){
                        arr.set(k , L[i]);
                        i++;
                        break innerloop;
                    }
                    else if(((int)L[i].charAt(z)) > ((int)R[j].charAt(z))){
                        arr.set(k , R[j]);
                        j++;
                        break innerloop;
                    }
                    else{
                        if(z == min_length-1){
                            if(r_lesser){
                                arr.set(k , R[j]);
                                j++;
                                break innerloop;
                            }
                            arr.set(k , L[i]);
                            i++;
                            break innerloop;
                        }
                    }
                }
            }
            k++;
        }
        while (i < n1) {
            arr.set(k , L[i]);
            i++;
            k++;
        }
        while (j < n2) {
            arr.set(k , R[j]);
            j++;
            k++;
        }
    }
    public static ArrayList<Object> sort_merge(ArrayList<Object> arr, int l, int r) {
        if (l < r) {
            int m = (l + r) / 2;
            sort_merge(arr, l, m);
            sort_merge(arr, m + 1, r);
            merge(arr, l, m, r);
        }
        return arr;
    }
    public static void merge2(ArrayList<Object> arr, int l, int m, int r) {
        int n1 = m - l + 1;
        int n2 = r - m;

        String L[] = new String[n1];
        String R[] = new String[n2];

        for (int i = 0; i < n1; ++i)
            L[i] = arr.get(l + i).toString();
        for (int j = 0; j < n2; ++j)
            R[j] = arr.get(m + 1 + j).toString();

        int i = 0, j = 0;
        int k = l;
        while (i < n1 && j < n2) {
            if (((L[i].toLowerCase()).compareTo(R[j].toLowerCase())) > 0) {
                arr.set(k , L[i]);
                i++;
            }
            else if(((L[i].toLowerCase()).compareTo(R[j].toLowerCase())) < 0) {
                arr.set(k , R[j]);
                j++;
            }
            else{
                int min_length = L[i].length();
                boolean r_lesser = false;
                if(L[i].length() > R[j].length()){
                    min_length = R[i].length();
                    r_lesser = true;
                }
                innerloop:
                for(int z = 0; z < min_length; z++){
                    if(((int)L[i].charAt(z)) > ((int)R[j].charAt(z))){
                        arr.set(k , L[i]);
                        i++;
                        break innerloop;
                    }
                    else if(((int)L[i].charAt(z)) < ((int)R[j].charAt(z))){
                        arr.set(k , R[j]);
                        j++;
                        break innerloop;
                    }
                    else{
                        if(z == min_length-1){
                            if(r_lesser){
                                arr.set(k , R[j]);
                                j++;
                                break innerloop;
                            }
                            arr.set(k , L[i]);
                            i++;
                            break innerloop;
                        }
                    }
                }
            }
            k++;
        }
        while (i < n1) {
            arr.set(k , L[i]);
            i++;
            k++;
        }
        while (j < n2) {
            arr.set(k , R[j]);
            j++;
            k++;
        }
    }
    public static ArrayList<Object> sort_merge2(ArrayList<Object> arr, int l, int r) {
        if (l < r) {
            int m = (l + r) / 2;
            sort_merge2(arr, l, m);
            sort_merge2(arr, m + 1, r);
            merge2(arr, l, m, r);
        }
        return arr;
    }
    @Override
    public void onBackPressed() {
        onBackButtonPressed = true;
        finish();
    }
    protected void onDestroy() {
        super.onDestroy();
        if(onLogoutPressed || onBackButtonPressed) {
            SessionManager<TwitterSession> sessionManager = TwitterCore.getInstance().getSessionManager();
            if (sessionManager.getActiveSession() != null){
                sessionManager.clearActiveSession();
            }
            LoginActivity.mGoogleSignInClient.signOut();
            LoginManager.getInstance().logOut();
            FirebaseAuth.getInstance().signOut();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            Intent intToMain = new Intent(ListActivity.this, LoginActivity.class);
            startActivity(intToMain);
            searchItem.collapseActionView();
        }
    }
}

