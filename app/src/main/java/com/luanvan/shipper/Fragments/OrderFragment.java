package com.luanvan.shipper.Fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.SystemClock;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.auth0.android.jwt.Claim;
import com.auth0.android.jwt.JWT;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.luanvan.shipper.Adapter.RecyclerViewOrderAdapter;
import com.luanvan.shipper.LoginActivity;
import com.luanvan.shipper.MainActivity;
import com.luanvan.shipper.ManagerProfileActivity;
import com.luanvan.shipper.R;
import com.luanvan.shipper.components.Branch;
import com.luanvan.shipper.components.CartItem;
import com.luanvan.shipper.components.Order;
import com.luanvan.shipper.components.RequestCode;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.ResultsCode;
import com.luanvan.shipper.components.Shared;
import com.luanvan.shipper.services.TrackingService;

import org.json.JSONArray;
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
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class OrderFragment extends Fragment implements View.OnClickListener {

    private SwitchMaterial switchOrderStatus;
    private MaterialToolbar toolbar;
    private ImageButton ibRefresh;
    private RecyclerView recyclerView;

    private ArrayList<Order> orders = new ArrayList<>();

    private String token;
    private int userId;
    private String userStatus;
    private final String TAG = "OrderFragment";

    private Service myService;
    private Intent mServiceIntent;

    private RelativeLayout layoutProgressBar;
    private ProgressBar progressBar;
    public OrderFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        switchOrderStatus = view.findViewById(R.id.switchOrderStatus);
        toolbar = view.findViewById(R.id.toolbar);
        ibRefresh = view.findViewById(R.id.ibRefresh);
        layoutProgressBar = view.findViewById(R.id.layoutProgressBar);
        recyclerView = view.findViewById(R.id.recyclerView);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        loadSettings();

        //Check this app has location permission
        int permission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //request permission
            ActivityCompat.requestPermissions(getActivity(), new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, RequestCode.REQUEST_PERMISSIONS);
        }

        // ProgressBar ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleSmall);
        layoutProgressBar.addView(progressBar, params);
        progressBar.setVisibility(View.INVISIBLE);
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //Check GPS is enabled
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getActivity().finish();
        }

        SharedPreferences sharedPreferences = Objects.requireNonNull(getActivity()).getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE);
        token = sharedPreferences.getString(Shared.KEY_BEARER, "");
        if (token.contains("Bearer")){
            // logged in
            userId = getUserId(token);

            new GetUserDataTask().execute();
            new GetShipperDataTask().execute();
        } else {
            startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        switchOrderStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Toast.makeText(getActivity(), getResources().getString(R.string.receiving_orders), Toast.LENGTH_LONG).show();
                    new GetUserDataTask().execute();
                    new GetShipperDataTask().execute();
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.not_receiving_orders), Toast.LENGTH_LONG).show();
                    if (myService != null)
                        getActivity().stopService(mServiceIntent);

                    recyclerView.setVisibility(View.INVISIBLE);
                }
                saveSettings();
            }
        });

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);

        ibRefresh.setOnClickListener(this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == RequestCode.REQUEST_PERMISSIONS && grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTrackerService();
        } else {
            Toast.makeText(getActivity(), "Please enable location services to allow GPS tracking", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTrackerService() {
        myService = new TrackingService();
        mServiceIntent = new Intent(getActivity(), myService.getClass());
        if (!isMyServiceRunning(myService.getClass())) {
            getActivity().startService(mServiceIntent);
            Toast.makeText(getActivity(), getString(R.string.tracking_enabled_notif), Toast.LENGTH_SHORT).show();
        }
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(50)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("Service status", "Running");
                return true;
            }
        }
        Log.i ("Service status", "Not running");
        return false;
    }

    private void saveSettings(){
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Shared.SETTINGS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(Shared.KEY_SWITCH_STATUS, switchOrderStatus.isChecked());

        editor.apply();
    }
    private void loadSettings(){
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Shared.SETTINGS, Context.MODE_PRIVATE);

        // updateUI
        switchOrderStatus.setChecked(sharedPreferences.getBoolean(Shared.KEY_SWITCH_STATUS, false));
    }

    public int getUserId(String token){
        String TOKEN_PREFIX = "Bearer ";
        JWT jwt = new JWT(token.replace(TOKEN_PREFIX,""));
        Claim claim = jwt.getClaim("userId");
        return claim.asInt();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ibRefresh){
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Shared.SHIPPER, Context.MODE_PRIVATE);
            String lat = sharedPreferences.getString(Shared.KEY_LATITUDE, "0");
            String lng = sharedPreferences.getString(Shared.KEY_LONGITUDE, "0");

            if (lat.equals("0") || lng.equals("0")) return;
            new GetOrderTask().execute(lat, lng);
        }
    }

    ////////////////////////////////////////////////////////
    @SuppressLint("StaticFieldLeak")
    private class GetUserDataTask extends AsyncTask<String, String, String> {

        private int resultCode;
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(RequestUrl.USER + userId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", token);
                connection.setRequestProperty("Accept", "application/json;charset=utf-8");
                connection.connect();

                InputStream is = null;
                int statusCode = connection.getResponseCode();
                if (statusCode >= 200 && statusCode < 400){
                    resultCode = ResultsCode.SUCCESS;
                    is = connection.getInputStream();
                } else {
                    resultCode = ResultsCode.FAILED;
                    is = connection.getErrorStream();
                }

                reader = new BufferedReader(new InputStreamReader(is));

                StringBuilder buffer = new StringBuilder();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                    Log.d("ResponseGetUser", "> " + line);
                }
                return buffer.toString();
            } catch (SocketTimeoutException e) {
                resultCode = ResultsCode.SOCKET_TIMEOUT;
            } catch (IOException e){
                resultCode = ResultsCode.IO_EXCEPTION;
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

            switch (resultCode){
                case ResultsCode.SUCCESS:
                    try {
                        JSONObject jsonUser = new JSONObject(result);

                        userStatus = jsonUser.getString("userStatus");

                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Shared.SHIPPER, Context.MODE_PRIVATE).edit();
                        editor.putString(Shared.KEY_USER_STATUS, userStatus);
                        editor.apply();

                        if (userStatus.equals("just_created")){

                        } else if (userStatus.equals("waiting_verify")){
                            switchOrderStatus.setChecked(false);
                            switchOrderStatus.setEnabled(false);
                            String sourceString = "<span style=\"text-align: center;\">"+getString(R.string.receive_order)+"</span><br><i style=\"font-size:8px;\">(Tài khoản chưa xác thực)</i>";
                            switchOrderStatus.setText(Html.fromHtml(sourceString));
                            ibRefresh.setEnabled(false);
                        } else {
                            switchOrderStatus.setText(getString(R.string.receive_order));
                            ibRefresh.setEnabled(true);
                        }
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

    ////////////////////////////////////////////////////////
    @SuppressLint("StaticFieldLeak")
    private class GetShipperDataTask extends AsyncTask<String, String, String> {

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
                URL url = new URL(RequestUrl.SHIPPER +"user/"+ userId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", token);
                connection.setRequestProperty("Accept", "application/json;charset=utf-8");
                connection.connect();

                InputStream is;
                int statusCode = connection.getResponseCode();
                if (statusCode >= 200 && statusCode < 400){
                    resultCode = ResultsCode.SUCCESS;
                    is = connection.getInputStream();
                } else {
                    resultCode = ResultsCode.FAILED;
                    is = connection.getErrorStream();
                }

                reader = new BufferedReader(new InputStreamReader(is));

                StringBuilder buffer = new StringBuilder();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                    Log.d("ResponseGetShipper ", "> " + line);
                }
                return buffer.toString();
            } catch (SocketTimeoutException e) {
                resultCode = ResultsCode.SOCKET_TIMEOUT;
            } catch (IOException e){
                resultCode = ResultsCode.IO_EXCEPTION;
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

            switch (resultCode){
                case ResultsCode.SUCCESS:
                    try {
                        JSONObject jsonShipper = new JSONObject(result);

                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Shared.SHIPPER, Context.MODE_PRIVATE).edit();
                        editor.putInt(Shared.KEY_SHIPPER_ID, jsonShipper.getInt("id"));
                        editor.putString(Shared.KEY_FIRST_NAME, jsonShipper.getString("firstName"));
                        editor.putString(Shared.KEY_LAST_NAME, jsonShipper.getString("lastName"));
                        editor.apply();

                        if (userStatus.equals("just_created")){
                            startActivity(new Intent(getActivity(), ManagerProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        }

                        //Check this app has location permission
                        int permission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
                        if (permission == PackageManager.PERMISSION_GRANTED) {
                            if (!isMyServiceRunning(TrackingService.class) && !userStatus.equals("just_created") && !userStatus.equals("waiting_verify")){
                                startTrackerService();

                                int i = 0;
                                while (i < 20){

                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Shared.SHIPPER, Context.MODE_PRIVATE);
                                    if (!sharedPreferences.getString(Shared.KEY_LATITUDE, "0").equals("0")){
                                        Log.i(TAG, "shipper location: " + sharedPreferences.getString(Shared.KEY_LATITUDE, "0") +
                                                sharedPreferences.getString(Shared.KEY_LONGITUDE, "0"));

                                        String lat = sharedPreferences.getString(Shared.KEY_LATITUDE, "0");
                                        String lng = sharedPreferences.getString(Shared.KEY_LONGITUDE, "0");

                                        new GetOrderTask().execute(lat, lng);

                                        break;
                                    }
                                    i++;
                                    Log.i(TAG, "count: "+i);
                                }
                            }
                        } else {
                            //request permission
                            ActivityCompat.requestPermissions(getActivity(), new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, RequestCode.REQUEST_PERMISSIONS);
                        }
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



    @SuppressLint("StaticFieldLeak")
    class GetOrderTask extends AsyncTask<String,String,String> {
        private InputStream is;
        private int resultCode;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(RequestUrl.ORDER + "/near?latitude="+strings[0]+"&longitude="+strings[1]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", token);
                connection.setRequestProperty("Accept", "application/json;charset=utf-8");
                connection.connect();

                int statusCode = connection.getResponseCode();
                Log.i("statusCode", statusCode+" | request: " + url);

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
                while ((line = reader.readLine()) != null){
                    buffer.append(line).append("\n");
                    Log.d("ResponseOrder: ", "> " + line);
                }

                return buffer.toString();
            } catch (SocketTimeoutException e) {
                resultCode = ResultsCode.SOCKET_TIMEOUT;
            } catch (IOException e){
                resultCode = ResultsCode.IO_EXCEPTION;
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
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            progressBar.setVisibility(View.INVISIBLE);

            if (s == null) return;

            switch (resultCode) {
                case ResultsCode.SUCCESS:
                    try {
                        orders.clear();
                        JSONArray jsonArray = new JSONArray(s);
                        for (int i=0; i<jsonArray.length(); i++){
                            JSONObject jsonOrder = jsonArray.getJSONObject(i);
                            JSONObject jsonBranch = jsonOrder.getJSONObject("branch");
                            JSONArray jsonOrderItems = jsonOrder.getJSONArray("orderItems");

                            Branch branch = new Branch(jsonBranch.getInt("id"), jsonBranch.getString("name"),
                                    jsonBranch.getString("phoneNumber"), jsonBranch.getString("imageUrl"),
                                    jsonBranch.getString("openingTime"), jsonBranch.getString("closingTime"),
                                    jsonBranch.getString("address"), jsonBranch.getDouble("latitude"), jsonBranch.getDouble("longitude"),
                                    jsonBranch.getBoolean("isSell"), jsonBranch.getInt("merchant"), jsonBranch.getString("branchStatus"));

                            ArrayList<CartItem> items = new ArrayList<>();
                            for (int j=0; j<jsonOrderItems.length(); j++){
                                JSONObject jsonOrderItem = jsonOrderItems.getJSONObject(j);
                                items.add(new CartItem(jsonOrderItem.getInt("id"), jsonOrderItem.getString("name"), jsonOrderItem.getString("imageUrl"),
                                        jsonOrderItem.getDouble("price"), jsonOrderItem.getDouble("discount"), jsonOrderItem.getInt("quantity"),
                                        jsonOrderItem.getInt("order"), jsonOrderItem.getInt("victuals")));
                            }

                            orders.add(new Order(jsonOrder.getInt("id"), jsonOrder.getDouble("totalPay"), jsonOrder.getDouble("victualsPrice"),
                                    jsonOrder.getDouble("shippingFee"), jsonOrder.getString("shippingAddress"), jsonOrder.getDouble("latitude"),
                                    jsonOrder.getDouble("longitude"), jsonOrder.getDouble("merchantCommission"), jsonOrder.getDouble("shipperCommission"),
                                    jsonOrder.getString("note"), jsonOrder.getString("time"), branch, jsonOrder.getInt("consumer"), -1, jsonOrder.getString("orderStatus"), items));
                        }

                        recyclerView.setAdapter(new RecyclerViewOrderAdapter(getActivity(), orders));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case ResultsCode.FAILED:
                    Log.i("result", "get failed");
                    break;
                case ResultsCode.SOCKET_TIMEOUT:
                    Toast.makeText(getContext(), getContext().getResources().getString(R.string.socket_timeout), Toast.LENGTH_SHORT).show();
                    break;
                case ResultsCode.IO_EXCEPTION:
                    Toast.makeText(getContext(), "error", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}