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
import java.util.TimeZone;


@Database(entities = {AreaOfflineAvailable.class}, version = 1)
public abstract class AreaOfflineAvailableRoomDatabase extends RoomDatabase {

  public abstract AreaOfflineAvailableDao areaOfflineAvailableDao();

  private static volatile AreaOfflineAvailableRoomDatabase INSTANCE;

  // Avoid having multiple databases.
  static AreaOfflineAvailableRoomDatabase getDatabase(final Context context) {
    if (INSTANCE == null) {
      synchronized (AreaOfflineAvailableRoomDatabase.class) {
        if (INSTANCE == null) {
          // create db here
          INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                  AreaOfflineAvailableRoomDatabase.class, "offline_areas")
                  .addCallback(sSynchronizedRoomDatabaseCallback)
                  .build();

        }

      }
    }
    return INSTANCE;
  }

  private static AreaOfflineAvailableRoomDatabase.Callback sSynchronizedRoomDatabaseCallback =
          new CameraRoomDatabase.Callback(){

            @Override
            public void onOpen (@NonNull SupportSQLiteDatabase db){
              super.onOpen(db);
              new PopulateDbAsync(INSTANCE).execute();
            }
          };



  private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

    private final AreaOfflineAvailableDao mDao;

    PopulateDbAsync(AreaOfflineAvailableRoomDatabase db) {
      mDao = db.areaOfflineAvailableDao();
    }

    @Override
    protected Void doInBackground(final Void... params) {
      mDao.deleteAll();

      long currentTimeInMillis = System.currentTimeMillis();
      SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd");
      timestampIso8601.setTimeZone(TimeZone.getTimeZone("UTC"));

      // 49.6391,50.3638,7.8648,8.6888
      AreaOfflineAvailable areaOfflineAvailable = new AreaOfflineAvailable(
              49.6391,
              50.3638,
              7.8648,
              8.6888,
              timestampIso8601.format(new Date(currentTimeInMillis))
      );


      mDao.insert(areaOfflineAvailable);


      return null;
    }
  }


}


