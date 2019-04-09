
package org.tensorflow.demo;

        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.preference.PreferenceManager;
        import android.support.annotation.Nullable;
        import android.support.v4.app.Fragment;
        import android.support.v4.view.ViewPager;
        import android.view.LayoutInflater;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Button;
        import android.widget.TextView;

        import org.osmdroid.api.IMapController;
        import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
        import org.osmdroid.util.BoundingBox;
        import org.osmdroid.util.GeoPoint;
        import org.osmdroid.views.MapView;


public class MapTutorialFragment extends android.support.v4.app.Fragment {

  SharedPreferences sharedPreferences;
  Boolean mapScrollingEnabled = true;


  public MapTutorialFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.map_tutorial, container,false);

    final TutorialViewPager tutorialViewPager = getActivity().findViewById(R.id.tutorial_viewpager);
    tutorialViewPager.setFragmentScrollingEnabled(false);



    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

    final TextView mapTutorialTextView = rootView.findViewById(R.id.map_tutorial_textview);
    mapTutorialTextView.setText("Please select an area for offline availability");

    final Button setHomeButton = rootView.findViewById(R.id.tutorial_set_home_button);
    final Button redrawButton = rootView.findViewById(R.id.tutorial_redraw_button);
    final MapView tutorialMapView = rootView.findViewById(R.id.tutorial_map);

    tutorialMapView.setTilesScaledToDpi(true);
    tutorialMapView.setClickable(true);

    //enable pinch to zoom
    tutorialMapView.setMultiTouchControls(true);

    tutorialMapView.setTileSource(TileSourceFactory.OpenTopo);

    final IMapController mapController = tutorialMapView.getController();

    // Setting starting position and zoom level.
    GeoPoint startPoint = new GeoPoint(50.0027, 8.2771);
    mapController.setZoom(10.0);
    mapController.setCenter(startPoint);


    tutorialMapView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mapScrollingEnabled) {
          return false;
        } else {
          return true;
        }

      }
    });


    setHomeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        BoundingBox mapBorders = tutorialMapView.getBoundingBox(); // S N E W

        String westBorder = String.valueOf(mapBorders.getLonWest());
        String northBorder = String.valueOf(mapBorders.getLatNorth());
        String eastBorder = String.valueOf(mapBorders.getLonEast());
        String southBorder = String.valueOf(mapBorders.getLatSouth());


        // I DON'T KNOW WTF IS GOING ON, VOLLEY REQUEST LIB DOESN'T LIKE PRECISE BORDERS
        String areaString =
                westBorder.substring(0, westBorder.length() - 11) + "," +
                northBorder.substring(0, northBorder.length() - 11) + "," +
                eastBorder.substring(0, eastBorder.length() - 11) + "," +
                southBorder.substring(0, southBorder.length() - 11);


        mapTutorialTextView.setText(areaString);
        sharedPreferences.edit().putString("area", "area=" + areaString).apply(); // W N E S

        mapScrollingEnabled = false;
        tutorialViewPager.setFragmentScrollingEnabled(true);
        tutorialViewPager.setCurrentItem(1);

      }
    });

    redrawButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        mapScrollingEnabled = true;
        tutorialViewPager.setFragmentScrollingEnabled(false);


      }
    });



    return rootView;

  }


}
