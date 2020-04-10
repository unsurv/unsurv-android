package org.unsurv;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;


/**
 * Database for SurveillanceCameras captured by the user. After CameraCaptures get processed a
 * SurveillanceCamera object is created and saved here. Other ways are in ManualCaptureActivity and
 * CaptureTrainingImageActivity
 */

@Database(entities = {SurveillanceCamera.class}, version = 1, exportSchema = false)
public abstract class CameraRoomDatabase extends RoomDatabase {

  public abstract CameraDao surveillanceCameraDao();

  private static CameraRoomDatabase INSTANCE;

  static CameraRoomDatabase getDatabase(final Context context) {
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


  // populates the database for debugging purposes
  private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

    private final CameraDao mDao;

            PopulateDbAsync(CameraRoomDatabase db) {
      mDao = db.surveillanceCameraDao();
    }

    @Override
    protected Void doInBackground(final Void... params) {
      mDao.deleteAll();

      SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
      timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

      for (int i = 0; i < 3; i++) {
        Random rng = new Random();
        double randomLat = (rng.nextDouble() * 2 - 1) / 10;
        double randomLong = (rng.nextDouble() * 2 - 1) / 10;

        // Populate database with placeholder data.
        SurveillanceCamera surveillanceCamera = new SurveillanceCamera(
                0,
                0,
                0,
                0,
                0,
                "test_pixel_2.jpg", "asd.jpg",
                null,
                50.0005 + randomLat, 8.2832 + randomLong,
                "",
                timestampIso8601.format(new Date(System.currentTimeMillis() - rng.nextInt(1000*60*60*24*24))),
                timestampIso8601.format(new Date(System.currentTimeMillis() + rng.nextInt(1000*60*60*24))),
                false,
                false,
                false,
                false,
                "",
                "");


        SurveillanceCamera trainingCamera = new SurveillanceCamera(
                0,
                0,
                0,
                0,
                0,
                "test_pixel_2.jpg", "asd.jpg",
                null,
                0, 0,
                "",
                timestampIso8601.format(new Date(System.currentTimeMillis() - rng.nextInt(1000*60*60*24*24))),
                timestampIso8601.format(new Date(System.currentTimeMillis() + rng.nextInt(1000*60*60*24))),
                false,
                false,
                false,
                true,
                "[{\"0\":\"357 240 691 440\"},{\"0\":\"42 296 364 500\"},{\"1\":\"5 494 367 677\"},{\"1\":\"372 482 708 711\"}]",
                "");


        // mDao.insert(trainingCamera);
      }


      return null;
    }
  }



}
