package org.tensorflow.demo;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SyncIntervalSchedulerJobService extends JobService {

  private String baseURL;
  private String areaQuery;
  private String startQuery;
  private SynchronizedCameraRepository synchronizedCameraRepository;
  private SharedPreferences sharedPreferences;
  private static String TAG = "SyncIntervalSchedulerJobService";
  private SimpleDateFormat timestampIso8601SecondsAccuracy;


  @Override
  public boolean onStartJob(JobParameters jobParameters) {

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());

    PersistableBundle intervalSchedulerExtras = jobParameters.getExtras();


    baseURL = intervalSchedulerExtras.getString("baseURL") + "cameras/?";
    areaQuery = "area=" + intervalSchedulerExtras.getString("area");
    startQuery = "start=" + sharedPreferences.getString("lastUpdated", "01-01-2000");

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


    SynchronizationUtils.synchronizeCamerasWithServer(
            baseURL,
            areaQuery,
            true,
            startQuery,
            synchronizedCameraRepository
            );

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
      SynchronizationUtils.getAPIkey(sharedPreferences);

      return null;
    }

    @Override
    protected void onPostExecute(Void nothingness) {

      SynchronizationUtils.synchronizeCamerasWithServer(
              baseURL,
              areaQuery,
              true,
              startQuery,
              synchronizedCameraRepository
      );

      SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
      timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

      sharedPreferences.edit().putString(
              "lastUpdated",
              timestampIso8601.format(new Date(System.currentTimeMillis())))
              .apply();
    }
  }







}
