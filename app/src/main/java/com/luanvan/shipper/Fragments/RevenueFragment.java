package com.luanvan.shipper.Fragments;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.luanvan.shipper.R;
import com.luanvan.shipper.components.RequestUrl;
import com.luanvan.shipper.components.ResultsCode;
import com.luanvan.shipper.components.Shared;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RevenueFragment extends Fragment {

    private String token;
    private int shipperId;
    private RelativeLayout layoutProgressBar;
    private ProgressBar progressBar;
    private TextView tvFromDay, tvToDay;
    private ImageButton ibSearch;
    private Calendar calendar = null;
    private DatePickerDialog.OnDateSetListener onDateSetListener = null;
    private TextView tvRevenueToday, tvRevenueRange;
    private SwipeRefreshLayout layoutRefresh;

    private String fromDay, toDay;
    private boolean fromDayClicked;

    public RevenueFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_revenue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        layoutProgressBar = view.findViewById(R.id.layoutProgressBar);
        tvFromDay = view.findViewById(R.id.tvFromDay);
        tvToDay = view.findViewById(R.id.tvToDay);
        ibSearch = view.findViewById(R.id.ibSearch);
        tvRevenueToday = view.findViewById(R.id.tvRevenueToday);
        tvRevenueRange = view.findViewById(R.id.tvRevenueRange);
        layoutRefresh = view.findViewById(R.id.layoutRefresh);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // ProgressBar ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleSmall);
        layoutProgressBar.addView(progressBar, params);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Shared.TOKEN, Context.MODE_PRIVATE);
        token = sharedPreferences.getString(Shared.KEY_BEARER, "");

        sharedPreferences = getActivity().getSharedPreferences(Shared.SHIPPER, Context.MODE_PRIVATE);
        shipperId = sharedPreferences.getInt(Shared.KEY_SHIPPER_ID, -1);

        fromDay = formatTime("yyyy-MM-dd", new Date(), new Locale("vi", "VN"));
        fromDay += " 00:00:00";
        toDay = formatTime("yyyy-MM-dd HH:mm:ss", new Date(), new Locale("vi", "VN"));
        tvFromDay.setText(formatTime("dd MMMM, yyyy", new Date(), new Locale("vi", "VN")));
        tvToDay.setText(formatTime("dd MMMM, yyyy", new Date(), new Locale("vi", "VN")));
        /////////////////////////////////////////////////////////////////////////
        calendar = Calendar.getInstance(new Locale("vi", "VN"));
        onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                if (fromDayClicked){
                    fromDay = formatTime("yyyy-MM-dd", calendar.getTime(), new Locale("vi", "VN"));
                    fromDay += " 00:00:00";
                    tvFromDay.setText(formatTime("dd MMMM, yyyy", calendar.getTime(), new Locale("vi", "VN")));
                } else {
                    toDay = formatTime("yyyy-MM-dd", calendar.getTime(), new Locale("vi", "VN"));
                    toDay += " 23:59:59";
                    tvToDay.setText(formatTime("dd MMMM, yyyy", calendar.getTime(), new Locale("vi", "VN")));
                }
            }
        };
        /////////////////////////////////////////////////////////////////////////

        String startToDay, endToDay;
        Calendar today = Calendar.getInstance(new Locale("vi", "VN"));

        today.setTimeZone(TimeZone.getTimeZone("Asia/Saigon"));
        Log.v("timezones", today.getTimeZone().getID());

        today.setTime(new Date());
        startToDay = formatTime("yyyy-MM-dd", today.getTime(), new Locale("vi", "VN"));
        startToDay += " 00:00:00";
        endToDay = formatTime("yyyy-MM-dd HH:mm:ss", new Date(), new Locale("vi", "VN"));

        String startToDayEncoded = "";
        String endToDayEncoded = "";
        try {
            startToDayEncoded = URLEncoder.encode(startToDay, "utf-8");
            endToDayEncoded = URLEncoder.encode(endToDay, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        new GetRevenueTask(startToDayEncoded, endToDayEncoded, true);

        ////////////////////////////////////////////////////////////////////////
        tvFromDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fromDayClicked = true;
                new DatePickerDialog(getActivity(), onDateSetListener,
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        tvToDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fromDayClicked = false;
                new DatePickerDialog(getActivity(), onDateSetListener,
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        ibSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fromDayEncoded = "";
                String toDayEncoded = "";
                try {
                    fromDayEncoded = URLEncoder.encode(fromDay, "utf-8");
                    toDayEncoded = URLEncoder.encode(toDay, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                new GetRevenueTask(fromDayEncoded, toDayEncoded, false);
            }
        });

        layoutRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                String startToDay, endToDay;
                Calendar today = Calendar.getInstance(new Locale("vi", "VN"));

                today.setTimeZone(TimeZone.getTimeZone("Asia/Saigon"));

                today.setTime(new Date());
                startToDay = formatTime("yyyy-MM-dd", today.getTime(), new Locale("vi", "VN"));
                startToDay += " 00:00:00";
                endToDay = formatTime("yyyy-MM-dd HH:mm:ss", new Date(), new Locale("vi", "VN"));
                new GetRevenueTask(startToDay, endToDay, true);
            }
        });
    }

    public static String formatTime(String timeFormat, Date time, Locale locale){
//        String timeFormat = "HH:mm dd MMMM, yyyy";
        SimpleDateFormat formatter;

        try {
            formatter = new SimpleDateFormat(timeFormat, locale);
        } catch(Exception e) {
            formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", locale);
        }
        return formatter.format(time);
    }

    @SuppressLint("StaticFieldLeak")
    class GetRevenueTask extends AsyncTask<String,String,String> {
        private InputStream is;
        private int resultCode;
        private String startTime, endTime;
        private boolean isToday;

        public GetRevenueTask(String startTime, String endTime, boolean isToday) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.isToday = isToday;
            execute();
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
                URL url = new URL(RequestUrl.ORDER + "/shipper/"+shipperId+"/income?startTime="+startTime+"&endTime="+endTime);
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

        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            progressBar.setVisibility(View.INVISIBLE);
            layoutRefresh.setRefreshing(false);

            switch (resultCode) {
                case ResultsCode.SUCCESS:
                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        if (isToday){
                            tvRevenueToday.setText(String.format("%,.0f", jsonObject.getDouble("income"))+" VNĐ");
                        } else
                            tvRevenueRange.setText(String.format("%,.0f", jsonObject.getDouble("income"))+" VNĐ");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case ResultsCode.FAILED:
                    Log.i("result", "get failed");
                    break;
                case ResultsCode.SOCKET_TIMEOUT:
                    Toast.makeText(getActivity(), getResources().getString(R.string.socket_timeout), Toast.LENGTH_SHORT).show();
                    break;
                case ResultsCode.IO_EXCEPTION:
                    Toast.makeText(getActivity(), "error", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}