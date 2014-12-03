package com.nuitdelinfo.los_tortugueros.n2ilostortugueros_logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

public class GPSLoggerService extends Service {

    public static final String DATABASE_NAME = "GPSLOGGERDB";
    public static final String POINTS_TABLE_NAME = "LOCATION_POINTS";
    public static final String TRIPS_TABLE_NAME = "TRIPS";

    private final DecimalFormat sevenSigDigits = new DecimalFormat("0.#######");
    private final DateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private LocationManager lm;
    private LocationListener locationListener;

    private static long minTimeMillis = 2000;
    private static long minDistanceMeters = 10;
    private static float minAccuracyMeters = 35;

    private int lastStatus = 0;
    private static boolean showingDebugToast = false;

    private static final String tag = "GPSLoggerService";

    /** Called when the activity is first created. */
    private void startLoggerService() {

        // ---use the LocationManager class to obtain GPS locations---
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new MyLocationListener();

        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                minTimeMillis,
                minDistanceMeters,
                locationListener);
    }

    private void shutdownLoggerService() {
        lm.removeUpdates(locationListener);
    }

    private class SendToServerTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... data) {
            Log.d("MainActivity","Send begin");

            // params comes from the execute() call: params[0] is the url.
            try {
                send(data[0]);
                return "Send gps ";
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String toto) {
            Log.d("MainActivity","Send end");
            Toast.makeText(getApplicationContext(),toto,Toast.LENGTH_SHORT).show();
        }
    }

    private void send(String send) throws IOException {
        int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
        HttpClient client = new DefaultHttpClient(httpParams);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String name = sharedPref.getString("id", "");

        HttpPost request = new HttpPost(MainActivity.URL + name);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        try {
            request.setEntity(new StringEntity(send));
            Log.d("Response", "" + client.execute(request).getStatusLine().getStatusCode());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    public class MyLocationListener implements LocationListener {



        public void onLocationChanged(Location loc) {
            if (loc != null) {
                boolean pointIsRecorded = false;
                try {
                    if (loc.hasAccuracy() && loc.getAccuracy() <= minAccuracyMeters) {
                        pointIsRecorded = true;
                        GregorianCalendar greg = new GregorianCalendar();
                        TimeZone tz = greg.getTimeZone();
                        int offset = tz.getOffset(System.currentTimeMillis());
                        greg.add(Calendar.SECOND, (offset/1000) * -1);
                        Date d = new Date();
                        JSONObject send = new JSONObject();
                        try {
                            send.put("source","mobile");
                            send.put("capteur","gps");
                            send.put("timestamp",d.getTime()/1000);
                            send.put("latitude",loc.getLatitude());
                            send.put("longitude",loc.getLongitude());
                            send.put("altitude",(loc.hasAltitude() ? loc.getAltitude() : ""));
                            Log.d("JSON",send.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        new SendToServerTask().execute(send.toString());

                    }
                } catch (Exception e) {
                    Log.e(tag, e.toString());
                }
                if (pointIsRecorded) {
                    if (showingDebugToast) Toast.makeText(
                            getBaseContext(),
                            "Location stored: \nLat: " + sevenSigDigits.format(loc.getLatitude())
                                    + " \nLon: " + sevenSigDigits.format(loc.getLongitude())
                                    + " \nAlt: " + (loc.hasAltitude() ? loc.getAltitude()+"m":"?")
                                    + " \nAcc: " + (loc.hasAccuracy() ? loc.getAccuracy()+"m":"?"),
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (showingDebugToast) Toast.makeText(
                            getBaseContext(),
                            "Location not accurate enough: \nLat: " + sevenSigDigits.format(loc.getLatitude())
                                    + " \nLon: " + sevenSigDigits.format(loc.getLongitude())
                                    + " \nAlt: " + (loc.hasAltitude() ? loc.getAltitude()+"m":"?")
                                    + " \nAcc: " + (loc.hasAccuracy() ? loc.getAccuracy()+"m":"?"),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }

        public void onProviderDisabled(String provider) {
            if (showingDebugToast) Toast.makeText(getBaseContext(), "onProviderDisabled: " + provider,
                    Toast.LENGTH_SHORT).show();

        }

        public void onProviderEnabled(String provider) {
            if (showingDebugToast) Toast.makeText(getBaseContext(), "onProviderEnabled: " + provider,
                    Toast.LENGTH_SHORT).show();

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            String showStatus = null;
            if (status == LocationProvider.AVAILABLE)
                showStatus = "Available";
            if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
                showStatus = "Temporarily Unavailable";
            if (status == LocationProvider.OUT_OF_SERVICE)
                showStatus = "Out of Service";
            if (status != lastStatus && showingDebugToast) {
                Toast.makeText(getBaseContext(),
                        "new status: " + showStatus,
                        Toast.LENGTH_SHORT).show();
            }
            lastStatus = status;
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();


        startLoggerService();

        // Display a notification about us starting. We put an icon in the
        // status bar.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        shutdownLoggerService();

    }


    // This is the object that receives interactions from clients. See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public static void setMinTimeMillis(long _minTimeMillis) {
        minTimeMillis = _minTimeMillis;
    }

    public static long getMinTimeMillis() {
        return minTimeMillis;
    }

    public static void setMinDistanceMeters(long _minDistanceMeters) {
        minDistanceMeters = _minDistanceMeters;
    }

    public static long getMinDistanceMeters() {
        return minDistanceMeters;
    }

    public static float getMinAccuracyMeters() {
        return minAccuracyMeters;
    }

    public static void setMinAccuracyMeters(float minAccuracyMeters) {
        GPSLoggerService.minAccuracyMeters = minAccuracyMeters;
    }

    public static void setShowingDebugToast(boolean showingDebugToast) {
        GPSLoggerService.showingDebugToast = showingDebugToast;
    }

    public static boolean isShowingDebugToast() {
        return showingDebugToast;
    }

    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        GPSLoggerService getService() {
            return GPSLoggerService.this;
        }
    }

}
