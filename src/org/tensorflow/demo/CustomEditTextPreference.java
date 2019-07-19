package org.tensorflow.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;

import java.util.ArrayList;
import java.util.List;


public class CustomEditTextPreference extends EditTextPreference {

  public CustomEditTextPreference(Context context) {
    super(context);
  }

  public CustomEditTextPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }


  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {


    super.onBindViewHolder(holder);
  }



  private List<View> getAllChildren(View v) {

    if (!(v instanceof ViewGroup)) {
      ArrayList<View> viewArrayList = new ArrayList<View>();
      viewArrayList.add(v);
      return viewArrayList;
    }

    ArrayList<View> result = new ArrayList<View>();

    ViewGroup viewGroup = (ViewGroup) v;
    for (int i = 0; i < viewGroup.getChildCount(); i++) {

      View child = viewGroup.getChildAt(i);

      //Do not add any parents, just add child elements
      result.addAll(getAllChildren(child));
    }
    return result;
  }


}
