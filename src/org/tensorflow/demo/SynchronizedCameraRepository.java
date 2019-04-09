package org.tensorflow.demo;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

public class SynchronizedCameraRepository {

  private SynchronizedCameraDao mSynchronizedCameraDao;
  private List<SynchronizedCamera> mAllCameras;

  SynchronizedCameraRepository (Application application) {
    SynchronizedCameraRoomDatabase synchronizedDb = SynchronizedCameraRoomDatabase.getDatabase(application);
    mSynchronizedCameraDao = synchronizedDb.synchronizedCameraDao();
    //mAllCameras = mSynchronizedCameraDao.getAllCameras();

  }

  List<SynchronizedCamera> getAllSynchronizedCameras() {
    return mSynchronizedCameraDao.getAllCameras();
  }

  public void insert(List<SynchronizedCamera> synchronizedCamera) {
    new insertAsyncTask(mSynchronizedCameraDao).execute(synchronizedCamera);
  }

  public void deleteAll() {
    mSynchronizedCameraDao.deleteAll();
  }


  private static class insertAsyncTask extends AsyncTask<List<SynchronizedCamera>, Void, Void> {

    private SynchronizedCameraDao mAsyncTaskDao;
    private String TAG = "SynchronizedCameraRepository insertAsyncTask";

    insertAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final List<SynchronizedCamera>... params) {
      for (int i = 0; i < params[0].size(); i++) {

        if (mAsyncTaskDao.findID(params[0].get(i).getExternalID()) != null) {

          Log.i(TAG, "id " + params[0].get(i).getExternalID() + " already in db" );
        } else {
          mAsyncTaskDao.insert(params[0].get(i));
        }
      }

      return null;
    }
  }

}