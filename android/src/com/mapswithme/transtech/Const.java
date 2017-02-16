package com.mapswithme.transtech;

/**
 * Class Const
 * <p/>
 * Created by agough on 30/01/17 1:27 PM
 */
public class Const
{
    private static final String PREFIX = "transtech.AF.Android.Comms.action.";

    //Create a comms record _with_ persistence for later publishing
    public static final String CREATE_COMMS_RECORD 			        = PREFIX + "CREATE_RECORD";
    public static final String PUBLISH_COMMS_RECORD 		        = PREFIX + "PUBLISH_RECORD";
    public static final String PUBLISH_COMMS_RECORD_NEVER_PERSIST   = PREFIX + "PUBLISH_RECORD_NEVER_PERSIST";
    public static final String ACTION_REESTABLISH_CONNECTION        = PREFIX + "REESTABLISH_CONNECTION";
    public static final String ACTION_AMQP_MESSAGE                  = PREFIX + "AMQP_MESSAGE";
    public static final String ACTION_AMQP_RECONNECTED              = PREFIX + "AMQP_RECONNECTED";

    public static final String EXTRA_COMMS_EVENT_ROUTE              = "CommsEventRoute";
    public static final String EXTRA_COMMS_EVENT_HEADERS            = "CommsEventHeaders";
    public static final String EXTRA_COMMS_EVENT_CONTENT            = "CommsEventContent";
    public static final String EXTRA_COMMS_EVENT_PRIORITY           = "CommsEventPriority";
    public static final String EXTRA_COMMS_EVENT_EXPIRY_DATE        = "CommsEventExpiryDate";
    public static final String EXTRA_COMMS_EVENT_URI                = "CommsEventURI";
    public static final String EXTRA_COMMS_EVENT_SKIP_ROUTING_KEY_SUBSTITUTION = "CommsEventSkipRoutingKeySubstitution";

    // AMQP Headers
    public static final String	AMQP_HEADER_APPLICATION_NAME		= "applicationName";
    public static final String	AMQP_HEADER_APPLICATION_VERSION		= "applicationVersion";
    public static final String	AMQP_HEADER_MESSAGE_TYPE			= "messageType";
    public static final String	AMQP_HEADER_MESSAGE_VERSION			= "messageVersion";
    public static final String	AMQP_HEADER_MESSAGE_ID				= "messageId";
    public static final String	AMQP_HEADER_MESSAGE_FORMAT			= "messageFormat";
    public static final String	AMQP_HEADER_CORRELATION_ID			= "correlationId";
    public static final String	AMQP_HEADER_DEVICE_ID				= "deviceId";
    public static final String	AMQP_HEADER_REFERENCE_KEY			= "referenceKey";
    public static final String	AMQP_HEADER_COMPANY_SLUG			= "companySlug";
    public static final String	AMQP_HEADER_API_KEY					= "apiKey";
    public static final String	AMQP_HEADER_ROUTING_KEY				= "routingKey";
    public static final String	AMQP_HEADER_EXCHANGE_NAME			= "exchangeName";
    public static final String  AMQP_HEADER_REDELIVERED         	= "redelivered";
    public static final String  AMQP_HEADER_ONE_WAY              	= "httpOneWay";

    public static final String AMQP_MSG_TYPE_MDM_COMMAND			= "transtech.command";
    public static final String AMQP_MSG_TYPE_MDM_RESPONSE			= "transtech.response";

    // Priorities
    public static final int COMMS_EVENT_PRIORITY_HIGH 	= 1;
    public static final int COMMS_EVENT_PRIORITY_NORMAL = 5;
    public static final int COMMS_EVENT_PRIORITY_LOW 	= 9;

    public static final String AMQP_ROUTING_KEY_ROUTE_TRIP     		= "iface.trip";
}
