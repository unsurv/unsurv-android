
package org.unsurv;

        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.preference.PreferenceManager;
        import androidx.annotation.Nullable;
        import android.view.LayoutInflater;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Button;
        import android.widget.TextView;

        import androidx.fragment.app.Fragment;

        import org.osmdroid.api.IMapController;
        import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
        import org.osmdroid.util.BoundingBox;
        import org.osmdroid.util.GeoPoint;
        import org.osmdroid.views.MapView;

/**
 * This Fragment is part of the tutorial the user sees when first launching the app.
 * The user is shown a big map and chooses a homezone for which data will then be downloaded and
 * synchronized.
 */
public class MapTutorialFragment extends Fragment {

  private SharedPreferences sharedPreferences;
  private Boolean mapScrollingEnabled = true;
  private TutorialViewPager tutorialViewPager;
  private boolean homeZoneIsSet;
  private boolean setHomeButtonUsed;

  public MapTutorialFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.map_tutorial, container,false);

    tutorialViewPager = getActivity().findViewById(R.id.tutorial_viewpager);
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

    homeZoneIsSet = sharedPreferences.getString("area", null) != null;

    final TextView mapTutorialTextView = rootView.findViewById(R.id.map_tutorial_textview);

    mapTutorialTextView.setText(getResources().getString(R.string.tutorial_map_message));

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
        // disable fragment scrolling while map scrolling is needed
        if (!homeZoneIsSet){
          tutorialViewPager.setFragmentScrollingEnabled(false);
          return true;
        }

        if (mapScrollingEnabled) {
          return false;
        } else {
          return true;
        }

      }
    });



    // saves current map borders as homezone if user clicks button
    // reenables fragment scrolling button is clicked
    setHomeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        BoundingBox mapBorders = tutorialMapView.getBoundingBox(); // S N E W

        String westBorder = String.valueOf(mapBorders.getLonWest());
        String northBorder = String.valueOf(mapBorders.getLatNorth());
        String eastBorder = String.valueOf(mapBorders.getLonEast());
        String southBorder = String.valueOf(mapBorders.getLatSouth());


        // I DON'T KNOW WTF IS GOING ON, VOLLEY REQUEST LIB DOESN'T LIKE PRECISE BORDERS
        // borders are shortened to 4 decimals
        String areaString =
                southBorder.substring(0, southBorder.length() - 10)
                + "," + northBorder.substring(0, northBorder.length() - 10)
                + "," + westBorder.substring(0, westBorder.length() - 10)
                + "," + eastBorder.substring(0, eastBorder.length() - 10);

        // areaString = southBorder + "," + northBorder + "," + westBorder + "," + eastBorder;


        mapTutorialTextView.setText(areaString);
        sharedPreferences.edit().putString("area", areaString).apply(); // W N E S

        mapScrollingEnabled = false;
        tutorialViewPager.setFragmentScrollingEnabled(true);

        setHomeButtonUsed = true;
        homeZoneIsSet = true;
        tutorialViewPager.setCurrentItem(tutorialViewPager.getCurrentItem() + 1);



      }
    });

    redrawButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        mapScrollingEnabled = true;
        setHomeButtonUsed = false;
        tutorialViewPager.setFragmentScrollingEnabled(false);


      }
    });



    return rootView;

  }


}
