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

@Database(entities = {SynchronizedCamera.class}, version = 1)
public abstract class SynchronizedCameraRoomDatabase extends RoomDatabase {

  public abstract SynchronizedCameraDao synchronizedCameraDao();

  private static volatile SynchronizedCameraRoomDatabase INSTANCE;

  // Avoid having multiple databases.
  static SynchronizedCameraRoomDatabase getDatabase(final Context context) {
    if (INSTANCE == null) {
      synchronized (SynchronizedCameraRoomDatabase.class) {
        if (INSTANCE == null) {
          // create db here
          INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                  SynchronizedCameraRoomDatabase.class, "synchronized_cameras")
                  .addCallback(sSynchronizedRoomDatabaseCallback)
                  .build();

        }

      }
    }
    return INSTANCE;
  }


  private static SynchronizedCameraRoomDatabase.Callback sSynchronizedRoomDatabaseCallback =
          new CameraRoomDatabase.Callback(){

            @Override
            public void onOpen (@NonNull SupportSQLiteDatabase db){
              super.onOpen(db);
              new PopulateDbAsync(INSTANCE).execute();
            }
          };


  private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

    private final SynchronizedCameraDao mDao;

    PopulateDbAsync(SynchronizedCameraRoomDatabase db) {
      mDao = db.synchronizedCameraDao();
    }

    @Override
    protected Void doInBackground(final Void... params) {
      //mDao.deleteAll();

      SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
      timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

      for (int i = 0; i < 10; i++) {
        Random rng = new Random();
        double randomLat = (rng.nextDouble() * 2 - 1) / 10;
        double randomLong = (rng.nextDouble() * 2 - 1) / 10;

        // Populate database with placeholder data.
        SynchronizedCamera synchronizedCamera = new SynchronizedCamera(
                "test_pixel_2.jpg",
                "asjdnjasdqwdmqwlo",
                0,
                50.0005 + randomLat, 8.2832 + randomLong,
                "no comments",
                timestampIso8601.format(new Date(System.currentTimeMillis() - rng.nextInt(1000*60*60*24*24))),
                timestampIso8601.format(new Date(System.currentTimeMillis() - rng.nextInt(1000*60*60*24*24))),
                false
                );
        //mDao.insert(synchronizedCamera);

      }


      return null;
    }
  }


}
