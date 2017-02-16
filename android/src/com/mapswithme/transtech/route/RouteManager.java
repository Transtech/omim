package com.mapswithme.transtech.route;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

/**
 * Class RouteManager
 * <p/>
 * Created by agough on 8/02/17 1:15 PM
 */
public class RouteManager
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
                null, selection,  selectionArgs, null);
        while (tripCursor.moveToNext())
            trips.add(new RouteTrip(tripCursor));

        tripCursor.close();
        return trips;
    }
}
