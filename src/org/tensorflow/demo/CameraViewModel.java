package org.tensorflow.demo;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

public class CameraViewModel extends AndroidViewModel {

  private CameraRepository mRepository;
  private LiveData<List<SurveillanceCamera>> mAllCameras;

  public CameraViewModel(Application application) {
    super(application);
    mRepository = new CameraRepository(application);
    mAllCameras = mRepository.getAllCamerasAsLiveData();
  }

  LiveData<List<SurveillanceCamera>> getAllCameras() {
    return mAllCameras;
  }

  public void insert(SurveillanceCamera surveillanceCamera) {mRepository.insert(surveillanceCamera);}

}
