package org.tensorflow.demo;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

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
            Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/unsurv/";

            PopulateDbAsync(CameraRoomDatabase db) {
      mDao = db.surveillanceCameraDao();
    }

    @Override
    protected Void doInBackground(final Void... params) {
      mDao.deleteAll();

      for (int i = 0; i < 200; i++) {
        SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        Random rng = new Random();
        double randomLat = (rng.nextDouble() * 2 - 1) / 10;
        double randomLong = (rng.nextDouble() * 2 - 1) / 10;

        // Populate database with placeholder data.
        SurveillanceCamera surveillanceCamera = new SurveillanceCamera(
                picturesPath + "190754878_thumbnail.jpg", picturesPath + "190754878.jpg",
                10, 120, 50, 140,
                50.0005 + randomLat, 8.2832 + randomLong,
                10.3345, 0.3653, 12.3313, 170.3332,
                "no comment",
                timestampIso8601.format(new Date(System.currentTimeMillis() - rng.nextInt(1000*60*60*24*24))),
                timestampIso8601.format(new Date(System.currentTimeMillis() + rng.nextInt(1000*60*60*24))));
        mDao.insert(surveillanceCamera);
      }

      return null;
    }
  }



}
