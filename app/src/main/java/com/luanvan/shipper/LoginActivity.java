package com.luanvan.shipper;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.ResultsCode;
import com.luanvan.shipper.components.Shared;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private ProgressBar progressBar;
    private RelativeLayout layoutProgressBar;
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvGotoSignup;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        layoutProgressBar = findViewById(R.id.layoutProgressBar);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGotoSignup = findViewById(R.id.tvGotoSignup);
        toolbar = findViewById(R.id.toolbar);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layoutProgressBar.addView(progressBar, params);
        progressBar.setVisibility(View.INVISIBLE);

        btnLogin.setOnClickListener(this);
        tvGotoSignup.setOnClickListener(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnLogin:
                if (etUsername.getText().toString().equals("") || etPassword.getText().toString().equals("")){
                    Toast.makeText(this, getResources().getString(R.string.invalid_input), Toast.LENGTH_SHORT).show();
                } else
                    new LoginTask(etUsername.getText().toString(), etPassword.getText().toString());
                break;
            case R.id.tvGotoSignup:
                startActivity(new Intent(this, SignupActivity.class));
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoginTask extends AsyncTask<String,String,String> {
        private OutputStream os;
        private int resultCode;

        private String username, password;

        public LoginTask(String username, String password){
            this.username = username;
            this.password = password;
            execute();
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            //http post
            try {
                URL url = new URL( RequestUrl.LOGIN );

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", username);
                jsonObject.put("password", password);
                jsonObject.put("role", new JSONObject().put("name", "shipper"));
                String data = jsonObject.toString();

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setRequestProperty("Authorization", "application/json;charset=utf-8");
                connection.setDoOutput(true);
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(15000);
                connection.setFixedLengthStreamingMode(data.getBytes().length);
                connection.connect();

                os = new BufferedOutputStream(connection.getOutputStream());
                os.write(data.getBytes());
                os.flush();

                int statusCode = connection.getResponseCode();
                Log.i("statusCode", statusCode+"");

                if (statusCode == HttpURLConnection.HTTP_OK){
                    resultCode = ResultsCode.SUCCESS;
                    return connection.getHeaderField("Authorization");
                } else {
                    resultCode = ResultsCode.FAILED;
                    return "";
                }
            } catch (SocketTimeoutException e) {
                resultCode = ResultsCode.SOCKET_TIMEOUT;
            } catch (IOException | JSONException e){
                resultCode = ResultsCode.FAILED;
            } finally {
                try {
                    if (os!=null) os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (connection != null) connection.disconnect();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            progressBar.setVisibility(View.INVISIBLE);

            switch (resultCode){
                case ResultsCode.SUCCESS:
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.login_success), Toast.LENGTH_LONG).show();
                    SharedPreferences.Editor editor = getSharedPreferences(Shared.TOKEN, MODE_PRIVATE).edit();
                    editor.putString(Shared.KEY_BEARER, s);
                    editor.apply();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK| Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    break;

                case ResultsCode.SOCKET_TIMEOUT:
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.socket_timeout), Toast.LENGTH_LONG).show();
                    break;

                default:
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.login_failed), Toast.LENGTH_LONG).show();
            }
        }
    }
}