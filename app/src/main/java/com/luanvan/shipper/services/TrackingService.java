package com.luanvan.shipper.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.luanvan.shipper.Fragments.OrderFragment;
import com.luanvan.shipper.MainActivity;
import com.luanvan.shipper.R;
import com.luanvan.shipper.components.RequestCode;
import com.luanvan.shipper.components.Shared;

public class TrackingService extends Service {
    private static final String TAG = "TrackingService";
    private int shipperId;
    public static double latitude = 0, longitude = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) buildNotificationNew();
        else buildNotificationOld();

        SharedPreferences sharedPreferences = getSharedPreferences(Shared.SHIPPER, MODE_PRIVATE);
        shipperId = sharedPreferences.getInt(Shared.KEY_SHIPPER_ID, -1);

        requestLocationUpdates();
    }

    private void buildNotificationOld() {
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.tracking_enabled_notif))
                //Make this notification ongoing to user can dismiss
                .setOngoing(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.tracking_enabled);
        startForeground(2, builder.build());
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private void buildNotificationNew()
    {
        String stop = "stop";
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, RequestCode.REQUEST_NOTIFICATION, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), RequestCode.REQUEST_NOTIFICATION, intent, 0);

        String NOTIFICATION_CHANNEL_ID = "location";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.tracking_enabled_notif))
                //Make this notification ongoing to user can dismiss
                .setOngoing(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.tracking_enabled)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(contentIntent)
                .build();

        startForeground(2, notification);
    }

    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Unregister the BroadcastReceiver when the notification is tapped
            unregisterReceiver(stopReceiver);
            stopSelf();
        }
    };

    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();

        //Specify how often app should request the device’s location
        request.setInterval(10000);

        //Get the most accurate location data available
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        final String path = getString(R.string.firebase_path);
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission == PackageManager.PERMISSION_GRANTED) {
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    String shipperID = "shipper" + shipperId;
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path).child(shipperID);
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        ref.setValue(location);
                        Log.i("location", location.getLatitude()+","+location.getLongitude());

                        SharedPreferences.Editor editor = getSharedPreferences(Shared.SHIPPER, MODE_PRIVATE).edit();
                        editor.putString(Shared.KEY_LATITUDE, location.getLatitude()+"");
                        editor.putString(Shared.KEY_LONGITUDE, location.getLongitude()+"");
                        editor.apply();
                    }
                }
            }, null);
        }
    }
}
