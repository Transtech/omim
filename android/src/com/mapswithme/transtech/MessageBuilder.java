package com.mapswithme.transtech;

import android.content.Intent;
import org.json.JSONObject;

import java.util.Date;

/**
 * Class MessageBuilder
 * <p/>
 * Created by agough on 17/02/17 1:52 PM
 */
public class MessageBuilder
{
    public static enum Priority
    {
        // Priorities
        HIGH(TranstechConstants.COMMS_EVENT_PRIORITY_HIGH),
        NORMAL(TranstechConstants.COMMS_EVENT_PRIORITY_NORMAL),
        LOW(TranstechConstants.COMMS_EVENT_PRIORITY_LOW);

        private int level =  TranstechConstants.COMMS_EVENT_PRIORITY_NORMAL;
        private Priority(int p) { this.level = p; }
        int getLevel() { return level; }
    }

    private Priority priority = Priority.NORMAL;
    private String   routingKey;
    private long     expiresAt = -1;
    private String   content;
    private String   headers;
    private Boolean  suppressRoutingKeySubstitution = null;
    private String   jsonPath;

    public MessageBuilder() {}

    public MessageBuilder routingKey( String rk )
    {
        routingKey = rk;
        return this;
    }

    public MessageBuilder expiresAt(Date when)
    {
        expiresAt = when.getTime();
        return this;
    }

    public MessageBuilder expiresAt(long when)
    {
        expiresAt = when;
        return this;
    }

    public MessageBuilder priority( Priority p )
    {
        priority = p;
        return this;
    }

    public MessageBuilder suppressRoutingKeySubstitution()
    {
        this.suppressRoutingKeySubstitution = true;
        return this;
    }

    public MessageBuilder injectBinaryTo( String jsonPath )
    {
        this.jsonPath = jsonPath;
        return this;
    }

    public MessageBuilder headers(JSONObject headerObj)
    {
        this.headers = headerObj == null ? null : headerObj.toString();
        return this;
    }

    public MessageBuilder content(JSONObject contentObj)
    {
        this.content = contentObj == null ? null : contentObj.toString();
        return this;
    }

    public Intent build()
    {
        if( content == null )
            throw new RuntimeException( "Message has no content set" );

        Intent intent = new Intent( TranstechConstants.CREATE_COMMS_RECORD);
        intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_ROUTE, routingKey);
        intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_CONTENT, content);
        intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_PRIORITY, priority.getLevel() );

        if( expiresAt > 0 )
            intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_EXPIRY_DATE, expiresAt );

        if( headers != null )
            intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_HEADERS, headers );

        if( suppressRoutingKeySubstitution != null )
            intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_SKIP_ROUTING_KEY_SUBSTITUTION, suppressRoutingKeySubstitution );

        if( jsonPath != null )
            intent.putExtra( TranstechConstants.EXTRA_COMMS_EVENT_INJECT_BINARY_CONTENT_TO, jsonPath );

        return intent;
    }
}
