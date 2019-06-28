package org.tensorflow.demo;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;



class SynchronizationUtils {
  // accessible for every app for now
  public static String PICTURES_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/cameras/";

  public static String TRAINING_IMAGES_PATH = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/training/";

  private static int UPLOAD_FAILED = 0;
  private static int UPLOAD_SUCCESSFUL = 1;

  private static String TAG = "SynchronizationUtils";



  static Boolean scheduleSyncIntervalJob (Context context, @Nullable PersistableBundle jobExtras) {

    //TODO Do I need jobExtras here? yes for location boundingbox

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    long syncIntervalInMillis = Long.parseLong(sharedPreferences.getString("synchronizationInterval", "0"));

    if (syncIntervalInMillis == 0) {
      return false; // TODO don't relist job after completion? check documentation again
    }

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    PersistableBundle syncJobExtras = new PersistableBundle();

    // server URL
    syncJobExtras.putString("baseUrl", sharedPreferences.getString("synchronizationURL", null));
    // area to keep updated, set in tutorial
    syncJobExtras.putString("area", sharedPreferences.getString("area", null));
    // last sync time
    syncJobExtras.putString("start", sharedPreferences.getString("lastUpdated", null));
    syncJobExtras.putBoolean("downloadImages", sharedPreferences.getBoolean("downloadImages", false));

    ComponentName componentName = new ComponentName(context, SyncIntervalSchedulerJobService.class);

    JobInfo.Builder jobBuilder  = new JobInfo.Builder(0, componentName); // Job ID

    jobBuilder.setExtras(syncJobExtras);
    // interval
    jobBuilder.setPeriodic(30*1000, 5*1000);
    //jobBuilder.setPeriodic(syncIntervalInMillis);

    //jobBuilder.setOverrideDeadline(15*1000); // force after 15 s for debug

    //jobBuilder.setPersisted(true);

    JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);


    // .schedule overrides previous Jobs with same ID.
    int scheduleResult = jobScheduler.schedule(jobBuilder.build());

