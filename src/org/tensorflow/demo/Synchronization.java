package org.tensorflow.demo;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

class Synchronization {

  private static int PERIODIC_SYNCHRONIZATION_TIMER_JOB = 0;
  private static int SYNCHRONIZATION_JOB = 1;


  static Boolean scheduleSynchronizationJob(Context context, PersistableBundle jobExtras) {

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    ComponentName componentName = new ComponentName(context, SyncJobService.class);

    JobInfo.Builder jobBuilder  = new JobInfo.Builder(SYNCHRONIZATION_JOB, componentName);

    //TODO check extras["periodicSync"]

    String lastUpdated = sharedPreferences.getString("lastUpdated", "");

    PersistableBundle serviceJobExtras = new PersistableBundle();


    //TODO check all query keys and add query values to Service which are present.

    //API parameters for background Service.

    String areaQuery = "";
    String startQuery = "";

    // TODO other query-pairs
    String endKey = "end=";
    String endValue = "";

    //TODO Do for every key/value pair.
    if (jobExtras.getString("area") != null) {
      String areaKey = "area=";
      areaQuery += areaKey +  "8.2699,50.0201,8.2978,50.0005"; // hardcoded for debugging
    }

    if (jobExtras.getString("start") != null) {
      String startKey = "start=";
      startQuery += startKey + lastUpdated.substring(0, 10); // yyyy-MM-dd
    }


    String[] queryParts = new String[]{areaQuery, startQuery}; //TODO add for each query key

    String queryString = "";

    for (int i = 0; i < queryParts.length; i++) {

      if (!queryParts[i].isEmpty()){
        queryString = queryString.concat(queryParts[i] + "&");
      }
    }

    queryString = queryString.substring(0, queryString.length() - 1);



    serviceJobExtras.putString("queryString", queryString);


    //-----------------------------

    jobBuilder.setExtras(serviceJobExtras);




    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
    JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);

    int scheduleResult = jobScheduler.schedule(jobBuilder.build());

    List<JobInfo> b = jobScheduler.getAllPendingJobs();

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
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
      return false;
    }

    SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
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


}
