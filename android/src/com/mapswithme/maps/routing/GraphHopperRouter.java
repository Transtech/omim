package com.mapswithme.maps.routing;

import android.content.Context;
import android.util.Log;
import au.net.transtech.geo.GeoEngine;
import au.net.transtech.geo.model.*;
import com.graphhopper.util.PMap;
import com.mapswithme.maps.Framework;
import com.mapswithme.transtech.Setting;
import com.mapswithme.transtech.SettingConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Class GraphHopperRouter
 * <p/>
 * Created by agough on 30/03/16 9:51 AM
 */
public class GraphHopperRouter implements IRouter
{
    private static final String TAG = "Maps_GraphHopperRouter";
    private static final String GRAPHHOPPER_PATH = "/sdcard/MapsWithMe/heavy-vehicle-gh";

    private static final int LEAVE_ROUNDABOUT = -6;
    private static final int TURN_SHARP_LEFT = -3;
    private static final int TURN_LEFT = -2;
    private static final int TURN_SLIGHT_LEFT = -1;
    private static final int CONTINUE_ON_STREET = 0;
    private static final int TURN_SLIGHT_RIGHT = 1;
    private static final int TURN_RIGHT = 2;
    private static final int TURN_SHARP_RIGHT = 3;
    private static final int FINISH = 4;
    private static final int REACHED_VIA = 5;
    private static final int USE_ROUNDABOUT = 6;

    public static final String NETWORK_CAR = "car";

    private final Context context;
    private final int routerType;
    private GeoEngine geoEngine;
    private EncoderProfile selectedProfile;

    public static final double TRUCK_MULTIPLIER = 1.4;  //Increase estimated times for TRUCK routing
    public static final double CAR_MULTIPLIER = 1.0;  //Increase estimated times for CAR routing

    public GraphHopperRouter( Context context, int routerType )
    {
        this.context = context;
        this.routerType = routerType;
    }

    @Override
    public String getName()
    {
        return "GraphHopper";
    }

    @Override
    public void clearState()
    {
        //Do nothing
    }

    public int getRouterType()
    {
        return routerType;
    }

    public GeoEngine getGeoEngine()
    {
        if( geoEngine == null )
        {
            geoEngine = new GeoEngine( GRAPHHOPPER_PATH );
            geoEngine.loadGraph();
        }
        return geoEngine;
    }


    @Override
    public Route calculateRoute( double startLat, double startLon, double finishLat, double finishLon )
    {
        Log.i( TAG, "Received routing request for GH network '" + routerType + "'");
        ComplianceController.get().setCurrentRouterType( routerType );
        if( routerType != Framework.ROUTER_TYPE_TRUCK )
            return routeByCar(startLat, startLon, finishLat, finishLon);

        return routeOnSelectedProfile( startLat, startLon, finishLat, finishLon );
    }

