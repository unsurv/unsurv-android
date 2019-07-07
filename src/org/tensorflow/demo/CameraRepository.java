package org.tensorflow.demo;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.graphics.Camera;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.List;

import static org.tensorflow.demo.SynchronizationUtils.PICTURES_PATH;

public class CameraRepository {

  private CameraDao mCameraDao;
  private LiveData<List<SurveillanceCamera>> mAllSurveillanceCameras;
  private List<SurveillanceCamera> mSurveillanceCamerasInArea;

  CameraRepository(Application application) {
    CameraRoomDatabase db = CameraRoomDatabase.getDatabase(application);
    mCameraDao = db.surveillanceCameraDao();
    mAllSurveillanceCameras = mCameraDao.getAllCamerasAsLiveData();
  }

  LiveData<List<SurveillanceCamera>> getAllCamerasAsLiveData() {
      return mAllSurveillanceCameras;
  }

  public long insert (SurveillanceCamera surveillanceCamera) {

    try {
      return new insertAsyncTask(mCameraDao).execute(surveillanceCamera).get();
    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
      return 0;
    }


  }

  int getCamerasAddedByUserCount(){
    try {
      return new CameraRepository.getCountAsyncTask(mCameraDao).execute().get();

    } catch (Exception e) {
      Log.i("Background findByID Error: " , e.toString());
      return 0;
    }
  }

  void updateCameras (SurveillanceCamera... surveillanceCameras) {
    new updateAsyncTask(mCameraDao).execute(surveillanceCameras);
  }

  void deleteCamera(SurveillanceCamera... surveillanceCameras) {

    new deleteAsyncTask(mCameraDao).execute(surveillanceCameras);
  }


  List<SurveillanceCamera> getAllCameras(){

    try {
      return new getAllCamerasAsyncTask(mCameraDao).execute().get();

    } catch (Exception e) {
      Log.i("Background getAllCameras Error: " , e.toString());
      return null;
    }

  }

  List<SurveillanceCamera> getCamerasForUpload(){

    try {
      List<SurveillanceCamera> camerasToUpload = new getCamerasForUploadAsyncTask(mCameraDao).execute().get();
      return camerasToUpload;

    } catch (Exception e) {
      Log.i("Background getIdsForImageUpload Error: " , e.toString());
      return null;
    }

  }

  List<SurveillanceCamera> getCamerasForImageUpload(){

    try {
      List<SurveillanceCamera> camerasToUploadImageFor = new getCamerasForImageUploadAsyncTask(mCameraDao).execute().get();
      return camerasToUploadImageFor;

    } catch (Exception e) {
      Log.i("Background getIdsForImageUpload Error: " , e.toString());
      return null;
    }

  }

  SurveillanceCamera findByDbId(int dbId){

    try {
      SurveillanceCamera camera = new findByIDAsyncTask(mCameraDao).execute(dbId).get();
      return camera;

    } catch (Exception e) {
      Log.i("Background findByDbId Error: " , e.toString());
      return null;
    }

  }



  private static class insertAsyncTask extends AsyncTask<SurveillanceCamera, Void, Long> {

    private CameraDao mAsyncTaskDao;

    insertAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Long doInBackground(final SurveillanceCamera... params) {
      return mAsyncTaskDao.insert(params[0]);
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

  private static class updateAsyncTask extends AsyncTask<SurveillanceCamera, Void, Void> {

    private CameraDao mAsyncTaskDao;

    updateAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final SurveillanceCamera... params) {
      mAsyncTaskDao.updateCameras(params);
      return null;
    }
  }

  private static class deleteAsyncTask extends AsyncTask<SurveillanceCamera, Void, Void> {

    private CameraDao mAsyncTaskDao;

    deleteAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final SurveillanceCamera... params) {

      int deletedFiles = SynchronizationUtils.deleteImagesForCamera(params[0]);
      Log.i("deleteCameraAsync", "deleted " + deletedFiles + " files");

      mAsyncTaskDao.deleteCameras(params);
      return null;
    }
  }

  private static class getAllCamerasAsyncTask extends AsyncTask<Void, Void, List<SurveillanceCamera>> {

    private CameraDao mAsyncTaskDao;

    getAllCamerasAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SurveillanceCamera> doInBackground(Void... params) {
      return mAsyncTaskDao.getAllCameras();
    }
  }

  private static class getCamerasForUploadAsyncTask extends AsyncTask<Void, Void, List<SurveillanceCamera>> {

    private CameraDao mAsyncTaskDao;

    getCamerasForUploadAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SurveillanceCamera> doInBackground(Void... params) {
      return mAsyncTaskDao.getCamerasToUpload();
    }
  }


  private static class getCamerasForImageUploadAsyncTask extends AsyncTask<Void, Void, List<SurveillanceCamera>> {

    private CameraDao mAsyncTaskDao;

    getCamerasForImageUploadAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SurveillanceCamera> doInBackground(Void... params) {
      return mAsyncTaskDao.getCamerasForImageUpload();
    }
  }

  private static class findByIDAsyncTask extends AsyncTask<Integer, Void, SurveillanceCamera> {

    private CameraDao mAsyncTaskDao;
    private String TAG = "SynchronizedCameraRepository findByIdAsyncTask";

    findByIDAsyncTask(CameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected SurveillanceCamera doInBackground(final Integer... params) {

      SurveillanceCamera queriedCamera = mAsyncTaskDao.findById(params[0]);

      return queriedCamera;
    }

  }


}
