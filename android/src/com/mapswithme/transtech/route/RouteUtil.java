package com.mapswithme.transtech.route;

import android.content.Context;
import android.database.Cursor;
import au.net.transtech.geo.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Class RouteManager
 * <p/>
 * Created by agough on 8/02/17 1:15 PM
 */
public class RouteUtil
{
    public static List<RouteTrip> findSelectedRoutes(Context ctx)
    {
        return findByCriteria(ctx,
                RouteTrip.TYPE + " = ? AND " + RouteTrip.SELECTED + " <> ?",
                new String[] { "ROUTE_PLANNED", "0" });
    }

    public static List<RouteTrip> findPlannedRoutes(Context ctx)
    {
        return findByCriteria(ctx, RouteTrip.TYPE + " = ?", new String[] { "ROUTE_PLANNED" });
    }

    public static RouteTrip findById(Context ctx, long id)
    {
        List<RouteTrip> trips = findByCriteria(ctx,
                RouteTrip.TYPE + " = ? AND " + RouteTrip.ID + " = ?",
                new String[] { "ROUTE_PLANNED", String.valueOf( id ) });
        if( trips.size() > 0 )
            return trips.get( 0 );

        return null;
    }

    public static List<RouteLeg> findLegsByTripId(Context ctx, long tripId)
    {
        List<RouteLeg> legs = new ArrayList<RouteLeg>();
        Cursor tripCursor = ctx.getContentResolver().query(RouteConstants.LEGS_CONTENT_URI, null,
                RouteLeg.TRIP_ID + " = ?", new String[] { String.valueOf( tripId ) }, RouteLeg.SEQ);
        while (tripCursor.moveToNext())
            legs.add(new RouteLeg(tripCursor));

        tripCursor.close();
        return legs;
    }

    private static List<RouteTrip> findByCriteria( Context ctx, String selection, String[] selectionArgs )
    {
        List<RouteTrip> trips = new ArrayList<RouteTrip>();
        Cursor tripCursor = ctx.getContentResolver().query(RouteConstants.TRIPS_CONTENT_URI,
                null, selection, selectionArgs, null);
        while (tripCursor.moveToNext())
            trips.add(new RouteTrip(tripCursor));

        tripCursor.close();
        return trips;
    }

    public static List<RouteGeofence> findTripGeofences( Context ctx, long tripId )
    {
        List<RouteGeofence> geofences = new ArrayList<RouteGeofence>();

        Cursor geofenceCursor = ctx.getContentResolver().query(RouteGeofence.getAllTripGeofencesUri(tripId),
                null, null, null, null);
        while (geofenceCursor.moveToNext())
            geofences.add(new RouteGeofence(geofenceCursor));

        geofenceCursor.close();
        return geofences;
    }

    public static RouteOffset distanceFromPath( double fromLat, double fromLon, List<Position> path )
    {
        RouteOffset info = new RouteOffset();
        if( path == null || path.size() == 0 )
            return info;

        double minDist = Double.MAX_VALUE;
        Position p1 = null;
        int i = 0;
        for( Position p : path )
        {
            if( p1 != null )
            {
                double dx = p.getLatitude() - p1.getLatitude();
                double dy = p.getLongitude() - p1.getLongitude();
                double d = ((dx * (fromLat - p1.getLatitude())) + (dy * (fromLon - p1.getLongitude()))) / ((dx * dx) + (dy * dy));
                double newLat = p1.getLatitude() + (dx * d);
                double newLng = p1.getLongitude() + (dy * d);
                double d1 = RouteUtil.haversineDistance( fromLat, fromLon, newLat, newLng );
                if( !Double.isNaN( d ) &&
                        newLat < p.getLatitude() &&
                        newLat > p1.getLatitude() &&
                        newLng < p.getLongitude() &&
                        newLng > p1.getLongitude() )
                {
                    if( Math.abs( d1 ) < minDist )
                    {
                        minDist = Math.abs( d1 );
                        info.nearestPoint = p1;
                        info.nextNearestPoint = p;
                    }
                }
            }
            p1 = p;
            i++;
        }
        info.distance = (minDist == Double.MAX_VALUE || minDist == Double.NaN ? 0.0 : minDist);
        return info;
    }



    /**
     * Calculate the distance between 2 lat/lon pairs using the haversine equation
     * <p/>
     * http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param sLat
     * @param sLon
     * @param fLat
     * @param fLon
     * @return distance in meters between the 2 points
     */
    private static final double R = 6371e3;

    public static double haversineDistance( double lat1, double lon1, double lat2, double lon2 )
    {
        double r1 = toRad( lat1 );
        double r2 = toRad( lat2 );

        double lat3 = toRad( lat2 - lat1 );
        double lon3 = toRad( lon2 - lon1 );

        double a = Math.sin( lat3 / 2 ) * Math.sin( lat3 / 2 ) +
                Math.cos( r1 ) * Math.cos( r2 ) *
                        Math.sin( lon3 / 2 ) * Math.sin( lon3 / 2 );
        double c = 2 * Math.atan2( Math.sqrt( a ), Math.sqrt( 1 - a ) );

        return R * c;
    }

    private static double toRad( double deg )
    {
        return deg * Math.PI / 180.0;
    }

    private static double toDeg( double rad )
    {
        return rad * 180.0 / Math.PI;
    }

}
