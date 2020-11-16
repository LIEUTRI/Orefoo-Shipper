package com.luanvan.shipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.luanvan.shipper.Adapter.ViewPagerFragmentAdapter;
import com.luanvan.shipper.Fragments.ManagerFragment;
import com.luanvan.shipper.Fragments.MeFragment;
import com.luanvan.shipper.Fragments.OrderFragment;
import com.luanvan.shipper.Fragments.RevenueFragment;
import com.luanvan.shipper.components.RequestCode;
import com.luanvan.shipper.services.TrackingService;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Service myService;
    private Intent mServiceIntent;

    private ViewPager2 viewPager;
    private ViewPagerFragmentAdapter viewPagerFragmentAdapter;
    private ViewPager2.OnPageChangeCallback onPageChangeCallback;
    private TextView tvOrder,tvManager,tvRevenue,tvMe;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewpager);
        tvOrder = findViewById(R.id.tvOrder);
        tvManager = findViewById(R.id.tvManager);
        tvRevenue = findViewById(R.id.tvRevenue);
        tvMe = findViewById(R.id.tvMe);

        tvOrder.setOnClickListener(this);
        tvManager.setOnClickListener(this);
        tvRevenue.setOnClickListener(this);
        tvMe.setOnClickListener(this);

        //Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            finish();
        }

        //Check this app has location permission
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        //If the location permission has been granted, then start the TrackerService
        if (permission == PackageManager.PERMISSION_GRANTED) {
//            startTrackerService();
        } else {
            //request permission
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, RequestCode.REQUEST_PERMISSIONS);
        }

        // ViewPager
        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(new OrderFragment());
        fragments.add(new ManagerFragment());
        fragments.add(new RevenueFragment());
        fragments.add(new MeFragment());

        viewPagerFragmentAdapter = new ViewPagerFragmentAdapter(getSupportFragmentManager(), getLifecycle());
        viewPagerFragmentAdapter.setFragments(fragments);
        viewPager.setAdapter(viewPagerFragmentAdapter);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.setPageTransformer(new MarginPageTransformer(500));
        onPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position){
                    case 0: updateIconUI(tvOrder);   break;
                    case 1: updateIconUI(tvManager); break;
                    case 2: updateIconUI(tvRevenue); break;
                    case 3: updateIconUI(tvMe);      break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        };
        viewPager.registerOnPageChangeCallback(onPageChangeCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == RequestCode.REQUEST_PERMISSIONS && grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTrackerService();
        } else {
            Toast.makeText(this, "Please enable location services to allow GPS tracking", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTrackerService() {
        myService = new TrackingService();
        mServiceIntent = new Intent(this, myService.getClass());
        if (!isMyServiceRunning(myService.getClass())) {
            startService(mServiceIntent);
            Toast.makeText(this, "GPS tracking enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(50)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("Service status", "Running");
                return true;
            }
        }
        Log.i ("Service status", "Not running");
        return false;
    }

    private void setTextViewDrawableColor(TextView textView, int color){
        for (Drawable drawable: textView.getCompoundDrawables()){
            if (drawable != null){
                drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            }
        }
    }
    private void updateIconUI(TextView textView){
        switch (textView.getId()){
            case R.id.tvOrder:
                // update icon color
                setTextViewDrawableColor(tvOrder, getResources().getColor(R.color.colorPrimary));
                setTextViewDrawableColor(tvManager, getResources().getColor(R.color.defaultIconColor));
                setTextViewDrawableColor(tvRevenue, getResources().getColor(R.color.defaultIconColor));
                setTextViewDrawableColor(tvMe, getResources().getColor(R.color.defaultIconColor));
                // update text color
                tvOrder.setTextColor(getResources().getColor(R.color.colorPrimary));
                tvManager.setTextColor(getResources().getColor(R.color.defaultIconColor));
                tvRevenue.setTextColor(getResources().getColor(R.color.defaultIconColor));
                tvMe.setTextColor(getResources().getColor(R.color.defaultIconColor));
                break;
            case R.id.tvManager:
                // update icon color
                setTextViewDrawableColor(tvManager, getResources().getColor(R.color.colorPrimary));
                setTextViewDrawableColor(tvOrder, getResources().getColor(R.color.defaultIconColor));
                setTextViewDrawableColor(tvRevenue, getResources().getColor(R.color.defaultIconColor));
                setTextViewDrawableColor(tvMe, getResources().getColor(R.color.defaultIconColor));
                // update text color
                tvManager.setTextColor(getResources().getColor(R.color.colorPrimary));
                tvOrder.setTextColor(getResources().getColor(R.color.defaultIconColor));
                tvRevenue.setTextColor(getResources().getColor(R.color.defaultIconColor));
                tvMe.setTextColor(getResources().getColor(R.color.defaultIconColor));
                break;
            case R.id.tvRevenue:
                // update icon color
                setTextViewDrawableColor(tvRevenue, getResources().getColor(R.color.colorPrimary));
                setTextViewDrawableColor(tvOrder, getResources().getColor(R.color.defaultIconColor));
                setTextViewDrawableColor(tvManager, getResources().getColor(R.color.defaultIconColor));
                setTextViewDrawableColor(tvMe, getResources().getColor(R.color.defaultIconColor));
                // update text color
                tvRevenue.setTextColor(getResources().getColor(R.color.colorPrimary));
                tvOrder.setTextColor(getResources().getColor(R.color.defaultIconColor));
                tvManager.setTextColor(getResources().getColor(R.color.defaultIconColor));
                tvMe.setTextColor(getResources().getColor(R.color.defaultIconColor));
                break;
            case R.id.tvMe:
                // update icon color
                setTextViewDrawableColor(tvMe, getResources().getColor(R.color.colorPrimary));
                setTextViewDrawableColor(tvOrder, getResources().getColor(R.color.defaultIconColor));
                setTextViewDrawableColor(tvManager, getResources().getColor(R.color.defaultIconColor));
                setTextViewDrawableColor(tvRevenue, getResources().getColor(R.color.defaultIconColor));
                // update text color
                tvMe.setTextColor(getResources().getColor(R.color.colorPrimary));
                tvOrder.setTextColor(getResources().getColor(R.color.defaultIconColor));
                tvManager.setTextColor(getResources().getColor(R.color.defaultIconColor));
                tvRevenue.setTextColor(getResources().getColor(R.color.defaultIconColor));
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tvOrder:
                viewPager.setCurrentItem(0); break;
            case R.id.tvManager:
                viewPager.setCurrentItem(1); break;
            case R.id.tvRevenue:
                viewPager.setCurrentItem(2); break;
            case R.id.tvMe:
                viewPager.setCurrentItem(3); break;
        }
    }
}