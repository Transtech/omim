package com.mapswithme.transtech.route;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import au.net.transtech.geo.model.Leg;
import au.net.transtech.geo.model.MultiPointRoute;
import au.net.transtech.geo.model.Position;
import au.net.transtech.geo.model.Segment;
import net.sf.geographiclib.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Class RouteManager
 * <p/>
 * Created by agough on 8/02/17 1:15 PM
 */
public class RouteUtil
{
    private static final String TAG = "RouteUtil";

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
        return distanceFromPath( fromLat, fromLon, path, false );
    }

    public static RouteOffset distanceFromPath( double fromLat, double fromLon, List<Position> path, boolean debug )
    {
        RouteOffset info = new RouteOffset();
        if( path == null || path.size() == 0 )
            return info;

        Geography g = new Geography();

        double minDist = Double.MAX_VALUE;
        Position currPos = new Position( fromLat, fromLon );
        Position p1 = null;
        int i = 0;
        for( Position p : path )
        {
            if( p1 != null )
            {
                double f1 = g.intercept( p1, p, currPos );
                f1 = (f1 > 1.0) ? 1.0 : (f1 < 0.0) ? 0.0 : f1;

                Position p2 = g.interpolate( p1, p, f1 );
                double d1 = g.distance( currPos, p2 );
                if( debug )
                    Log.i( TAG, "REROUTE:" + i
                            + ": currPos[" + currPos.getLatitude() + "," + currPos.getLongitude()
                            + "], p1[" + p1.getLatitude() + "," + p1.getLongitude()
                            + "], p[" + p.getLatitude() + "," + p.getLongitude()
                            + "], p2[" + p2.getLatitude() + "," + p2.getLongitude()
                            + "], d1 " + d1 + ", f1 " + f1 );
                if( !Double.isNaN( d1 ) )
                {
                    if( d1 < minDist )
                    {
                        minDist = d1;
                        info.nearestPoint = p2;
                        info.index = i;
                    }
                }
            }
            p1 = p;
            i++;
        }
        info.distance = (minDist == Double.MAX_VALUE || minDist == Double.NaN ? 0.0 : minDist);
        return info;
    }

    public static MultiPointRoute clone( MultiPointRoute mpr )
    {
        if( mpr == null )
            return null;

        MultiPointRoute route = new MultiPointRoute();
        route.setDistance( mpr.getDistance() );
        route.setTime( mpr.getTime() );
        if( mpr.getStops() != null )
        {
            route.setStops( new ArrayList<Position>(mpr.getStops().size()));
            route.getStops().addAll( mpr.getStops() );
        }
        if( mpr.getLegs() != null )
        {
            ArrayList<Leg> newLegs = new ArrayList<Leg>(mpr.getLegs().size());
            for( Leg l : mpr.getLegs() )
            {
                Leg newLeg = new Leg();
                newLeg.setDistance( l.getDistance() );
                newLeg.setTime( l.getTime() );
                newLeg.setStart( l.getStart() );
                newLeg.setEnd( l.getEnd() );
                if( l.getPath() != null )
                {
                    newLeg.setPath( new ArrayList<Position>(l.getPath().size()) );
                    newLeg.getPath().addAll( l.getPath() );
                }

                if( l.getSegments() != null )
                {
                    newLeg.setSegments( new ArrayList<Segment>( l.getSegments().size() ) );
                    for( Segment s : l.getSegments() )
                    {
                        Segment newSeg = new Segment();
                        newSeg.setDistance( s.getDistance() );
                        newSeg.setTime( s.getTime() );
                        newSeg.setStart( s.getStart() );
                        newSeg.setEnd( s.getEnd() );
                        newSeg.setDirection( s.getDirection() );
                        newSeg.setInstruction( s.getInstruction() );
                        newSeg.setName( s.getName() );
                        if( s.getPath() != null )
                        {
                            newSeg.setPath( new ArrayList<Position>( s.getPath().size() ) );
                            newSeg.getPath().addAll( s.getPath() );
                        }
                        newLeg.getSegments().add( newSeg );
                    }
                }
            }
            route.setLegs( newLegs );
        }
        if( mpr.getPath() != null )
        {
            route.setPath( new ArrayList<Position>( mpr.getPath().size() ) );
            route.getPath().addAll( mpr.getPath() );
        }
        return route;
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

    public static class Geography
    {
        public double distance(Position a, Position b) {
            return Geodesic.WGS84.Inverse(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude()).s12;
        }

        public double intercept(Position a, Position b, Position c) {

            if (a.getLongitude() == b.getLongitude() && a.getLatitude() == b.getLatitude()) {
                return 0;
            }
            GeodesicData ci =
                    intercept( a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude(), c.getLatitude(), c.getLongitude() );
            GeodesicData ai = Geodesic.WGS84.Inverse(a.getLatitude(), a.getLongitude(), ci.lat2, ci.lon2);
            GeodesicData ab = Geodesic.WGS84.Inverse(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());

            return (Math.abs(ai.azi1 - ab.azi1) < 1) ? ai.s12 / ab.s12 : (-1) * ai.s12 / ab.s12;
        }

        public Position interpolate(Position a, Position b, double f) {
            GeodesicData inv = Geodesic.WGS84.Inverse(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
            GeodesicData pos = Geodesic.WGS84.Line(inv.lat1, inv.lon1, inv.azi1).Position(inv.s12 * f);

            return new Position(pos.lat2, pos.lon2);
        }

        public double azimuth(Position a, Position b, double f) {
            double azi = 0;
            if (f < 0 + 1E-10) {
                azi = Geodesic.WGS84.Inverse(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude()).azi1;
            } else if (f > 1 - 1E-10) {
                azi = Geodesic.WGS84.Inverse(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude()).azi2;
            } else {
                Position c = interpolate(a, b, f);
                azi = Geodesic.WGS84.Inverse(a.getLatitude(), a.getLongitude(), c.getLatitude(), c.getLongitude()).azi2;
            }
            return azi < 0 ? azi + 360 : azi;
        }

        public double length(List<Position> p) {
            double d = 0;

            for (int i = 1; i < p.size(); ++i) {
                d += distance(p.get( i - 1 ), p.get( i ));
            }

            return d;
        }

        public double intercept(List<Position> p, Position c) {
            double d = Double.MAX_VALUE;
            Position a = p.get( 0 );
            double s = 0, sf = 0, ds = 0;

            for (int i = 1; i < p.size(); ++i) {
                Position b = p.get( i );

                ds = distance(a, b);

                double f_ = intercept(a, b, c);
                f_ = (f_ > 1) ? 1 : (f_ < 0) ? 0 : f_;
                Position x = interpolate(a, b, f_);
                double d_ = distance(c, x);

                if (d_ < d) {
                    sf = (f_ * ds) + s;
                    d = d_;
                }

                s = s + ds;
                a = b;
            }

            return s == 0 ? 0 : sf / s;
        }

        public Position interpolate(List<Position> path, double f) {
            return interpolate(path, length(path), f);
        }

        public Position interpolate(List<Position> p, double l, double f) {
            assert (f >= 0 && f <= 1);

            Position a = p.get(0);
            double d = l * f;
            double s = 0, ds = 0;

            if (f < 0 + 1E-10)
            {
                return p.get(0);
            }

            if (f > 1 - 1E-10)
            {
                return p.get(p.size() - 1);
            }

            for (int i = 1; i < p.size(); ++i) {
                Position b = p.get(i);
                ds = distance(a, b);

                if ((s + ds) >= d) {
                    return interpolate(a, b, (d - s) / ds);
                }

                s = s + ds;
                a = b;
            }

            return null;
        }
/*
        @Override
        public double azimuth(Polyline p, double f) {
            return azimuth(p, length(p), f);
        }

        @Override
        public double azimuth(Polyline p, double l, double f) {
            assert (f >= 0 && f <= 1);

            Position a = p.getPoint(0);
            double d = l * f;
            double s = 0, ds = 0;

            if (f < 0 + 1E-10) {
                return azimuth(p.getPoint(0), p.getPoint(1), 0);
            }

            if (f > 1 - 1E-10) {
                return azimuth(p.getPoint(p.getPointCount() - 2), p.getPoint(p.getPointCount() - 1), f);
            }

            for (int i = 1; i < p.getPointCount(); ++i) {
                Position b = p.getPoint(i);
                ds = distance(a, b);

                if ((s + ds) >= d) {
                    return azimuth(a, b, (d - s) / ds);
                }

                s = s + ds;
                a = b;
            }

            return Double.NaN;
        }
*/
        private final double eps = 0.01 * Math.sqrt(GeoMath.epsilon);
        public GeodesicData intercept(double lata1, double lona1, double lata2, double lona2,
                              double latb1, double lonb1)
        {
            Gnomonic gnom = new Gnomonic( Geodesic.WGS84 );
            if( lata1 == lata2 && lona1 == lona2 )
            {
                return Geodesic.WGS84.Inverse( latb1, lonb1, lata1, lona1 );
            }

            double latb2 = (lata1 + lata2) / 2, latb2_ = Double.NaN, lonb2_ = Double.NaN;
            double lonb2 = ((lona1 >= 0 ? lona1 % 360 : (lona1 % 360) + 360)
                    + (lona2 >= 0 ? lona2 % 360 : (lona2 % 360) + 360)) / 2;
            lonb2 = (lonb2 > 180 ? lonb2 - 360 : lonb2);

            for( int i = 0; i < 10; ++i )
            {
                GnomonicData xa1 = gnom.Forward( latb2, lonb2, lata1, lona1 );
                GnomonicData xa2 = gnom.Forward( latb2, lonb2, lata2, lona2 );
                GnomonicData xb1 = gnom.Forward( latb2, lonb2, latb1, lonb1 );

                Vector va1 = new Vector( xa1.x, xa1.y, 1 );
                Vector va2 = new Vector( xa2.x, xa2.y, 1 );
                Vector la = va1.cross( va2 );
                Vector lb = new Vector( la.y, -(la.x), la.x * xb1.y - la.y * xb1.x );
                Vector p0 = la.cross( lb );
                p0 = p0.multiply( 1d / p0.z );

                latb2_ = latb2;
                lonb2_ = lonb2;

                GnomonicData rev = gnom.Reverse( latb2, lonb2, p0.x, p0.y );
                latb2 = rev.lat;
                lonb2 = rev.lon;

                if( Math.abs( lonb2_ - lonb2 ) < eps && Math.abs( latb2_ - latb2 ) < eps )
                {
                    break;
                }
            }

            return Geodesic.WGS84.Inverse( latb1, lonb1, latb2, lonb2 );
        }
   }
    public static class Vector {
        public double x = Double.NaN;
        public double y = Double.NaN;
        public double z = Double.NaN;

        public Vector(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vector add(Vector other) {
            return new Vector(this.x + other.x, this.y + other.y, this.z + other.z);
        }

        public Vector multiply(double a) {
            return new Vector(this.x * a, this.y * a, this.z * a);
        }

        public Vector cross(Vector other) {
            return new Vector((y * other.z) - (z * other.y), (z * other.x) - (x * other.z),
                    (x * other.y) - (y * other.x));
        }

        public double dot(Vector other) {
            return this.x * other.x + this.y * other.y + this.z * other.z;
        }
    }
}
