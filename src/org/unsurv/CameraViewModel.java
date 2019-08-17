package org.unsurv;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Used as a layer above a database repository. Used in RecyclerView in HistoryActivity with
 * LiveData objects. Adapted from Google RoomDataBase tutorial.
 */

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

  public void update(SurveillanceCamera surveillanceCamera) {mRepository.updateCameras(surveillanceCamera);}

  public void delete(SurveillanceCamera surveillanceCamera) {
    StorageUtils.deleteImagesForCamera(surveillanceCamera);
    mRepository.deleteCamera(surveillanceCamera);
  }

}
