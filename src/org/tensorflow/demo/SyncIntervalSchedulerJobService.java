package org.tensorflow.demo;

import android.app.job.JobParameters;
import android.app.job.JobService;
import androidx.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SyncIntervalSchedulerJobService extends JobService {

  private String baseUrl;
  private String areaQuery;
  private String startQuery;
  private boolean downloadImages;
  private SynchronizedCameraRepository synchronizedCameraRepository;
  private CameraRepository cameraRepository;
  private SharedPreferences sharedPreferences;
  private static String TAG = "SyncIntervalSchedulerJobService";
  private SimpleDateFormat timestampIso8601SecondsAccuracy;


  @Override
  public boolean onStartJob(JobParameters jobParameters) {

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());
    cameraRepository = new CameraRepository(getApplication());

    PersistableBundle intervalSchedulerExtras = jobParameters.getExtras();


    baseUrl = intervalSchedulerExtras.getString("baseUrl");
    areaQuery = "area=" + intervalSchedulerExtras.getString("area");
    startQuery = "start=" + sharedPreferences.getString("lastUpdated", "01-01-2000");
    downloadImages = intervalSchedulerExtras.getBoolean("downloadImages");

    timestampIso8601SecondsAccuracy = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    // aborts current query if API key expired. starts same query after a new API key is aquired in refreshApiKeyAsyncTask
    try {

      Date apiKeyExpiration = timestampIso8601SecondsAccuracy.parse(sharedPreferences.getString("apiKeyExpiration", null));

      Date currentDate = new Date(System.currentTimeMillis());

      if (apiKeyExpiration.before(currentDate)){

        // Abort current query after requesting a new API key.
        new SyncIntervalSchedulerJobService.refreshApiKeyAsyncTask().execute();
        return false;
      }


    } catch (ParseException pse) {
      Log.i(TAG, "queryServerForCamera: " + pse.toString());
    }


    SynchronizationUtils.downloadCamerasFromServer(
            baseUrl,
            areaQuery,
            sharedPreferences,
            true,
            startQuery,
            synchronizedCameraRepository
            );

    if (downloadImages) {

      Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {

          List<SynchronizedCamera> recentCameras = synchronizedCameraRepository.getCamerasAddedInLastTwoMinutes();

          SynchronizationUtils.downloadImagesFromServer(
                  baseUrl,
                  recentCameras,
                  sharedPreferences
                  );
        }
      }, 30000);


    }

    List<SurveillanceCamera> camerasToUpload = cameraRepository.getCamerasForUpload();

    SynchronizationUtils.uploadSurveillanceCamera(camerasToUpload, baseUrl, sharedPreferences, null, cameraRepository, true);

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

    sharedPreferences.edit().putString(
            "lastUpdated",
            timestampIso8601.format(new Date(System.currentTimeMillis())))
            .apply();

    return false;
  }

  @Override
  public boolean onStopJob(JobParameters jobParameters) {
    return false;
  }


  // gets a new API key if it's expired. requeues original server query afterwards.
  private class refreshApiKeyAsyncTask extends AsyncTask<Void, Void, Void> {

    private String TAG = "SynchronizedCameraRepository insertAsyncTask";


    refreshApiKeyAsyncTask(){}

    @Override
    protected Void doInBackground(Void... params) {
      SynchronizationUtils.getAPIkey(getApplicationContext(), sharedPreferences);

      return null;
    }

    @Override
    protected void onPostExecute(Void nothingness) {

      Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {

          SynchronizationUtils.downloadCamerasFromServer(
                  baseUrl,
                  areaQuery,
                  sharedPreferences,
                  true,
                  startQuery,
                  synchronizedCameraRepository
          );
        }
      }, 10000);



      if (downloadImages) {

        Handler imageDownloadHandler = new Handler();
        imageDownloadHandler.postDelayed(new Runnable() {
          @Override
          public void run() {

            List<SynchronizedCamera> recentCameras = synchronizedCameraRepository.getCamerasAddedInLastTwoMinutes();

            SynchronizationUtils.downloadImagesFromServer(
                    baseUrl,
                    recentCameras,
                    sharedPreferences
            );
          }
        }, 30000);


      }

      SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
      timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

      sharedPreferences.edit().putString(
              "lastUpdated",
              timestampIso8601.format(new Date(System.currentTimeMillis())))
              .apply();
    }
  }

}
