package org.tensorflow.demo;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;


public class DebugActivity extends AppCompatActivity {


  private BottomNavigationView bottomNavigationView;
  private JSONObject JSONToSynchronize;
  private SynchronizedCamera cameraToAdd;

  private SynchronizedCameraRepository synchronizedCameraRepository;

  private List<SynchronizedCamera> allSynchronizedCameras;

  private int CHECK_DB_SIZE = 0;
  private int DELETE_DB = 1;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //return super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.actionbar, menu);
    return true;
  }



  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      case R.id.action_settings:
        Intent settingsIntent = new Intent(DebugActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);

        return true;



      default:
        // Fall back on standard behaviour when user choice not recognized.
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_debug);
    final String TAG = "DebugActivity";
    final TextView debugTextView = findViewById(R.id.debug_textview);
    final Button debugDbSync = findViewById(R.id.sync_db);
    final Button debugDbCheck = findViewById(R.id.check_db);
    final Button debugDbDelete = findViewById(R.id.delete_db);

    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());

    android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
    setSupportActionBar(myToolbar);
    debugTextView.setText(String.valueOf(myToolbar.isOverflowMenuShowing()));


    debugDbSync.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        //new DbAsyncTask(getApplication(), SYNC_DB).execute();

        RequestQueue mRequestQueue;

        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();

        String url = "http://192.168.2.159:5000/cameras/?area=8.2699,50.0201,8.2978,50.0005";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                  @Override
                  public void onResponse(JSONObject response) {
                    //debugTextView.setText("Response: " + response.toString());

                    try {


                      for (int i = 0; i < response.getJSONArray("cameras").length(); i++) {
                        JSONToSynchronize = new JSONObject(String.valueOf(response.getJSONArray("cameras").get(i)));

                        cameraToAdd = new SynchronizedCamera(JSONToSynchronize.getString("image_url"),
                                JSONToSynchronize.getDouble("lat"),
                                JSONToSynchronize.getDouble("lon"),
                                JSONToSynchronize.getString("comments"),
                                JSONToSynchronize.getString("last_updated")

                        );

                        // TODO same db entry just gets appended. Check if already there. time based? value based?
                        synchronizedCameraRepository.insert(cameraToAdd);
                        //new checkDbAsyncTask(getApplication()).execute();

                      }
                      int a = response.getJSONArray("cameras").length();


                    } catch (Exception e) {
                      Log.i(TAG, "onResponse: " + e.toString());
                      debugTextView.setText(e.toString());

                    }


                  }
                }, new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            // TODO: Handle Errors
            debugTextView.setText(error.toString());
          }
        }
        );

        mRequestQueue.add(jsonObjectRequest);


      }
    });

    debugDbCheck.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        new DbAsyncTask(getApplication(), CHECK_DB_SIZE).execute();

      }
    });

    debugDbDelete.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        new DbAsyncTask(getApplication(), DELETE_DB).execute();

      }
    });



    //synchronizedCameraViewModel = ViewModelProviders.of(this).get(SynchronizedCameraViewModel.class);


    // TODO Sychronisation Class

    bottomNavigationView = findViewById(R.id.navigation);
    bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

          case R.id.bottom_navigation_history:
            Intent historyIntent = new Intent(DebugActivity.this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;

          case R.id.bottom_navigation_camera:
            Intent cameraIntent = new Intent(DebugActivity.this, DetectorActivity.class);
            startActivity(cameraIntent);
            return true;

          case R.id.bottom_navigation_map:
            Intent mapIntent = new Intent(DebugActivity.this, MapActivity.class);
            startActivity(mapIntent);
            return true;

          case R.id.bottom_navigation_stats:
            Intent statsIntent = new Intent(DebugActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
            return true;

        }

        return false;

      }
    });

  }

  private class DbAsyncTask extends AsyncTask<Void, Void, Void> {

    final TextView debugTextView = findViewById(R.id.debug_textview);
    private SynchronizedCameraRepository synchronizedCameraRepository;
    private List<SynchronizedCamera> allSynchronizedCameras;
    private int dbSize;
    private int MODE;

    private int CHECK_DB_SIZE = 0;
    private int DELETE_DB = 1;

    private String TAG = "checkDbAsync";


    public DbAsyncTask(Application application, int DbMode) {
      synchronizedCameraRepository = new SynchronizedCameraRepository(application);
      MODE = DbMode;
    }


    @Override
    protected Void doInBackground(Void... voids) {

      //synchronizedCameraRepository.deleteAll();

      if (MODE == CHECK_DB_SIZE) {
        dbSize = synchronizedCameraRepository.getAllSynchronizedCameras().size();
        Log.i(TAG, "doInBackground: " + String.valueOf(synchronizedCameraRepository.getAllSynchronizedCameras().size()));
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            debugTextView.setText(String.valueOf(dbSize));
            Toast DbSizeToast = Toast.makeText(getApplicationContext(), String.valueOf(dbSize), Toast.LENGTH_SHORT);
            DbSizeToast.show();

          }
        });


      } else if (MODE == DELETE_DB) {
        synchronizedCameraRepository.deleteAll();

        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Toast DbSizeToast = Toast.makeText(getApplicationContext(), "DB RESET", Toast.LENGTH_SHORT);
            DbSizeToast.show();

          }
        });
      }

      return null;
    }
  }




}


