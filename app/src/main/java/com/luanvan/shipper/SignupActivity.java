package com.luanvan.shipper;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

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

public class SignupActivity extends AppCompatActivity implements View.OnClickListener {
    private ProgressBar progressBar;
    private RelativeLayout layoutProgressBar;
    private EditText etUsername, etPassword, etConfirmPassword;
    private Button btnSignup;

    public final String TAG = "SignupActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        layoutProgressBar = findViewById(R.id.layoutProgressBar);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layoutProgressBar.addView(progressBar, params);
        progressBar.setVisibility(View.INVISIBLE);

        btnSignup.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnSignup:
                if (etUsername.getText().toString().equals("") || etPassword.getText().toString().equals("") ||
                        !etPassword.getText().toString().equals(etConfirmPassword.getText().toString())){
                    Toast.makeText(this, getResources().getString(R.string.invalid_input), Toast.LENGTH_SHORT).show();
                } else
                    new SignupTask(etUsername.getText().toString(), etPassword.getText().toString());
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SignupTask extends AsyncTask<String,String,String> {

        private OutputStream os;
        private InputStream is;
        private BufferedReader reader = null;
        private int resultCode;

        private final String username;
        private final String password;

        public SignupTask(String username, String password){
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
                URL url = new URL( RequestUrl.SIGNUP );

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", username);
                jsonObject.put("password", password);
                String data = jsonObject.toString();
                Log.i(TAG, "request: "+data);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setRequestProperty("Accept", "application/json;charset=utf-8");
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

                if (statusCode >= 200 && statusCode < 400){
                    resultCode = ResultsCode.SUCCESS;
                    is = connection.getInputStream();
                } else if (statusCode == 409){
                    resultCode = ResultsCode.CONFLICT;
                    is = connection.getErrorStream();
                } else {
                    resultCode = ResultsCode.FAILED;
                    is = connection.getErrorStream();
                }

                reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                    Log.d("ResponseSignup: ", ">>> " + line);
                }

                return buffer.toString();

            } catch (SocketTimeoutException e) {
                resultCode = ResultsCode.SOCKET_TIMEOUT;
            } catch (IOException e){
                resultCode = ResultsCode.IO_EXCEPTION;
            } catch (JSONException e){
                resultCode = ResultsCode.FAILED;
            } finally {
                try {
                    if (os != null && is != null) {
                        os.close();
                        is.close();
                    }
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

            switch (resultCode) {
                case ResultsCode.SUCCESS:
                    Toast.makeText(SignupActivity.this, getResources().getString(R.string.signup_success), Toast.LENGTH_LONG).show();

                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        SharedPreferences.Editor editor = getSharedPreferences(Shared.SHIPPER, MODE_PRIVATE).edit();
                        editor.putInt(Shared.KEY_SHIPPER_ID, jsonObject.getInt("id"));
                        editor.apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    break;
                case ResultsCode.SOCKET_TIMEOUT:
                    Toast.makeText(SignupActivity.this, getResources().getString(R.string.socket_timeout), Toast.LENGTH_LONG).show();
                    break;

                case ResultsCode.IO_EXCEPTION:
                    Toast.makeText(SignupActivity.this, getResources().getString(R.string.check_internet_conection), Toast.LENGTH_LONG).show();
                    break;

                case ResultsCode.CONFLICT:
                    Toast.makeText(SignupActivity.this, getResources().getString(R.string.account_conflict), Toast.LENGTH_LONG).show();
                    break;

                default:
                    Toast.makeText(SignupActivity.this, getResources().getString(R.string.signup_failed), Toast.LENGTH_LONG).show();
            }
        }
    }
}