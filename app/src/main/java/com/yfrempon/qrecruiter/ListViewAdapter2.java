package com.yfrempon.qrecruiter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.yfrempon.qrecruiter.EventActivity.deleteSelected;
import static java.lang.Character.isWhitespace;

public class ListViewAdapter2 extends ArrayAdapter<String> {
    public CheckBox checkBox;
    public ImageView img_star;
    public ImageView img_edit;
    public Map<String, String> favList = new HashMap<>();
    private Map<String, Object> results = new HashMap<>();
    
    private List<String> labels;
    private List<String> labelsFull;
    private Context context;
    private String m_Text = "";
    
    //constructor(load profile names and favorite status)
    public ListViewAdapter2(List<String> labels, Context context, Map<String, String> favList){
        super(context, R.layout.row_item_2, labels);
        this.context = context;
        this.labels = labels;
        labelsFull = new ArrayList<>(labels);
        this.favList.putAll(favList);
    }
    
    //for every list element set the label name and favorite star
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        final int position_star = position;
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();
        View row = inflater.inflate(R.layout.row_item_2,parent, false);
        
        //store activity content in variables
        //set labels, checkbox location, and favorite status
        TextView label = row.findViewById(R.id.txtName);
        label.setText(labels.get(position));
        checkBox = row.findViewById(R.id.checkBox);
        checkBox.setTag(position);
        img_star = row.findViewById(R.id.favorite);
        img_star.setTag(position);
        img_edit = row.findViewById(R.id.editButton_2);
        img_edit.setTag(position);
        
        //toggle star filled/not filled based on favorite status
        if(favList.get(labels.get(position)).equals("t")){
            img_star.setImageResource(R.drawable.ic_star_filled);
        } else {
            img_star.setImageResource(R.drawable.ic_star_not_filled);
        }
        
        //toggle checkbox based on selected items
        if(EventActivity.userSelection.contains(labels.get(position))){
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }
        
        //hide or show stars, checkboxes, trash cans based on whether delete, edit, or neither is selected
        if(deleteSelected && !EventActivity.editSelected){
            checkBox.setVisibility(View.VISIBLE);
            img_star.setVisibility(View.GONE);
        }
        else if(!deleteSelected && EventActivity.editSelected){
            img_edit.setVisibility(View.VISIBLE);
            img_star.setVisibility(View.GONE);
        }
        else{
            checkBox.setVisibility(View.GONE);
            img_edit.setVisibility(View.GONE);
            img_star.setVisibility(View.VISIBLE);
        }
        
        //detect when the current checkbox is toggled update dependent variables
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int position = (int)buttonView.getTag();
                
