package org.tensorflow.demo;

import android.app.Application;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SyncJobService extends JobService {

  //private SharedPreferences sharedPreferences;
  private JSONObject JSONToSynchronize;
  private SynchronizedCamera cameraToAdd;
  private SynchronizedCameraRepository synchronizedCameraRepository;

  private String TAG = "SyncJobService";


  @Override
  public boolean onStartJob(JobParameters jobParameters) {

    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    final PersistableBundle jobExtras = jobParameters.getExtras();

    RequestQueue mRequestQueue;

    // Instantiate the cache
    Cache cache = new DiskBasedCache(getCacheDir(), 0); // 1MB cap

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(cache, network);

    // Start the queue
    mRequestQueue.start();

    String baseURL = "http://192.168.2.159:5000/cameras/?";
    //String baseURL = sharedPreferences.getString("synchronizationUrl", "");

    String completeQueryURL = baseURL + jobExtras.get("queryString") + "&";

    final List<SynchronizedCamera> camerasSynced = new ArrayList<>();

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            completeQueryURL,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {
                //debugTextView.setText("Response: " + response.toString());

                try {


                  for (int i = 0; i < response.getJSONArray("cameras").length(); i++) {
                    JSONToSynchronize = new JSONObject(String.valueOf(response.getJSONArray("cameras").get(i)));

                    cameraToAdd = new SynchronizedCamera(
                            JSONToSynchronize.getString("image_url"),
                            JSONToSynchronize.getDouble("lat"),
                            JSONToSynchronize.getDouble("lon"),
                            JSONToSynchronize.getString("comments"),
                            JSONToSynchronize.getString("id")


                    );

                    camerasSynced.add(cameraToAdd);

                    // TODO same db entry just gets appended. Check if already there. time based? value based?
                    //new checkDbAsyncTask(getApplication()).execute();


                  }

                  //TODO add refresh function for areas not in "home" zone
                  synchronizedCameraRepository.insert(camerasSynced);



                } catch (Exception e) {
                  Log.i(TAG, "onResponse: " + e.toString());

                }


              }
            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        // TODO: Handle Errors
      }
    }
    );

    mRequestQueue.add(jsonObjectRequest);


    return false;
  }




  @Override
  public boolean onStopJob(JobParameters jobParameters) {
    return false;
  }



}


