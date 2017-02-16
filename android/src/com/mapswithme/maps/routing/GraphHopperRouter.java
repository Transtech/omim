package com.mapswithme.maps.routing;

import android.content.Context;
import android.util.Log;
import au.net.transtech.geo.GeoEngine;
import au.net.transtech.geo.model.*;
import com.graphhopper.util.PMap;
import com.mapswithme.maps.Framework;
import com.mapswithme.transtech.Setting;
import com.mapswithme.transtech.SettingConstants;
import com.mapswithme.transtech.TranstechUtil;
import com.mapswithme.transtech.route.RouteLeg;
import com.mapswithme.transtech.route.RouteManager;
import com.mapswithme.transtech.route.RouteTrip;

import java.util.ArrayList;
import java.util.List;

/**
 * Class GraphHopperRouter
 * <p/>
 * Created by agough on 30/03/16 9:51 AM
 */
public class GraphHopperRouter implements IRouter
{
    private static final String TAG = "SmartNav2_GraphHopper";
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
    private VehicleProfile selectedProfile;
    private Integer plannedRoute = null;
    private RouteTrip complianceTrip = null;

    public static final double TRUCK_MULTIPLIER = 1.4;  //Increase estimated times for TRUCK routing
    public static final double CAR_MULTIPLIER = 1.0;  //Increase estimated times for CAR routing

    public GraphHopperRouter( Context context )
    {
        this.context = context;
        this.routerType = Framework.ROUTER_TYPE_EXTERNAL;
    }

    @Override
    public String getName()
    {
        Log.i( TAG, "getName() returning 'GraphHopper'");
        return "GraphHopper";
    }

    @Override
    public void clearState()
    {
        //Do nothing
        Log.i( TAG, "clearState() called");
    }

    public int getRouterType()
    {
        return routerType;
    }
    public RouteTrip getComplianceTrip() { return complianceTrip; }

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
        ComplianceController.get().setCurrentRouterType( routerType );

        Log.i( TAG, "Received routing request for GH network '" + routerType + "'");
        if( plannedRoute != null && plannedRoute.intValue() > 0 )
        {
            Log.i( TAG, "Returning pre-planned route ID '" + plannedRoute + "'");
            return toMwmRoute( null, constructPreplannedRoute( plannedRoute.intValue() ), null );
        }
        else if( routerType != Framework.ROUTER_TYPE_EXTERNAL )
            return routeByCar(startLat, startLon, finishLat, finishLon);

