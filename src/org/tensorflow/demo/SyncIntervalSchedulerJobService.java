package org.tensorflow.demo;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.PersistableBundle;

public class SyncIntervalSchedulerJobService extends JobService {

  @Override
  public boolean onStartJob(JobParameters jobParameters) {


    PersistableBundle intervalSchedulerExtras = jobParameters.getExtras();

    PersistableBundle syncJobExtras = new PersistableBundle();
    PersistableBundle asd = (PersistableBundle) intervalSchedulerExtras.clone();


    syncJobExtras.putString("start", intervalSchedulerExtras.getString("start"));
    syncJobExtras.putString("area", intervalSchedulerExtras.getString("area"));


    Synchronization.scheduleSynchronizationJob(getApplicationContext(), syncJobExtras);

    return false;
  }

  @Override
  public boolean onStopJob(JobParameters jobParameters) {
    return false;
  }
}
