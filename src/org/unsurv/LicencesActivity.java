package org.unsurv;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * LicenceActivity to show when user clicks on "Show licences" settings
 */

// TODO add Openstreetmap contributors

public class LicencesActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    TextView tensorflowText;
    TextView osmdroidText;
    TextView osmdroidBonusText;
    TextView picassoText;
    TextView googleTinkText;
    TextView googleVolleyText;
    TextView licenceTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licences);

        tensorflowText = findViewById(R.id.licences_tensorflow);
        osmdroidText = findViewById(R.id.licences_osmdroid);
        osmdroidBonusText = findViewById(R.id.licences_osmdroid_bonus);
        picassoText = findViewById(R.id.licences_picasso);
        googleTinkText = findViewById(R.id.licences_tink);
        googleVolleyText = findViewById(R.id.licences_volley);

        licenceTextView = findViewById(R.id.licence_text);

        licenceTextView.setMovementMethod(new ScrollingMovementMethod());

        // set licence text and scroll to top
        tensorflowText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenceTextView.setText(R.string.tensorflow_licence);
                licenceTextView.scrollTo(0,0);
            }
        });

        osmdroidText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenceTextView.setText(R.string.osmdroid_licence);
                licenceTextView.scrollTo(0,0);
            }
        });

        osmdroidBonusText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenceTextView.setText(R.string.osmdroid_bonuspack_licence);
                licenceTextView.scrollTo(0,0);
            }
        });

        picassoText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenceTextView.setText(R.string.picasso_licence);
                licenceTextView.scrollTo(0,0);
            }
        });

        googleTinkText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenceTextView.setText(R.string.google_tink_licence);
                licenceTextView.scrollTo(0,0);
            }
        });

        googleVolleyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenceTextView.setText(R.string.google_volley_licence);
                licenceTextView.scrollTo(0,0);
            }
        });




        androidx.appcompat.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        bottomNavigationView = findViewById(R.id.navigation);

        // deselect all options in bottom navigation
        int size = bottomNavigationView.getMenu().size();
        for (int i = 0; i < size; i++) {
            bottomNavigationView.getMenu().getItem(i).setCheckable(false);
        }

        // Handle bottom navigation bar clicks
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()){

                    case R.id.bottom_navigation_history:
                        Intent historyIntent = new Intent(LicencesActivity.this, HistoryActivity.class);
                        startActivity(historyIntent);
                        return true;

                    case R.id.bottom_navigation_camera:
                        Intent cameraIntent = new Intent(LicencesActivity.this, DetectorActivity.class);
                        startActivity(cameraIntent);
                        return true;

                    case R.id.bottom_navigation_map:
                        Intent mapIntent = new Intent(LicencesActivity.this, MapActivity.class);
                        startActivity(mapIntent);
                        return true;

                    case R.id.bottom_navigation_stats:
                        Intent statsIntent = new Intent(LicencesActivity.this, StatisticsActivity.class);
                        startActivity(statsIntent);
                        return true;

                }

                return false;

            }
        });


    }
}
