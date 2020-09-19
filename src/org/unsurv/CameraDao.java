package org.unsurv;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CameraDao {

  @Insert(onConflict = 1)
  long insert(SurveillanceCamera surveillanceCamera);

  @Update
  void updateCameras(SurveillanceCamera... surveillanceCameras);

  @Delete
  void deleteCameras(SurveillanceCamera... surveillanceCameras);

  @Query("DELETE FROM local_surveillance_cameras")
  void deleteAll();

  @Query("SELECT * FROM local_surveillance_cameras ORDER BY -id")
  LiveData<List<SurveillanceCamera>> getAllCamerasAsLiveData();

  @Query("SELECT * FROM local_surveillance_cameras " +
          "WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax")
  List<SurveillanceCamera> getCamerasInArea(double latMin, double latMax, double lonMin, double lonMax);

  @Query("SELECT * FROM local_surveillance_cameras WHERE timestamp " +
          "BETWEEN datetime('now', :startpoint) AND datetime('now', :endpoint)")
  List<SurveillanceCamera> getCamerasAddedInTimeframe(String startpoint, String endpoint);

  @Query("SELECT count(*) FROM local_surveillance_cameras WHERE timestamp " +
          "BETWEEN datetime('2000-01-01') AND datetime('now', :endpoint)")
  int getTotalCamerasUpTo(String endpoint);

  @Query("SELECT count(*) FROM local_surveillance_cameras")
  int getTotalCamerasAddedByUser();

  @Query("SELECT * FROM local_surveillance_cameras WHERE timeToSync " +
          "BETWEEN datetime('2000-01-01') AND datetime('now')")
  List<SurveillanceCamera> getCamerasToUpload();

  @Query("SELECT * from local_surveillance_cameras")
  List<SurveillanceCamera> getAllCameras();

  @Query("SELECT * from local_surveillance_cameras WHERE locationUploaded")
  List<SurveillanceCamera> getCamerasForImageUpload();

  @Query("SELECT * from local_surveillance_cameras WHERE uploadCompleted")
  List<SurveillanceCamera> getUploadedCameras();

  @Query("SELECT * from local_surveillance_cameras WHERE id = :id")
  SurveillanceCamera findById(long id);

  @Query("SELECT * FROM local_surveillance_cameras ORDER BY id DESC LIMIT 1")
  SurveillanceCamera getLastCamera();


}
// example db timestamp 2018-09-17T15:26:33+0000
