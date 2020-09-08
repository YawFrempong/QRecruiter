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
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;
import static java.lang.Character.isWhitespace;

public class ListViewAdapter extends ArrayAdapter<String> implements Filterable {
    public CheckBox checkBox;
    public ImageView img_edit;
    
    private List<String> labels;
    private List<String> labelsFull;
    private Context context;
    private String m_Text = "";
    
    //constructor(load profile names)
    public ListViewAdapter(List<String> labels, Context context){
        super(context, R.layout.row_item, labels);
        this.context = context;
        this.labels = labels;
        labelsFull = new ArrayList<>(labels);
    }
    
    //customize each element in the listview
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();
        View row = inflater.inflate(R.layout.row_item,parent, false);
        
        //store activity content in variables
        //set labels and checkbox/edit button location
        TextView label = row.findViewById(R.id.txtName);
        label.setText(labels.get(position));
        checkBox = row.findViewById(R.id.checkBox);
        checkBox.setTag(position);
        img_edit = row.findViewById(R.id.editButton_1);
        img_edit.setTag(position);
        
        //toggle checkbox based on selected items
        if(ListActivity.userSelection.contains(labels.get(position))){
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }
        
        //hide or show checkboxes, and pencil based on whether delete, edit, or neither is selected
        if(ListActivity.deleteSelected && !ListActivity.editSelected){
            checkBox.setVisibility(View.VISIBLE);
        }
        else if(!ListActivity.deleteSelected && ListActivity.editSelected){
            img_edit.setVisibility(View.VISIBLE);
        }
        else{
            checkBox.setVisibility(View.GONE);
            img_edit.setVisibility(View.GONE);
        }
        
        //detect when the current checkbox is toggled update dependent variables
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int position = (int)buttonView.getTag();
                if(ListActivity.userSelection.contains(labels.get(position))){
                    ListActivity.userSelection.remove(labels.get(position));
                }
                else {
                    ListActivity.userSelection.add(labels.get(position));
                }
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
    @Override
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
    
    //handle input text for renaming the event(make sure input text is valid & no duplicate events) & update listview and dependent variables
    private void showEditItemDialog(Context c, String clicked_name) {
        final EditText taskEditText = new EditText(c);
        taskEditText.setSingleLine(true);
        taskEditText.setLines(1);
        taskEditText.setMaxLines(1);
        
        final AlertDialog dialog = new AlertDialog.Builder(c)
            .setTitle("Rename Event")
            .setMessage("Give a new name: ")
            .setView(taskEditText)
            .setPositiveButton("Save Changes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    m_Text = String.valueOf(taskEditText.getText());
                    
                    //update dependent variables and listview
                    if(!m_Text.isEmpty()){
                        for(int i = 0; i < ListActivity.profiles.size(); i++){
                            if(ListActivity.profiles.get(i).equals(clicked_name)){
                                ListActivity.profiles.set(i, m_Text);
                            }
                        }
                        
                        ArrayList<String> temp_names = new ArrayList<>();
                        
                        for(int i = 0; i < ListActivity.profiles.size(); i++){
                            temp_names.add(ListActivity.profiles.get(i));
                        }
                        
                        labels = (List<String>) temp_names.clone();
                        ArrayList<String> old_info = (ArrayList<String>)ListActivity.finalDocumentData.get(clicked_name);
                        ListActivity.finalDocumentData.remove(clicked_name);
                        ListActivity.finalDocumentData.put(m_Text, old_info);
                        LoginActivity.rootRef.child(ListActivity.currentUserUID).setValue(ListActivity.finalDocumentData);
                        ListActivity.loadListViewStatic();
                        notifyDataSetChanged();
                        return;
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
                    
                    if(ListActivity.profiles.contains(taskEditText.getText().toString()) || ListActivity.profiles.contains(taskEditText.getText().toString().trim()) || taskEditText.getText().toString().length() > 25){
                        if(ListActivity.profiles.contains(taskEditText.getText().toString()) || ListActivity.profiles.contains(taskEditText.getText().toString().trim())) {
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
}

