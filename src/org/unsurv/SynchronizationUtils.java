package org.unsurv;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NoCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;


/**
 * Helper class for synchronization methods
 */
class SynchronizationUtils {

  // TODO common naming scheme baseUrl, area/areaString/areaQuery etc.
  // TODO Localbroadcast with info on when sync etc was successful
  // TODO should then be used to create toast or similar alert in an activity

  private static int UPLOAD_FAILED = 0;
  private static int UPLOAD_SUCCESSFUL = 1;

  private static String TAG = "SynchronizationUtils";


  /**
   * Starts a recurring job to synchronize data from a db server. Interval and URL are
   * @param context ctx of starting the job
   * @param jobExtras not used for now
   * @return whether job has been queued successfully
   */
  static Boolean scheduleSyncIntervalJob (Context context, @Nullable PersistableBundle jobExtras, long syncInterval) {

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    long syncIntervalInMillis;

    // if no specific interval given use default one in SharedPreferences
    if (syncInterval == 0) {
      syncIntervalInMillis = Long.parseLong(sharedPreferences.getString("synchronizationInterval", "0"));
    } else {
      syncIntervalInMillis = syncInterval;
    }


    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    PersistableBundle syncJobExtras = new PersistableBundle();

    // server URL
    syncJobExtras.putString("baseUrl", sharedPreferences.getString("synchronizationUrl", null));
    // area to keep updated, set in tutorial
    syncJobExtras.putString("area", sharedPreferences.getString("area", null));
    // last sync time
    syncJobExtras.putString("start", sharedPreferences.getString("lastUpdated", null));
    // download images or not
    syncJobExtras.putBoolean("downloadImages", sharedPreferences.getBoolean("downloadImages", false));
    // sync interval
    syncJobExtras.putLong("syncIntervalInMillis", syncIntervalInMillis);

    ComponentName componentName = new ComponentName(context, SyncIntervalSchedulerJobService.class);

    JobInfo.Builder jobBuilder  = new JobInfo.Builder(0, componentName); // Job ID

    jobBuilder.setExtras(syncJobExtras);

    // interval min is 15 mins in Android
    // jobBuilder.setPeriodic(syncIntervalInMillis, 1000*60*60*3); // 3 h flex
    jobBuilder.setPeriodic(15000, 1000*10);

    //jobBuilder.setOverrideDeadline(15*1000); // force after 15 s for debug

    jobBuilder.setPersisted(true);

    JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);


    // .schedule overrides previous Jobs with same ID.
    int scheduleResult = jobScheduler.schedule(jobBuilder.build());

