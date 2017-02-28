package com.mapswithme.transtech.route;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import org.opengts.util.GeoPolygon;

import java.util.Date;

/**
 * Class RouteGeofence
 * <p/>
 * Created by agough on 23/02/17 9:21 AM
 */
public class RouteGeofence implements Parcelable
{
    private static final long serialVersionUID = 1L;
    private static final String LOG_TAG = "RouteGeofence";

    public static enum GeofenceState { OUTSIDE, INSIDE, INIT, UNLISTED };

    /**
     * Represents the column names used in the database
     */
    public static final String ID = "_id";
    public static final String ENTRY_ODO = "ENTRY_ODO";
    public static final String ENTRY_TIME = "ENTRY_TIME";
    public static final String STATE = "STATE";
    public static final String NAME = "NAME";
    public static final String VERSION = "VERSION";
    public static final String CATEGORY = "CATEGORY";
    public static final String CIRCLE_LAT = "CIRCLE_LAT";
    public static final String CIRCLE_LNG = "CIRCLE_LNG";
    public static final String CIRCLE_RAD = "CIRCLE_RAD";
    // Over Time Theshold in mins
    public static final String OT_THRS = "OT_THRS";
    // Under Time Theshold in mins
    public static final String UT_THRS = "UT_THRS";
    // Speed Event Threshold Speed in km/h
    public static final String SE_THRS_SPD = "SE_THRS_SPD";
    // Speed Event Offset Speed in km/h
    public static final String SE_OFFSET_SPD = "SE_OFFSET_SPD";
    // Speed Event Speed Duration above threashold in sec
    public static final String SE_SPD_DUR = "SE_SPD_DUR";
    // Use the current speed limit in speed assist
    public static final String USE_IN_SPEED_ASSIST = "USE_IN_SPEED_ASSIST";

    public static final String DEF = "DEF";
    public static final String GROUP_ID = "GROUP_ID";
    public static final String FLAGS = "FLAGS";

    public static final String SELECTED = "SELECTED";

    public static final String NOTIFICATION = "NOTIFICATION";

    // Flag options to be used for "FLAGS" field
    public static final int FLAG_PLAUSIBLE				 = 1;
    public static final int FLAG_FIRST_SCAN 			 = 2;
    public static final int FLAG_OVERTIME_GENERATED 	 = 4;
    public static final int FLAG_ALREADY_IN_NEW_GEOFENCE = 8;
    public static final int FLAG_DELETED				 = 16;

    /**
     * URI used to represent the list of geofences.
     * Individual geofences will be represented with this plus /ID on the end
     */
    public static final Uri CONTENT_URI_NORMAL_GEOFENCE = Uri.parse("content://" + RouteConstants.GEOFENCE_AUTHORITY + "/geofences");
    public static final Uri CONTENT_URI_ROUTE_GEOFENCE = Uri.parse("content://" + RouteConstants.ROUTE_AUTHORITY + "/geofences");
    public static final Uri ALL_TRIP_GEOFENCE_CONTENT_URI = Uri.parse("content://" +RouteConstants.ROUTE_AUTHORITY + "/geofences/trips");

    /**
     * Content types used for the list, or an individual item of this type.
     */
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.transtech.geofences";
    public static final String ITEM_TYPE = "vnd.android.cursor.item/vnd.transtech.geofences";

    /**
     * Boradcast action, send when the geofence change
     */
    public static final String ACTION_GEOFENCE_CHANGED = "transtech.AF.Android.messaging.ACTION_GEOFENCE_UPDATED";


    private int id;
    private float entryOdo;
    private Date entryTime;
    private GeofenceState state;
    private String name;
    private int version;
    private String category;
    private String circleLat;
    private String circleLng;
    private int circleRad;
    private int otThrs = -1;
    private int utThrs = -1;
    private int seThrsSpd = -1;
    private int seOffSetSpd;
    private int seSpdDur;
    private boolean useInSpeedAssist;
    private int flags;
    private String def;
    private String grpId;
    private String notification;

    private GeoPolygon geoPolygon;

