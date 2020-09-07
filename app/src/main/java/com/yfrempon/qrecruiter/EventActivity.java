package com.yfrempon.qrecruiter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.twitter.sdk.android.core.SessionManager;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterSession;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventActivity extends AppCompatActivity {
    
    public static ListView mListView;
    public static String selected_event;
    public static ArrayList<String> EventData;
    public static ArrayList<Object> names;
    public static Map<String, String> name2link;
    public static Map<String, String> userSelectionFavorite;
    public static ListViewAdapter2 adapter;
    public static Map<String, Object> exportMap;
    public static boolean deleteSelected = false;
    public static boolean editSelected = false;
    public static List<String> userSelection = new ArrayList<>();
    public static int listview_position = 0;
    public static int listview_index = 0;
    
    private SearchView searchView;
    private MenuItem searchItem;
    private ActionMode mActionMode;
    private boolean logoutFlag;
    private boolean onBackButtonPressed;
    private boolean search_active_flag;
    private boolean search_inactive_flag;
    private boolean search_skip_flag;
    private int sortVal = -1;
    
    Intent intent;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        setTitle("Saved Profiles");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        //store activity content in variables
        mListView = findViewById(R.id.listView);
        webView = findViewById(R.id.webView1);
        
        //set webview to LinkedIn website
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://www.linkedin.com/");
        
        //initialize variables
        exportMap = new HashMap<>();
        EventData = new ArrayList<>();
        names = new ArrayList<>();
        name2link = new HashMap<>();
        userSelectionFavorite = new HashMap<>();
        logoutFlag = false;
        onBackButtonPressed = false;
        search_active_flag = false;
        search_inactive_flag = false;
        search_skip_flag = false;
        intent = getIntent();
        selected_event = intent.getStringExtra("EVENT");
        EventData = (ArrayList<String>)ListActivity.finalDocumentData.get(selected_event);
        
        //load data into listview
        populateListView();
        
        //load clicked LinkedIn profile in webview
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = ((TextView)view.findViewById(R.id.txtName)).getText().toString();
                for(String x : EventData){
                    if(x.contains("~~" + item + "~~")){
                        String link_key = x.split("~~")[0];
                        webView.setWebViewClient(new WebViewClient());
                        webView.loadUrl(link_key);
                        webView.setVisibility(View.VISIBLE);
                        searchItem.collapseActionView();
                    }
                }
            }
        });
    }
    
    //remove profile from database and local data then refresh listview with updated local data(minimizes writes to the database)
    private void deleteProfile(List<String> selected_labels) {
        for(String label: selected_labels) {
            for (int i = 0; i < EventData.size(); i++) {
                if (EventData.get(i).contains("~~" + label + "~~")) {
                    EventData.remove(i);
                }
            }
        }
        LoginActivity.rootRef.child(ListActivity.currentUserUID).child(selected_event).setValue(EventData);
        populateListView();
    }
    
    //show action bar when 'delete' is selected
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        
        //initialize action bar
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu, menu);
            mode.setTitle("Delete");
            return true;
        }
        
        //not used
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
        
        //delete selected items
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch(item.getItemId()){
                case R.id.action_delete:
                    deleteSelected = false;
                    adapter = new ListViewAdapter2((ArrayList<String>)(ArrayList<?>)(names),EventActivity.this, userSelectionFavorite);
                    mListView.setAdapter(adapter);
                    deleteProfile(userSelection);
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }
        
        //clear variables and reload listview
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            deleteSelected = false;
            mListView.setAdapter(adapter);
            mActionMode = null;
            userSelection.clear();
        }
    };
    
    //show action bar when 'rename' is selected
    private ActionMode.Callback mActionModeCallback2 = new ActionMode.Callback() {
        //initialize action bar
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu_2, menu);
            mode.setTitle("Rename");
            return true;
        }
        
        //not used
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
        
        //not used
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }
        
        //clear variables and reload listview
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            editSelected = false;
            mListView.setAdapter(adapter);
            mActionMode = null;
        }
    };

    //reload listview using local data
    private void populateListView() {
        names.clear();
        name2link.clear();
        userSelectionFavorite.clear();
        String temp_name, temp_link, temp_starred;
        
        for(String info: EventData){
            if(info.equals("link~~name~~starred")){
                continue;
            }
            String[] split_data = info.split("~~");
            temp_link = split_data[0];
            temp_name = split_data[1];
            temp_starred = split_data[2];
            names.add(temp_name);
            name2link.put(temp_name, temp_link);
            userSelectionFavorite.put(temp_name, temp_starred);
        }
        loadListView();
    }
    
    //load data into the listview
    private void loadListView(){
        if(sortVal == 0){
            sortList(0);
        } else if(sortVal == 1){
            sortList(1);
        } else {
            adapter = new ListViewAdapter2((ArrayList<String>)(ArrayList<?>)(names),EventActivity.this, userSelectionFavorite);
            mListView.setAdapter(adapter);
        }
    }
    
    //static version of loadListView method to use globally
    //loads listview without shifting the listview to the first element(previous UI issue)
    public static void loadListViewStatic(){
        listview_index = mListView.getFirstVisiblePosition();
        View v = mListView.getChildAt(0);
        listview_position = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
        adapter = new ListViewAdapter2((ArrayList<String>)(ArrayList<?>)(names),MyApplication.getAppContext(), userSelectionFavorite);
        mListView.setAdapter(adapter);
        mListView.setSelectionFromTop(listview_index, listview_position);
    }
    
    //export to .csv file
    private void export(){
        StringBuilder exportSTR = new StringBuilder();
        StringBuilder exportSTR_false = new StringBuilder();
        exportSTR.append("Name,Profile,Favorite");
        
        //parse profiles in event data
        for(String x: EventData) {
            String local_key = x.split("~~")[0];
            ArrayList<String> local_value = new ArrayList<>();
            local_value.add(x.split("~~")[1]);
            local_value.add(x.split("~~")[2]);
            exportMap.put(local_key, local_value);
        }
        
        //default value: link~~name~~starred doens't count
        if(exportMap.size() == 1){
            Toast.makeText(EventActivity.this,"You need one or more profiles to export.",Toast.LENGTH_SHORT).show();
            return;
        }
        
        //convert map to string
        for (Map.Entry<String,Object> entry : exportMap.entrySet()) {
            ArrayList<String> export_temp = (ArrayList<String>)entry.getValue();
            if(entry.getKey().equals("link")){
                continue;
            }
            if(export_temp.get(1).equals("t")){
                exportSTR.append("\n" + export_temp.get(0) + "," + entry.getKey() + ",Yes" );
            }
            else{
                exportSTR_false.append("\n" + export_temp.get(0) + "," + entry.getKey() + ",No");
            }
        }
        
        exportSTR.append(exportSTR_false);
        
        //export to csv
        try {
            exportMap.clear();
            FileOutputStream out = openFileOutput(selected_event + ".csv", Context.MODE_PRIVATE);
            out.write((exportSTR.toString()).getBytes());
            out.close();
            Context temp_context = getApplicationContext();
            File filelocation = new File(getFilesDir(), selected_event + ".csv");
            Uri path = FileProvider.getUriForFile(temp_context, "com.yfrempon.qrecruiter.fileprovider", filelocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, selected_event + " Spreadsheet");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            
            if (fileIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(fileIntent, "Send mail"));
            }
        } catch(Exception e) {
            Toast.makeText(EventActivity.this,"Failed to export profiles.",Toast.LENGTH_SHORT).show();
        }
    }
    
    //search through listview
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        
        //search UI bug fix
        if(search_skip_flag){
            inflater.inflate(R.menu.example_menu, menu);
            search_skip_flag = false;
        }
        else if(search_active_flag && search_inactive_flag){
            search_skip_flag = true;
            inflater.inflate(R.menu.example_menu_plain, menu);
        } else {
            inflater.inflate(R.menu.example_menu, menu);
        }
        
        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        
        //filter list during search
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
        
        //detect when search has ended
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                webView.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                webView.setVisibility(View.VISIBLE);
                return true;
            }
        });
        
        //detect when search has closed
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
    
    //detect when menu option is selected(add, rename, delete, sort, export, logout, delete account)
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
                
            case R.id.action_menu_item_1_sub_1:     //sort A-Z
                sortList(0);
                sortVal = 0;
                return true;
                
            case R.id.action_menu_item_1_sub_2:     //sort Z-A
                sortList(1);
                sortVal = 1;
                return true;
                
            case R.id.action_menu_item_1_point_5:   //add
                Intent intToEvent = new Intent(EventActivity.this, QRActivity.class);
                intToEvent.putExtra("EVENT2", selected_event);
                intToEvent.putExtra("listOfNames", names);
                startActivity(intToEvent);
                searchItem.collapseActionView();
                return true;
                
            case  R.id.action_menu_item_2:          //rename
                if(mActionMode != null){
                    return false;
                }
                mActionMode = startSupportActionMode(mActionModeCallback2);
                editSelected = true;
                mListView.setAdapter(adapter);
                return true;
                
            case R.id.action_menu_item_2_point_5:   //delete event
                if(mActionMode != null){
                    return false;
                }
                mActionMode = startSupportActionMode(mActionModeCallback);
                deleteSelected = true;
                mListView.setAdapter(adapter);
                return true;
                
            case R.id.action_menu_item_3:       //export to csv
                export();
                return true;
                
            case  R.id.action_menu_item_3_point_5:  //logout
                logoutFlag = true;
                Intent intToMain = new Intent(EventActivity.this, LoginActivity.class);
                startActivity(intToMain);
                searchItem.collapseActionView();
                return true;
                
            case  R.id.action_menu_item_4:      //delete account
                showDeleteAccountDialog(EventActivity.this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    //handle deleting current account(remove account from Firebase and remove email, UID, & user data from database)
    private void showDeleteAccountDialog(Context c) {
        final AlertDialog dialog = new AlertDialog.Builder(c)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? All your data will be erased.")
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String curr_email = FirebaseAuth.getInstance().getCurrentUser().getEmail().replaceAll("\\.","7702910").replaceAll("\\#","6839189").replaceAll("\\$","5073014").replaceAll("\\[","3839443").replaceAll("\\]","6029018").replaceAll("/","2528736");
                    LoginActivity.rootRef.child(ListActivity.currentUserUID).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            LoginActivity.emailRef.child(curr_email).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    FirebaseAuth.getInstance().getCurrentUser().delete();
                                    Intent intToMain = new Intent(EventActivity.this, LoginActivity.class);
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
    
    //sort options
    public void sortList(int value){
        if(value == 0){
            ArrayList<Object> temp = (ArrayList<Object>)names.clone();
            ArrayList<String> temp_final = (ArrayList<String>)(ArrayList<?>)(sort_merge(temp, 0, temp.size()-1));
            names = new ArrayList<>(temp_final);
            adapter = new ListViewAdapter2(temp_final,EventActivity.this, userSelectionFavorite);
            mListView.setAdapter(adapter);
        }
        else if(value == 1){
            ArrayList<Object> temp = (ArrayList<Object>)names.clone();
            ArrayList<String> temp_final = (ArrayList<String>)(ArrayList<?>)(sort_merge2(temp, 0, temp.size()-1));
            names = new ArrayList<>(temp_final);
            adapter = new ListViewAdapter2(temp_final,EventActivity.this, userSelectionFavorite);
            mListView.setAdapter(adapter);
        }
    }
    
    //merge sort helper function(A-Z)
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
    
    //merge sort helper function(Z-A)
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
    
    //merge sort in alphabetical order(A-Z)
    public static ArrayList<Object> sort_merge(ArrayList<Object> arr, int l, int r) {
        if (l < r) {
            int m = (l + r) / 2;
            sort_merge(arr, l, m);
            sort_merge(arr, m + 1, r);
            merge(arr, l, m, r);
        }
        return arr;
    }

    //merge sort in reverse alphabetical order(Z-A)
    public static ArrayList<Object> sort_merge2(ArrayList<Object> arr, int l, int r) {
        if (l < r) {
            int m = (l + r) / 2;
            sort_merge2(arr, l, m);
            sort_merge2(arr, m + 1, r);
            merge2(arr, l, m, r);
        }
        return arr;
    }
    
    //bottom bar back button pressed
    @Override
    public void onBackPressed() {
        onBackButtonPressed = true;
        finish();
    }
    
    //get current activity context and reload listview when activity is active again
    @Override
    public void onResume() {
        super.onResume();
        populateListView();
        MyApplication.setContext(this);
    }
    
    //sign-out and clear webview session when logout is pressed
    //update global user data when back button is pressed
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(logoutFlag){
            SessionManager<TwitterSession> sessionManager = TwitterCore.getInstance().getSessionManager();
            if (sessionManager.getActiveSession() != null){
                sessionManager.clearActiveSession();
            }
            LoginActivity.mGoogleSignInClient.signOut();
            LoginManager.getInstance().logOut();
            FirebaseAuth.getInstance().signOut();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
        }
        if(onBackButtonPressed){
            ListActivity.finalDocumentData.put(selected_event, EventData);
        }
    }
}

