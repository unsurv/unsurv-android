package org.unsurv;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * LicenceActivity to show when user clicks on licence settings
 */

public class LicencesActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licences);


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