    if (scheduleResult == JobScheduler.RESULT_SUCCESS) {
      return true;
    } else {
      return false;  // TODO CHECK FOR RETURN VALUE WHEN SYNC STARTED AND LET USER KNOW IF IT FAILED
    }


  }


  static void downloadCamerasFromServer(String baseUrl, String areaQuery, final SharedPreferences sharedPreferences, final boolean insertIntoDb, @Nullable String startQuery, @Nullable SynchronizedCameraRepository synchronizedCameraRepository){

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

                    SynchronizedCamera cameraToAdd = new SynchronizedCamera(
                            null,
                            JSONToSynchronize.getString("id"),
                            JSONToSynchronize.getDouble("lat"),
                            JSONToSynchronize.getDouble("lon"),
                            JSONToSynchronize.getString("comments"),
                            JSONToSynchronize.getString("last_updated"),
                            JSONToSynchronize.getString("uploaded_at"),
                            JSONToSynchronize.getBoolean("manual_capture")

                    );

                    camerasToSync.add(cameraToAdd);

                  }

                  // local db insert
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

      }
    });

    mRequestQueue.add(jsonObjectRequest);

  }


  static void downloadImagesFromServer(String baseUrl, final List<SynchronizedCamera> cameras, final SharedPreferences sharedPreferences) {

    final List<String> idsFromCameras = new ArrayList<>();

    for (SynchronizedCamera camera : cameras) {
      idsFromCameras.add(camera.getExternalID());
    }

    // object to add ids of images
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

                    saveBytesToFile(imageAsBytes, id + ".jpg", PICTURES_PATH);

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


  static void getAPIkey(final Context context, final SharedPreferences sharedPreferences) {

    final boolean errorInQuery;

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();

    String baseURL = sharedPreferences.getString("synchronizationURL", null);

    String completeURL = baseURL + "getKey";

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            completeURL,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {

                List<SynchronizedCamera> camerasToSync = new ArrayList<>();


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
   * repository as fallback if viewmodel not available (here in Jobservice)
   *
   * @param camerasToUpload
   * @param baseUrl
   * @param sharedPreferences
   * @param cameraViewModel
   * @param cameraRepository
   */
  static void uploadSurveillanceCamera(final List<SurveillanceCamera> camerasToUpload, final String baseUrl, final SharedPreferences sharedPreferences, @Nullable  final CameraViewModel cameraViewModel, @Nullable final CameraRepository cameraRepository, final boolean useRepository) {

    JSONArray postArray = new JSONArray();

    // local map to prevent camera mixups if list order not the same.
    // id is just an int incremented from 0.
    final HashMap<Integer, SurveillanceCamera> cameraMap = new HashMap<>();

    for (int i=0; i < camerasToUpload.size(); i++) {

      SurveillanceCamera currentCamera = camerasToUpload.get(i);
      // single camera data
      JSONObject tmpJsonObject = new JSONObject();

      // TODO add a non image upload option for manual captures

      try {

        String drawn_cameras = currentCamera.getDrawnRectsAsString();

        tmpJsonObject.put("training_capture", currentCamera.getTrainingCapture());
        tmpJsonObject.put("drawn_cameras", drawn_cameras);


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


  static void uploadImages(final List<SurveillanceCamera> camerasForImageUpload, String url, final SharedPreferences sharedPreferences, @Nullable final CameraViewModel cameraViewModel, @Nullable final CameraRepository cameraRepository, final boolean useRepository) {


    HashMap<String, String> idToEncodedImageMap = new HashMap<>();
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

      // if camera is a training image
      if (currentCamera.getTrainingCapture()){
        imageFile = new File(TRAINING_IMAGES_PATH+ currentCamera.getImagePath());
      } else {
        imageFile = new File(PICTURES_PATH + currentCamera.getThumbnailPath());
      }
      JSONObject singleCamera = new JSONObject();

      // create a map with tmpUuidFromServer: imageAsBase64
      try {

        imageAsBytes = readFileToBytes(imageFile);

        imageAsBase64 =  Base64.encodeToString(imageAsBytes, Base64.DEFAULT);

        idToEncodedImageMap.put(currentCamera.getExternalId(), imageAsBase64);
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
    mRequestQueue.start();

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


  static boolean refreshApiKeyIfExpired(SharedPreferences sharedPreferences, Context context){

    SimpleDateFormat timestampIso8601SecondsAccuracy = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    Date apiKeyExpiration;
    Date currentDate = new Date(System.currentTimeMillis());

    try {
      String expiration = sharedPreferences.getString("apiKeyExpiration", null);
      apiKeyExpiration = timestampIso8601SecondsAccuracy.parse(expiration);

      if (apiKeyExpiration.before(currentDate)){

        getAPIkey(context, sharedPreferences);
        return true;

      } else {
        return false;
      }

    } catch (ParseException pse) {
      Log.i(TAG, "apiKeyParse: " + pse.toString());
    }

    return false;

  }

  static void cleanupUploadedCameras(List<SurveillanceCamera> cameras, @Nullable CameraViewModel cameraViewModel, @Nullable CameraRepository cameraRepository, boolean useRepository){

    if (useRepository){
      for (SurveillanceCamera camera : cameras){
        cameraRepository.deleteCameras(camera);

      }
    } else {

      for (SurveillanceCamera camera : cameras){
        cameraViewModel.delete(camera);

      }
    }

  }



  static byte[] readFileToBytes(File f) throws IOException {
    int size = (int) f.length();
    byte[] bytes = new byte[size];
    byte[] tmpBuff = new byte[size];
    FileInputStream fis = new FileInputStream(f);

    try {

      // tries to read file in one go
      int read = fis.read(bytes, 0, size);

      // if file is too big
      if (read < size) {
        int remain = size - read;
        // reads and appends to read from tmpBuff until file is read
        while (remain > 0) {
          read = fis.read(tmpBuff, 0, remain);
          System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
          remain -= read;
        }
      }
    }  catch (IOException e){
      throw e;
    } finally {
      fis.close();
    }

    return bytes;
  }

  static void saveBytesToFile(byte[] bytes, String filename, String path) throws IOException {

    File directoryCheck = new File(path);
    if (!directoryCheck.exists()){
      directoryCheck.mkdirs();
    }

    File file = new File(path + filename);

    FileOutputStream fileOutputStream = new FileOutputStream(file.getPath());

    fileOutputStream.write(bytes);
    fileOutputStream.close();
  }

  /**
   * returns a YYYY-MM-DD string with random delay in range minUploadDelay < x < maxUploadDelay
   * @param currentTime
   * @param sharedPreferences
   * @return
   */
  static String getSynchronizationDateWithRandomDelay(long currentTime, SharedPreferences sharedPreferences){

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    Random random = new Random();

    long minDelay = sharedPreferences.getInt("minUploadDelay", 60*60*24*2) * 1000; // 2 d
    long maxDelay = sharedPreferences.getInt("maxUploadDelay", 60*60*24*7) * 1000; // 7 d

    long timeframe = maxDelay - minDelay;

    long randomDelay = Math.round(timeframe * random.nextDouble()); // minDelay < x < maxDelay

    return timestampIso8601.format(new Date(currentTime + randomDelay));

  }

}
