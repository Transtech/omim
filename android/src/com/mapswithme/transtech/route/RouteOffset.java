package com.mapswithme.transtech.route;

import au.net.transtech.geo.model.Position;

/**
 * Class RouteOffset
 * <p/>
 * Created by agough on 27/02/17 4:42 PM
 */
public class RouteOffset
{
    public Position nearestPoint;
    public double distance;
    public int geofenceCount;
    public int index = -1;

    @Override
    public String toString()
    {
        return "RouteOffset{" +
                "distance=" + distance +
                ", geofenceCount=" + geofenceCount +
                ", nearestPoint=" + (nearestPoint == null ? "[null]" : "[" + nearestPoint.getLatitude() + "," + nearestPoint.getLongitude() + "]") +
                '}';
    }
}
