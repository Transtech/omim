package com.mapswithme.transtech.route;

import android.content.ContentValues;
import android.database.Cursor;
import au.net.transtech.geo.model.Position;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;


public class RouteTrip implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;


    /**
     * Represents the column names used in the database
     */
    public static final String ID = "ID";
    public static final String NAME = "NAME";
    public static final String VERSION = "VERSION";
    public static final String TYPE = "TYPE";
    public static final String GRP_ID = "GRP_ID";
    public static final String EST_DURATION = "EST_DURATION";
    public static final String SELECTED = "SELECTED";

    private int id;
    private String name;
    private int version;
    private String type;
    private String groupId;
    private int estimatedDuration;
    boolean selected;

    private List<Position> path;

    public RouteTrip() {
    }

    /**
     *  Populates a trip from a database cursor
     * @param tripCursor
     */
    public RouteTrip(Cursor tripCursor) {
        int idColumn = tripCursor.getColumnIndex(ID);
        int nameColumn = tripCursor.getColumnIndex(NAME);
        int versionColumn = tripCursor.getColumnIndex(VERSION);
        int estDurColumn = tripCursor.getColumnIndex(EST_DURATION);
        int selectedColumn = tripCursor.getColumnIndex(SELECTED);
        int typeColumn = tripCursor.getColumnIndex(TYPE);
        int groupIdColumn = tripCursor.getColumnIndex(GRP_ID);


        id = tripCursor.getInt(idColumn);
        name = tripCursor.getString(nameColumn);
        version = tripCursor.getInt(versionColumn);
        estimatedDuration = tripCursor.getInt(estDurColumn);
        type = tripCursor.getString(typeColumn);
        selected = tripCursor.getInt(selectedColumn) == 1 ? true : false;
        groupId = tripCursor.getString(groupIdColumn);
    }

    public RouteTrip(JSONObject tripJSON) {

        try {

            id = tripJSON.getInt( RouteConstants.ID);
            estimatedDuration = tripJSON.getInt(RouteConstants.ESTIMATED_DUR);
            name = tripJSON.getString(RouteConstants.NAME);
            version = tripJSON.getInt(RouteConstants.VERSION);
            type = tripJSON.getString(RouteConstants.TYPE);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public ContentValues toContent() {
        ContentValues v = new ContentValues();

        v.put(ID, id);
        v.put(NAME, name);
        v.put(VERSION, version);
        v.put(TYPE, type);
        v.put(EST_DURATION, estimatedDuration);
        v.put(SELECTED, selected);
        v.put(GRP_ID, groupId);

        return v;
    }

    @Override
    public String toString() {
        return name;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(int estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<Position> getPath()
    {
        return path;
    }

    public void setPath( List<Position> path )
    {
        this.path = path;
    }
}