                if(EventActivity.userSelection.contains(labels.get(position))){
                    EventActivity.userSelection.remove(labels.get(position));
                }
                else{
                    EventActivity.userSelection.add(labels.get(position));
                }
            }
        });
        
         //detect when the current star is toggled and update listview appearance and dependent variables
        img_star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String selected = labels.get(position_star);
                final String selected_link = EventActivity.name2link.get(selected);
                EventActivity.exportMap.clear();
                
                for(String x: EventActivity.EventData){
                    String local_key = x.split("~~")[0];
                    ArrayList<String> local_value = new ArrayList<>();
                    local_value.add(x.split("~~")[1]);
                    local_value.add(x.split("~~")[2]);
                    EventActivity.exportMap.put(local_key, local_value);
                    results.put(local_key, local_value);
                }
                
                ArrayList<String> status = (ArrayList<String>) results.get(selected_link);
                ArrayList<String> replacement = new ArrayList<>();
                
                if(status.get(1).equals("t")){
                    replacement.add(selected);
                    replacement.add("f");
                    results.put(selected_link, replacement);
                    favList.put(status.get(0), "f");
                    EventActivity.userSelectionFavorite.put(status.get(0), "f");
                }
                else if(status.get(1).equals("f")){
                    replacement.add(selected);
                    replacement.add("t");
                    results.put(selected_link, replacement);
                    favList.put(status.get(0), "t");
                    EventActivity.userSelectionFavorite.put(status.get(0), "t");
                }
                
                EventActivity.EventData.clear();
                
                for (Map.Entry mapElement : results.entrySet()) {
                    String key = (String)mapElement.getKey();
                    ArrayList<String> val = (ArrayList<String>) mapElement.getValue();
                    String input_str = key + "~~" + val.get(0) + "~~" + val.get(1);
                    EventActivity.EventData.add(input_str);
                }
                
                LoginActivity.rootRef.child(ListActivity.currentUserUID).child(EventActivity.selected_event).setValue(EventActivity.EventData);
                notifyDataSetChanged();
            }
        });
        
        //detect when the pencil is selected and show the dialog box
        img_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditItemDialog(context, labels.get(position));
            }
        });
        return row;
    }
    
    //filter list items based on search query and update list
    public Filter getFilter() {
        return exampleFilter;
    }
    private Filter exampleFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<String> filteredList = new ArrayList<>();
            
            if(constraint == null || constraint.length() == 0) {
                filteredList.addAll(labelsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for(String item : labelsFull) {
                    if(item.toLowerCase().startsWith(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            labels.clear();
            labels.addAll((List)results.values);
            notifyDataSetChanged();
        }
    };
    
    //handle input text for renaming the profile(make sure input text is valid & no duplicate profiles) & update listview and dependent variables
    private void showEditItemDialog(Context c, String clicked_name) {
        final EditText taskEditText = new EditText(c);
        taskEditText.setSingleLine(true);
        taskEditText.setLines(1);
        taskEditText.setMaxLines(1);
        final AlertDialog dialog = new AlertDialog.Builder(c)
            .setTitle("Rename profile")
            .setMessage("Give a new name: ")
            .setView(taskEditText)
            .setPositiveButton("Save Changes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    m_Text = String.valueOf(taskEditText.getText());
                    
                    //update dependent variables and listview
                    if(!m_Text.isEmpty()){
                        String prev_val_name2link = EventActivity.name2link.get(clicked_name);
                        EventActivity.name2link.put(m_Text, prev_val_name2link);
                        EventActivity.name2link.remove(clicked_name);
                        for(int i = 0; i < EventActivity.names.size(); i++){
                            if(EventActivity.names.get(i).equals(clicked_name)){
                                EventActivity.names.set(i, m_Text);
                            }
                        }
                        ArrayList<String> temp_names = new ArrayList<>();
                        for(int i = 0; i < EventActivity.names.size(); i++){
                            temp_names.add(EventActivity.names.get(i).toString());
                        }
                        labels = (List<String>) temp_names.clone();
                        String prev_val_fav = favList.get(clicked_name);
                        favList.put(m_Text, prev_val_fav);
                        favList.remove(clicked_name);

                        String prev_val_fav_2 = EventActivity.userSelectionFavorite.get(clicked_name);
                        EventActivity.userSelectionFavorite.put(m_Text, prev_val_fav_2);
                        EventActivity.userSelectionFavorite.remove(clicked_name);

                        for (int i = 0; i < EventActivity.EventData.size(); i++) {
                            if (EventActivity.EventData.get(i).contains("~~" + clicked_name + "~~")) {
                                String match = EventActivity.EventData.get(i);
                                String edited_str = match.split("~~")[0] + "~~" + m_Text + "~~" + match.split("~~")[2];
                                EventActivity.EventData.set(i, edited_str);
                                LoginActivity.rootRef.child(ListActivity.currentUserUID).child(EventActivity.selected_event).setValue(EventActivity.EventData);
                                EventActivity.loadListViewStatic();
                                notifyDataSetChanged();
                                return;
                            }
                        }
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        
        //make sure input text is valid and prevent duplicate profile names
        taskEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                dialog.setMessage(Html.fromHtml("<font color='#000000'>Give a new name: </font>"));
                
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
                    
                    if(EventActivity.names.contains(taskEditText.getText().toString()) || EventActivity.names.contains(taskEditText.getText().toString().trim()) || taskEditText.getText().toString().length() > 25){
                        if(EventActivity.names.contains(taskEditText.getText().toString()) || EventActivity.names.contains(taskEditText.getText().toString().trim())) {
                            dialog.setMessage(Html.fromHtml("<font color='#FF0000'>Profile Already Exist</font>"));
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
}