    private Route routeOnSelectedProfile(double startLat, double startLon, double finishLat, double finishLon)
    {
        EncoderProfile currentProfile = getSelectedProfile();
        if( currentProfile == null )
        {
            Log.e( TAG, "Vehicle profile is NULL? Cannot route" );
            return null;
        }
        Log.i( TAG, "Received routing request starting from (" + startLat + ", " + startLon + ") to ("
                + finishLat + ", " + finishLon + ") - using configured network profile '" + currentProfile.getCode() + "'");

        try
        {
            List<Position> req = new ArrayList<Position>();
            req.add( new Position( startLat, startLon ) );
            req.add( new Position( finishLat, finishLon ) );

            MultiPointRoute ghStartCar = null, ghFinishCar = null;

            PMap carParams = new PMap();
            carParams.put( GeoEngine.PARAM_TRUCK_TYPE, NETWORK_CAR );

            PMap currParams = new PMap();
            currParams.put( GeoEngine.PARAM_TRUCK_TYPE, currentProfile.getCode() );

            MultiPointRoute ghRoute = getGeoEngine().route( req, currParams );

            if( ghRoute != null && ghRoute.getPath() != null && ghRoute.getPath().size() > 0 )
            {
                //do we need to supplement the route from current position to the start of the GH route
                //on the specified road network with additional routing purely on the car network?
                Position startPos = ghRoute.getPath().get( 0 );
                double dist = haversineDistance( startLat, startLon, startPos.getLatitude(), startPos.getLongitude() );
                Log.i( TAG, "Start position (" + startLat + "," + startLon + ") is "
                        + dist + " meters from returned '" + currentProfile.getCode() + "' route - "
                        + (dist > ComplianceController.OFFROUTE_THRESHOLD ? " routing by CAR to start" : "looks good") );
                if( dist > ComplianceController.OFFROUTE_THRESHOLD )
                {
                    //yes we do
                    List<Position> req2 = new ArrayList<Position>();
                    req2.add( new Position( startLat, startLon ) );
                    req2.add( startPos );
                    ghStartCar = getGeoEngine().route( req2, carParams );
                }

                if( ghRoute.getPath().size() > 1 )
                {
                    //check the end position also
                    Position finishPos = ghRoute.getPath().get( ghRoute.getPath().size() - 1 );
                    dist = haversineDistance( finishLat, finishLon, finishPos.getLatitude(), finishPos.getLongitude() );
                    Log.i( TAG, "Finish position (" + finishLat + "," + finishLon + ") is "
                            + dist + " meters from returned '" + currentProfile.getCode() + "' route - "
                            + (dist > ComplianceController.OFFROUTE_THRESHOLD ? " routing by CAR to finish" : "looks good") );
                    if( dist > ComplianceController.OFFROUTE_THRESHOLD )
                    {
                        List<Position> req2 = new ArrayList<Position>();
                        req2.add( new Position( finishLat, finishLon ) );
                        req2.add( finishPos );
                        ghFinishCar = getGeoEngine().route( req2, carParams );
                    }
                }
            }

            Route route = toMwmRoute( ghStartCar, ghRoute, ghFinishCar );

            Log.i( TAG, "Responding with GH route (" + route.path.length + " path items) and (" + route.turns.length + " turns)" );
            return route;
        }
        catch( Exception e )
        {
            Log.e( TAG, "Routing request from (" + startLat + ", " + startLon + ") to (" + finishLat + ", " + finishLon + ") FAILED", e );
        }
        return null;
    }

    private Route routeByCar(double startLat, double startLon, double finishLat, double finishLon)
    {
        try
        {
            List<Position> req = new ArrayList<Position>();
            req.add( new Position( startLat, startLon ) );
            req.add( new Position( finishLat, finishLon ) );

            PMap params = new PMap();
            params.put( GeoEngine.PARAM_TRUCK_TYPE, NETWORK_CAR );
            MultiPointRoute ghRoute = getGeoEngine().route( req, params );

            Route route = toMwmRoute( null, ghRoute, null );

            Log.i( TAG, "Responding with GH route (" + route.path.length + " path items) and (" + route.turns.length + " turns)" );
            return route;
        }
        catch( Exception e )
        {
            Log.e( TAG, "Routing request from (" + startLat + ", " + startLon + ") to (" + finishLat + ", " + finishLon + ") FAILED", e );
        }
        return null;
    }

