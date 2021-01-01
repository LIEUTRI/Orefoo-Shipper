package com.luanvan.shipper.Fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.luanvan.shipper.Adapter.RecyclerViewOrderAdapter;
import com.luanvan.shipper.R;
import com.luanvan.shipper.components.Branch;
import com.luanvan.shipper.components.CartItem;
import com.luanvan.shipper.components.Order;
import com.luanvan.shipper.components.RequestCode;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.ResultsCode;
import com.luanvan.shipper.components.Shared;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

public class ManagerFragment extends Fragment {
    public final String TAG = "ManagerFragment";
    private RecyclerView recyclerViewAccepted, recyclerViewPicked;
    private RelativeLayout layoutProgressBar1, layoutProgressBar2;
    private ProgressBar progressBar1, progressBar2;
    private ArrayList<Order> orders = new ArrayList<>();
    private ArrayList<Order> orders2 = new ArrayList<>();

    private String token;
    private int shipperId;

    public ManagerFragment() { }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manager, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerViewAccepted = view.findViewById(R.id.recyclerViewAccepted);
        recyclerViewPicked = view.findViewById(R.id.recyclerViewPicked);
        layoutProgressBar1 = view.findViewById(R.id.layoutProgressBar1);
        layoutProgressBar2 = view.findViewById(R.id.layoutProgressBar2);
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LinearLayoutManager linearLayoutManager1 = new LinearLayoutManager(getActivity());
        recyclerViewAccepted.setHasFixedSize(true);
        recyclerViewAccepted.setLayoutManager(linearLayoutManager1);
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(getActivity());
        recyclerViewPicked.setHasFixedSize(true);
        recyclerViewPicked.setLayoutManager(linearLayoutManager2);

        // ProgressBar ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        progressBar1 = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleSmall);
        progressBar2 = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleSmall);

        layoutProgressBar1.addView(progressBar1, params);
        layoutProgressBar2.addView(progressBar2, params);
        progressBar1.setVisibility(View.INVISIBLE);
        progressBar2.setVisibility(View.INVISIBLE);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE);
        token = sharedPreferences.getString(Shared.KEY_BEARER, "");
        sharedPreferences = getActivity().getSharedPreferences(Shared.SHIPPER, Context.MODE_PRIVATE);
        shipperId = sharedPreferences.getInt(Shared.KEY_SHIPPER_ID, -1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case RequestCode.ORDER_STATUS:
                if (resultCode == Activity.RESULT_OK){
                    new GetOrderAcceptedTask().execute(shipperId+"", "2");
                    new GetOrderPickedTask().execute(shipperId+"", "3");
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (token.contains("Bearer")){
            new GetOrderAcceptedTask().execute(shipperId+"", "2"); //get accepted orders
            new GetOrderPickedTask().execute(shipperId+"", "3"); //get picked orders
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("StaticFieldLeak")
    class GetOrderAcceptedTask extends AsyncTask<String,String,String> {
        private InputStream is;
        private int resultCode;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar1.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(RequestUrl.ORDER + "/shipper/"+strings[0]+"?status-id="+strings[1]);
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
                String line;
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

            progressBar1.setVisibility(View.INVISIBLE);

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
                                    jsonOrder.getString("note"), jsonOrder.getString("time"), branch, jsonOrder.getInt("consumer"), jsonOrder.getInt("shipper"), jsonOrder.getString("orderStatus"), items));
                        }

                        recyclerViewAccepted.setAdapter(new RecyclerViewOrderAdapter(getActivity(), orders));
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
    @SuppressLint("StaticFieldLeak")
    class GetOrderPickedTask extends AsyncTask<String,String,String> {
        private InputStream is;
        private int resultCode;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar2.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(RequestUrl.ORDER + "/shipper/"+strings[0]+"?status-id="+strings[1]);
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
                String line;
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

            progressBar2.setVisibility(View.INVISIBLE);

            if (s == null) return;

            switch (resultCode) {
                case ResultsCode.SUCCESS:
                    try {
                        orders2.clear();

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

                            orders2.add(new Order(jsonOrder.getInt("id"), jsonOrder.getDouble("totalPay"), jsonOrder.getDouble("victualsPrice"),
                                    jsonOrder.getDouble("shippingFee"), jsonOrder.getString("shippingAddress"), jsonOrder.getDouble("latitude"),
                                    jsonOrder.getDouble("longitude"), jsonOrder.getDouble("merchantCommission"), jsonOrder.getDouble("shipperCommission"),
                                    jsonOrder.getString("note"), jsonOrder.getString("time"), branch, jsonOrder.getInt("consumer"), jsonOrder.getInt("shipper"), jsonOrder.getString("orderStatus"), items));
                        }

                        recyclerViewPicked.setAdapter(new RecyclerViewOrderAdapter(getActivity(), orders2));
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