    // TODO CHECK FOR RETURN VALUE WHEN SYNC JOB STARTED AND LET USER KNOW IF IT FAILED
    return scheduleResult == JobScheduler.RESULT_SUCCESS;

  }


  /**
   * Downloads cameras from a database server specified by the parameters
   * @param baseUrl https://api.unsurv.org/
   * @param areaQuery "area=latmin,latmax,lonmin,lonmax"
   * @param sharedPreferences sharedPreference object
   * @param insertIntoDb boolean should result be inserted in local db
   * @param startQuery "start=latmin,latmax,lonmin,lonmax"
   * @param synchronizedCameraRepository synchronizedCameraRepository object
   */
  static void downloadCamerasFromServerOld(String baseUrl, String areaQuery, final SharedPreferences sharedPreferences, final boolean insertIntoDb, @Nullable String startQuery, @Nullable SynchronizedCameraRepository synchronizedCameraRepository, final Context context){

    //TODO check api for negative values in left right top bottom see if still correct

    final SynchronizedCameraRepository crep = synchronizedCameraRepository;

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    // see API documentation
    String URL = baseUrl  + "cameras/?" + areaQuery;
    // add time contraint (pseudo: "cameras since 2018-01-01" = "start=2018-01-01" in URL) to query if needed
    if (startQuery != null) {
      URL += "&" + startQuery;
    }

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            URL,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                List<SynchronizedCamera> camerasToSync = new ArrayList<>();
                JSONObject JSONToSynchronize;

                try {

                  for (int i = 0; i < response.getJSONArray("cameras").length(); i++) {
                    JSONToSynchronize = new JSONObject(String.valueOf(response.getJSONArray("cameras").get(i)));

                    int area = 0;
                    int direction = -1;
                    int mount = 0;
                    int height = -1;
                    int angle = -1;

                    if (JSONToSynchronize.has("area")) {
                      area = JSONToSynchronize.getInt("area");
                    }

                    if (JSONToSynchronize.has("mount")) {
                      mount = JSONToSynchronize.getInt("mount");
                    }

                    if (JSONToSynchronize.has("direction")) {
                      direction = JSONToSynchronize.getInt("direction");
                    }

                    if (JSONToSynchronize.has("height")) {
                      height = JSONToSynchronize.getInt("height");
                    }

                    SynchronizedCamera cameraToAdd = new SynchronizedCamera(
                            null,
                            JSONToSynchronize.getString("id"),
                            JSONToSynchronize.getInt("type"),
                            area,
                            direction,
                            mount,
                            height,
                            angle,
                            JSONToSynchronize.getDouble("lat"),
                            JSONToSynchronize.getDouble("lon"),
                            JSONToSynchronize.getString("comments"),
                            JSONToSynchronize.getString("last_updated"),
                            JSONToSynchronize.getString("uploaded_at"),
                            JSONToSynchronize.getBoolean("manual_capture")

                    );

                    camerasToSync.add(cameraToAdd);

                  }

                  // local db insertAll
                  if (insertIntoDb) {
                    crep.insertAll(camerasToSync);
                  }

                } catch (Exception e) {
                  Log.i(TAG, "onResponse: " + e.toString());

                }

              }
            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        // TODO: Handle Errors
        Log.i(TAG, "Error in connection " + error.toString());
      }
    }) {
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {

        Map<String, String> headers = new HashMap<>();
        // temporary API key
        String apiKey = sharedPreferences.getString("apiKey", null);
        headers.put("Authorization", apiKey);
        headers.put("Content-Type", "application/json");

        return headers;
      }
    };

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {
        if (request.hasHadResponseDelivered()){
          Intent intent = new Intent();
          intent.setAction("org.unsurv.SYNCHRONIZATION_SUCCESSFUL");
          LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
          localBroadcastManager.sendBroadcast(intent);

          long currentTime = System.currentTimeMillis();
          SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

          sharedPreferences.edit().putString("lastUpdated", timestampIso8601.format(new Date(currentTime))).apply();

        }


      }
    });

    mRequestQueue.add(jsonObjectRequest);

  }



  static void downloadCamerasFromServer(String baseUrl, String area, final boolean insertIntoDb, @Nullable SynchronizedCameraRepository synchronizedCameraRepository, final SharedPreferences sharedPreferences, final Context context){


    // baseURL = "https://overpass-api.de/api/interpreter?data=[out:json];node[man_made=surveillance](52.5082248,13.3780064,52.515041,13.3834472);out meta;";

    // https://overpass-api.de/api/interpreter?

    // String homeZone = "52.5082248,52.515041,13.3780064,13.3834472";

    String[] borders = area.split(",");

    double latMin = Double.valueOf(borders[0]);
    double latMax = Double.valueOf(borders[1]);
    double lonMin = Double.valueOf(borders[2]);
    double lonMax = Double.valueOf(borders[3]);

    String completeURL = String.format(baseUrl + "data=[out:json];node[man_made=surveillance](%s,%s,%s,%s);out meta;", latMin, lonMin, latMax, lonMax);

    final SynchronizedCameraRepository crep = synchronizedCameraRepository;

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();


    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            completeURL,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                List<SynchronizedCamera> camerasToSync = new ArrayList<>();

                JSONObject cameraJSON;

                try {

                  String osm_db_timestamp = response.getJSONObject("osm3s").getString("timestamp_osm_base");

                  for (int i = 0; i < response.getJSONArray("elements").length(); i++) {

                    cameraJSON = new JSONObject(String.valueOf(response.getJSONArray("elements").get(i)));
                    String timestamp = cameraJSON.getString("timestamp");

                    JSONObject tags = cameraJSON.getJSONObject("tags");

                    int type = 0; // default for fixed camera
                    int area = 0; // outdoor
                    int direction = -1; // unknown
                    int mount = 0; // unknown
                    int height = -1; // unknown
                    int angle = -1; // unknown

                    List<String> tagsAvailable = new ArrayList<>();

                    // loop through tag keys for more information: area, angle, height etc.
                    Iterator<String> iterTags = tags.keys();
                    while (iterTags.hasNext()) {
                      String key = iterTags.next();
                      tagsAvailable.add(key);
                    }

                    for (String tag : tagsAvailable){

                      try {

                        switch (tag) {

                          case "surveillance":
                            area = StorageUtils.areaList.indexOf(tags.getString(tag));
                            break;

                          case "camera:type":
                            type = StorageUtils.typeList.indexOf(tags.getString(tag));
                            break;

                          case "camera:mount":
                            mount = StorageUtils.mountList.indexOf(tags.getString(tag));
                            break;

                          case "camera:direction":
                            direction = tags.getInt("camera:direction");
                            break;

                          case "height":
                            height = tags.getInt("height");
                            break;
                        }

                      } catch (Exception ex) {
                        Log.i(TAG, "Error creating value from overpass api response: " + ex.toString());
                        continue;
                      }



                    }

                    SynchronizedCamera cameraToAdd = new SynchronizedCamera(
                            cameraJSON.getString("id") + ".jpg",
                            cameraJSON.getString("id"),
                            type,
                            area,
                            mount,
                            direction,
                            height,
                            angle,
                            cameraJSON.getDouble("lat"),
                            cameraJSON.getDouble("lon"),
                            "",
                            osm_db_timestamp,
                            timestamp,
                            false


                    );

                    camerasToSync.add(cameraToAdd);

                  }

                  if (insertIntoDb) {
                    crep.insertAll(camerasToSync);

                  }

                } catch (Exception e) {
                  Log.i(TAG, "onResponse: " + e.toString());

                }

              }
            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        // TODO: Handle Errors
        Log.i(TAG, "Error in connection " + error.toString());
      }
    }) {
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {

        Map<String, String> headers = new HashMap<>();
        // headers.put("Content-Type", "application/json");

        return headers;
      }
    };

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            10000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));



    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {
        if (request.hasHadResponseDelivered()){
          Intent intent = new Intent();
          intent.setAction("org.unsurv.SYNCHRONIZATION_SUCCESSFUL");
          LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
          localBroadcastManager.sendBroadcast(intent);

          long currentTime = System.currentTimeMillis();
          SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

          sharedPreferences.edit().putString("lastUpdated", timestampIso8601.format(new Date(currentTime))).apply();

        }


      }
    });


    mRequestQueue.add(jsonObjectRequest);

  }





  /**
   * Downloads images for a given list of SynchronizedCameras
   * @param baseUrl https://api.unsurv.org/
   * @param cameras List of SynchronizedCameras
   * @param sharedPreferences SharePreferences object
   */
  static void downloadImagesFromServerOld(String baseUrl, final List<SynchronizedCamera> cameras, final SharedPreferences sharedPreferences) {

    final List<String> idsFromCameras = new ArrayList<>();

    for (SynchronizedCamera camera : cameras) {
      idsFromCameras.add(camera.getExternalID());
    }

    // object to add external ids of cameras to be downloaded
    JSONObject postObject = new JSONObject();

    JSONArray ids = new JSONArray();

    for (String id : idsFromCameras) {
      ids.put(id);
    }

    try {

      postObject.put("ids", ids);

    } catch (JSONException jse){
      Log.i(TAG, "downloadImages: " + jse.toString());
    }

    String url = baseUrl + "images/";

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    // posts JsonObject with array of wanted ids
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.POST,
            url,
            postObject,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                try {

                  for (String id : idsFromCameras) {

                    // get image with key id
                    String base64Image = response.getString(id);

                    byte[] imageAsBytes = Base64.decode(base64Image, Base64.DEFAULT);

                    StorageUtils.saveBytesToFile(imageAsBytes, id + ".jpg", StorageUtils.SYNCHRONIZED_PATH);

                  }

                } catch (Exception e) {
                  Log.i(TAG, "response to jpg error: " + e.toString());
                }

              }
            },

            new Response.ErrorListener() {
              @Override
              public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "downloadImages: ");

              }
            }
    ){
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        String apiKey = sharedPreferences.getString("apiKey", null);
        headers.put("Authorization", apiKey);
        headers.put("Content-Type", "application/json");

        return headers;
      }
    };

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {
        Log.i(TAG, "post request finished");
      }
    });

    mRequestQueue.add(jsonObjectRequest);


  }


  static void downloadImageFromServer(final String id) {

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    // always download images from unsurv.org
    String queryUrl = "https://unsurv.org/static/images/" + id + ".jpg";


    ImageRequest imageRequest = new ImageRequest(
            queryUrl,
            new Response.Listener<Bitmap>() {
              @Override
              public void onResponse(Bitmap response) {

                StorageUtils.saveBitmap(response, StorageUtils.SYNCHRONIZED_PATH,  id + ".jpg");

              }
            },

            256,
            256,
            ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.ALPHA_8,
            new Response.ErrorListener() {
              @Override
              public void onErrorResponse(VolleyError error) {
                Log.i("SyncUtils: ", "Error in ImageRequest: " + error.toString());
              }
            }
    );

    imageRequest.setRetryPolicy(new DefaultRetryPolicy(
            10000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mRequestQueue.add(imageRequest);

  }

  /**
   * Requests a new api key and saves it to SharedPreferences. Uses LocalBroadCastManager from
   * ctx given to broadcast that a new API key has been successfully acquired.
   * @param context Used to get an Instance of LocalBroadCastManager.
   * @param sharedPreferences SharedPreferences object
   */
  static void getAPIkey(final Context context, final SharedPreferences sharedPreferences) {

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    String baseURL = sharedPreferences.getString("synchronizationUrl", null);

    String completeURL = baseURL + "getKey";

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            completeURL,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                // List<SynchronizedCamera> camerasToSync = new ArrayList<>();

                try {

                  String key = response.getString("key");
                  String expiration = response.getString("utcExpiration");

                  sharedPreferences.edit().putString("apiKey", key).apply();
                  sharedPreferences.edit().putString("apiKeyExpiration", expiration).apply();



                } catch (Exception e) {
                  Log.i(TAG, "onResponse: " + e.toString());

                }

              }
            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        // TODO: Handle Errors
        Log.i(TAG, error.toString());

      }
    });

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {

        if (request.hasHadResponseDelivered()){
          Intent intent = new Intent();
          intent.setAction("org.unsurv.API_KEY_CHANGED");
          LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
          localBroadcastManager.sendBroadcast(intent);
        }

      }
    });

    mRequestQueue.add(jsonObjectRequest);

  }

  /**
   * Uploads cameras specified in parameters to the server.
   * Uses repository as fallback if viewmodel not available (here in Jobservice).
   * Starts image upload after uploading locations first.
   *
   * @param camerasToUpload List of SurveillanceCameras to upload
   * @param baseUrl https://api.unsurv.org/
   * @param sharedPreferences SharedPreferences object
   * @param cameraViewModel db access via ViewModel
   * @param cameraRepository db access via ViewModel
   * @param useRepository boolean specifying which db access should be used
   */
  static void uploadSurveillanceCameras(final List<SurveillanceCamera> camerasToUpload, final String baseUrl, final SharedPreferences sharedPreferences, @Nullable final CameraViewModel cameraViewModel, @Nullable final CameraRepository cameraRepository, final boolean useRepository) {

    JSONArray postArray = new JSONArray();

    // local map (id, SurveillanceCamera) to not be dependant on list order.
    // id is just an int incremented from 0.
    final HashMap<Integer, SurveillanceCamera> cameraMap = new HashMap<>();

    // TODO check this
    // final SparseArray<SurveillanceCamera> cameraMap = new SparseArray<>();

    for (int i=0; i < camerasToUpload.size(); i++) {

      SurveillanceCamera currentCamera = camerasToUpload.get(i);
      // single camera data
      JSONObject tmpJsonObject = new JSONObject();

      // TODO add a non image upload option for manual captures

      try {

        String drawn_cameras = currentCamera.getDrawnRectsAsString();

        tmpJsonObject.put("training_capture", currentCamera.getTrainingCapture());
        tmpJsonObject.put("drawn_cameras", drawn_cameras);
        tmpJsonObject.put("type", currentCamera.getCameraType());

        tmpJsonObject.put("lat", currentCamera.getLatitude());
        tmpJsonObject.put("lon", currentCamera.getLongitude());
        tmpJsonObject.put("manual_capture", currentCamera.getManualCapture());

        // here so we don't have to rely on list order
        tmpJsonObject.put("tmp_id", i);
        // tmp_id : SurveillanceCamera mapping to query in response
        cameraMap.put(i, camerasToUpload.get(i));

      } catch (JSONException jse) {
        Log.i(TAG, "JsonException: " + jse.toString());
      }

      postArray.put(tmpJsonObject);
    }

    JSONObject postObject = new JSONObject();

    try {

      postObject.put("cameras", postArray);

    } catch (JSONException jse) {
      Log.i(TAG, "JsonException: " + jse.toString());
    }

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    String locationUrl = baseUrl + "cameras/upload/location";

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.POST,
            locationUrl,
            postObject,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                // Server sets tmp uuids and sends them back
                try {

                  JSONArray updatedInfo = response.getJSONArray("updated_info");

                  for (int j = 0; j < updatedInfo.length(); j++){

                    // access map to not mix things
                    SurveillanceCamera currentCamera = cameraMap.get(j);

                    // access object from server with same id as tmp_id from local map
                    JSONObject updatedInfoForCamera = updatedInfo.getJSONObject(j);

                    // tmp uuid4 set by server
                    String idSetByServer = updatedInfoForCamera.getString(String.valueOf(j));

                    currentCamera.setExternalId(idSetByServer);
                    currentCamera.setLocationUploaded(true);

                    // completed upload here when no img was taken
                    if (currentCamera.getManualCapture()) {
                      currentCamera.setUploadCompleted(true);
                    }

                    if (useRepository) {
                      cameraRepository.updateCameras(currentCamera);
                    } else {

                      cameraViewModel.update(currentCamera);
                    }

                  }

                  String imageUploadUrl = baseUrl + "cameras/upload/image";

                  if (useRepository) {
                    uploadImages(camerasToUpload, imageUploadUrl, sharedPreferences, null, cameraRepository, true);
                  } else {
                    uploadImages(camerasToUpload, imageUploadUrl, sharedPreferences, cameraViewModel, null, false);
                  }


                } catch (JSONException jse) {
                  Log.i(TAG, "JsonException in response: " + jse.toString());
                }

              }
            },

            new Response.ErrorListener() {
              @Override
              public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "error in ");

              }
            }
    ){
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        String apiKey = sharedPreferences.getString("apiKey", null);
        headers.put("Authorization", apiKey);
        headers.put("Content-Type", "application/json");

        return headers;
      }
    };

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {
        Log.i(TAG, "post request finished");
      }
    });

    mRequestQueue.add(jsonObjectRequest);

  }


  /**
   * Uploads images for a given list to the server. base64 encodes images and sends them as a string
   *
   * @param camerasForImageUpload List of SurveillanceCamera objects
   * @param url "api.unsurv.org/cameras/upload/image"
   * @param sharedPreferences SharedPreferences object
   * @param cameraViewModel db access via ViewModel
   * @param cameraRepository db access via ViewModel
   * @param useRepository boolean specifying which db access should be used
   */

  static void uploadImages(final List<SurveillanceCamera> camerasForImageUpload, String url, final SharedPreferences sharedPreferences, @Nullable final CameraViewModel cameraViewModel, @Nullable final CameraRepository cameraRepository, final boolean useRepository) {

    // HashMap<String, String> idToEncodedImageMap = new HashMap<>();
    final HashMap<String, SurveillanceCamera> cameraMap = new HashMap<>();

    JSONArray postArray = new JSONArray();

    byte[] imageAsBytes;
    String imageAsBase64;

    for (int i = 0; i < camerasForImageUpload.size(); i++){

      SurveillanceCamera currentCamera = camerasForImageUpload.get(i);

      // skip manual captures
      if (currentCamera.getManualCapture() || currentCamera.getUploadCompleted()) {
        continue;
      }

      File imageFile;

      // check if camera is a training image
      if (currentCamera.getTrainingCapture()){
        imageFile = new File(StorageUtils.TRAINING_CAPTURES_PATH + currentCamera.getImagePath());
      } else {
        imageFile = new File(StorageUtils.CAMERA_CAPTURES_PATH + currentCamera.getThumbnailPath());
      }
      JSONObject singleCamera = new JSONObject();

      // create a map with tmpUuidFromServer: imageAsBase64
      try {

        imageAsBytes = StorageUtils.readFileToBytes(imageFile);

        imageAsBase64 =  Base64.encodeToString(imageAsBytes, Base64.DEFAULT);

        // idToEncodedImageMap.put(currentCamera.getExternalId(), imageAsBase64);
        cameraMap.put(currentCamera.getExternalId(), camerasForImageUpload.get(i));

        singleCamera.put(currentCamera.getExternalId(), imageAsBase64);

        postArray.put(singleCamera);

      } catch (Exception e){
        Log.i(TAG, "convertFileToBase64: " + e.toString());
      }

    }

    JSONObject postObject = new JSONObject();

    try {

      // JsonObject where images contains an array with (uuid1:base64img1, uuid2:base64img2, ...)
      postObject.put("images", postArray);

    } catch (JSONException jse) {
      Log.i(TAG, "postJSON: " + jse.toString());
    }


    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    if (postArray.length() != 0){
      mRequestQueue.start();

    }

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.POST,
            url,
            // id:img map gets uploaded
            postObject,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                // response contains a upload successful map (uuid1:integer1, ...) check this and
                // update local db accordingly
                try {

                  JSONArray updatedInfo = response.getJSONArray("updated_info");

                  for (int j = 0; j < updatedInfo.length(); j++){

                    JSONObject currentResponseObject = (JSONObject) updatedInfo.get(j);

                    // access local map with uuid from server
                    SurveillanceCamera currentCamera = cameraMap.get(currentResponseObject.keys().next());

                    JSONObject updatedInfoForCamera = updatedInfo.getJSONObject(j);

                    int returnCodeByServer = updatedInfoForCamera.getInt(currentCamera.getExternalId());

                    // update camera if upload successful
                    if (returnCodeByServer == UPLOAD_SUCCESSFUL){
                      currentCamera.setUploadCompleted(true);
                    }

                    // TODO add logic if upload failed

                    if (useRepository) {
                      cameraRepository.updateCameras(currentCamera);
                    } else {
                      cameraViewModel.update(currentCamera);
                    }

                  }

                  boolean deleteOnUpload = sharedPreferences.getBoolean("deleteOnUpload", false);

                  if (deleteOnUpload){

                    if (useRepository) {
                      cleanupUploadedCameras(camerasForImageUpload, null, cameraRepository, true);
                    } else {

                      cleanupUploadedCameras(camerasForImageUpload, cameraViewModel, null, false);
                    }

                  }

                } catch (JSONException jse) {
                  Log.i(TAG, "JsonException in response: " + jse.toString());
                }

              }
            },

            new Response.ErrorListener() {
              @Override
              public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "error in ");

              }
            }
    ){
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        String apiKey = sharedPreferences.getString("apiKey", null);
        headers.put("Authorization", apiKey);
        headers.put("Content-Type", "application/json");

        return headers;
      }
    };

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {
        Log.i(TAG, "post request finished");
      }
    });

    mRequestQueue.add(jsonObjectRequest);

  }


  /**
   * Checks if local API key is expired. Compares only to local expiration date saved.
   * @param sharedPreferences SharedPreferences object
   * @return boolean isApiKeyExpiredOrMissing
   */
  static boolean isApiKeyExpiredOrMissing(SharedPreferences sharedPreferences){
    SimpleDateFormat timestampIso8601SecondsAccuracy = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    Date apiKeyExpiration;
    Date currentDate = new Date(System.currentTimeMillis());

    try {
      String expiration = sharedPreferences.getString("apiKeyExpiration", null);
      apiKeyExpiration = timestampIso8601SecondsAccuracy.parse(expiration);


      return apiKeyExpiration.before(currentDate);

    } catch (ParseException pse) {
      Log.i(TAG, "apiKeyParse: " + pse.toString());
      return true;

    } catch (NullPointerException npe){
      // api key is missing
      return true;
    }

  }


  /**
   * Checks if local API key is expired. Compares only to local expiration date saved.
   * If key is expired queries a new one and uses a LocalBroadCastManager to
   * @param sharedPreferences
   * @param context
   * @return
   */
  static boolean refreshApiKeyIfExpired(SharedPreferences sharedPreferences, Context context){

    if (isApiKeyExpiredOrMissing(sharedPreferences)){

      getAPIkey(context, sharedPreferences);
      return true;

    } else {
      return false;
    }

  }

  /**
   * Deletes cameras from db using the specified method of db access.
   * Images will get deleted too this way.
   * @param cameras list of SurveillanceCameras
   * @param cameraViewModel db access via ViewModel
   * @param cameraRepository db access via ViewModel
   * @param useRepository boolean specifying which db access should be used
   */
  static void cleanupUploadedCameras(List<SurveillanceCamera> cameras, @Nullable CameraViewModel cameraViewModel, @Nullable CameraRepository cameraRepository, boolean useRepository){

    if (useRepository){
      for (SurveillanceCamera camera : cameras){
        cameraRepository.deleteCamera(camera);

      }
    } else {

      for (SurveillanceCamera camera : cameras){
        cameraViewModel.delete(camera);

      }
    }

  }


  /**
   * returns a YYYY-MM-DD string with random delay in range minUploadDelay < x < maxUploadDelay
   * @param currentTime in millis since 1970
   * @param sharedPreferences SharedPreferences object
   * @return YYYY-MM-DD string with random delay in range minUploadDelay < x < maxUploadDelay
   */
  static String getSynchronizationDateWithRandomDelay(long currentTime, SharedPreferences sharedPreferences){

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    Random random = new Random();

    // stored as String since Preferences can't take arrays with long or int as type
    long minDelay = Long.parseLong(sharedPreferences.getString("minUploadDelay", String.valueOf(1000*60*60*6))); // 6 h
    long maxDelay = Long.parseLong(sharedPreferences.getString("maxUploadDelay", String.valueOf(1000*60*60*24*3))); // 3 d

    long timeframe = maxDelay - minDelay;

    long randomDelay = Math.round(timeframe * random.nextDouble()); // minDelay < x < maxDelay

    return timestampIso8601.format(new Date(currentTime + randomDelay));

  }


}
