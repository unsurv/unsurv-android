package org.unsurv;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.List;

/**
 * Database access class.
 * All access spawns a seperate AsyncTask to enable easy db access in gui threads.
 */

// TODO create insertAll/delete single camera
public class SynchronizedCameraRepository {

  private SynchronizedCameraDao mSynchronizedCameraDao;
  private List<SynchronizedCamera> mAllCameras;

  private String startDate;
  private String endDate;

  SynchronizedCameraRepository (Application application) {
    SynchronizedCameraRoomDatabase synchronizedDb = SynchronizedCameraRoomDatabase.getDatabase(application);
    mSynchronizedCameraDao = synchronizedDb.synchronizedCameraDao();
    //mAllCameras = mSynchronizedCameraDao.getAllCamerasAsLiveData();

  }


  List<SynchronizedCamera> getAllSynchronizedCameras(boolean useAsyncTask) {

    if (useAsyncTask){

      try {
        List<SynchronizedCamera> cameras = new getAllAsyncTask(mSynchronizedCameraDao).execute().get();
        return cameras;

      } catch (Exception e) {
        Log.i("Background findByID Error: " , e.toString());
      }

    } else {
      return mSynchronizedCameraDao.getAllCameras();
    }

    return null;

  }


  List<SynchronizedCamera> getSynchronizedCamerasInArea(double latMin, double latMax, double lonMin, double lonMax) {
    return mSynchronizedCameraDao.getCamerasInArea(latMin, latMax, lonMin, lonMax);
  }

  List<SynchronizedCamera> getSynchronizedCamerasInAreaAsync(double latMin, double latMax, double lonMin, double lonMax) {

    try {

      return new getAllSynchronizedCamerasInAreaAsyncTask(mSynchronizedCameraDao).execute(latMin, latMax, lonMin, lonMax).get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
    }

    return null;


  }


  List<String> getIDsInArea(double latMin, double latMax, double lonMin, double lonMax) {
    return mSynchronizedCameraDao.getIDsInArea(latMin, latMax, lonMin, lonMax);
  }



  List<SlimSynchronizedCamera> getSlimCamerasInArea(double latMin, double latMax, double lonMin, double lonMax) {
    try {

      return new findSlimCamerasInArea(mSynchronizedCameraDao).execute(latMin, latMax, lonMin, lonMax).get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
    }

    return null;
  }


  SynchronizedCamera findByID(String uuid) {
    try {

      return new findByIDAsyncTask(mSynchronizedCameraDao).execute(uuid).get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
    }

    return null;
  }


  List<StatisticsMap> getPerDayStatistics(double latMin, double latMax, double lonMin, double lonMax, String startDate, String endDate) {
    try {
      this.startDate = startDate;
      this.endDate = endDate;
      return new getStatisticsAsyncTask(mSynchronizedCameraDao).execute(latMin, latMax, lonMin, lonMax).get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
    }

    return null;
  }

  int getAmountInTimeframe(String startDate, String endDate){
    try {
      return new camerasInTimeframeAsyncTask(mSynchronizedCameraDao).execute(startDate, endDate).get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
      return 0;
    }

  }


  List<SynchronizedCamera> getCamerasAddedInLastTwoMinutes(){
    try {

      return new camerasInLastTwoMinutesAsyncTask(mSynchronizedCameraDao).execute().get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
      return null;
    }

  }

  int getTotalAmountInDb(){
    try {
      return new camerasTotalAsyncTask(mSynchronizedCameraDao).execute().get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
      return 0;
    }
  }



  void insertAll(List<SynchronizedCamera> synchronizedCameras) {

      for (SynchronizedCamera camera : synchronizedCameras) {
          new insertSingleAsyncTask(mSynchronizedCameraDao).execute(camera);
      }

  }

  void insert(SynchronizedCamera synchronizedCamera){
      new insertSingleAsyncTask(mSynchronizedCameraDao).execute(synchronizedCamera);
  }

  public void update(SynchronizedCamera synchronizedCamera) {
    new updateAsyncTask(mSynchronizedCameraDao).execute(synchronizedCamera);
  }

  public void delete(SynchronizedCamera synchronizedCamera) {

    String current_path = StorageUtils.SYNCHRONIZED_PATH + synchronizedCamera.getImagePath();

    File file = new File(current_path);
    boolean deleted = file.delete();

    new deleteAsyncTask(mSynchronizedCameraDao).execute(synchronizedCamera);
  }

