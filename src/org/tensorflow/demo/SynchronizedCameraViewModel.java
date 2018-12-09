package org.tensorflow.demo;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;

import java.util.List;

public class SynchronizedCameraViewModel extends AndroidViewModel {

  private SynchronizedCameraRepository mSynchronizedCameraRepository;
  private List<SynchronizedCamera> mAllSynchronizedCameras;

  public SynchronizedCameraViewModel (Application application) {
    super(application);
    mSynchronizedCameraRepository = new SynchronizedCameraRepository(application);
    mAllSynchronizedCameras = mSynchronizedCameraRepository.getAllSynchronizedCameras();

  }

  List<SynchronizedCamera> getAllSynchronizedCameras() { return mAllSynchronizedCameras; }

  public void insert(SynchronizedCamera synchronizedCamera) {
    mSynchronizedCameraRepository.insert(synchronizedCamera);
  }
}
