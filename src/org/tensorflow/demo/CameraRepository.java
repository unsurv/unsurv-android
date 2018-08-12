package org.tensorflow.demo;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class CameraRepository {

  private CameraDao mCameraDao;
  private LiveData<List<SurveillanceCamera>> mAllSurveillanceCameras;

  CameraRepository(Application application) {
    CameraRoomDatabase db = CameraRoomDatabase.getDatabase(application);
    mCameraDao = db.surveillanceCameraDao();
    mAllSurveillanceCameras = mCameraDao.getAllCameras();
  }

  LiveData<List<SurveillanceCamera>> getAllCameras() {
      return mAllSurveillanceCameras;
  }

  public void insert (SurveillanceCamera surveillanceCamera) {
    new insertAsyncTask(mCameraDao).execute(surveillanceCamera);
  }


  private static class insertAsyncTask extends AsyncTask<SurveillanceCamera, Void, Void> {

    private CameraDao mAsyncTaskDao;

    insertAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final SurveillanceCamera... params) {
      mAsyncTaskDao.insert(params[0]);
      return null;
    }
  }


}
