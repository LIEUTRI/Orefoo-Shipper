package com.luanvan.shipper.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.luanvan.shipper.R;
import com.luanvan.shipper.components.Victual;

import java.util.List;

public class RecyclerViewVictualAdapter extends RecyclerView.Adapter<RecyclerViewVictualAdapter.ViewHolder> {
    private List<Victual> list;
    private Activity activity;

    public RecyclerViewVictualAdapter(Activity activity, List<Victual> list){
        this.activity = activity;
        this.list = list;
    }
    @NonNull
    @Override
    public RecyclerViewVictualAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.victual_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull final RecyclerViewVictualAdapter.ViewHolder holder, final int position) {
        final Victual victual = list.get(position);

        final double price = Double.parseDouble(victual.getPrice().equals("null") ? "0":victual.getPrice());
        final double discount = Double.parseDouble(victual.getDiscountPrice().equals("null") ? "0":victual.getDiscountPrice());
        holder.tvName.setText(victual.getName());
        if (victual.getQuantity()==0){
            holder.tvQuantity.setVisibility(View.INVISIBLE);
        } else {
            holder.tvQuantity.setText("x"+victual.getQuantity());
            holder.tvQuantity.setVisibility(View.VISIBLE);
        }

        String priceTotal = String.format("%,.0f", (price-discount))+"đ";
        holder.tvPrice.setText(priceTotal);
        holder.tvPriceOrigin.setPaintFlags(holder.tvPriceOrigin.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        String priceOrigin = (price-discount)==price ? "":String.format("%,.0f", price)+"đ";
        holder.tvPriceOrigin.setText(priceOrigin);

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.loading)
                .error(R.drawable.image_not_found);
        Glide.with(activity).load(victual.getImageUrl()).apply(options).into(holder.ivVictual);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName;
        public TextView tvPrice;
        public TextView tvPriceOrigin;
        public TextView tvQuantity;
        public ImageView ivVictual;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvPriceOrigin = itemView.findViewById(R.id.tvPriceOrigin);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            ivVictual = itemView.findViewById(R.id.ivVictual);
        }
    }
    @Override
    public int getItemCount() {
        return list.size();
    }
}