package org.tensorflow.demo;

import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

public class TutorialActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

  setContentView(R.layout.activity_tutorial);

  ViewPager viewPager = findViewById(R.id.tutorial_viewpager);


  TutorialFragmentAdapter adapter = new TutorialFragmentAdapter(this, getSupportFragmentManager());

  viewPager.setAdapter(adapter);

  }
}
