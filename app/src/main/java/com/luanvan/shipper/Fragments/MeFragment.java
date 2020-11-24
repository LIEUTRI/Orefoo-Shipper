package com.luanvan.shipper.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.android.jwt.Claim;
import com.auth0.android.jwt.JWT;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.luanvan.shipper.LoginActivity;
import com.luanvan.shipper.MainActivity;
import com.luanvan.shipper.ManagerProfileActivity;
import com.luanvan.shipper.R;
import com.luanvan.shipper.SignupActivity;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.ResultsCode;
import com.luanvan.shipper.components.Shared;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MeFragment extends Fragment implements View.OnClickListener {
    private MaterialButton btnLogin, btnSignup;
    private TextView tvAddress, tvManagerProfile, tvLogout;
    private RelativeLayout layoutProgressBar;
    private ProgressBar progressBar;
    private LinearLayout layoutNotLogin, layoutLogin;
    private TextView tvUsername, tvName;
    private CircleImageView ivProfile;

    private String token;
    private int userId = -1;
    private int shipperID;
    private String username = "";
    private String firstName = "";
    private String lastName = "";
    private String phoneNumber = "";
    private String dayOfBirth = "";
    private String gender = "";
    private String email = "";
    private String driverLicense = "";
    private String idCardNumber = "";
    private String idCardFront = "";
    private String idCardBack = "";
    private String profileImage = "";
    public MeFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_me, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnSignup = view.findViewById(R.id.btnSignup);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvManagerProfile = view.findViewById(R.id.tvManagerProfile);
        tvLogout = view.findViewById(R.id.tvLogout);
        layoutProgressBar = view.findViewById(R.id.layoutProfile);
        layoutNotLogin = view.findViewById(R.id.layoutNotLogin);
        layoutLogin = view.findViewById(R.id.layoutLogin);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvName = view.findViewById(R.id.tvName);
        ivProfile = view.findViewById(R.id.ivProfile);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleSmall);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(250, 250);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layoutProgressBar.addView(progressBar, params);
        progressBar.setVisibility(View.INVISIBLE);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE);
        token = sharedPreferences.getString(Shared.KEY_BEARER, "")+"";
        if (token.contains("Bearer")){
            // logged in
            userId = getUserId(token);
            username = getUsername(token);
        } else {
            layoutNotLogin.setVisibility(View.VISIBLE);
        }

        btnLogin.setOnClickListener(this);
        btnSignup.setOnClickListener(this);
        tvManagerProfile.setOnClickListener(this);
        tvLogout.setOnClickListener(this);
    }

    public int getUserId(String token){
        String TOKEN_PREFIX = "Bearer ";
        JWT jwt = new JWT(token.replace(TOKEN_PREFIX,""));
        Claim claim = jwt.getClaim("userId");
        return claim.asInt();
    }
    public String getUsername(String token){
        String TOKEN_PREFIX = "Bearer ";
        JWT jwt = new JWT(token.replace(TOKEN_PREFIX,""));
        return jwt.getSubject();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (userId > 0){
            new GetUserDataTask().execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetUserDataTask extends AsyncTask<String, String, String> {
        private int resultCode;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(RequestUrl.SHIPPER +"user/"+userId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", token);
                connection.setRequestProperty("Accept", "application/json;charset=utf-8");
                connection.connect();

                InputStream is = null;
                int statusCode = connection.getResponseCode();
                if (statusCode >= 200 && statusCode < 400){
                    is = connection.getInputStream();
                    resultCode = ResultsCode.SUCCESS;
                } else {
                    is = connection.getErrorStream();
                    resultCode = ResultsCode.FAILED;
                }

                reader = new BufferedReader(new InputStreamReader(is));

                StringBuilder buffer = new StringBuilder();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                    Log.d("Response: ", "> " + line);
                }
                return buffer.toString();
            } catch (SocketTimeoutException e) {
                resultCode = ResultsCode.SOCKET_TIMEOUT;
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressBar.setVisibility(View.INVISIBLE);
            layoutLogin.setVisibility(View.VISIBLE);

            switch (resultCode){
                case ResultsCode.SUCCESS:
                    try {
                        JSONObject consumer = new JSONObject(result);
                        tvManagerProfile.setEnabled(true);
                        shipperID = consumer.getInt("id");
                        firstName = consumer.getString("firstName").equals("null") ? "":consumer.getString("firstName");
                        lastName = consumer.getString("lastName").equals("null") ? "":consumer.getString("lastName");
                        phoneNumber = consumer.getString("phoneNumber").equals("null") ? "":consumer.getString("phoneNumber");
                        dayOfBirth = consumer.getString("dayOfBirth").equals("null") ? "":consumer.getString("dayOfBirth");
                        gender = consumer.getString("gender").equals("null") ? "":consumer.getString("gender");
                        email = consumer.getString("email").equals("null") ? "":consumer.getString("email");
                        driverLicense = consumer.getString("driverLicense").equals("null") ? "":consumer.getString("driverLicense");
                        idCardNumber = consumer.getString("idCardNumber").equals("null") ? "":consumer.getString("idCardNumber");
                        idCardFront = consumer.getString("idCardFront").equals("null") ? "":consumer.getString("idCardFront");
                        idCardBack = consumer.getString("idCardBack").equals("null") ? "":consumer.getString("idCardBack");
                        profileImage = consumer.getString("profileImage").equals("null") ? "":consumer.getString("profileImage");

                        tvUsername.setText(username);
                        tvName.setText(firstName.equals("null")&&lastName.equals("null") ? "":lastName+" "+firstName);

                        RequestOptions options = new RequestOptions()
                                .centerCrop()
                                .placeholder(R.drawable.ic_person_24)
                                .error(R.drawable.ic_person_24);
                        Glide.with(getActivity()).load(profileImage).apply(options).into(ivProfile);

                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Shared.SHIPPER, Context.MODE_PRIVATE).edit();
                        editor.putInt(Shared.KEY_SHIPPER_ID, shipperID);
                        editor.putString(Shared.KEY_FIRST_NAME, firstName);
                        editor.putString(Shared.KEY_LAST_NAME, lastName);
                        editor.putString(Shared.KEY_USERNAME, username);
                        editor.apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    break;

                    case ResultsCode.SOCKET_TIMEOUT:
                        Toast.makeText(getActivity(), getString(R.string.socket_timeout), Toast.LENGTH_SHORT).show();
                        break;

                    default:
                        Toast.makeText(getActivity(), getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnLogin:
                startActivity(new Intent(getActivity(), LoginActivity.class));
                break;
            case R.id.btnSignup:
                startActivity(new Intent(getActivity(), SignupActivity.class));
                break;
            case R.id.tvManagerProfile:
                Intent intent = new Intent(getActivity(), ManagerProfileActivity.class);
                intent.putExtra("id", shipperID);
                intent.putExtra("username", username);
                intent.putExtra("phoneNumber", phoneNumber);
                intent.putExtra("firstName", firstName);
                intent.putExtra("lastName", lastName);
                intent.putExtra("dayOfBirth", dayOfBirth);
                intent.putExtra("gender", gender);
                intent.putExtra("email", email);
                intent.putExtra("shipperID", shipperID);
                intent.putExtra("driverLicense", driverLicense);
                intent.putExtra("idCardNumber", idCardNumber);
                intent.putExtra("idCardFront", idCardFront);
                intent.putExtra("idCardBack", idCardBack);
                intent.putExtra("profileImage", profileImage);
                startActivity(intent);
                break;
            case R.id.tvLogout:
                SharedPreferences.Editor editor = Objects.requireNonNull(getActivity()).getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE).edit();
                editor.putString(Shared.KEY_BEARER, "");
                editor.apply();

                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.logout_success), Toast.LENGTH_LONG).show();

                layoutLogin.setVisibility(View.INVISIBLE);
                layoutNotLogin.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);

                Intent i = new Intent(getActivity(), LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                break;
        }
    }
}