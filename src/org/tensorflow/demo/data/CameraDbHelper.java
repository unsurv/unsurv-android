package org.tensorflow.demo.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.tensorflow.demo.data.CamerasContract.LocalCameras;

public class CameraDbHelper extends SQLiteOpenHelper {

  public static final String LOG_TAG = CameraDbHelper.class.getSimpleName();

  /** Name of database file */
  private static final String DATABASE_NAME = "localCameras.db";

  /** Increment if db schema changes. */
  private static final int DATABASE_VERSION = 1;

  /**
   * @param context context of the app
   */
  public CameraDbHelper(Context context){
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {

    // SQL CREATE TABLE statement
    String SQL_CREATE_LOCALCAMERAS_TABLE = "CREATE TABLE " + LocalCameras.TABLE_NAME + " ("
            + LocalCameras._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + LocalCameras.COLUMN_THUMBNAIL_PATH + " TEXT NOT NULL, "
            + LocalCameras.COLUMN_IMAGE_PATH + " TEXT NOT NULL, "
            + LocalCameras.COLUMN_CAMERA_LEFT + " INTEGER NOT NULL, "
            + LocalCameras.COLUMN_CAMERA_TOP + " INTEGER NOT NULL, "
            + LocalCameras.COLUMN_CAMERA_RIGHT + " INTEGER NOT NULL, "
            + LocalCameras.COLUMN_CAMERA_BOTTOM + " INTEGER NOT NULL, "
            + LocalCameras.COLUMN_LATITUDE + " REAL, "
            + LocalCameras.COLUMN_LONGITUDE + " REAL, "
            + LocalCameras.COLUMN_ACCURACY + " REAL, "
            + LocalCameras.COLUMN_COMMENT + " TEXT DEFAULT \"" + LocalCameras.DEFAULT_COMMENT + "\");"
            ;

    db.execSQL(SQL_CREATE_LOCALCAMERAS_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    
  }
}
