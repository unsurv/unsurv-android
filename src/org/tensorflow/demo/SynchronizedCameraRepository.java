package org.tensorflow.demo;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SynchronizedCameraRepository {

  private SynchronizedCameraDao mSynchronizedCameraDao;
  private List<SynchronizedCamera> mAllCameras;

  private String startDate;
  private String endDate;

  SynchronizedCameraRepository (Application application) {
    SynchronizedCameraRoomDatabase synchronizedDb = SynchronizedCameraRoomDatabase.getDatabase(application);
    mSynchronizedCameraDao = synchronizedDb.synchronizedCameraDao();
    //mAllCameras = mSynchronizedCameraDao.getAllCameras();

  }


  List<SynchronizedCamera> getAllSynchronizedCameras() {
    return mSynchronizedCameraDao.getAllCameras();
  }


  List<SynchronizedCamera> getSynchronizedCamerasInArea(double latMin, double latMax, double lonMin, double lonMax) {
    return mSynchronizedCameraDao.getCamerasInArea(latMin, latMax, lonMin, lonMax);
  }


  List<String> getIDsInArea(double latMin, double latMax, double lonMin, double lonMax) {
    return mSynchronizedCameraDao.getIDsInArea(latMin, latMax, lonMin, lonMax);
  }


  SynchronizedCamera findByID(String uuid) {
    try {
      SynchronizedCamera camera = new findByIDAsyncTask(mSynchronizedCameraDao).execute(uuid).get();
      return camera;

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
    }

    return null;
  }


  public List<StatisticsMap> getStatistics(double latMin, double latMax, double lonMin, double lonMax, String startDate, String endDate) {
    try {
      this.startDate = startDate;
      this.endDate = endDate;
      return new getStatisticsAsyncTask(mSynchronizedCameraDao).execute(latMin, latMax, lonMin, lonMax).get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
    }

    return null;
  }


  public void insert(List<SynchronizedCamera> synchronizedCamera) {
    new insertAsyncTask(mSynchronizedCameraDao).execute((List)synchronizedCamera);
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

  private static class findByIDAsyncTask extends AsyncTask<String, Void, SynchronizedCamera> {

    private SynchronizedCameraDao mAsyncTaskDao;
    private String TAG = "SynchronizedCameraRepository insertAsyncTask";

    findByIDAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected SynchronizedCamera doInBackground(final String... params) {

      SynchronizedCamera queriedCamera = mAsyncTaskDao.findByID(params[0]);

      return queriedCamera;
    }

  }


  private static class getStatisticsAsyncTask extends AsyncTask<Double, Void, List<StatisticsMap>> {

    private SynchronizedCameraDao mAsyncTaskDao;
    private String TAG = "SynchronizedCameraRepository insertAsyncTask";

    getStatisticsAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<StatisticsMap> doInBackground(final Double... params) {

      List<StatisticsMap> statistics = mAsyncTaskDao.getStatistics(params[0], params[1], params[2], params[3]);

      return statistics;
    }

  }



}