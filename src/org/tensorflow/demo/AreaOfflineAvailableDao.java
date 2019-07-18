package org.tensorflow.demo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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
