package com.luanvan.shipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.luanvan.shipper.Adapter.RecyclerViewOrderAdapter;
import com.luanvan.shipper.Adapter.RecyclerViewVictualAdapter;
import com.luanvan.shipper.components.Branch;
import com.luanvan.shipper.components.CartItem;
import com.luanvan.shipper.components.Order;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.ResultsCode;
import com.luanvan.shipper.components.Shared;
import com.luanvan.shipper.components.Victual;

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
import java.util.Objects;

public class DeliveryHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RelativeLayout layoutProgressBar;
    private ProgressBar progressBar;
    private Spinner spinner;
    private MaterialToolbar toolbar;

    private int orderStatusId = 4;
    private String token;
    private int shipperId;
    private ArrayList<Order> orders = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_history);

        layoutProgressBar = findViewById(R.id.layoutProgressBar);
        recyclerView = findViewById(R.id.recyclerView);
        spinner = findViewById(R.id.spinner);
        toolbar = findViewById(R.id.toolbar);

        // ProgressBar ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        layoutProgressBar.addView(progressBar, params);

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        SharedPreferences sharedPreferences = getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE);
        token = sharedPreferences.getString(Shared.KEY_BEARER, "");

        sharedPreferences = getSharedPreferences(Shared.SHIPPER, Context.MODE_PRIVATE);
        shipperId = sharedPreferences.getInt(Shared.KEY_SHIPPER_ID, -1);

        new GetDeliveryHistoryTask().execute(shipperId+"", "4");

        String[] items = new String[]{getString(R.string.success_delivery), getString(R.string.cancel_order)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        new GetDeliveryHistoryTask().execute(shipperId+"", "4"); // success
                        break;
                    case 1:
                        new GetDeliveryHistoryTask().execute(shipperId+"", "6"); // shipper_cancel
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    class GetDeliveryHistoryTask extends AsyncTask<String,String,String> {
        private InputStream is;
        private int resultCode;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
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
                    Log.d("ResponseHistory: ", "> " + line);
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
                                    jsonOrder.getString("note"), jsonOrder.getString("time"), branch, jsonOrder.getInt("consumer"), jsonOrder.getInt("shipper"), jsonOrder.getString("orderStatus"), items));
                        }

                        recyclerView.setAdapter(new RecyclerViewOrderAdapter(DeliveryHistoryActivity.this, orders));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case ResultsCode.FAILED:
                    Log.i("result", "get failed");
                    break;
                case ResultsCode.SOCKET_TIMEOUT:
                    Toast.makeText(DeliveryHistoryActivity.this, getResources().getString(R.string.socket_timeout), Toast.LENGTH_SHORT).show();
                    break;
                case ResultsCode.IO_EXCEPTION:
                    Toast.makeText(DeliveryHistoryActivity.this, "error", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}