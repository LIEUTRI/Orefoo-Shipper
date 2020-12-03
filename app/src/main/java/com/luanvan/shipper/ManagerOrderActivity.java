package com.luanvan.shipper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.luanvan.shipper.Adapter.RecyclerViewVictualAdapter;
import com.luanvan.shipper.components.Branch;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.Shared;
import com.luanvan.shipper.components.Victual;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

public class ManagerOrderActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvBranchName, tvAddress;
    private ImageView ivBranch;
    private TextView btnShipping, btnShipped, btnCancelOrder;
    private RelativeLayout layoutProgressBar;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvTotal, tvMerchantCommission, tvTienDuaChoMerchant, tvTienThuCuaKhach;
    private MaterialToolbar toolbar;

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
        setContentView(R.layout.activity_manager_order);

        ivBranch = findViewById(R.id.ivBranch);
        tvBranchName = findViewById(R.id.tvBranchName);
        tvAddress = findViewById(R.id.tvAddress);
        btnShipping = findViewById(R.id.btnShipping);
        layoutProgressBar = findViewById(R.id.layoutProgressBar);
        recyclerView = findViewById(R.id.recyclerView);
        btnShipped = findViewById(R.id.btnShipped);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        tvTotal = findViewById(R.id.tvTotal);
        tvMerchantCommission = findViewById(R.id.tvMerchantCommission);
        tvTienDuaChoMerchant = findViewById(R.id.tvTienDuaChoMerchant);
        tvTienThuCuaKhach = findViewById(R.id.tvTienThuCuaKhach);
        toolbar = findViewById(R.id.toolbar);

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
        orderStatus = getIntent().getStringExtra("orderStatus");
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
        tvMerchantCommission.setText(String.format("%,.0f", merchantCommission) + "đ");
        tvTienDuaChoMerchant.setText(String.format("%,.0f", victualsPrice-merchantCommission) + "đ");
        tvTienThuCuaKhach.setText(String.format("%,.0f", totalPay) + "đ");

        btnShipping.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_check_24), null,null,null);
        btnShipped.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_done_all_24), null,null,null);
        btnCancelOrder.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_cancel_24), null,null,null);

        switch (orderStatus){
            case "accepted":
                btnShipping.setEnabled(true);
                break;

            case "picked":
                btnShipping.setEnabled(false);
                break;
        }

        btnShipping.setOnClickListener(this);
        btnShipped.setOnClickListener(this);
        btnCancelOrder.setOnClickListener(this);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.btnMap){
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+latitude+","+longitude+"&travelmode=DRIVING"));
                    startActivity(intent);
                }
                return true;
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnShipping){
            showDialogConfirm(this, getString(R.string.confirm_picked), v.getId());
        } else if (v.getId() == R.id.btnShipped){
            showDialogConfirm(this, getString(R.string.confirm_shipped), v.getId());
        } else if (v.getId() == R.id.btnCancelOrder){
            showDialogConfirm(this, getString(R.string.confirm_cancel), v.getId());
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void showDialogConfirm(Context context, String message, int id){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.confirm));
        builder.setMessage(message);
        builder.setIcon(getDrawable(R.drawable.ic_warning_24));

        switch (id){
            case R.id.btnShipping:
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new OrderTask().execute("3");
                    }
                });
                break;

            case R.id.btnShipped:
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new OrderTask().execute("4");
                    }
                });
                break;

            case R.id.btnCancelOrder:
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new OrderTask().execute("6"); //shipper cancel order
                    }
                });
                break;
        }

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.show();
    }

    @SuppressLint("StaticFieldLeak")
    private class OrderTask extends AsyncTask<String,String,String> {
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
                URL url = new URL(RequestUrl.ORDER + "/" + orderId + "/status/" + strings[0]);
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
                Toast.makeText(ManagerOrderActivity.this, "OK", Toast.LENGTH_LONG).show();

                Log.d("orderStatus", orderStatus);

                Intent intent = new Intent();
                intent.putExtra("orderStatus", orderStatus);
                setResult(RESULT_OK, intent);
                finish();
            } else if (s.equals("0")){
                Toast.makeText(ManagerOrderActivity.this, getString(R.string.socket_timeout), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ManagerOrderActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
            }
        }
    }
}