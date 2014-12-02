package com.nuitdelinfo.los_tortugueros.n2ilostortugueros_logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;


public class MainActivity extends ActionBarActivity {

    private static final String URL = "http://etud.insa-toulouse.fr/~livet/logger.php?user=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this,SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void performClick(View v){
        Log.d("MainActivity","Click");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String syncConnPref = sharedPref.getString("sync_frequency", "1");
        switch (v.getId()){
            case R.id.buttonCafe:
                new SendToServerTask().execute(URL+syncConnPref,"cafe");
                break;
            case R.id.buttonThe:
                new SendToServerTask().execute(URL+syncConnPref,"the");
                break;
            case R.id.buttonBiere:
                new SendToServerTask().execute(URL+syncConnPref,"biere");
                break;
            case R.id.buttonAutre:
                new SendToServerTask().execute(URL+syncConnPref,"autre");
                break;
            case R.id.buttonCoca:
                new SendToServerTask().execute(URL+syncConnPref,"coca");
                break;
            case R.id.buttonEau:
                new SendToServerTask().execute(URL+syncConnPref,"eau");
                break;
            case R.id.buttonRedbull:
                new SendToServerTask().execute(URL+syncConnPref,"redbull");
                break;
            case R.id.buttonSoda:
                new SendToServerTask().execute(URL+syncConnPref,"soda");
                break;

        }
    }

    private String getDataToSend(String name){
        Date d = new Date();
        JSONObject send = new JSONObject();
        try {
            send.put("source","mobile");
            send.put("capteur","boisson");
            send.put("timestamp",d.getTime()/1000);
            send.put("type",name);
            Log.d("JSON",send.toString());
            return send.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class SendToServerTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... data) {
            Log.d("MainActivity","Send begin");

            // params comes from the execute() call: params[0] is the url.
            try {
                send(data[0],getDataToSend(data[1]));
                return "Ajout de la boisson "+data[1];
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

    // Given a URL, establishes an HttpUrlConnection and retrieves
// the web page content as a InputStream, which it returns as
// a string.
    private void send(String myurl, String toSend) throws IOException {

        int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
        HttpClient client = new DefaultHttpClient(httpParams);

        HttpPost request = new HttpPost(myurl);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        request.setEntity(new StringEntity(toSend));
        Log.d("Response",""+client.execute(request).getStatusLine().getStatusCode());
    }
}
