package com.mapswithme.maps.routing;

/**
 * Class IRouter
 * <p/>
 * Created by agough on 21/03/16 3:21 PM
 */
public interface IRouter
{
    String getName();
    void clearState();
    int calculateRoute(double startLat, double startLon, double finishLat, double finishLon);
}
