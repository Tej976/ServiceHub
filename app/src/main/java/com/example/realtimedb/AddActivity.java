package com.example.realtimedb;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class AddActivity extends AppCompatActivity {

    private EditText quoteeditText;
    private EditText authoreditText;
    private Button addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        quoteeditText = findViewById(R.id.editTextQuote);
        authoreditText = findViewById(R.id.editTextAuthor);
        addButton = findViewById(R.id.addButton);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String quote = quoteeditText.getText().toString();
                String author = authoreditText.getText().toString();

                if(quote.isEmpty()) {
                    quoteeditText.setError("Can't be empty");
                }
                if(author.isEmpty()) {
                    quoteeditText.setError("Can't be empty");
                }

                addQuoteToDB(quote, author);

            }

            private void addQuoteToDB(String quote, String author) {

                HashMap<String , Object> quoteHashMap = new HashMap<>();
                quoteHashMap.put("quote", quote);
                quoteHashMap.put("author", author);

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference quoteRef = database.getReference("quotes");

                String key = quoteRef.push().getKey();
                quoteHashMap.put("key", key);

                quoteRef.child(key).setValue(quoteHashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(AddActivity.this, "Added", Toast.LENGTH_SHORT).show();
                        quoteeditText.getText().clear();
                        authoreditText.getText().clear();
                    }
                });
            }
        });
    }
}