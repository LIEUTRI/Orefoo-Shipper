package com.luanvan.shipper.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.luanvan.shipper.Adapter.RecyclerViewOrderAdapter;
import com.luanvan.shipper.R;
import com.luanvan.shipper.components.Branch;
import com.luanvan.shipper.components.CartItem;
import com.luanvan.shipper.components.Order;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.ResultsCode;
import com.luanvan.shipper.components.Shared;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class OrderFragment extends Fragment {

    private SwitchMaterial switchOrderStatus;
    private MaterialToolbar toolbar;
    private ImageButton ibRefresh;
    private RecyclerView recyclerView;

    private ArrayList<Order> orders = new ArrayList<>();

    private String token;

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

        // ProgressBar ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleSmall);
        layoutProgressBar.addView(progressBar, params);
        progressBar.setVisibility(View.INVISIBLE);
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        SharedPreferences sharedPreferences = Objects.requireNonNull(getActivity()).getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE);
        token = sharedPreferences.getString(Shared.KEY_BEARER, "");

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        switchOrderStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Toast.makeText(getActivity(), getResources().getString(R.string.receiving_orders), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.not_receiving_orders), Toast.LENGTH_LONG).show();
                }
                saveSettings();
            }
        });

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);

        ArrayList<CartItem> items = new ArrayList<>();
        items.add(new CartItem(1, "Com ga", "", 10000, 0, 2, 1, 1));
        orders.add(new Order(1, 20000, 10000, 10000, "Can Tho", "note", "2020-11-17T13:14:00.000+00:00",
                new Branch(1, "Com ga Le Trang","https://cdn.daynauan.info.vn/wp-content/uploads/2017/02/cha-gio-tom-thit.jpg", "3/2 Ninh Kiều, tp Cần Thơ"), 1, -1, "ordered", items));
        recyclerView.setAdapter(new RecyclerViewOrderAdapter(getActivity(), orders));
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



    @SuppressLint("StaticFieldLeak")
    class OrderTask extends AsyncTask<String,String,String> {
        private InputStream is;
        private int cartID;
        private int resultCode;

        public void getAllItem(int cartID){
            this.cartID = cartID;
            if (cartID != -1) execute();
        }

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
                URL url = new URL(RequestUrl.ORDER);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", token);
                connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                connection.setRequestProperty("Accept", "application/json;charset=utf-8");
                connection.connect();

                int statusCode = connection.getResponseCode();
                Log.i("statusCode", statusCode+"");

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
                    Log.d("ResponseCartItem: ", "> " + line);
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
                    Log.i("result", "get cart item success");
                    try {
                        JSONArray jsonArray = new JSONArray(s);
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
                    Toast.makeText(getContext(), "IO Exception", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}