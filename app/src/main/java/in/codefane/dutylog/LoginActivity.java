package in.codefane.dutylog;

import android.util.Log;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import android.os.Bundle;
import android.content.Intent;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

interface ApiService {
    @POST("api-token-auth/")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);
}

class LoginRequest {
    private String username;
    private String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

class LoginResponse {
    private String token;

    public String getToken() {
        return token;
    }
}

class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String baseUrl) {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

public class LoginActivity extends AppCompatActivity {
    private EditText employeeIdEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ApiService apiService;
    private static final String TAG = "LoginActivity";
    private Sqlitedatabase dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        dbHelper=new Sqlitedatabase(this);

        Retrofit retrofit = RetrofitClient.getClient("http://dutylog.codefane.in/");
        apiService = retrofit.create(ApiService.class);

        employeeIdEditText = findViewById(R.id.employee_id);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String employeeid = employeeIdEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                loginUser(employeeid, password);
            }
        });
    }

    private void loginUser(String employeeid, String password) {
        LoginRequest loginRequest = new LoginRequest(employeeid, password);
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                LoginResponse loginResponse = response.body();
                String token=loginResponse.getToken();
                if (response.isSuccessful() && token != null) {
                    dbHelper.saveToken(token);

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid Employee ID or password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Error: " + t.getMessage());
            }
        });
    }
}