    private Route toMwmRoute( MultiPointRoute ghStartCar, MultiPointRoute ghRoute, MultiPointRoute ghFinishCar )
    {
        Route result = new Route();

        int pathLen = ghRoute.getPath().size()
                + (ghStartCar == null ? 0 : ghStartCar.getPath().size() - 1)
                + (ghFinishCar == null ? 0 : ghFinishCar.getPath().size() - 1);

        Log.i( TAG, "Starting route path length " + (ghStartCar == null ? 0 : ghStartCar.getPath().size())
                + ", final route path length " + (ghFinishCar == null ? 0 : ghFinishCar.getPath().size())
                + ", core path length " + ghRoute.getPath().size()
                + ", total length " + pathLen );

        result.path = new Route.Position[ pathLen ];
        int i = 0;
        double MULTIPLIER = (routerType == Framework.ROUTER_TYPE_TRUCK ? TRUCK_MULTIPLIER : CAR_MULTIPLIER);

        // Add start if it exists
        if( ghStartCar != null )
        {
            for( Position pos : ghStartCar.getPath() )
                result.path[ i++ ] = new Route.Position( pos.getLatitude(), pos.getLongitude() );

            //remove finish location...
            if( result.path.length > 1 )
                i--;
        }

        // Add main route
        for( Position pos : ghRoute.getPath() )
            result.path[ i++ ] = new Route.Position( pos.getLatitude(), pos.getLongitude() );

        // Add finish if it exists
        if( ghFinishCar != null )
        {
            //remove finish location...
            if( result.path.length > 1 )
                i--;

            for( Position pos : ghFinishCar.getPath() )
                result.path[ i++ ] = new Route.Position( pos.getLatitude(), pos.getLongitude() );
        }

        int segLen = 0;
        if( ghStartCar != null )
        {
            for( int j = 0; j < ghStartCar.getLegs().size() - 1; j++ )
                segLen += ghStartCar.getLegs().get( j ).getSegments().size();
        }

        for( Leg leg : ghRoute.getLegs() )
            segLen += leg.getSegments().size();

        if( ghFinishCar != null )
        {
            for( int j = 1; j < ghFinishCar.getLegs().size(); j++ )
                segLen += ghFinishCar.getLegs().get( j ).getSegments().size();
        }

        result.turns = new Route.TurnItem[ segLen ];
        result.times = new Route.TimeItem[ segLen ];
        result.streets = new Route.StreetItem[ segLen ];

        i = 0;
        Long timeTotal = 0L;
        if( ghStartCar != null )
        {
            for( int j = 0; j < ghStartCar.getLegs().size() - 1; j++ )
            {
                Leg leg = ghStartCar.getLegs().get( j );
                for( Segment seg : leg.getSegments() )
                {
                    addSegment( result, i, timeTotal, seg );
                    timeTotal += new Double( seg.getTime() * MULTIPLIER ).longValue();
                    i++;
                }
            }
        }

        for( Leg leg : ghRoute.getLegs() )
        {
            for( Segment seg : leg.getSegments() )
            {
                addSegment( result, i, timeTotal, seg );
                timeTotal += new Double( seg.getTime() * MULTIPLIER ).longValue();
                i++;
            }
        }

        if( ghFinishCar != null )
        {
            for( int j = 1; j < ghFinishCar.getLegs().size(); j++ )
            {
                Leg leg = ghFinishCar.getLegs().get( j );
                for( Segment seg : leg.getSegments() )
                {
                    addSegment( result, i, timeTotal, seg );
                    timeTotal += new Double( seg.getTime() * MULTIPLIER ).longValue();
                    i++;
                }
            }
        }

        dumpRoute(result);
        return result;
    }

    private void addSegment( Route result, int pos, Long timeTotal, Segment seg )
    {
        int index = findIndex( result.path, seg.getPath().get( 0 ) );

        result.turns[ pos ] = new Route.TurnItem();
        result.turns[ pos ].index = index;
        result.turns[ pos ].direction = toMwmDirection( seg.getDirection() ).ordinal();
        result.turns[ pos ].sourceName = seg.getInstruction();
        result.turns[ pos ].targetName = seg.getInstruction();

        result.times[ pos ] = new Route.TimeItem();
        result.times[ pos ].index = index;
        result.times[ pos ].time = timeTotal;

        result.streets[ pos ] = new Route.StreetItem();
        result.streets[ pos ].index = index;
        result.streets[ pos ].name = seg.getName();
    }

