package com.mapswithme.maps.routing;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import au.net.transtech.geo.model.VehicleProfile;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.MwmActivity;
import com.mapswithme.maps.R;
import com.mapswithme.maps.bookmarks.data.DistanceAndAzimut;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.maps.settings.SettingsActivity;
import com.mapswithme.maps.sound.TtsPlayer;
import com.mapswithme.maps.traffic.TrafficManager;
import com.mapswithme.maps.widget.FlatProgressView;
import com.mapswithme.maps.widget.menu.NavMenu;
import com.mapswithme.transtech.TranstechConstants;
import com.mapswithme.transtech.TranstechUtil;
import com.mapswithme.transtech.route.RouteConstants;
import com.mapswithme.transtech.route.RouteTrip;
import com.mapswithme.transtech.route.RouteUtil;
import com.mapswithme.util.StringUtils;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.Utils;
import com.mapswithme.util.statistics.AlohaHelper;
import com.mapswithme.util.statistics.Statistics;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NavigationController implements TrafficManager.TrafficCallback
{
  private static final String STATE_SHOW_TIME_LEFT = "ShowTimeLeft";

  private final View mFrame;
  private final View mBottomFrame;
  //IFACE-1163 private final View mSearchButtonFrame;
  private final NavMenu mNavMenu;

  private final ImageView mNextTurnImage;
  private final TextView mNextTurnDistance;
  private final TextView mCircleExit;

  private final View mNextNextTurnFrame;
  private final ImageView mNextNextTurnImage;

  private final View mStreetFrame;
  private final TextView mNextStreet;

  private final TextView mSpeedValue;
  private final TextView mSpeedUnits;
  private final TextView mTimeHourValue;
  private final TextView mTimeHourUnits;
  private final TextView mTimeMinuteValue;
  private final TextView mTimeMinuteUnits;
  private final ImageView mDotTimeLeft;
  private final ImageView mDotTimeArrival;
  private final TextView mDistanceValue;
  private final TextView mDistanceUnits;
  private final FlatProgressView mRouteProgress;

  //IFACE-1163 @NonNull
  //IFACE-1163 private final SearchWheel mSearchWheel;

  private boolean mShowTimeLeft = true;

  private double mNorth;

  public NavigationController(Activity activity)
  {
    mFrame = activity.findViewById(R.id.navigation_frame);
    mBottomFrame = mFrame.findViewById(R.id.nav_bottom_frame);
    mBottomFrame.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        switchTimeFormat();
      }
    });
    mNavMenu = createNavMenu();
    mNavMenu.refresh();

    // Top frame
    View topFrame = mFrame.findViewById(R.id.nav_top_frame);
    View turnFrame = topFrame.findViewById(R.id.nav_next_turn_frame);
    mNextTurnImage = (ImageView) turnFrame.findViewById(R.id.turn);
    mNextTurnDistance = (TextView) turnFrame.findViewById(R.id.distance);
    mCircleExit = (TextView) turnFrame.findViewById(R.id.circle_exit);

    mNextNextTurnFrame = topFrame.findViewById(R.id.nav_next_next_turn_frame);
    mNextNextTurnImage = (ImageView) mNextNextTurnFrame.findViewById(R.id.turn);

    mStreetFrame = topFrame.findViewById(R.id.street_frame);
    mNextStreet = (TextView) mStreetFrame.findViewById(R.id.street);
    View shadow = topFrame.findViewById(R.id.shadow_top);
    UiUtils.showIf(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP, shadow);

    UiUtils.extendViewWithStatusBar(mStreetFrame);
    UiUtils.extendViewMarginWithStatusBar(turnFrame);

    // Bottom frame
    mSpeedValue = (TextView) mBottomFrame.findViewById(R.id.speed_value);
    mSpeedUnits = (TextView) mBottomFrame.findViewById(R.id.speed_dimen);
    mTimeHourValue = (TextView) mBottomFrame.findViewById(R.id.time_hour_value);
    mTimeHourUnits = (TextView) mBottomFrame.findViewById(R.id.time_hour_dimen);
    mTimeMinuteValue = (TextView) mBottomFrame.findViewById(R.id.time_minute_value);
    mTimeMinuteUnits = (TextView) mBottomFrame.findViewById(R.id.time_minute_dimen);
    mDotTimeArrival = (ImageView) mBottomFrame.findViewById(R.id.dot_estimate);
    mDotTimeLeft = (ImageView) mBottomFrame.findViewById(R.id.dot_left);
    mDistanceValue = (TextView) mBottomFrame.findViewById(R.id.distance_value);
    mDistanceUnits = (TextView) mBottomFrame.findViewById(R.id.distance_dimen);
    mRouteProgress = (FlatProgressView) mBottomFrame.findViewById(R.id.navigation_progress);

    //IFACE-1163 mSearchButtonFrame = activity.findViewById(R.id.search_button_frame);
    //IFACE-1163 mSearchWheel = new SearchWheel(mSearchButtonFrame);

      //TRANSTECH
      requestRouteSync(activity);
      if( activity instanceof LocationHelper.UiCallback)
        ComplianceController.get().init( (LocationHelper.UiCallback) activity );
  }

  public void onResume()
  {
    mNavMenu.onResume(null);
    //IFACE-1163 mSearchWheel.onResume();
    ComplianceController.get().start("NavigationController::onResume()");
  }

  private NavMenu createNavMenu()
  {
    return new NavMenu(mBottomFrame, new NavMenu.ItemClickListener<NavMenu.Item>()
    {
      @Override
      public void onItemClick(NavMenu.Item item)
      {
        final MwmActivity parent = ((MwmActivity) mFrame.getContext());
        switch (item)
        {
        case STOP:
          RoutingController.get().cancel();
          stop( parent );
          break;
        case SETTINGS:
          parent.closeMenu(Statistics.EventName.ROUTING_SETTINGS, AlohaHelper.MENU_SETTINGS, new Runnable()
          {
            @Override
            public void run()
            {
              parent.startActivity(new Intent(parent, SettingsActivity.class));
            }
          });
          break;
        case TTS_VOLUME:
          TtsPlayer.setEnabled(!TtsPlayer.isEnabled());
          mNavMenu.refreshTts();
          Statistics.INSTANCE.trackEvent(Statistics.EventName.ROUTING_CLOSE);
          AlohaHelper.logClick(AlohaHelper.ROUTING_CLOSE);
          break;
        case TRAFFIC:
          TrafficManager.INSTANCE.toggle();
          mNavMenu.refreshTraffic();
          //TODO: Add statistics reporting (in separate task)
          break;
        case TOGGLE:
          mNavMenu.toggle(true);
          parent.refreshFade();
        }
      }
    });
  }

  private void stop(MwmActivity parent)
  {
    Statistics.INSTANCE.trackEvent(Statistics.EventName.ROUTING_CLOSE);
    AlohaHelper.logClick(AlohaHelper.ROUTING_CLOSE);
    parent.refreshFade();
    //IFACE-1163 mSearchWheel.reset();
    ComplianceController.get().stop("NavigationController::stop()");
  }

  private void updateVehicle(RoutingInfo info)
  {
    mNextTurnDistance.setText(Utils.formatUnitsText(mFrame.getContext(),
                                                    R.dimen.text_size_nav_number,
                                                    R.dimen.text_size_nav_dimension,
                                                    info.distToTurn,
                                                    info.turnUnits));
    info.vehicleTurnDirection.setTurnDrawable(mNextTurnImage);
    if (RoutingInfo.VehicleTurnDirection.isRoundAbout(info.vehicleTurnDirection))
      UiUtils.setTextAndShow(mCircleExit, String.valueOf(info.exitNum));
    else
      UiUtils.hide(mCircleExit);

    UiUtils.showIf(info.vehicleNextTurnDirection.containsNextTurn(), mNextNextTurnFrame);
    if (info.vehicleNextTurnDirection.containsNextTurn())
      info.vehicleNextTurnDirection.setNextTurnDrawable(mNextNextTurnImage);
  }

  private void updatePedestrian(RoutingInfo info)
  {
    Location next = info.pedestrianNextDirection;
    Location location = LocationHelper.INSTANCE.getSavedLocation();
    DistanceAndAzimut da = Framework.nativeGetDistanceAndAzimuthFromLatLon(next.getLatitude(), next.getLongitude(),
                                                                           location.getLatitude(), location.getLongitude(),
                                                                           mNorth);
    String[] splitDistance = da.getDistance().split(" ");
    mNextTurnDistance.setText(Utils.formatUnitsText(mFrame.getContext(),
                                                    R.dimen.text_size_nav_number,
                                                    R.dimen.text_size_nav_dimension,
                                                    splitDistance[0],
                                                    splitDistance[1]));
    if (info.pedestrianTurnDirection != null)
      RoutingInfo.PedestrianTurnDirection.setTurnDrawable(mNextTurnImage, da);
  }

  public void updateNorth(double north)
  {
    if (!RoutingController.get().isNavigating())
      return;

    mNorth = north;
    update(Framework.nativeGetRouteFollowingInfo());
  }

  public void update(RoutingInfo info)
  {
//      Log.i("Map_NavigationController", "Updating routing info " + info);
    if (info == null)
      return;

    if (Framework.nativeGetRouter() == Framework.ROUTER_TYPE_PEDESTRIAN)
      updatePedestrian(info);
    else
      updateVehicle(info);

      StringBuilder complianceText = new StringBuilder();
      if( ComplianceController.get().getCurrentMode() != ComplianceController.ComplianceMode.NONE )
      {
          VehicleProfile profile = ComplianceController.get().getRouter( mFrame.getContext() ).getSelectedProfile();
          String[] words = TextUtils.split( ComplianceController.get().getCurrentMode().name(), "_" );
          for( int i = 0; i < words.length; ++i )
          {
              if( !TextUtils.isEmpty(words[i]) )
                complianceText.append( (i > 0 ? " " : "") )
                        .append( words[ i ].substring( 0, 1 ).toUpperCase() )
                        .append( words[ i ].substring( 1 ).toLowerCase() );
          }
          complianceText.append( " (" ).append( profile.getDescription() ).append( ")" );
      }

    boolean hasStreet = !TextUtils.isEmpty(info.nextStreet) || !TextUtils.isEmpty( complianceText );
    UiUtils.showIf(hasStreet, mStreetFrame);
    if (hasStreet)
    {
        if(TextUtils.isEmpty(complianceText))
            mNextStreet.setText( info.nextStreet );
        else if(TextUtils.isEmpty(info.nextStreet))
            mNextStreet.setText( complianceText );
        else
            mNextStreet.setText( info.nextStreet + " | " + complianceText );
    }
    final Location last = LocationHelper.INSTANCE.getLastKnownLocation();
    if (last != null)
    {
      Pair<String, String> speedAndUnits = StringUtils.nativeFormatSpeedAndUnits(last.getSpeed());
      mSpeedValue.setText(speedAndUnits.first);
      mSpeedUnits.setText(speedAndUnits.second);
    }
    updateTime(info.totalTimeInSeconds);
    mDistanceValue.setText(info.distToTarget);
    mDistanceUnits.setText(info.targetUnits);
    mRouteProgress.setProgress((int) info.completionPercent);
  }

  private void updateTime(int seconds)
  {
    if (mShowTimeLeft)
      updateTimeLeft(seconds);
    else
      updateTimeEstimate(seconds);

    mDotTimeLeft.setEnabled(mShowTimeLeft);
    mDotTimeArrival.setEnabled(!mShowTimeLeft);
  }

  private void updateTimeLeft(int seconds)
  {
    final long hours = TimeUnit.SECONDS.toHours(seconds);
    final long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
    UiUtils.setTextAndShow(mTimeMinuteValue, String.valueOf(minutes));
    String min = mFrame.getResources().getString(R.string.minute);
    UiUtils.setTextAndShow(mTimeMinuteUnits, min);
    if (hours == 0)
    {
      UiUtils.hide(mTimeHourUnits, mTimeHourValue);
      return;
    }
    UiUtils.setTextAndShow(mTimeHourValue, String.valueOf(hours));
    String hour = mFrame.getResources().getString(R.string.hour);
    UiUtils.setTextAndShow(mTimeHourUnits, hour);
  }

  private void updateTimeEstimate(int seconds)
  {
    final Calendar currentTime = Calendar.getInstance();
    currentTime.add(Calendar.SECOND, seconds);
    UiUtils.setTextAndShow(mTimeMinuteValue, DateFormat.getTimeInstance(DateFormat.SHORT)
                                                       .format(currentTime.getTime()));
    UiUtils.hide(mTimeHourUnits, mTimeHourValue, mTimeMinuteUnits);
  }

  private void switchTimeFormat()
  {
    mShowTimeLeft = !mShowTimeLeft;
    update(Framework.nativeGetRouteFollowingInfo());
  }

  public void show(boolean show)
  {
    UiUtils.showIf(show, mFrame);
    //IFACE-1163 UiUtils.showIf(show, mSearchButtonFrame);
    mNavMenu.show(show);
  }

  public NavMenu getNavMenu()
  {
    return mNavMenu;
  }

  public void onSaveState(@NonNull Bundle outState)
  {
    outState.putBoolean(STATE_SHOW_TIME_LEFT, mShowTimeLeft);
    //IFACE-1163 mSearchWheel.saveState(outState);
  }

  public void onRestoreState(@NonNull Bundle savedInstanceState)
  {
    mShowTimeLeft = savedInstanceState.getBoolean(STATE_SHOW_TIME_LEFT);
    //IFACE-1163 mSearchWheel.restoreState(savedInstanceState);
  }

  public boolean cancel()
  {
    if (RoutingController.get().cancel())
    {
      final MwmActivity parent = ((MwmActivity) mFrame.getContext());
      stop(parent);
      return true;
    }
    return false;
  }

  @Override
  public void onEnabled()
  {
    mNavMenu.refreshTraffic();
  }

  @Override
  public void onDisabled()
  {
    mNavMenu.refreshTraffic();
  }

  @Override
  public void onWaitingData()
  {
    // no op
  }

  @Override
  public void onOutdated()
  {
    // no op
  }

  @Override
  public void onNoData(boolean notify)
  {
    // no op
  }

  @Override
  public void onNetworkError()
  {
    // no op
  }

  @Override
  public void onExpiredData(boolean notify)
  {
    // no op
  }

  @Override
  public void onExpiredApp(boolean notify)
  {
    // no op
  }

    private void requestRouteSync(Activity activity)
    {
        try
        {
            JSONObject payload = new JSONObject();
            payload.put( RouteConstants.RECORD_TYPE, RouteConstants.MESSAGE_TYPE_MFT);
            payload.put( RouteConstants.SUB_TYPE, RouteConstants.SUB_TYPE_ROUTE_PLANNED );

            // populate all local trips
            List<RouteTrip> allTrips = RouteUtil.findPlannedRoutes( activity );
            JSONArray tripsJSON = new JSONArray();
            for (RouteTrip trip : allTrips) {
                JSONObject tripObj = new JSONObject();
                tripObj.put(RouteConstants.ID, trip.getId());
                tripObj.put(RouteConstants.VERSION, trip.getVersion());

                tripsJSON.put(tripObj);
            }
            payload.put(RouteConstants.TRIPS, tripsJSON);

            TranstechUtil.publish( activity, TranstechConstants.AMQP_ROUTING_KEY_ROUTE_TRIP, TranstechConstants.COMMS_EVENT_PRIORITY_NORMAL, payload );
        }
        catch(JSONException ex)
        {

        }
    }
}