  public void deleteAll() {
    List<SynchronizedCamera> allCameras = mSynchronizedCameraDao.getAllCameras();



    for (SynchronizedCamera camera : allCameras) {
      String current_path = StorageUtils.SYNCHRONIZED_PATH + camera.getImagePath();

      File file = new File(current_path);
      boolean deleted = file.delete();

    }

    mSynchronizedCameraDao.deleteAll();
  }



  private static class getAllAsyncTask extends AsyncTask<Void, Void, List<SynchronizedCamera>> {

    private SynchronizedCameraDao mAsyncTaskDao;

    getAllAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SynchronizedCamera> doInBackground(Void... voids) {

      return mAsyncTaskDao.getAllCameras();


    }
  }


  private static class getAllSynchronizedCamerasInAreaAsyncTask extends AsyncTask<Double, Void, List<SynchronizedCamera>> {

    private SynchronizedCameraDao mAsyncTaskDao;

    getAllSynchronizedCamerasInAreaAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SynchronizedCamera> doInBackground(Double... params) {

      // latMin, latMax, lonMin, lonMax
      return mAsyncTaskDao.getCamerasInArea(params[0], params[1], params[2], params[3]);


    }
  }



  private static class insertSingleAsyncTask extends AsyncTask<SynchronizedCamera, Void, Void> {

        private SynchronizedCameraDao mAsyncTaskDao;
        String TAG = "SynchronizedCameraRepository InsertAsyncTask";

        insertSingleAsyncTask(SynchronizedCameraDao dao) {
            mAsyncTaskDao = dao;
        }


        @Override
        protected Void doInBackground(final SynchronizedCamera... params) {

            if (mAsyncTaskDao.findID(params[0].getExternalID()) != null) {
                Log.i(TAG, "id " + params[0].getExternalID() + " already in db" );
            } else {
                mAsyncTaskDao.insert(params[0]);
            }

            return null;
        }
    }

  private static class findByIDAsyncTask extends AsyncTask<String, Void, SynchronizedCamera> {

    private SynchronizedCameraDao mAsyncTaskDao;

    findByIDAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected SynchronizedCamera doInBackground(final String... params) {

      return mAsyncTaskDao.findByID(params[0]);
    }

  }

  private static class updateAsyncTask extends AsyncTask<SynchronizedCamera, Void, Void> {

    private SynchronizedCameraDao mAsyncTaskDao;

    updateAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final SynchronizedCamera... params) {

       mAsyncTaskDao.update(params[0]);

      return null;
    }

  }

  private static class deleteAsyncTask extends AsyncTask<SynchronizedCamera, Void, Void> {

    private SynchronizedCameraDao mAsyncTaskDao;

    deleteAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final SynchronizedCamera... params) {

      mAsyncTaskDao.delete(params[0]);

      return null;
    }

  }


  private static class getStatisticsAsyncTask extends AsyncTask<Double, Void, List<StatisticsMap>> {

    private SynchronizedCameraDao mAsyncTaskDao;

    getStatisticsAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<StatisticsMap> doInBackground(final Double... params) {

      return mAsyncTaskDao.getStatistics(params[0], params[1], params[2], params[3]);

    }

  }


  private static class camerasInTimeframeAsyncTask extends AsyncTask<String, Void, Integer> {

    private SynchronizedCameraDao mAsyncTaskDao;

    camerasInTimeframeAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Integer doInBackground(final String... params) {

      return mAsyncTaskDao.getCamerasAddedInTimeframeAmount(params[0], params[1]);

    }

  }


  private static class camerasInLastTwoMinutesAsyncTask extends AsyncTask<Void, Void, List<SynchronizedCamera>> {

    private SynchronizedCameraDao mAsyncTaskDao;

    camerasInLastTwoMinutesAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SynchronizedCamera> doInBackground(final Void... params) {

      return mAsyncTaskDao.getRecentlyUpdatedCameras();

    }

  }


  private static class camerasTotalAsyncTask extends AsyncTask<Void, Void, Integer> {

    private SynchronizedCameraDao mAsyncTaskDao;

    camerasTotalAsyncTask(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Integer doInBackground(final Void... params) {

      return mAsyncTaskDao.getNumberOfCameras();

    }

  }


  private static class findSlimCamerasInArea extends AsyncTask<Double, Void, List<SlimSynchronizedCamera>> {

    private SynchronizedCameraDao mAsyncTaskDao;

    findSlimCamerasInArea(SynchronizedCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SlimSynchronizedCamera> doInBackground(final Double... params) {

      return mAsyncTaskDao.getSlimCamerasInArea(params[0], params[1], params[2], params[3]);

    }

  }



}