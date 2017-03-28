package com.mapswithme.maps.routing;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import au.net.transtech.geo.GeoEngine;
import au.net.transtech.geo.model.*;
import com.graphhopper.util.PMap;
import com.mapswithme.maps.Framework;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.transtech.Setting;
import com.mapswithme.transtech.SettingConstants;
import com.mapswithme.transtech.route.RouteLeg;
import com.mapswithme.transtech.route.RouteOffset;
import com.mapswithme.transtech.route.RouteTrip;
import com.mapswithme.transtech.route.RouteUtil;

import java.io.File;
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
    private static final String GRAPHHOPPER_PATH = "heavy-vehicle-gh";

    public static final String NETWORK_CAR = "car";
    public static final double EPSILON = 0.00001;

    private final Context context;
    private final int routerType;
    private GeoEngine geoEngine;
    private VehicleProfile selectedProfile;
    private RouteListener listener;
    private Integer plannedRouteId = null;
    private MultiPointRoute originalPlannedRoute;

    public static final double TRUCK_MULTIPLIER = 1.4;  //Increase estimated times for TRUCK routing
    public static final double CAR_MULTIPLIER = 1.0;  //Increase estimated times for CAR routing

    public static interface RouteListener
    {
        public static enum Response
        {
            SUCCESS,
            REROUTE_TO_PLANNED_ROUTE,
            REROUTE_TO_DESTINATION
        }

        Response onRouteCalculated( int type, MultiPointRoute route );
    }

    public GraphHopperRouter( Context context )
    {
        this.context = context;
        this.routerType = Framework.ROUTER_TYPE_EXTERNAL;
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
            String path = Framework.nativeGetWritableDir();
            if( !path.endsWith( File.separator ) )
                path = path + File.separator;

            geoEngine = new GeoEngine( path + GRAPHHOPPER_PATH );
            geoEngine.setInMemory( false );
//            Log.i( TAG, "PRE loading Graphopper data");
            geoEngine.loadGraph();
//            Log.i( TAG, "POST loading Graphopper data");
        }
        return geoEngine;
    }

    public void setListener( RouteListener listener )
    {
        this.listener = listener;
    }

    public void removeListener()
    {
        this.listener = null;
    }

    @Override
    public Route calculateRoute( double startLat, double startLon, double finishLat, double finishLon )
    {
        try
        {
            return calculateRouteImpl( startLat, startLon, finishLat, finishLon );
        }
        catch( Exception e )
        {
            Log.e( TAG, "Failed to calculate route", e );
        }
        return null;
    }

    private Route calculateRouteImpl(double startLat, double startLon, double finishLat, double finishLon )
    {
        MultiPointRoute route = null;
        Log.i( TAG, "Received routing request for GH network '" + routerType + "': hash " + hashCode() + ", listener " + listener);
        if( plannedRouteId != null && plannedRouteId.intValue() > 0 )
        {
            Log.i( TAG, "Returning pre-planned route ID '" + plannedRouteId + "'");
            route = constructPreplannedRoute( plannedRouteId.intValue() );
            originalPlannedRoute = route;
        }
        else if( routerType != Framework.ROUTER_TYPE_EXTERNAL )
            route = routeByCar(startLat, startLon, finishLat, finishLon);
        else
            route = routeOnSelectedProfile( startLat, startLon, finishLat, finishLon );

        if( listener != null )
        {
            try
            {
                RouteListener.Response response = listener.onRouteCalculated( routerType, route );
                Log.i( TAG, "RouteListener returned: " + response.name() );

                switch( response )
                {
                    case REROUTE_TO_PLANNED_ROUTE:
                        //the listener does not like our current route and would like it reworked
                        //using the nearest point on the preplanned route, and our current
                        //position as the initial point
                        Log.i( TAG, "REROUTE: Reconstructing pre-planned route ID '" + plannedRouteId + "' to nearest point" );
                        route = reconstructPlannedRoute();
                        break;
                    case REROUTE_TO_DESTINATION:
                        break;
                    case SUCCESS:
                    default:
                        break;
                }
            }
            catch( Exception e )
            {
                Log.e(TAG, "RouteListener failed with exception", e);
            }
        }
        else
            Log.w( TAG, "No RouteListener registered!" );

        //if we are within some threshold of the calculated route start, we replace the first entry with our
        //current location as maps.me will just attempt to re-route if the route isn't our current loc
        Position firstPos = (route.getPath() != null && route.getPath().size() > 0 ? route.getPath().get( 0 ) : null);
        if( firstPos != null && route.getPath().size() > 2 &&
                RouteUtil.haversineDistance( firstPos.getLatitude(), firstPos.getLongitude(), startLat, startLon ) <= ComplianceController.OFFROUTE_THRESHOLD )
        {
            Log.i( TAG, "REROUTE: Replacing first route point with current location" );
            route.getPath().set( 0, new Position( startLat, startLon ) );
        }
        return toMwmRoute( null, route, null );
    }

    private MultiPointRoute routeOnSelectedProfile(double startLat, double startLon, double finishLat, double finishLon)
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

            return getGeoEngine().route( req, currParams );
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
                double dist = RouteUtil.haversineDistance( startLat, startLon, startPos.getLatitude(), startPos.getLongitude() );
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
                    dist = RouteUtil.haversineDistance( finishLat, finishLon, finishPos.getLatitude(), finishPos.getLongitude() );
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

    private MultiPointRoute routeByCar(double startLat, double startLon, double finishLat, double finishLon)
    {
        try
        {
            List<Position> req = new ArrayList<Position>();
            req.add( new Position( startLat, startLon ) );
            req.add( new Position( finishLat, finishLon ) );

            PMap params = new PMap();
            params.put( GeoEngine.PARAM_TRUCK_TYPE, NETWORK_CAR );
            MultiPointRoute ghRoute = getGeoEngine().route( req, params );

            return ghRoute;
        }
        catch( Exception e )
        {
            Log.e( TAG, "Routing request from (" + startLat + ", " + startLon + ") to (" + finishLat + ", " + finishLon + ") FAILED", e );
        }
        return null;
    }

    private MultiPointRoute constructPreplannedRoute( int routeId )
    {
        RouteTrip trip = RouteUtil.findById( context, routeId );
        if( trip == null )
        {
            Log.i( TAG, "Failed to locate route ID " + routeId + " from RouteManager - context " + context );
            return null;
        }

        List<RouteLeg> legs = RouteUtil.findLegsByTripId( context, routeId );
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
        return route;
    }

    private MultiPointRoute reconstructPlannedRoute()
    {
        Location currentLoc = LocationHelper.INSTANCE.getSavedLocation();
        if( currentLoc == null )
        {
            Log.w( TAG, "REROUTE: Failed to retrieve LocationHelper.savedLocation?! - returning original route" );
            return originalPlannedRoute;
        }

        RouteOffset offset = RouteUtil.distanceFromPath( currentLoc.getLatitude(), currentLoc.getLongitude(),
                originalPlannedRoute.getPath() );

        if( offset == null || offset.index < 0 )
        {
            Log.w( TAG, "REROUTE: Failed to determine valid route offset - returning original route" );
            return originalPlannedRoute;
        }

        MultiPointRoute backToPlannedRoute = findBestPathToPlannedRoute( offset, currentLoc );
        if( backToPlannedRoute != null && backToPlannedRoute.getPath() != null && backToPlannedRoute.getPath().size() > 0 )
        {
            //we need to remove the path, legs & segments up to 'newPos' from originalRoute as
            //we've already travelled that part of the path
            MultiPointRoute updatedRoute = new MultiPointRoute();

            Log.i(TAG, "REROUTE: Updating pre-planned route from index " +
                    offset.index + ": new route path size " + backToPlannedRoute.getPath().size()
                    + ", original path size " + originalPlannedRoute.getPath().size()
                    + ", original legs size " + originalPlannedRoute.getLegs().size() );

            List<Position> newPath = new ArrayList<Position>();
            newPath.addAll( backToPlannedRoute.getPath() );

            List<Leg> newLegs = new ArrayList<Leg>();
            newLegs.addAll( backToPlannedRoute.getLegs() );

            //need to remove the last segment from the last leg as that is the 'finish' segment
            Leg lastLeg = newLegs.get( newLegs.size() - 1 );
            lastLeg.getSegments().remove( lastLeg.getSegments().size() - 1 );

            Leg newLeg = new Leg();
            for( Leg l : originalPlannedRoute.getLegs() )
            {
                for( Segment seg : l.getSegments() )
                {
                    int index = findIndex( originalPlannedRoute.getPath(), seg.getPath() != null && seg.getPath().size() > 0 ? seg.getPath().get( 0 ) : seg.getStart() );
                    if( index >= offset.index )
                        newLeg.getSegments().add( seg );
                }
            }
            newLegs.add( newLeg );

            for( int i = offset.index; i < originalPlannedRoute.getPath().size(); ++i )
                newPath.add( originalPlannedRoute.getPath().get( i ) );

            updatedRoute.setPath( newPath );
            updatedRoute.setLegs( newLegs );
            Log.i( TAG, "REROUTE: Updated route path size " + updatedRoute.getPath().size()
                    + ", updated legs size " + updatedRoute.getLegs().size() );
            return updatedRoute;
        }
        else
            Log.w(TAG, "REROUTE: No updated route found - returning original route");

        return originalPlannedRoute;
    }

    private MultiPointRoute findBestPathToPlannedRoute(RouteOffset offset, Location currentLoc)
    {
        int diff = originalPlannedRoute.getPath().size() - offset.index;

        Position newPos = null;
        if( diff > 10 ) //if we're far enough from the finish, do route bisect to determine best/shortest path
        {
            Log.w( TAG, "REROUTE: We are " + diff + " segments from the finish, try bisecting the route" );
            return bisectRoute(offset, currentLoc);
        }
        else if( diff < 5 ) //we're very close to the end, so just route straight to the finish
        {
            Log.w( TAG, "REROUTE: Close enough to the finish, just reroute to the end" );
            newPos = originalPlannedRoute.getPath().get( originalPlannedRoute.getPath().size() - 1 ); //just reroute to the finish
            offset.index = originalPlannedRoute.getPath().size();
        }
        else
        {
            Log.w( TAG, "REROUTE: Rerouting to the found closest point+1" );
            newPos = originalPlannedRoute.getPath().get( offset.index + 1 ); //find the next segment along from the closest point
        }

        if( newPos == null )
        {
            Log.w( TAG, "REROUTE: Failed to determine nearest point - returning original route" );
            return originalPlannedRoute;
        }

        // Try to locate a compliant route back to the nearest point on the original planned route
        Log.i(TAG, "REROUTE: Creating compliant re-route from current["
                + currentLoc.getLatitude() + "," + currentLoc.getLongitude() + "] -> nearest["
                + newPos.getLatitude() + "," + newPos.getLongitude() + "] distance " + offset.distance + " - reconfiguring reconstructed route");
        return routeOnSelectedProfile( currentLoc.getLatitude(), currentLoc.getLongitude(),
                newPos.getLatitude(), newPos.getLongitude() );
    }

    private MultiPointRoute bisectRoute(RouteOffset offset, Location currentLoc)
    {
        int diff = originalPlannedRoute.getPath().size() - offset.index;

        //Find the position 1/3 between closest point and the route end, and route to it
        int r1Idx = offset.index + (diff / 3);
        Position pos = originalPlannedRoute.getPath().get( r1Idx );
        MultiPointRoute r1 = routeOnSelectedProfile( currentLoc.getLatitude(), currentLoc.getLongitude(),
                pos.getLatitude(), pos.getLongitude() );

        //Find the position 2/3 between closest point and the route end, and route to it
        int r2Idx = originalPlannedRoute.getPath().size() - (diff / 3);
        pos = originalPlannedRoute.getPath().get( r2Idx );
        MultiPointRoute r2 = routeOnSelectedProfile( currentLoc.getLatitude(), currentLoc.getLongitude(),
                pos.getLatitude(), pos.getLongitude() );

        Log.i( TAG, "R1 [" + r1.getTime() + "ms, " + r1.getDistance() + "m] - R2 ["
                + r2.getTime() + "ms, " + r2.getDistance() + "m] - returning " + (r1.getTime() < r2.getTime() ? "R1" : "R2") );
        if( r1.getTime() < r2.getTime() )
        {
            offset.index = r1Idx;
            return r1;
        }
        offset.index = r2Idx;
        return r2;
    }

    private boolean isDifferrent(Segment s1, Segment s2)
    {
        if( s1 == null && s2 != null )
            return true;
        if( s2 == null && s1 != null )
            return true;

        return s1.getDirection() != s2.getDirection()
                || !s1.getName().equals( s2.getName() )
                || !s1.getInstruction().equals( s2.getInstruction() );
    }

    private Route toMwmRoute( MultiPointRoute ghStartCar, MultiPointRoute ghRoute, MultiPointRoute ghFinishCar )
    {
        Route result = new Route();

        int pathLen = ghRoute.getPath().size()
                + (ghStartCar == null ? 0 : ghStartCar.getPath().size() - 1)
                + (ghFinishCar == null ? 0 : ghFinishCar.getPath().size() - 1);

//        Log.i( TAG, "Starting route path length " + (ghStartCar == null ? 0 : ghStartCar.getPath().size())
//                + ", final route path length " + (ghFinishCar == null ? 0 : ghFinishCar.getPath().size())
//                + ", core path length " + ghRoute.getPath().size()
//                + ", total length " + pathLen );

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

        Segment lastSeg = null, finishSeg = null;
        List<Segment> segments = new ArrayList<Segment>();
        if( ghStartCar != null )
        {
            for( Leg leg : ghRoute.getLegs() )
            {
                for( Segment s : leg.getSegments() )
                {
                    if( s.getDirection() != Segment.Direction.FINISH && isDifferrent(lastSeg, s) )
                    {
                        segments.add( s );
                        lastSeg = s;
                    }
                }
            }
        }

        for( Leg leg : ghRoute.getLegs() )
        {
            for( Segment s : leg.getSegments() )
            {
                if( s.getDirection() != Segment.Direction.FINISH && isDifferrent(lastSeg, s) )
                {
                    segments.add( s );
                    lastSeg = s;
                }
                else
                    finishSeg = s;
            }
        }

        if( ghFinishCar != null )
        {
            for( Leg leg : ghRoute.getLegs() )
            {
                for( Segment s : leg.getSegments() )
                {
                    if( s.getDirection() != Segment.Direction.FINISH && isDifferrent(lastSeg, s) )
                    {
                        segments.add( s );
                        lastSeg = s;
                    }
                    else
                        finishSeg = s;
                }
            }
        }

        if( finishSeg != null )
            segments.add( finishSeg );

//        Log.i( TAG, "Number of segments " + segLen );
        int segLen = segments.size();
        result.turns = new Route.TurnItem[ segLen ];
        result.times = new Route.TimeItem[ segLen ];
        result.streets = new Route.StreetItem[ segLen ];

        i = 0;
        Long timeTotal = 0L;
        for( Segment seg : segments )
        {
            addSegment( result, i, timeTotal, seg );
            timeTotal += new Double( seg.getTime() * MULTIPLIER ).longValue();
            i++;
        }

//        dumpRoute(result);
        return result;
    }

    private void addSegment( Route result, int pos, Long timeTotal, Segment seg )
    {
        int index = findIndex( result.path, seg.getPath() != null && seg.getPath().size() > 0 ? seg.getPath().get( 0 ) : seg.getStart() );

        if( index < 0 && pos > 0 )
            return;

        result.turns[ pos ] = new Route.TurnItem();
        result.turns[ pos ].index = Math.max( 0, index );
        result.turns[ pos ].direction = toMwmDirection( seg.getDirection() ).ordinal();
        result.turns[ pos ].sourceName = seg.getInstruction();
        result.turns[ pos ].targetName = seg.getInstruction();

        result.times[ pos ] = new Route.TimeItem();
        result.times[ pos ].index = Math.max( 0, index );
        result.times[ pos ].time = Math.max(0,timeTotal);

        result.streets[ pos ] = new Route.StreetItem();
        result.streets[ pos ].index = Math.max( 0, index );
        result.streets[ pos ].name = seg.getName();

        Log.i(TAG, "Add segment to MWM route: pos = " + pos + ", index= " + index + ", name = " + seg.getName() + ", direction = " + seg.getDirection().name() );
    }

    private RoutingInfo.VehicleTurnDirection toMwmDirection( Segment.Direction dir )
    {
        if( dir == null )
            return RoutingInfo.VehicleTurnDirection.GO_STRAIGHT;

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
        for( int i = 0; i < path.length; ++i )
        {
            if( equalEpsilon( path[i].lat, point.getLatitude() ) &&
                    equalEpsilon( path[i].lng, point.getLongitude() ) )
                return i;
        }
        return -1; //not found
    }

    private static int findIndex( List<Position> path, Position point )
    {
        for( int i = 0; i < path.size(); ++i )
        {
            if( equalEpsilon( path.get( i ).getLatitude(), point.getLatitude() ) &&
                    equalEpsilon( path.get( i ).getLongitude(), point.getLongitude() ) )
                return i;
        }
        return -1; //not found
    }

    private static boolean equalEpsilon( double d1, double d2 )
    {
        return Math.abs( d1 - d2 ) < EPSILON;
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
        return posFromSeg == null
                ? 0.0
                : RouteUtil.haversineDistance( lat, lon, posFromSeg.getLatitude(), posFromSeg.getLongitude() );
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
        if( routeId == null || plannedRouteId != routeId )
            originalPlannedRoute = null;

        plannedRouteId = routeId;
    }
}
