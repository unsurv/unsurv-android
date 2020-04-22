package org.unsurv;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity that holds the tutorial fragments
 */
public class TutorialActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

  setContentView(R.layout.activity_tutorial);

  ViewPager viewPager = findViewById(R.id.tutorial_viewpager);


  LinearLayout progressView = findViewById(R.id.tutorial_progress_bar);
  progressView.getChildAt(0).setBackgroundColor(Color.LTGRAY);

  TutorialFragmentAdapter adapter = new TutorialFragmentAdapter(this, getSupportFragmentManager());


  viewPager.setAdapter(adapter);


  // set progress bar to current step of tutorial
  viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

      LinearLayout tutorialProgress = findViewById(R.id.tutorial_progress_bar);

      for (int i = 0; i < 4; i++) {
        tutorialProgress.getChildAt(i).setBackgroundColor(Color.DKGRAY);
      }

      tutorialProgress.getChildAt(position).setBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
  });

  }
}
