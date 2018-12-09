package org.tensorflow.demo;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

public class SynchronizedCameraRepository {

  private SynchronizedCameraDao mSynchronizedCameraDao;
  private List<SynchronizedCamera> mAllCameras;

  SynchronizedCameraRepository (Application application) {
    SynchronizedCameraRoomDatabase synchronizedDb = SynchronizedCameraRoomDatabase.getDatabase(application);
    mSynchronizedCameraDao = synchronizedDb.synchronizedCameraDao();
    mAllCameras = mSynchronizedCameraDao.getAllCameras();

  }

  List<SynchronizedCamera> getAllSynchronizedCameras() {
    return mAllCameras;
  }

  public void insert(SynchronizedCamera synchronizedCamera) {
    new insertAsyncTask(mSynchronizedCameraDao).execute();
  }


  private static class insertAsyncTask extends AsyncTask<SynchronizedCamera, Void, Void> {

    private SynchronizedCameraDao mAsyncTaskDao;

    insertAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final SynchronizedCamera... params) {
      mAsyncTaskDao.insert(params[0]);
      return null;
    }
  }

}