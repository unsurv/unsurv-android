package org.tensorflow.demo;

import android.support.annotation.Nullable;
import android.util.Log;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NoCache;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;


class ConnectionUtils {

  private static String TAG = "ConnectionUtils";


  static void synchronizeCamerasWithServer(String baseURL, String areaQuery, final boolean insertIntoDb, @Nullable String startQuery, @Nullable SynchronizedCameraRepository synchronizedCameraRepository){

    //TODO check api for negative values in left right top bottom see if still correct

    final SynchronizedCameraRepository crep = synchronizedCameraRepository;

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

   String url = baseURL + areaQuery;

    if (startQuery != null) {
      url.concat("&" + startQuery);
    }

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                List<SynchronizedCamera> camerasToSync = new ArrayList<>();
                JSONObject JSONToSynchronize;

                try {

                  for (int i = 0; i < response.getJSONArray("cameras").length(); i++) {
                    JSONToSynchronize = new JSONObject(String.valueOf(response.getJSONArray("cameras").get(i)));

                    SynchronizedCamera cameraToAdd = new SynchronizedCamera(
                            "test_nexus_10.jpg",
                            JSONToSynchronize.getString("id"),
                            JSONToSynchronize.getDouble("lat"),
                            JSONToSynchronize.getDouble("lon"),
                            JSONToSynchronize.getString("comments"),
                            JSONToSynchronize.getString("lastUpdated"),
                            JSONToSynchronize.getString("uploadedAt")

                    );

                    camerasToSync.add(cameraToAdd);

                  }

                  if (insertIntoDb) {
                    crep.insert(camerasToSync);
                  }


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

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mRequestQueue.add(jsonObjectRequest);

  }

}