        return routeOnSelectedProfile( startLat, startLon, finishLat, finishLon );
    }

    private Route routeOnSelectedProfile(double startLat, double startLon, double finishLat, double finishLon)
    {
        VehicleProfile currentProfile = getSelectedProfile();
        if( currentProfile == null )
        {
            Log.e( TAG, "Vehicle profile is NULL? Cannot route" );
            return null;
        }
        Log.i( TAG, "Received routing request starting from (" + startLat + ", " + startLon + ") to ("
                + finishLat + ", " + finishLon + ") - using configured network profile '" + currentProfile.getCode() + "'" );

        try
        {
            List<Position> req = new ArrayList<Position>();
            req.add( new Position( startLat, startLon ) );
            req.add( new Position( finishLat, finishLon ) );

            PMap currParams = new PMap();
            currParams.put( GeoEngine.PARAM_TRUCK_TYPE, currentProfile.getCode() );

            //if we are not close to the start of the route, we route by car
            return supplementJourneyIfRequired(
                    getGeoEngine().route( req, currParams ), startLat, startLon, finishLat, finishLon );
        }
        catch( Exception e )
        {
            Log.e( TAG, "Routing request from (" + startLat + ", " + startLon + ") to (" + finishLat + ", " + finishLon + ") FAILED", e );
        }
        return null;
    }

    private Route supplementJourneyIfRequired( MultiPointRoute ghRoute, double startLat, double startLon, double finishLat, double finishLon )
    {
        try
        {
            MultiPointRoute ghStartCar = null, ghFinishCar = null;

            PMap carParams = new PMap();
            carParams.put( GeoEngine.PARAM_TRUCK_TYPE, NETWORK_CAR );

            if( ghRoute != null && ghRoute.getPath() != null && ghRoute.getPath().size() > 0 )
            {
                //do we need to supplement the route from current position to the start of the GH route
                //on the specified road network with additional routing purely on the car network?
                Position startPos = ghRoute.getPath().get( 0 );
                double dist = TranstechUtil.haversineDistance( startLat, startLon, startPos.getLatitude(), startPos.getLongitude() );
                Log.i( TAG, "Start position (" + startLat + "," + startLon + ") is "
                        + dist + " meters from route - "
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
                    dist = TranstechUtil.haversineDistance( finishLat, finishLon, finishPos.getLatitude(), finishPos.getLongitude() );
                    Log.i( TAG, "Finish position (" + finishLat + "," + finishLon + ") is "
                            + dist + " meters from route - "
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

    private MultiPointRoute constructPreplannedRoute( int routeId )
    {
        RouteTrip trip = RouteManager.findById( context, routeId );
        if( trip == null )
        {
            Log.i( TAG, "Failed to locate route ID " + routeId + " from RouteManager - context " + context );
            return null;
        }

        List<RouteLeg> legs = RouteManager.findLegsByTripId( context, routeId );
        Log.i( TAG, "Located " + legs.size() + " legs on route ID " + routeId );

        MultiPointRoute route = new MultiPointRoute();
        if( route.getPath() == null )
            route.setPath( new ArrayList<Position>() );

        if( route.getLegs() == null )
            route.setLegs( new ArrayList<Leg>() );

        for( RouteLeg leg : legs )
        {
            Leg leg1 = new Leg();
            leg1.setSegments( leg.getRouteSegments() );
            if( leg1.getPath() == null )
                leg1.setPath( new ArrayList<Position>() );

            Log.i( TAG, "Processing leg " + leg.getId() + " on route ID " + routeId + " with id " + leg.getId() + " seq " + leg.getSeq() + " and " + leg.getPath().size() + " positions" );
            route.getLegs().add( leg1 );
            route.getPath().addAll( leg.getPath() );
        }
        Log.i( TAG, "Preplanned route " + routeId + " has " + route.getLegs().size() + " legs and " + route.getPath().size() + " positions" );
        complianceTrip = trip;
        complianceTrip.setPath( route.getPath() );
        return route;
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
        double MULTIPLIER = (routerType == Framework.ROUTER_TYPE_EXTERNAL ? TRUCK_MULTIPLIER : CAR_MULTIPLIER);

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

        Log.i( TAG, "Number of segments " + segLen );
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
        Log.i(TAG, "Add segment " + pos + " to MWM route: segment name = " + seg.getName() );
        int index = findIndex( result.path, seg.getPath() != null && seg.getPath().size() > 0 ? seg.getPath().get( 0 ) : seg.getStart() );

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
        VehicleProfile selVp = null;
        for( VehicleProfile vp : getGeoEngine().getVehicleProfiles() )
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

    public VehicleProfile getSelectedProfile()
    {
        if( selectedProfile == null )
        {
            String network = Setting.getString( context,
                    Setting.currentEnvironment( context ),
                    Setting.Scope.SMARTNAV2,
                    SettingConstants.ROUTE_NETWORK,
                    NETWORK_CAR );

            for( VehicleProfile vp : getGeoEngine().getVehicleProfiles() )
                if( vp.getCode().equals( network ) )
                    selectedProfile = vp;
        }
        return selectedProfile;
    }

    public double distanceFromNetwork(double lat, double lon)
    {
        VehicleProfile current = getSelectedProfile();
        if( current == null )
            return -0.1;

        List<Position> pos = new ArrayList<Position>();
        pos.add( new Position( lat, lon ));

        PMap params = new PMap();
        params.put( GeoEngine.PARAM_ENCODER, current.getCode() );

        List<SnappedPosition> result = getGeoEngine().findClosestPoints( pos, params );
        if( result == null || result.size() == 0 )
            return -0.2;

        Position posFromSeg = result.get(0).getSnapped();
        return TranstechUtil.haversineDistance( lat, lon, posFromSeg.getLatitude(), posFromSeg.getLongitude() );
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

    public void setPlannedRouteId( Integer routeId )
    {
        plannedRoute = routeId;
    }
}
