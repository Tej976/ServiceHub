package com.example.servicehub.Book;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.servicehub.R;

import java.util.ArrayList;
import java.util.Arrays;
public class ServiceSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_selection);

        ListView serviceListView = findViewById(R.id.serviceListView);

        // Sample service list
        final ArrayList<String> services = new ArrayList<>(Arrays.asList(
                "Haircut - $30",
                "Massage - $60",
                "Manicure - $25",
                "Pedicure - $35",
                "Facial - $45"
        ));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                services
        );

        serviceListView.setAdapter(adapter);

        serviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedService = services.get(position);
                Intent intent = new Intent(ServiceSelectionActivity.this, ScheduleActivity.class);
                intent.putExtra("selectedService", selectedService);
                startActivity(intent);
            }
        });
    }
}
