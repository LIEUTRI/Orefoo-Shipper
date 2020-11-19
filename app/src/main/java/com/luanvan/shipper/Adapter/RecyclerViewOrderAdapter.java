package com.luanvan.shipper.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.luanvan.shipper.OrderDetailActivity;
import com.luanvan.shipper.R;
import com.luanvan.shipper.components.Order;
import com.luanvan.shipper.components.Shared;
import com.luanvan.shipper.components.Victual;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecyclerViewOrderAdapter extends RecyclerView.Adapter<RecyclerViewOrderAdapter.ViewHolder>{
    private List<Order> list;
    private Activity activity;
    private String token;
    public RecyclerViewOrderAdapter(Activity activity, List<Order> list){
        this.activity = activity;
        this.list = list;
    }
    @NonNull
    @Override
    public RecyclerViewOrderAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        SharedPreferences sharedPreferences = activity.getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE);
        token = sharedPreferences.getString(Shared.KEY_BEARER, "");

        View view = LayoutInflater.from(activity).inflate(R.layout.order, parent, false);
        return new RecyclerViewOrderAdapter.ViewHolder(view);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull final RecyclerViewOrderAdapter.ViewHolder holder, final int position) {
        final Order order = list.get(position);

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.loading)
                .error(R.drawable.image_not_found);
        Glide.with(activity).load(order.getBranch().getImageUrl()).apply(options).into(holder.ivBranch);

        holder.tvBranchName.setText(order.getBranch().getName());
        Calendar calendar = Calendar.getInstance();
        Timestamp timestamp = Timestamp.valueOf(order.getTime().substring(0, order.getTime().indexOf("+")).replace("T", " "));
        calendar.setTime(timestamp);
        calendar.add(Calendar.HOUR_OF_DAY, 7);
        holder.tvTime.setText(activity.getResources().getString(R.string.order_at)+calendar.getTime().toString().substring(0, calendar.getTime().toString().indexOf("GMT")-1));
        holder.tvAddress.setText(order.getShippingAddress());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Victual> victuals = new ArrayList<>();
                for (int index=0; index<order.getOrderItems().size(); index++){
                    victuals.add(new Victual(order.getOrderItems().get(index).getName(), order.getOrderItems().get(index).getImageUrl(),
                            order.getOrderItems().get(index).getQuantity(), order.getOrderItems().get(index).getPrice()+"", order.getOrderItems().get(index).getDiscount()+""));
                }
                Intent intent = new Intent(activity, OrderDetailActivity.class);
                intent.putExtra("imageUrl", order.getBranch().getImageUrl());
                intent.putExtra("name", order.getBranch().getName());
                intent.putExtra("address", order.getBranch().getAddress());
                intent.putExtra("shippingFee", order.getShippingFee());
                intent.putExtra("note", order.getNote());
                intent.putExtra("victualsPrice", order.getVictualsPrice());
                intent.putExtra("victuals", victuals);
                activity.startActivity(intent);
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvBranchName;
        public TextView tvTime;
        public TextView tvAddress;
        public ImageView ivBranch;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBranch = itemView.findViewById(R.id.ivBranch);
            tvBranchName = itemView.findViewById(R.id.tvBranchName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAddress = itemView.findViewById(R.id.tvAddress);
        }
    }
    @Override
    public int getItemCount() {
        return list.size();
    }
}
