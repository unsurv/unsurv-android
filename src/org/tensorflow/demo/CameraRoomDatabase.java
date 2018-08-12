package org.tensorflow.demo;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;

@Database(entities = {SurveillanceCamera.class}, version = 1)
public abstract class CameraRoomDatabase extends RoomDatabase {

  public abstract CameraDao surveillanceCameraDao();

  private static CameraRoomDatabase INSTANCE;

  public static CameraRoomDatabase getDatabase(final Context context) {
    if (INSTANCE == null) {
      synchronized (CameraRoomDatabase.class) {
        if (INSTANCE == null) {

          INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                  CameraRoomDatabase.class, "local_surveillance_cameras")
                  .addCallback(sRoomDatabaseCallback)
                  .build();

        }
      }
    }
    return INSTANCE;
  }


  private static CameraRoomDatabase.Callback sRoomDatabaseCallback =
          new CameraRoomDatabase.Callback(){

            @Override
            public void onOpen (@NonNull SupportSQLiteDatabase db){
              super.onOpen(db);
              new PopulateDbAsync(INSTANCE).execute();
            }
          };


  private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

    private final CameraDao mDao;
    private static String picturesPath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES).getAbsolutePath();

            PopulateDbAsync(CameraRoomDatabase db) {
      mDao = db.surveillanceCameraDao();
    }

    @Override
    protected Void doInBackground(final Void... params) {
      mDao.deleteAll();
      SurveillanceCamera surveillanceCamera = new SurveillanceCamera(picturesPath + "/73457629_thumbnail.jpg", picturesPath + "/nsurv//73457629.jpg", 10, 120, 50, 140, 50.0005, 8.2832, 10.3345, "no comment");

      mDao.insert(surveillanceCamera);
      return null;
    }
  }



}
