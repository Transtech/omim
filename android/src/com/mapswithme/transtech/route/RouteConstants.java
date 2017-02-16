package com.mapswithme.transtech.route;

import android.net.Uri;

/**
 * Created by Afzal on 6/10/2015.
 */
public class RouteConstants {

    public static final String AUTHORITY = "transtech.af.android.route.routeprovider";
    public static final Uri TRIPS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/trips");
    public static final Uri LEGS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/legs");

    public static final String EXTRA_SELECTED_ROUTE_ID = "transtech.AF.Android.route.EXTRA_SELECTED_ROUTE_ID";
    public static final String EXTRA_SELECTED_ROUTE_NAME = "transtech.AF.Android.route.EXTRA_SELECTED_ROUTE_NAME";
    public static final String EXTRA_CURRENT_QJ_TRIP_ID = "transtech.AF.Android.route.EXTRA_CURRENT_QJ_TRIP_ID";

    // For payload
    public static final String MESSAGE_TYPE_MFT             = "TRIP-MFT";
    public static final String MESSAGE_TYPE_TRIP_STARTED    = "TRIP-STARTED";
    public static final String MESSAGE_TYPE_TRIP_FINISHED   = "TRIP-FINISHED";
    public static final String MESSAGE_TYPE_TRIP_ENTRY      = "TRIP-ENTRY";
    public static final String MESSAGE_TYPE_TRIP_EXIT       = "TRIP-EXIT";

    public static final String TRIPS = "trips";
    public static final String RECORD_TYPE = "RecordType";
    public static final String SUB_TYPE = "SubType";
    public static final String ID = "id";
    public static final String VERSION = "version";
    public static final String ESTIMATED_DUR = "estimatedDuration";
    public static final String STOPS = "stops";
    public static final String LOCATION = "location";
    public static final String SEQ = "seq";
    public static final String NAME = "name";
    public static final String GPS = "GPS";
    public static final String GPS_LAT = "Lat";
    public static final String GPS_LNG = "Lng";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_NUMBER = "number";
    public static final String ADDRESS_STREET = "street";
    public static final String ADDRESS_SUBURB = "suburb";
    public static final String ADDRESS_STATE = "state";
    public static final String ADDRESS_POSTCODE = "postcode";
    public static final String ADDRESS_COUNTRY = "country";
    public static final String ADDRESS_PHONE = "phone";
    public static final String ADDRESS_FAX = "fax";
    public static final String TYPE = "type";
    public static final String GEOFENCE = "geofence";
    public static final String GEOFENCE_ID = "Id";

    // for trip events from device
    public static final String TRIP = "Trip";
    public static final String TRIP_ID = "TripId";
    public static final String TRIP_VERSION = "TripVersion";
    public static final String CORRELATION_ID = "CorrelationId";
    public static final String SOURCE = "Source";
    public static final String STOP_ID = "StopId";
    public static final String GROUP_ID = "GroupId";
    public static final String NETWORK = "Network";
    public static final String MODE = "Mode";

    // for sync response
    public static final String DELETE = "delete";
    public static final String ADD = "add";


    public static final String NETWORK_CODE = "Code";
    public static final String NETWORK_DESC = "Desc";

    public static final String SUB_TYPE_ROUTE_PLANNED = "ROUTE_PLANNED";
    public static final String SUB_TYPE_DEVICE_NETWORK = "NETWORK_ACTUAL";

}
