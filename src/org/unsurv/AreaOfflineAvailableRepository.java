package org.unsurv;

import android.app.Application;
import android.os.AsyncTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

class AreaOfflineAvailableRepository {

  private AreaOfflineAvailableDao areaOfflineAvailableDao;

  AreaOfflineAvailableRepository (Application application) {
    AreaOfflineAvailableRoomDatabase areaDb = AreaOfflineAvailableRoomDatabase.getDatabase(application);
    areaOfflineAvailableDao = areaDb.areaOfflineAvailableDao();
    }

  List<AreaOfflineAvailable> getAllOfflineAreas() {
    return areaOfflineAvailableDao.getAllAreas();
  }

  void deleteAll() {
    new DeleteAsyncTask(areaOfflineAvailableDao).execute();
  }

  List<AreaOfflineAvailable> isOfflineavailable(double latMin, double latMax, double lonMin, double lonMax) {
    return areaOfflineAvailableDao.isOfflineAvailable(latMin, latMax, lonMin, lonMax);
  }

  void insert(AreaOfflineAvailable areaOfflineAvailable) {
    new InsertAsyncTask(areaOfflineAvailableDao).execute(areaOfflineAvailable);
  }

  void update(AreaOfflineAvailable areaOfflineAvailable) {
    new UpdateAsyncTask(areaOfflineAvailableDao).execute(areaOfflineAvailable);
  }


  private static class InsertAsyncTask extends AsyncTask<AreaOfflineAvailable, Void, Void> {

    private AreaOfflineAvailableDao mAsyncTaskDao;

    InsertAsyncTask(AreaOfflineAvailableDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final AreaOfflineAvailable... params) {

      mAsyncTaskDao.insert(params[0]);
      return null;
    }
  }

  private static class UpdateAsyncTask extends AsyncTask<AreaOfflineAvailable, Void, Void> {

    private AreaOfflineAvailableDao mAsyncTaskDao;

    UpdateAsyncTask(AreaOfflineAvailableDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final AreaOfflineAvailable... params) {

      SimpleDateFormat timestampIso8601;

      timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
      timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

      long currentTimeInMillis = System.currentTimeMillis();
      String isoDate =timestampIso8601.format(new Date(currentTimeInMillis));
      params[0].setLastUpdated(isoDate);
      mAsyncTaskDao.updateArea(params[0]);
      return null;
    }
  }

  private static class DeleteAsyncTask extends AsyncTask<Void, Void, Void> {

    private AreaOfflineAvailableDao mAsyncTaskDao;

    DeleteAsyncTask(AreaOfflineAvailableDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(final Void... params) {

      mAsyncTaskDao.deleteAll();
      return null;
    }
  }







}
