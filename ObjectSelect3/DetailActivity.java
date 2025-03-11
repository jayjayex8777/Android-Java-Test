package com.example.objectselect2;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        String coordinate = getIntent().getStringExtra("coordinate");
        TextView detailTextView = findViewById(R.id.detailTextView);
        detailTextView.setText(coordinate);
    }
}
