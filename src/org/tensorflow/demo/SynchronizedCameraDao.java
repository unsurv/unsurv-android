package org.tensorflow.demo;


import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface SynchronizedCameraDao {

  @Insert
  void insert(SynchronizedCamera synchronizedCamera);

  @Query("SELECT * FROM synchronized_cameras")
  List<SynchronizedCamera> getAllCameras();


}
