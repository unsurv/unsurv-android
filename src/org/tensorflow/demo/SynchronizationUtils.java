package org.tensorflow.demo;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

class SynchronizationUtils {

  private static String TAG = "SynchronizationUtils";

  private static int PERIODIC_SYNCHRONIZATION_TIMER_JOB = 0;
  private static int SYNCHRONIZATION_JOB = 1;


  static Boolean scheduleSynchronizationJob(Context context, PersistableBundle jobExtras) {

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    ComponentName componentName = new ComponentName(context, SyncJobService.class);

    JobInfo.Builder jobBuilder  = new JobInfo.Builder(SYNCHRONIZATION_JOB, componentName);

    //TODO check extras["periodicSync"]

    String lastUpdated = sharedPreferences.getString("lastUpdated", "");

    String areaQuery = sharedPreferences.getString("area", "");

    PersistableBundle serviceJobExtras = new PersistableBundle();


    //TODO check all query keys and add query values to Service which are present.

    //API parameters for background Service.


    String startQuery = "";
    String endQuery = "";


    String[] queryParts = new String[]{areaQuery, startQuery, endQuery}; //TODO add for each query key

    String queryString = "";

    for (int i = 0; i < queryParts.length; i++) {

      if (!queryParts[i].isEmpty()){
        queryString = queryString.concat(queryParts[i] + "&");
      }
    }

    queryString = queryString.substring(0, queryString.length() - 1); // remove last "&"


    serviceJobExtras.putString("queryString", queryString);


    //-----------------------------

    jobBuilder.setExtras(serviceJobExtras);

    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
    JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);


    // debug
    int scheduleResult = jobScheduler.schedule(jobBuilder.build());
    List<JobInfo> b = jobScheduler.getAllPendingJobs();


    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    sharedPreferences.edit().putString(
            "lastUpdated",
            timestampIso8601.format(new Date(System.currentTimeMillis()))).apply();


    return jobScheduler.schedule(jobBuilder.build()) == JobScheduler.RESULT_SUCCESS;

  }


  static Boolean scheduleSyncIntervalJob (Context context, @Nullable PersistableBundle jobExtras) {

    //TODO Do I need jobExtras here? yes for location boundingbox

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    long syncIntervalInMillis= sharedPreferences.getLong("synchronizationInterval", 0);

    if (syncIntervalInMillis == 0) {
      return false; // TODO don't relist job after completion? check documentation again
    }

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    PersistableBundle syncJobExtras = new PersistableBundle();
    syncJobExtras.putBoolean("periodicSync", true);

    syncJobExtras.putString("area", sharedPreferences.getString("area", null));
    syncJobExtras.putString("start", "2018-01-01");




    ComponentName componentName = new ComponentName(context, SyncIntervalSchedulerJobService.class);

    JobInfo.Builder jobBuilder  = new JobInfo.Builder(PERIODIC_SYNCHRONIZATION_TIMER_JOB, componentName);

    jobBuilder.setExtras(syncJobExtras);

    jobBuilder.setPeriodic(30*1000);
    //jobBuilder.setPeriodic(syncIntervalInMillis);

    jobBuilder.setPersisted(true);

    JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);


    // .schedule overrides previous Jobs with same ID.
    int scheduleResult = jobScheduler.schedule(jobBuilder.build());



    return scheduleResult == JobScheduler.RESULT_SUCCESS;


  }

  static List<SynchronizedCamera> getCamerasInAreaFromServer(Context context, double latMin, double latMax, double lonMin, double lonMax){

    //TODO check api for negative values in left right top bottom see if still correct

    String mLatMin = String.valueOf(latMin);
    String mLatMax = String.valueOf(latMax);
    String mLonMin = String.valueOf(lonMin);
    String mLonMax = String.valueOf(lonMax);

    String areaQueryValue = mLatMin + "," + mLonMax + "," + mLatMax + "," + mLonMin;

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    RequestQueue mRequestQueue;

    // Instantiate the cache
    Cache cache = new DiskBasedCache(context.getCacheDir(), 0); // no cache for privacy

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(cache, network);

    // Start the queue
    mRequestQueue.start();

    String baseURL = "http://192.168.2.159:5000/cameras/?area=8.2699,50.0201,8.2978,50.0005";
    //String baseURL = sharedPreferences.getString("synchronizationUrl", "https://api.unsurv.org/q=");

    String completeQueryURL = baseURL + "area=" + areaQueryValue;

    final List<SynchronizedCamera> camerasFromServer = new ArrayList<>();

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            completeQueryURL,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {
                //debugTextView.setText("Response: " + response.toString());
                JSONObject JSONToSynchronize;
                SynchronizedCamera cameraToAdd;

                try {


                  for (int i = 0; i < response.getJSONArray("cameras").length(); i++) {
                    JSONToSynchronize = new JSONObject(String.valueOf(response.getJSONArray("cameras").get(i)));

                    cameraToAdd = new SynchronizedCamera(
                            JSONToSynchronize.getString("imageURL"),
                            JSONToSynchronize.getString("id"),
                            JSONToSynchronize.getDouble("lat"),
                            JSONToSynchronize.getDouble("lon"),
                            JSONToSynchronize.getString("comments"),
                            JSONToSynchronize.getString("lastUpdated")


                    );

                    camerasFromServer.add(cameraToAdd);

                    // TODO same db entry just gets appended. Check if already there. time based? value based?
                    //new checkDbAsyncTask(getApplication()).execute();


                  }

                  //TODO add refresh function for areas not in "home" zone



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

    return camerasFromServer;

  }


}
