package org.tensorflow.demo;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface CameraDao {

  @Insert(onConflict = 1)
  void insert(SurveillanceCamera surveillanceCamera);

  @Query("DELETE FROM local_surveillance_cameras")
  void deleteAll();

  @Query("SELECT * FROM local_surveillance_cameras")
  LiveData<List<SurveillanceCamera>> getAllCameras();

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


}
// example db timestamp 2018-09-17T15:26:33+0000
