package in.codefane.dutylog;


import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import android.os.Bundle;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText employeeIdEditText;
    private EditText passwordEditText;
    private Button loginButtion;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        employeeIdEditText = findViewById(R.id.employee_id);
        passwordEditText = findViewById(R.id.password);
        loginButtion = findViewById(R.id.login_button);

        loginButtion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String employeeid = employeeIdEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (authenticate(employeeid, password)) {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid Employee id or password",Toast.LENGTH_SHORT).show();
                }
            }

        });
    }

        private boolean authenticate(String employeeid, String password){
            return "admin".equals(employeeid) && "1234".equals(password);
        }
    }

