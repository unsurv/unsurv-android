package org.tensorflow.demo;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;
@Dao
public interface AreaOfflineAvailableDao {

  @Query("DELETE FROM offline_areas")
  void deleteAll();

  @Insert
  void insert(AreaOfflineAvailable areaOfflineAvailable);

  @Query("SELECT * FROM offline_areas")
  List<AreaOfflineAvailable> getAllAreas();


  @Query("SELECT * FROM offline_areas " +
          "WHERE :latMin AND :latMax BETWEEN latMin AND latMax AND :lonMin AND :lonMax BETWEEN lonMin AND lonMax")
  List<AreaOfflineAvailable> isOfflineAvailable(double latMin, double latMax, double lonMin, double lonMax);

  @Update
  void updateArea(AreaOfflineAvailable area);

}
