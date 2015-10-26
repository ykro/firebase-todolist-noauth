package com.bitandik.labs.todolist.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bitandik.labs.todolist.R;
import com.bitandik.labs.todolist.ToDoApplication;
import com.bitandik.labs.todolist.models.ToDoItem;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.FirebaseRecyclerViewAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @Bind(R.id.recycler_view_items) RecyclerView recyclerView;
    @Bind(R.id.editTextItem) EditText editTextItem;

    private ChildEventListener toDoItemListener;
    private FirebaseRecyclerViewAdapter adapter;
    private Firebase dataReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setupUsername();
        SharedPreferences prefs = getApplication().getSharedPreferences("ToDoPrefs", 0);
        String username = prefs.getString("username", null);
        setTitle(username);

        ToDoApplication app = (ToDoApplication)getApplicationContext();
        dataReference = app.getFirebase();
        adapter = new FirebaseRecyclerViewAdapter<ToDoItem, ToDoItemViewHolder>(ToDoItem.class,
                                                                                R.layout.row,
                                                                                ToDoItemViewHolder.class,
                                                                                dataReference) {
            @Override
            public void populateViewHolder(ToDoItemViewHolder holder, ToDoItem item) {
                String itemDescription = item.getItem();
                String username = item.getUsername();

                holder.txtItem.setText(itemDescription);
                holder.txtUser.setText(username);

                if (item.isCompleted()) {
                    holder.imgDone.setVisibility(View.VISIBLE);
                } else {
                    holder.imgDone.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public ToDoItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(mModelLayout, parent, false);
                return new ToDoItemViewHolder(view);
            }

        };

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        toDoItemListener = dataReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }
    private void setupUsername() {
        SharedPreferences prefs = getApplication().getSharedPreferences("ToDoPrefs", 0);
        String username = prefs.getString("username", null);
        if (username == null) {
            Random r = new Random();
            username = "AndroidUser" + r.nextInt(100000);
            prefs.edit().putString("username", username).commit();
        }
    }

    @OnClick(R.id.fab)
    public void addToDoItem() {
        SharedPreferences prefs = getApplication().getSharedPreferences("ToDoPrefs", 0);
        String username = prefs.getString("username", null);

        String itemText = editTextItem.getText().toString();
        editTextItem.setText("");

        InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

        if (!itemText.isEmpty()) {
            ToDoItem toDoItem = new ToDoItem(itemText.trim(), username);
            dataReference.push().setValue(toDoItem);
        }
    }

    public class ToDoItemViewHolder extends RecyclerView.ViewHolder
                                           implements View.OnClickListener,
                                                      View.OnLongClickListener {
        @Bind(R.id.txtItem) TextView txtItem;
        @Bind(R.id.txtUser) TextView txtUser;
        @Bind(R.id.imgDone) ImageView imgDone;

        public ToDoItemViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            ButterKnife.bind(this, view);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            ToDoItem currentItem = (ToDoItem)adapter.getItem(position);
            Firebase reference = adapter.getRef(position);
            boolean completed = !currentItem.isCompleted();

            currentItem.setCompleted(completed);
            Map<String, Object> updates = new HashMap<String, Object>();
            updates.put("completed", completed);
            reference.updateChildren(updates);
        }

        @Override
        public boolean onLongClick(View view) {
            int position = getAdapterPosition();
            Firebase reference = adapter.getRef(position);
            reference.removeValue();
            return true;
        }
    }
}