    // For route compliance
    private boolean selected;
    private boolean isExternalGeofence;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelFlags) {
        dest.writeInt(id);
        dest.writeFloat(entryOdo);
        dest.writeSerializable(entryTime);
        dest.writeSerializable(state);
        dest.writeString(name);
        dest.writeInt(version);
        dest.writeString(category);
        dest.writeString(circleLat);
        dest.writeString(circleLng);
        dest.writeInt(circleRad);
        dest.writeInt(otThrs);
        dest.writeInt(utThrs);
        dest.writeInt(seThrsSpd);
        dest.writeInt(seOffSetSpd);
        dest.writeInt(seSpdDur);
        dest.writeInt(flags);
        dest.writeString(def);
        dest.writeString(grpId);
        dest.writeByte((byte) (selected ? 1 : 0));
        dest.writeByte((byte) (isExternalGeofence ? 1 : 0));
        dest.writeString(notification);
        dest.writeByte((byte) (useInSpeedAssist ? 1 : 0));
    }

    public static final Parcelable.Creator<RouteGeofence> CREATOR = new Parcelable.Creator<RouteGeofence>() {
        public RouteGeofence createFromParcel(Parcel in) {
            return new RouteGeofence(in);
        }

        public RouteGeofence[] newArray(int size) {
            return new RouteGeofence[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private RouteGeofence(Parcel in) {

        id = in.readInt();
        entryOdo = in.readFloat();
        entryTime = (Date) in.readSerializable();
        state = (GeofenceState)  in.readSerializable();
        name = in.readString();
        version = in.readInt();
        category = in.readString();
        circleLat = in.readString();
        circleLng = in.readString();
        circleRad = in.readInt();
        otThrs = in.readInt();
        utThrs = in.readInt();
        seThrsSpd = in.readInt();
        seOffSetSpd = in.readInt();
        seSpdDur = in.readInt();
        flags = in.readInt();
        def = in.readString();
        grpId = in.readString();
        selected = in.readByte() == 1;
        isExternalGeofence = in.readByte() == 1;
        notification = in.readString();
        useInSpeedAssist = in.readByte() == 1;
    }



    public RouteGeofence() {

    }

    /**
     *  Populates a geofence from a databse cursor
     * @param geofenceCursor
     */
    public RouteGeofence(Cursor geofenceCursor) {
        int idColumn = geofenceCursor.getColumnIndex(ID);
        int entryOdoColumn = geofenceCursor.getColumnIndex(ENTRY_ODO);
        int entryTimeColumn = geofenceCursor.getColumnIndex(ENTRY_TIME);
        int stateColumn = geofenceCursor.getColumnIndex(STATE);
        int nameColumn = geofenceCursor.getColumnIndex(NAME);
        int versionColumn = geofenceCursor.getColumnIndex(VERSION);
        int categoryColumn = geofenceCursor.getColumnIndex(CATEGORY);
        int circleLatColumn = geofenceCursor.getColumnIndex(CIRCLE_LAT);
        int circleLngColumn = geofenceCursor.getColumnIndex(CIRCLE_LNG);
        int circleRadColumn = geofenceCursor.getColumnIndex(CIRCLE_RAD);
        int otThrsColumn = geofenceCursor.getColumnIndex(OT_THRS);
        int utThrsColumn = geofenceCursor.getColumnIndex(UT_THRS);
        int seThrsSpdColumn = geofenceCursor.getColumnIndex(SE_THRS_SPD);
        int seOffSetSpdColumn = geofenceCursor.getColumnIndex(SE_OFFSET_SPD);
        int seSpdDurColumn = geofenceCursor.getColumnIndex(SE_SPD_DUR);
        int defColumn = geofenceCursor.getColumnIndex(DEF);
        int flagsColumn = geofenceCursor.getColumnIndex(FLAGS);
        int grpIdColumn = geofenceCursor.getColumnIndex(GROUP_ID);
        int selectedColumn = geofenceCursor.getColumnIndex(SELECTED);
        int notificationColumn = geofenceCursor.getColumnIndex(NOTIFICATION);
        int speedAssistColumn = geofenceCursor.getColumnIndex(USE_IN_SPEED_ASSIST);


        id = geofenceCursor.getInt(idColumn);
        entryOdo = geofenceCursor.getFloat(entryOdoColumn);
        entryTime = new Date(geofenceCursor.getLong(entryTimeColumn));
        state = geofenceCursor.getString(stateColumn) != null ? GeofenceState.valueOf(geofenceCursor.getString(stateColumn)) : null;
        name = geofenceCursor.getString(nameColumn);
        version = geofenceCursor.getInt(versionColumn);
        category = geofenceCursor.getString(categoryColumn);
        circleLat = geofenceCursor.getString(circleLatColumn);
        circleLng = geofenceCursor.getString(circleLngColumn);
        circleRad = geofenceCursor.getInt(circleRadColumn);
        otThrs = geofenceCursor.getInt(otThrsColumn);
        utThrs = geofenceCursor.getInt(utThrsColumn);
        seThrsSpd = geofenceCursor.getInt(seThrsSpdColumn);
        seOffSetSpd = geofenceCursor.getInt(seOffSetSpdColumn);
        seSpdDur = geofenceCursor.getInt(seSpdDurColumn);
        def = geofenceCursor.getString(defColumn);
        flags = geofenceCursor.getInt(flagsColumn);
        grpId = geofenceCursor.getString(grpIdColumn);
        notification = geofenceCursor.getString(notificationColumn);

        if (selectedColumn != -1) {
            selected = geofenceCursor.getInt(selectedColumn) == 1 ? true : false;
        }

        if (speedAssistColumn != -1) {
            useInSpeedAssist = geofenceCursor.getInt(speedAssistColumn) == 1 ? true : false;
        }

        generateGeoPolygon();
    }
/*
    public GeofenceEntity(JSONObject geoJSON) {

        try {

            setId(geoJSON.getInt(GeofenceConstants.GEOFENCE_ID));

            if (geoJSON.has(GeofenceConstants.GEOFENCE_VER)) {
                setVersion(geoJSON.getInt(GeofenceConstants.GEOFENCE_VER));
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_NAME)) {
                setName(geoJSON.getString(GeofenceConstants.GEOFENCE_NAME));
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_CATEGORY)) {
                setCategory(geoJSON.getString(GeofenceConstants.GEOFENCE_CATEGORY));
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_CIRCLE_LAT)) {
                try {
                    setCircleLat(String.valueOf(geoJSON.getDouble(GeofenceConstants.GEOFENCE_CIRCLE_LAT)));
                }
                catch (Exception e) {
                    Log.e( LOG_TAG, "Error parsing geofence JSON: " + GeofenceConstants.GEOFENCE_CIRCLE_LAT );
                }
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_CIRCLE_LNG)) {
                try {
                    setCircleLng(String.valueOf(geoJSON.getDouble(GeofenceConstants.GEOFENCE_CIRCLE_LNG)));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error parsing geofence JSON: " + GeofenceConstants.GEOFENCE_CIRCLE_LNG);
                }
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_CIRCLE_RAD)) {
                try {
                    setCircleRad(geoJSON.getInt(GeofenceConstants.GEOFENCE_CIRCLE_RAD));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error parsing geofence JSON: " + GeofenceConstants.GEOFENCE_CIRCLE_RAD);
                }
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_DEF)) {
                setDef(geoJSON.getString(GeofenceConstants.GEOFENCE_DEF));
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_OVERTIME)) {
                JSONObject OTobj = geoJSON.getJSONObject(GeofenceConstants.GEOFENCE_OVERTIME);
                setOtThrs(OTobj.getInt(GeofenceConstants.GEOFENCE_THRESHOLD));
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_UNDERTIME)) {
                JSONObject UTobj = geoJSON.getJSONObject(GeofenceConstants.GEOFENCE_UNDERTIME);
                setUtThrs(UTobj.getInt(GeofenceConstants.GEOFENCE_THRESHOLD));
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_SPEED_EVENT)) {
                JSONObject speedEvt = geoJSON.getJSONObject(GeofenceConstants.GEOFENCE_SPEED_EVENT);
                setSeOffSetSpd(speedEvt.getInt(GeofenceConstants.GEOFENCE_SPEED_OFFSET));
                setSeSpdDur(speedEvt.getInt(GeofenceConstants.GEOFENCE_SPEED_DUR));
                setSeThrsSpd(speedEvt.getInt(GeofenceConstants.GEOFENCE_SPEED_THRS));

                if (speedEvt.has(GeofenceConstants.GEOFENCE_SPEED_USE_IN_SPEED_ASSIST)) {
                    setUseInSpeedAssist(speedEvt.getBoolean(GeofenceConstants.GEOFENCE_SPEED_USE_IN_SPEED_ASSIST));
                }
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_GRP_ID)) {
                setGrpId(geoJSON.getString(GeofenceConstants.GEOFENCE_GRP_ID));
            }

            if (geoJSON.has(GeofenceConstants.GEOFENCE_NOTIFICATION)) {
                setNotification(geoJSON.getString(GeofenceConstants.GEOFENCE_NOTIFICATION));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        generateGeoPolygon();
    }
*/

    public ContentValues toContent() {
        ContentValues v = new ContentValues();

        v.put(ID, id);
        v.put(ENTRY_ODO, entryOdo);
        v.put(ENTRY_TIME, (entryTime == null) ? null : entryTime.getTime());
        v.put(STATE, state != null ? state.name() : null);
        v.put(NAME, name);
        v.put(VERSION, version);
        v.put(CATEGORY, category);
        v.put(CIRCLE_LAT, circleLat);
        v.put(CIRCLE_LNG, circleLng);
        v.put(CIRCLE_RAD, circleRad);
        v.put(OT_THRS, otThrs);
        v.put(UT_THRS, utThrs);
        v.put(SE_THRS_SPD, seThrsSpd);
        v.put(SE_OFFSET_SPD, seOffSetSpd);
        v.put(SE_SPD_DUR, seSpdDur);
        v.put(DEF, def);
        v.put(GROUP_ID, grpId);
        v.put(FLAGS, flags);
        if (isExternalGeofence()) {
            v.put(SELECTED, selected);
        }

        v.put(NOTIFICATION, notification);
        v.put(USE_IN_SPEED_ASSIST, useInSpeedAssist);

        return v;
    }

    /**
     * @return a URI that uniquely identifies this geofence.  can be used in ContentProvider update/delete
     */
    public Uri getURI() {
        if (isExternalGeofence) {
            return Uri.withAppendedPath(CONTENT_URI_ROUTE_GEOFENCE, String.valueOf(id));
        }
        else {
            return Uri.withAppendedPath(CONTENT_URI_NORMAL_GEOFENCE, String.valueOf(id));
        }
    }



    @Override
    public String toString() {
        return "Geofence [name=" + name + ", id=" + id +
                ", Lat=" + circleLat +
                ", Long=" + circleLng +
                ", Rad=" + circleRad +
                ", version=" + version + "]";
    }

    public static Uri getAllTripGeofencesUri(long tripId) {
        return ContentUris.withAppendedId( ALL_TRIP_GEOFENCE_CONTENT_URI, tripId );
    }

    private void generateGeoPolygon() {
        if (def != null && def.length() > 0) {
            String[] points = def.split(",");

            float[][] allPoints = new float[points.length + 1][2];

            int i = 0;
            for (String point : points) {
                String[] position = point.split(" ");
                String lat = position[0];
                String lon = position[1];
                allPoints[i][0] = Float.parseFloat(lat);
                allPoints[i][1] = Float.parseFloat(lon);
                i++;
            }

            // Add the first point again as the last point to
            // close the ploygon.
            allPoints[points.length][0] = allPoints[0][0];
            allPoints[points.length][1] = allPoints[0][1];

            geoPolygon = new GeoPolygon(name, allPoints);
        }
    }


    /**
     * @param context
     * @param
     * @return
     */
    public static Cursor createQuery(Context context) {
        return context.getContentResolver().query(
                CONTENT_URI_NORMAL_GEOFENCE,
                null,
                null,
                null,
                ID + " ASC");
    }

    public GeoPolygon getGeoPolygon() {
        if (geoPolygon == null) {
            generateGeoPolygon();
        }

        return geoPolygon;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getEntryOdo() {
        return entryOdo;
    }

    public void setEntryOdo(float entryOdo) {
        this.entryOdo = entryOdo;
    }

    public Date getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(Date entryTime) {
        this.entryTime = entryTime;
    }

    public GeofenceState getState() {
        return state;
    }

    public void setState(GeofenceState state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


    public String getCircleLat() {
        return circleLat;
    }

    public void setCircleLat(String circleLat) {
        this.circleLat = circleLat;
    }

    public String getCircleLng() {
        return circleLng;
    }

    public void setCircleLng(String circleLng) {
        this.circleLng = circleLng;
    }

    public int getCircleRad() {
        return circleRad;
    }

    public void setCircleRad(int circleRad) {
        this.circleRad = circleRad;
    }

    public int getOtThrs() {
        return otThrs;
    }

    public void setOtThrs(int otThrs) {
        this.otThrs = otThrs;
    }

    public int getUtThrs() {
        return utThrs;
    }

    public void setUtThrs(int utThrs) {
        this.utThrs = utThrs;
    }

    public int getSeThrsSpd() {
        return seThrsSpd;
    }

    public void setSeThrsSpd(int seThrsSpd) {
        this.seThrsSpd = seThrsSpd;
    }

    public int getSeOffSetSpd() {
        return seOffSetSpd;
    }

    public void setSeOffSetSpd(int seOffSetSpd) {
        this.seOffSetSpd = seOffSetSpd;
    }

    public int getSeSpdDur() {
        return seSpdDur;
    }

    public void setSeSpdDur(int seSpdDur) {
        this.seSpdDur = seSpdDur;
    }

    public String getDef() {
        return def;
    }

    public void setDef(String def) {
        this.def = def;
    }

    public boolean isFlagSet(int flagToCheck) {
        return ((flags & flagToCheck) == flagToCheck);
    }

    public void setFlag(int flagToSet) {
        this.flags |= flagToSet;
    }

    public void clearFlag(int flagToClear) {
        this.flags &= ~flagToClear;
    }

    public String getGrpId() {
        return grpId;
    }

    public void setGrpId(String grpId) {
        this.grpId = grpId;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isExternalGeofence() {
        return isExternalGeofence;
    }

    public void setIsExternalGeofence(boolean isExternalGeofence) {
        this.isExternalGeofence = isExternalGeofence;
    }

    public boolean getUseInSpeedAssist() {
        return useInSpeedAssist;
    }

    public void setUseInSpeedAssist(boolean useInSpeedAssist) {
        this.useInSpeedAssist = useInSpeedAssist;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }
/*
    public JSONObject getGeofenceJson(boolean allFields) throws JSONException {
        JSONObject geo = new JSONObject();
        geo.put(GeofenceConstants.GEOFENCE_NAME, getName());
        geo.put(GeofenceConstants.GEOFENCE_ID, getId());
        geo.put(GeofenceConstants.GEOFENCE_VER, getVersion());
        geo.put(GeofenceConstants.GEOFENCE_GRP_ID, getGrpId());

        if (allFields) {
            geo.put(GeofenceConstants.GEOFENCE_CATEGORY, getCategory());
            geo.put(GeofenceConstants.GEOFENCE_CIRCLE_LAT, Double.valueOf(getCircleLat()));
            geo.put(GeofenceConstants.GEOFENCE_CIRCLE_LNG, Double.valueOf(getCircleLng()));
            geo.put(GeofenceConstants.GEOFENCE_CIRCLE_RAD, getCircleRad());

            // Overtime object
            JSONObject otObj = new JSONObject();
            otObj.put(GeofenceConstants.GEOFENCE_THRESHOLD, getOtThrs());
            geo.put(GeofenceConstants.GEOFENCE_OVERTIME, otObj);

            // Undertime object
            JSONObject utObj = new JSONObject();
            utObj.put(GeofenceConstants.GEOFENCE_THRESHOLD, getUtThrs());
            geo.put(GeofenceConstants.GEOFENCE_UNDERTIME, utObj);

            // Speed event object
            JSONObject speedObj = new JSONObject();
            speedObj.put(GeofenceConstants.GEOFENCE_SPEED_THRS, getSeThrsSpd());
            speedObj.put(GeofenceConstants.GEOFENCE_SPEED_OFFSET, getSeOffSetSpd());
            speedObj.put(GeofenceConstants.GEOFENCE_SPEED_DUR, getSeSpdDur());
            speedObj.put(GeofenceConstants.GEOFENCE_SPEED_USE_IN_SPEED_ASSIST, getUseInSpeedAssist());
            geo.put(GeofenceConstants.GEOFENCE_SPEED_EVENT, speedObj);

            geo.put(GeofenceConstants.GEOFENCE_DEF, getDef());
            geo.put(GeofenceConstants.GEOFENCE_STATE, getState());

            geo.put(GeofenceConstants.GEOFENCE_NOTIFICATION, getNotification());
        }

        return geo;
    }
*/
    public long getTotalMinutesInside() {
        return (new Date().getTime()/60000) - (getEntryTime().getTime()/60000); // Mins
    }

    public long getTotalSecondsInside() {
        return (new Date().getTime()/1000) - (getEntryTime().getTime()/1000); // Mins
    }
}

