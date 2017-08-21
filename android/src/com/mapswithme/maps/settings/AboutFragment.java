package com.mapswithme.maps.settings;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mapswithme.maps.BuildConfig;
import com.mapswithme.maps.R;
import com.mapswithme.maps.widget.BaseShadowController;
import com.mapswithme.maps.widget.ObservableScrollView;
import com.mapswithme.maps.widget.ScrollViewShadowController;
import com.mapswithme.transtech.OtaMapdataUpdater;

public class AboutFragment extends BaseSettingsFragment
{
  @Override
  protected int getLayoutRes()
  {
    return R.layout.about;
  }

  @Override
  protected BaseShadowController createShadowController()
  {
    clearPaddings();
    return new ScrollViewShadowController((ObservableScrollView) mFrame.findViewById(R.id.content_frame));
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
  {
    super.onCreateView(inflater, container, savedInstanceState);

    ((TextView) mFrame.findViewById(R.id.version))
        .setText(getString(R.string.version, BuildConfig.VERSION_NAME));

    ((TextView) mFrame.findViewById(R.id.data_version))
        .setText(getString(R.string.data_version, OtaMapdataUpdater.getCurrentMapdataVersion()));

    return mFrame;
  }
}
