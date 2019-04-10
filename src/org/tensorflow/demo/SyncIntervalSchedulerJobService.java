package org.tensorflow.demo;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SyncIntervalSchedulerJobService extends JobService {

  @Override
  public boolean onStartJob(JobParameters jobParameters) {

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    SynchronizedCameraRepository synchronizedCameraRepository = new SynchronizedCameraRepository(getApplication());

    PersistableBundle intervalSchedulerExtras = jobParameters.getExtras();


    String baseURL = intervalSchedulerExtras.getString("baseURL");
    String areaQuery = "area=" + intervalSchedulerExtras.getString("area");
    String startQuery = "start=" + intervalSchedulerExtras.getString("start");


    SynchronizationUtils.synchronizeWithServer(
            baseURL,
            areaQuery,
            true,
            startQuery,
            synchronizedCameraRepository);

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
}
