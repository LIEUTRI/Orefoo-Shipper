package com.luanvan.shipper;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class OrderDetailActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tvBranchName, tvAddress;
    private ImageView ivBranch;
    private TextView tvAccept, tvReject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        ivBranch = findViewById(R.id.ivBranch);
        tvBranchName = findViewById(R.id.tvBranchName);
        tvAddress = findViewById(R.id.tvAddress);
        tvAccept = findViewById(R.id.tvAccept);
        tvReject = findViewById(R.id.tvReject);

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.loading)
                .error(R.drawable.image_not_found);
        Glide.with(this).load(getIntent().getStringExtra("imageUrl")).apply(options).into(ivBranch);

        tvBranchName.setText(getIntent().getStringExtra("name"));
        tvAddress.setText(getIntent().getStringExtra("address"));

        tvAccept.setOnClickListener(this);
        tvReject.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tvAccept:
                break;

            case R.id.tvReject:
                break;
        }
    }
}