    private RoutingInfo.VehicleTurnDirection toMwmDirection( Segment.Direction dir )
    {
        switch( dir )
        {
            case USE_ROUNDABOUT:
                return RoutingInfo.VehicleTurnDirection.ENTER_ROUND_ABOUT;
            case LEAVE_ROUNDABOUT:
                return RoutingInfo.VehicleTurnDirection.LEAVE_ROUND_ABOUT;
            case TURN_SHARP_LEFT:
                return RoutingInfo.VehicleTurnDirection.TURN_SHARP_LEFT;
            case TURN_LEFT:
                return RoutingInfo.VehicleTurnDirection.TURN_LEFT;
            case TURN_SLIGHT_LEFT:
                return RoutingInfo.VehicleTurnDirection.TURN_SLIGHT_LEFT;
            case CONTINUE_ON_STREET:
                return RoutingInfo.VehicleTurnDirection.GO_STRAIGHT;
            case TURN_SLIGHT_RIGHT:
                return RoutingInfo.VehicleTurnDirection.TURN_SLIGHT_RIGHT;
            case TURN_RIGHT:
                return RoutingInfo.VehicleTurnDirection.TURN_RIGHT;
            case TURN_SHARP_RIGHT:
                return RoutingInfo.VehicleTurnDirection.TURN_SHARP_RIGHT;
            case FINISH:
                return RoutingInfo.VehicleTurnDirection.REACHED_YOUR_DESTINATION;
            case REACHED_VIA:
                return RoutingInfo.VehicleTurnDirection.START_AT_THE_END_OF_STREET;
        }
        return RoutingInfo.VehicleTurnDirection.GO_STRAIGHT;
    }

    private static int findIndex( Route.Position[] path, Position point )
    {
        int i = 0;
        for( Route.Position p : path )
        {
            if( Math.abs( p.lat - point.getLatitude() ) < 0.000001 &&
                    Math.abs( p.lng - point.getLongitude() ) < 0.000001 )
                return i;
            i++;
        }
        return -1; //not found
    }

    public boolean setSelectedProfile(String profile)
    {
        EncoderProfile selVp = null;
        for( EncoderProfile vp : getGeoEngine().getEncoderProfiles() )
            if( vp.getCode().equals( profile ) )
                selVp = vp;

        if (selVp == null)
            return false;

        Setting.setString( context,
                Setting.currentEnvironment( context ),
                Setting.Scope.SMARTNAV2,
                SettingConstants.ROUTE_NETWORK,
                profile,
                Setting.Origin.LOCAL,
                Setting.Source.USER.name() );

        selectedProfile = selVp;
        return true;
    }

    public EncoderProfile getSelectedProfile()
    {
        if( selectedProfile == null )
        {
            String network = Setting.getString( context,
                    Setting.currentEnvironment( context ),
                    Setting.Scope.SMARTNAV2,
                    SettingConstants.ROUTE_NETWORK,
                    NETWORK_CAR );

            for( EncoderProfile vp : getGeoEngine().getEncoderProfiles() )
                if( vp.getCode().equals( network ) )
                    selectedProfile = vp;
        }
        return selectedProfile;
    }

    public double distanceFromNetwork(double lat, double lon)
    {
        EncoderProfile current = getSelectedProfile();
        if( current == null )
            return -0.1;

        List<Position> pos = new ArrayList<Position>();
        pos.add( new Position( lat, lon ));
        List<SnappedPosition> result = getGeoEngine().findClosestPoints( pos, current.getCode() );
        if( result == null || result.size() == 0 )
            return -0.2;

        Position posFromSeg = result.get(0).getSnapped();
        return haversineDistance( lat, lon, posFromSeg.getLatitude(), posFromSeg.getLongitude() );
    }

    private void dumpRoute(Route route)
    {
        int i = 0;
        for( Route.Position pos : route.path )
            Log.d(TAG, "Path[" + (i++) + "] - " + pos.lat + "," + pos.lng);

        i = 0;
        for(Route.TimeItem t : route.times)
            Log.d(TAG, "Time[" + (i++) + "] - i:" + t.index + ",t:" + t.time );

        i = 0;
        for( Route.TurnItem turn : route.turns)
            Log.d(TAG, "Turn[" + (i++) + "] - i:" + turn.index + ",d:" + turn.direction + ",s:" + turn.sourceName + ",t:" + turn.targetName );

        i = 0;
        for(Route.StreetItem st : route.streets)
            Log.d(TAG, "Street[" + (i++) + "] - i:" + st.index + ",n:" + st.name );
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

    public double haversineDistance( double lat1, double lon1, double lat2, double lon2 )
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

    private double toRad( double deg )
    {
        return deg * Math.PI / 180.0;
    }

    private double toDeg( double rad )
    {
        return rad * 180.0 / Math.PI;
    }
}
