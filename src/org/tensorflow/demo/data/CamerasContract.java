package org.tensorflow.demo.data;

import android.provider.BaseColumns;

public final class CamerasContract {

  public static abstract class LocalCameras implements BaseColumns {

    public static final String _ID = BaseColumns._ID;
    public static final String TABLE_NAME = "localCameras";
    public static final String COLUMN_THUMBNAIL_PATH = "thumbnail_path";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_CAMERA_LEFT = "camera_left";
    public static final String COLUMN_CAMERA_TOP = "camera_top";
    public static final String COLUMN_CAMERA_RIGHT = "camera_right";
    public static final String COLUMN_CAMERA_BOTTOM = "camera_bottom";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_ACCURACY = "accuracy";
    public static final String COLUMN_COMMENT = "comment";

    public static final String DEFAULT_COMMENT = "no comment available";

  }

}
