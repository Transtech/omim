package com.mapswithme.transtech.route;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import au.net.transtech.geo.model.Position;
import au.net.transtech.geo.model.Segment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class RouteLeg implements Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Represents the column names used in the database
     */
    public static final String ID = "ID";
    public static final String TRIP_ID = "TRIP_ID";
    public static final String SEQ = "SEQ";
    public static final String START_STOP_ID = "START_STOP_ID";
    public static final String END_STOP_ID = "END_STOP_ID";
    public static final String DURATION = "DURATION";
    public static final String DISTANCE = "DISTANCE";
    public static final String ROUTE_DEF = "ROUTE_DEF";


    /**
     * URI used to represent the list of stop.
     * Individual stops will be represented with this plus /ID on the end
     */
    public static final Uri CONTENT_URI = Uri.parse( "content://" + RouteConstants.AUTHORITY + "/legs" );

    /**
     * Content types used for the list, or an individual item of this type.
     */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.transtech.legs";
    public static final String ITEM_TYPE = "vnd.android.cursor.item/vnd.transtech.legs";


    private int id;
    private int tripId;
    private int seq;
    private int startStopId;
    private int endStopId;
    private double duration;
    private double distance;
    private String routeDef;

    private List<Position> path;
    private List<Segment> segments;

    public RouteLeg()
    {
    }

    /**
     * Populates a trip from a database cursor
     *
     * @param legCursor
     */
    public RouteLeg( Cursor legCursor )
    {
        int idColumn = legCursor.getColumnIndex( ID );
        int tripIdColumn = legCursor.getColumnIndex( TRIP_ID );
        int seqColumn = legCursor.getColumnIndex( SEQ );
        int startStopColumn = legCursor.getColumnIndex( START_STOP_ID );
        int endStopColumn = legCursor.getColumnIndex( END_STOP_ID );
        int durationColumn = legCursor.getColumnIndex( DURATION );
        int distanceColumn = legCursor.getColumnIndex( DISTANCE );
        int routeDefColumn = legCursor.getColumnIndex( ROUTE_DEF );


        id = legCursor.getInt( idColumn );
        tripId = legCursor.getInt( tripIdColumn );
        seq = legCursor.getInt( seqColumn );
        startStopId = legCursor.getInt( startStopColumn );
        endStopId = legCursor.getInt( endStopColumn );
        duration = legCursor.getDouble( durationColumn );
        distance = legCursor.getDouble( distanceColumn );
        routeDef = legCursor.getString( routeDefColumn );
    }


    public ContentValues toContent()
    {
        ContentValues v = new ContentValues();

        v.put( ID, id );
        v.put( TRIP_ID, tripId );
        v.put( SEQ, seq );
        v.put( START_STOP_ID, startStopId );
        v.put( END_STOP_ID, endStopId );
        v.put( DURATION, duration );
        v.put( DISTANCE, distance );
        v.put( ROUTE_DEF, routeDef );

        return v;
    }

    /**
     * @return a URI that uniquely identifies this stop.  can be used in ContentProvider update/delete
     */
    public Uri getURI()
    {
        return Uri.withAppendedPath( CONTENT_URI, String.valueOf( id ) );
    }


    @Override
    public String toString()
    {
        return "RouteLeg{" +
                "id=" + id +
                ", tripId=" + tripId +
                ", seq=" + seq +
                ", startStopId=" + startStopId +
                ", endStopId=" + endStopId +
                ", duration=" + duration +
                ", distance=" + distance +
                ", routeDef='" + routeDef + '\'' +
                '}';
    }

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public int getTripId()
    {
        return tripId;
    }

    public void setTripId( int tripId )
    {
        this.tripId = tripId;
    }

    public int getSeq()
    {
        return seq;
    }

    public void setSeq( int seq )
    {
        this.seq = seq;
    }

    public int getStartStopId()
    {
        return startStopId;
    }

    public void setStartStopId( int startStopId )
    {
        this.startStopId = startStopId;
    }

    public int getEndStopId()
    {
        return endStopId;
    }

    public void setEndStopId( int endStopId )
    {
        this.endStopId = endStopId;
    }

    public double getDuration()
    {
        return duration;
    }

    public void setDuration( double duration )
    {
        this.duration = duration;
    }

    public double getDistance()
    {
        return distance;
    }

    public void setDistance( double distance )
    {
        this.distance = distance;
    }

    public String getRouteDef()
    {
        return routeDef;
    }

    public void setRouteDef( String routeDef )
    {
        this.routeDef = routeDef;
    }

    public List<Segment> getRouteSegments()
    {
        if( segments == null )
            parseRouteDef();

        return segments;
    }

    public void setRouteSegments(List<Segment> segments)
    {
        this.segments = segments;
    }

    public List<Position> getPath()
    {
        if( path == null )
            parseRouteDef();

        return path;
    }

    public void setPath( List<Position> path )
    {
        this.path = path;
    }

    private void parseRouteDef()
    {
        if( routeDef == null || routeDef.length() == 0 )
            return;

        try
        {
            JSONObject jsonObj = new JSONObject( routeDef );
            if( jsonObj.has("segments") && !jsonObj.isNull( "segments" ) )
            {
                JSONArray jsonSegs = jsonObj.getJSONArray( "segments" );
                segments = new ArrayList<Segment>( jsonSegs.length() );

                for( int i = 0; i < jsonSegs.length(); ++i )
                {
                    JSONObject jsonSeg = jsonSegs.getJSONObject( i );

                    Segment seg = new Segment();
                    seg.setName( jsonSeg.optString( "name" ) );
                    seg.setInstruction( jsonSeg.optString( "instruction" ) );
                    if( jsonSeg.has("direction") && !jsonSeg.isNull( "direction" ))
                        seg.setDirection( Segment.Direction.valueOf( jsonSeg.getString( "direction" ) ) );
                    seg.setTime( jsonSeg.optLong( "time" ) );
                    seg.setDistance( jsonSeg.optDouble( "distance" ) );

                    if( jsonSeg.has( "start" ) && !jsonSeg.isNull( "start" ) )
                    {
                        JSONObject jsonPos = jsonSeg.getJSONObject( "start" );
                        seg.setStart( new Position( jsonPos.optDouble( "Lat" ), jsonPos.optDouble( "Lng" ) ) );
                    }

                    if( jsonSeg.has( "end" ) && !jsonSeg.isNull( "end" ) )
                    {
                        JSONObject jsonPos = jsonSeg.getJSONObject( "end" );
                        seg.setEnd( new Position( jsonPos.optDouble( "Lat" ), jsonPos.optDouble( "Lng" ) ) );
                    }

                    segments.add( seg );
                }
            }

            if( jsonObj.has("path") && !jsonObj.isNull( "path" ) )
            {
                path = new ArrayList<Position>();
                JSONArray jsonPath = jsonObj.getJSONArray( "path" );
                for( int i = 0; i < jsonPath.length(); ++i )
                {
                    JSONObject posObj = jsonPath.getJSONObject( i );
                    path.add( new Position( posObj.optDouble( "Lat" ), posObj.optDouble( "Lng" ) ) );
                }
            }
        }
        catch( JSONException ex )
        {
            Log.e( "RouteLeg", "Failed to process leg definitions of trip " + tripId, ex );
        }

    }
}
