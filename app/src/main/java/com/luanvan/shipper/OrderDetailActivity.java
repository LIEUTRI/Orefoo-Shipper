package com.luanvan.shipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.luanvan.shipper.Adapter.RecyclerViewVictualAdapter;
import com.luanvan.shipper.Fragments.ManagerFragment;
import com.luanvan.shipper.components.Branch;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.Shared;
import com.luanvan.shipper.components.Victual;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

public class OrderDetailActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tvBranchName, tvAddress;
    private ImageView ivBranch;
    private TextView tvAccept, tvReject;
    private RelativeLayout layoutProgressBar;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvTotal, tvShippingFee, tvTotalFinal;

    private String token;
    private int shipperId;

    private int orderId;
    private double totalPay;
    private double victualsPrice;
    private double shippingFee;
    private String shippingAddress;
    private double latitude, longitude;
    private double merchantCommission;
    private double shipperCommission;
    private String note;
    private String time;
    private Branch branch;
    private int consumer;
    private String orderStatus;
    private ArrayList<Victual> victuals = new ArrayList<>();

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        ivBranch = findViewById(R.id.ivBranch);
        tvBranchName = findViewById(R.id.tvBranchName);
        tvAddress = findViewById(R.id.tvAddress);
        tvAccept = findViewById(R.id.tvAccept);
        tvReject = findViewById(R.id.tvReject);
        layoutProgressBar = findViewById(R.id.layoutProgressBar);
        recyclerView = findViewById(R.id.recyclerView);
        tvTotal = findViewById(R.id.tvTotal);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvTotalFinal = findViewById(R.id.tvTotalFinal);

        orderId = getIntent().getIntExtra("id", -1);
        totalPay = getIntent().getDoubleExtra("totalPay", 0);
        victualsPrice = getIntent().getDoubleExtra("victualsPrice", 0);
        shippingFee = getIntent().getDoubleExtra("shippingFee", 0);
        shippingAddress = getIntent().getStringExtra("shippingAddress");
        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);
        merchantCommission = getIntent().getDoubleExtra("merchantCommission", 0);
        shipperCommission = getIntent().getDoubleExtra("shipperCommission", 0);
        note = getIntent().getStringExtra("note");
        time = getIntent().getStringExtra("time");
        branch = (Branch) getIntent().getSerializableExtra("branch");
        consumer = getIntent().getIntExtra("consumer", -1);
        orderStatus = getIntent().getStringExtra("ordered");
        victuals = (ArrayList<Victual>) getIntent().getSerializableExtra("orderItems");

        // ProgressBar ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        layoutProgressBar.addView(progressBar, params);
        progressBar.setVisibility(View.INVISIBLE);

        SharedPreferences sharedPreferences = getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE);
        token = sharedPreferences.getString(Shared.KEY_BEARER, "");

        sharedPreferences = getSharedPreferences(Shared.SHIPPER, MODE_PRIVATE);
        shipperId = sharedPreferences.getInt(Shared.KEY_SHIPPER_ID, -1);

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.loading)
                .error(R.drawable.image_not_found);
        Glide.with(this).load(branch.getImageUrl()).apply(options).into(ivBranch);

        tvBranchName.setText(branch.getName());
        tvAddress.setText(branch.getAddress());

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(new RecyclerViewVictualAdapter(this, victuals));

        tvTotal.setText(String.format("%,.0f", victualsPrice) + "đ");
        tvShippingFee.setText(String.format("%,.0f", shippingFee) + "đ");
        tvTotalFinal.setText(String.format("%,.0f", totalPay) + "đ");

        tvAccept.setOnClickListener(this);
        tvReject.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tvAccept:
                new AcceptOrderTask().execute(orderId+"", shipperId+"");
                break;

            case R.id.tvReject:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AcceptOrderTask extends AsyncTask<String,String,String> {
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
                URL url = new URL(RequestUrl.ORDER + "/" + strings[0] + "/shipper/" + strings[1]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PATCH");
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setRequestProperty("Authorization", token);
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(15000);
                connection.connect();

                int statusCode = connection.getResponseCode();
                Log.i("statusCode", statusCode+" | request: "+url);
                if (statusCode == 200) return "200";

                return "-1";

            } catch (SocketTimeoutException e) {
                return "0";
            } catch (IOException e){
                return "-1";
            } finally {
                if (connection != null) connection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            progressBar.setVisibility(View.INVISIBLE);

            if (s == null) return;
            if (s.equals("200")){
                Toast.makeText(OrderDetailActivity.this, getString(R.string.received_orders), Toast.LENGTH_LONG).show();

                Intent intent = new Intent();
                intent.putExtra("consumer", consumer);
                setResult(RESULT_OK);
                finish();
            } else if (s.equals("0")){
                Toast.makeText(OrderDetailActivity.this, getString(R.string.socket_timeout), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(OrderDetailActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setResult(RESULT_CANCELED);
    }
}