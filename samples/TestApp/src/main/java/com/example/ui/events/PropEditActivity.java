/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.events;

import static com.example.testdata.TestEvents.TTContentParams;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.R;
import com.example.model.Property;
import com.example.utils.PropListAdapter;
import com.example.utils.SwipeDismissListener;

import java.util.ArrayList;
import java.util.Objects;

public class PropEditActivity extends AppCompatActivity {

    private ListView myList;
    private EditText eventProp;
    public static final String SOURCE_CONTENT = "content";
    public static final String SOURCE = "source";
    private ArrayList<Property> props;
    private PropListAdapter adapter;
    private Integer nextPropID = 0;
    private int resultCode = 2;

    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prop_editor);

        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.actionbar_layout);
        getSupportActionBar().setTitle("Property Editor");

        myList = findViewById(R.id.list);
        ImageButton fabImageButton = findViewById(R.id.fab_image_button);
        eventProp = findViewById(R.id.event_prop);

        props = new ArrayList<>();
        if(SOURCE_CONTENT.equals(getIntent().getStringExtra(SOURCE))){
            for (String key : TTContentParams) {
                nextPropID++;
                props.add(new Property(nextPropID.toString(), key, ""));
            }
            resultCode = 3;
            fabImageButton.setVisibility(View.GONE);
        }else {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    if (bundle.get(key) != null) {
                        nextPropID++;
                        props.add(new Property(nextPropID.toString(), key, (String) bundle.get(key)));
                    }
                }
            }
        }

        SwipeDismissListener touchListener =
                new SwipeDismissListener(
                        findViewById(R.id.list),
                        new SwipeDismissListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int id : reverseSortedPositions) {
                                    Property pty = (Property) adapter.getItem(id);
                                    if (pty != null) {
                                        props.remove(pty);
                                    }
                                }
                                updateListUI();
                            }
                        });
        findViewById(R.id.list).setOnTouchListener(touchListener);

        fabImageButton.setOnClickListener(v -> {
            adapter.notifyDataSetChanged();
            AlertDialog.Builder propBuilder = new AlertDialog.Builder(PropEditActivity.this);
            propBuilder.setTitle("Add a new Property");
            propBuilder.setMessage("Enter property name and value");

            LinearLayout layout = new LinearLayout(PropEditActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            lp.setMargins(40, 0, 40, 0);
            final EditText nameBox = new EditText(PropEditActivity.this);
            nameBox.setHint("Prop Name");
            nameBox.setLayoutParams(lp);
            layout.addView(nameBox);
            final EditText valueBox = new EditText(PropEditActivity.this);
            valueBox.setHint("Prop Value");
            valueBox.setLayoutParams(lp);
            layout.addView(valueBox);
            propBuilder.setView(layout);
            propBuilder.setPositiveButton("Add Prop", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    nextPropID++;
                    props.add(new Property(nextPropID.toString(), nameBox.getText().toString(), valueBox.getText().toString()));
                }
            });
            propBuilder.setNegativeButton("Cancel", null);
            propBuilder.create().show();
        });
        updateListUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.prop_action_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.done_btn) {
            Intent intent= new Intent();
            Bundle bundlePros = new Bundle();
            for (Property p : props) {
                if(!TextUtils.isEmpty(p.value)){
                    convertValue(bundlePros, p.key, p.value);
                }
            }
            if (!TextUtils.isEmpty(eventProp.getText())) {
                bundlePros.putString("event_prop", String.valueOf(eventProp.getText()));
                resultCode = 2;
            }
            intent.putExtras(bundlePros);
            setResult(resultCode, intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void convertValue(Bundle bundlePros, String key, String value) {
        switch (key){
            case "value":
                bundlePros.putFloat(key, Float.parseFloat(value));
                break;
            case "price":
                bundlePros.putFloat(key, Float.parseFloat(value));
                break;
            case "quantity":
                bundlePros.putInt(key, Integer.parseInt(value));
                break;
            default:
                bundlePros.putString(key, value);
        }
    }

    public void onDeleteButtonClick(View view) {
        View v = (View) view.getParent();
        TextView itemID = (TextView) v.findViewById(R.id.item_id);
        String itemString = itemID.getText().toString();
        for (Property p : props) {
            if (p._id.equals(itemString)) {
                props.remove(p);
                updateListUI();
                break;
            }
        }
    }

    private void updateListUI() {
        adapter = new PropListAdapter(this, props);
        myList.setAdapter(adapter);
    }

    public void onEditButtonClick(View view) {
        adapter.notifyDataSetChanged();
        View v = (View) view.getParent();
        TextView itemID = (TextView) v.findViewById(R.id.item_id);
        String itemString = itemID.getText().toString();
        Property property = null;
        for (Property p : props) {
            if (p._id.equals(itemString)) {
                property = p;
                break;
            }
        }
        // form to input prop name and value
        AlertDialog.Builder propBuilder = new AlertDialog.Builder(PropEditActivity.this);
        propBuilder.setTitle("Edit Property");
        propBuilder.setMessage("Enter property name and value");

        LinearLayout layout = new LinearLayout(PropEditActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(40, 0, 40, 0);
        final EditText nameBox = new EditText(PropEditActivity.this);
        nameBox.setHint("Prop Name");
        nameBox.setText(property.key);
        nameBox.setLayoutParams(lp);
        layout.addView(nameBox);
        final EditText valueBox = new EditText(PropEditActivity.this);
        valueBox.setHint("Prop Value");
        valueBox.setText(property.value);
        valueBox.setLayoutParams(lp);
        layout.addView(valueBox);
        propBuilder.setView(layout);
        final Property finalProperty = property;
        propBuilder.setPositiveButton("Save", (dialogInterface, i) -> props.set(props.indexOf(finalProperty), new Property(finalProperty._id, nameBox.getText().toString(), valueBox.getText().toString())));
        propBuilder.setNegativeButton("Cancel", null);
        propBuilder.create().show();
    }
}