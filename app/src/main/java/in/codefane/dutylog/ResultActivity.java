package in.codefane.dutylog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);

        // Get distance from intent
        Intent intent = getIntent();
        String distance = intent.getStringExtra("distance");

        // Display the distance
        TextView resultTextView = findViewById(R.id.total_distance);
        resultTextView.setText("Distance Travelled: " + distance);
    }
}
