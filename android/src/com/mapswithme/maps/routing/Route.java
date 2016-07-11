package com.mapswithme.maps.routing;

/**
 * Class Route
 * <p/>
 * Created by agough on 29/03/16 11:08 AM
 */
public class Route
{
    public static class Position
    {
        public Position() {}
        public Position(double _lat, double _lng) { lat = _lat; lng = _lng; }
        public double lat;
        public double lng;
    }

    public static class TurnItem
    {
        public int index;       //index into 'path'
        public int direction;   //MUST be one of RoutingInfo.VehicleTurnDirection
        public int exitNum;             /*!< Number of exit on roundabout. */
        public String sourceName;            /*!< Name of the street which the ingoing edge belongs to */
        public String targetName;            /*!< Name of the street which the outgoing edge belongs to */
        public boolean keepAnyway;
//        vector<SingleLaneInfo> m_lanes; /*!< Lane information on the edge before the turn. */
    }

    public static class TimeItem
    {
        public int index; //index into 'path'
        public double time;
    }

    public TurnItem[] turns;
    public TimeItem[] times;
    public Position[] path;
}
