package org.unsurv;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;


public class PermissionTutorialFragment extends Fragment {

  public PermissionTutorialFragment(){

  }


  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

    Button grantStoragePermission;
    Button grantCameraPermission;
    Button grantLocationPermission;
    Button grantAllPermissions;

    final View rootView = inflater.inflate(R.layout.tutorial_permission, container,false);

    grantStoragePermission = rootView.findViewById(R.id.permission_tutorial_grant_storage_button);
    grantCameraPermission = rootView.findViewById(R.id.permission_tutorial_grant_camera_button);
    grantLocationPermission = rootView.findViewById(R.id.permission_tutorial_grant_location_button);
    grantAllPermissions = rootView.findViewById(R.id.permission_tutorial_grant_all);

    grantStoragePermission.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        List<String> permissionList = new ArrayList<>();

        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        String[] neededPermissions = permissionList.toArray(new String[0]);
        ActivityCompat.requestPermissions(getActivity(), neededPermissions, 2);
      }
    });

    grantCameraPermission.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        List<String> permissionList = new ArrayList<>();

        permissionList.add(Manifest.permission.CAMERA);
        String[] neededPermissions = permissionList.toArray(new String[0]);
        ActivityCompat.requestPermissions(getActivity(), neededPermissions, 2);
      }
    });

    grantLocationPermission.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        List<String> permissionList = new ArrayList<>();

        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        String[] neededPermissions = permissionList.toArray(new String[0]);
        ActivityCompat.requestPermissions(getActivity(), neededPermissions, 2);
      }
    });

    grantAllPermissions.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        List<String> permissionList = new ArrayList<>();

        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionList.add(Manifest.permission.CAMERA);

        String[] neededPermissions = permissionList.toArray(new String[0]);
        ActivityCompat.requestPermissions(getActivity(), neededPermissions, 2);

      }
    });

    return rootView;
  }
}
