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

  @Query("SELECT * from local_surveillance_cameras")
  LiveData<List<SurveillanceCamera>> getAllCameras();

  //TODO add time taken to database to order by



}
