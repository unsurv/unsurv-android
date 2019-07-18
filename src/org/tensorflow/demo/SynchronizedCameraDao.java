package org.tensorflow.demo;


import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SynchronizedCameraDao {

  @Query("DELETE FROM synchronized_cameras")
  void deleteAll();

  @Insert
  void insert(SynchronizedCamera synchronizedCamera);

  @Update
  void update(SynchronizedCamera synchronizedCamera);

  @Delete
  void delete(SynchronizedCamera synchronizedCamera);

  @Query("SELECT * FROM synchronized_cameras")
  List<SynchronizedCamera> getAllCameras();

  @Query("SELECT * FROM synchronized_cameras WHERE externalID = :externalID")
  SynchronizedCamera findID(String externalID);

  @Query("SELECT * FROM synchronized_cameras " +
          "WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax")
  List<SynchronizedCamera> getCamerasInArea(double latMin, double latMax, double lonMin, double lonMax);

  @Query("SELECT latitude, longitude, externalID FROM synchronized_cameras " +
          "WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax")
  List<SlimSynchronizedCamera> getSlimCamerasInArea(double latMin, double latMax, double lonMin, double lonMax);

  @Query("SELECT externalID FROM synchronized_cameras " +
          "WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax")
  List<String> getIDsInArea(double latMin, double latMax, double lonMin, double lonMax);

  @Query("SELECT * FROM synchronized_cameras WHERE :uuid = externalID")
  SynchronizedCamera findByID(String uuid);

  @Query("SELECT uploadedAt, COUNT(uploadedAt) " +
          "from synchronized_cameras " +
          "WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax " +
          "GROUP BY uploadedAt " +
          "ORDER BY uploadedAt")
  List<StatisticsMap> getStatistics(double latMin, double latMax, double lonMin, double lonMax);

  @Query("SELECT count(*) FROM synchronized_cameras " +
          "WHERE uploadedAt BETWEEN datetime(:startDate) and  datetime(:endDate)")
  int getCamerasAddedInTimeframeAmount(String startDate, String endDate);

  @Query("SELECT * FROM synchronized_cameras " +
          "WHERE uploadedAt BETWEEN datetime('now') and  datetime('now', '-2 minutes') AND imagePath IS NULL")
  List<SynchronizedCamera> getRecentlyUpdatedCameras();

  @Query("SELECT count(*) from synchronized_cameras")
  int getNumberOfCameras();
}
