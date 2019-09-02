package org.unsurv;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;

import java.util.List;

/**
 * ViewModel can be used t access LiveData objects
 */
public class SynchronizedCameraViewModel extends AndroidViewModel {

  private SynchronizedCameraRepository mSynchronizedCameraRepository;
  private List<SynchronizedCamera> mAllSynchronizedCameras;

  public SynchronizedCameraViewModel (Application application) {
    super(application);
    mSynchronizedCameraRepository = new SynchronizedCameraRepository(application);
    //mAllSynchronizedCameras = mSynchronizedCameraRepository.getAllSynchronizedCameras();

  }

  List<SynchronizedCamera> getAllSynchronizedCameras() { return mAllSynchronizedCameras; }

  public void insert(List<SynchronizedCamera> synchronizedCamera) {
    mSynchronizedCameraRepository.insert(synchronizedCamera);
  }
}
