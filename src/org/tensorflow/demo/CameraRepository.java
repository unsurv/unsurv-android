package org.tensorflow.demo;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

public class CameraRepository {

  private CameraDao mCameraDao;
  private LiveData<List<SurveillanceCamera>> mAllSurveillanceCameras;
  private List<SurveillanceCamera> mSurveillanceCamerasInArea;

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

  int getCamerasAddedByUserCount(){
    try {
      return new CameraRepository.getCountAsyncTask(mCameraDao).execute().get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
      return 0;
    }
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

  private static class getCountAsyncTask extends AsyncTask<Void, Void, Integer> {

    private CameraDao mAsyncTaskDao;

    getCountAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Integer doInBackground(final Void... params) {
      return mAsyncTaskDao.getTotalCamerasAddedByUser();
    }
  }


}
