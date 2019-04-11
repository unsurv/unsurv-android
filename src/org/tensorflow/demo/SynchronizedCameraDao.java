package org.tensorflow.demo;


import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface SynchronizedCameraDao {

  @Query("DELETE FROM synchronized_cameras")
  void deleteAll();

  @Insert
  void insert(SynchronizedCamera synchronizedCamera);

  @Query("SELECT * FROM synchronized_cameras")
  List<SynchronizedCamera> getAllCameras();

  @Query("SELECT * FROM synchronized_cameras WHERE externalID = :externalID")
  SynchronizedCamera findID(String externalID);

  @Query("SELECT * FROM synchronized_cameras " +
          "WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax")
  List<SynchronizedCamera> getCamerasInArea(double latMin, double latMax, double lonMin, double lonMax);


}
