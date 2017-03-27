package com.mapswithme.transtech.route;

import au.net.transtech.geo.model.Position;
import org.json.JSONException;
import org.json.JSONObject;

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

    public JSONObject toJSON()
    {
        JSONObject info = new JSONObject();
        try
        {
            info.put( "Distance", distance );
            info.put( "Geofences", geofenceCount );
            info.put( "Index", index );
            if( nearestPoint != null )
            {
                JSONObject np = new JSONObject();
                np.put( "Lat", nearestPoint.getLatitude() );
                np.put( "Lng", nearestPoint.getLongitude() );
                info.put( "NearestPoint", np );
            }
            else
                info.put( "NearestPoint", JSONObject.NULL );
        }
        catch( JSONException e ) {}

        return info;
    }
}
