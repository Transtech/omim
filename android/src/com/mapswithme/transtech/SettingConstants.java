package com.mapswithme.transtech;

/**
 * Class SettingsConstants
 * <p/>
 * Created by agough on 4/08/16 9:28 AM
 */
public interface SettingConstants
{

    public final static String SOURCE_USER  = "USER";
    public final static String SOURCE_DEVICE  = "DEVICE";

    /////////////////////////////// GLOBAL SETTINGS /////////////////////////////////////

    public final static String GLOBAL_ENVIRONMENT        = "environment";
    public final static String GLOBAL_VERSION            = "version";
    public final static String GLOBAL_LAST_UPDATE_DATE   = "update_date";
    public final static String GLOBAL_UPDATE_STATUS      = "updateStatus";
    public final static String GLOBAL_SCREEN_LAYOUT      = "screen.layout";
    public final static String GLOBAL_COMPANY_ID         = "company.id";
    public final static String GLOBAL_COMPANY_SLUG       = "company.slug";
    public final static String GLOBAL_WEBSERVICE_API_KEY = "company.key";
    public final static String GLOBAL_DATE_FORMAT        = "dateFormat";
    public final static String GLOBAL_DEVICE_TYPE        = "deviceType";
    public final static String GLOBAL_DEVICE_ID          = "device.imei";
    public final static String GLOBAL_IVU_ID             = "IvuId";
    public final static String GLOBAL_PAN_ENABLED        = "panEnabled";
    public final static String GLOBAL_VEHICLE_REG        = "vehicle.registration";
    public final static String GLOBAL_VEHICLE_REG_STATE  = "vehicle.state";
    public final static String GLOBAL_VEHICLE_ID         = "vehicle.id";
    public final static String GLOBAL_VEHICLE_NAME       = "vehicle.name";
    public final static String GLOBAL_COUNTRY_NAME       = "countryName";
    public final static String GLOBAL_QJ_CONFIG_URL      = "QJConfigURL";
    public final static String GLOBAL_WEBSERVICE_URL     = "WebServiceURL";
    public final static String GLOBAL_DRIVER_AUTH_URL    = "DriverAuthURL";

    public final static String GLOBAL_DECLARED_REG       = "declared.registration";
    public final static String GLOBAL_DECLARED_REG_STATE = "declared.state";

    public final static String GLOBAL_USER               = "User";
    public final static String GLOBAL_VEHICLE_TYPE       = "VehicleTypeSettings";
    public final static String GLOBAL_COUNTRIES          = "Countries";
    public final static String GLOBAL_VEHICLE_TYPES      = "VehicleTypes";
    public final static String GLOBAL_IAP_COMMENT_TYPES  = "IAPCommentTypes";

    /////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////// SHELL SETTINGS ///////////////////////////////////

    public final static String SHELL_SECOND_MOVEMENT_ALERT_INTERVAL = "SecondMovementAlertAfterInterval";
    public final static String SHELL_ADMIN_PASSWORD                 = "admin.password";
    public final static String SHELL_DEMO_ADMIN_PASSWORD            = "admin.password.demo";

    /////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////// GPSLIB SETTINGS ////////////////////////////////////

    public final static String GPSLIB_IGNORE_POWER        = "power.ignore";
    public final static String GPSLIB_SPEED_DELTA         = "filter.speed.delta";
    public final static String GPSLIB_TIME_DELAY          = "filter.delay";
    public final static String GPSLIB_MIN_MOVING_SPEED    = "filter.speed.min";
    public final static String GPSLIB_ODO_SAVE_INTERVAL   = "odo.save.interval";

    /////////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////// GPS TRACKING SETTINGS ////////////////////////////////////

    public final static String GPS_ENDPONT             = "Endpoint";
    public final static String GPS_SEND_DATA_INTERVAL  = "transmit.interval";
    public final static String GPS_ONTRACK_DISABLED     = "ontrack.disabled";

    //////////////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////// GEOFENCE SETTINGS ////////////////////////////////////

    static final String GEOFENCE_ENABLED = "geofence.enabled";
    static final String GEOFENCE_SAFETY_FACTOR = "geofence.safety.factor";
    static final String GEOFENCE_INVALID_GPS_THRESHOLD = "invalid.gps.thershold";
    static final String GEOFENCE_SYNC_INTERVAL = "geofence.sync.interval";

    //////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////// ROUTE COMPLIANCE SETTINGS ///////////////////////////////

    static final String ROUTE_NETWORK = "route.network";
    static final String ROUTE_ALERT_REPEAT_DUR = "route.alert.repeat.dur";
    static final String ROUTE_CURRENT = "route.current";

    //////////////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////// COMMS SETTINGS /////////////////////////////////////////

    public final static String COMMS_AMQP_USER      = "amqp.username";
    public final static String COMMS_AMQP_PASSWORD  = "amqp.password";
    public final static String COMMS_AMQP_URL       = "amqp.url";
    public final static String COMMS_IN_BOUND_EXPIRE_DUR       = "inbound.expire.dur";

    /////////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////// COMMON SETTINGS /////////////////////////////////////

    public final static String COMMON_LOG_LEVEL      = "log.level";

    /////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////SUM RECORDS SETTINGS /////////////////////////////////////////

    public final static String IGN_ON_GPS      	= "ign.on.gps";
    public final static String IGN_ON_TIME		= "ign.on.time";
    public final static String IGN_ON_ODO		= "ign.on.odo";
    public final static String SUM_PLS			= "sum.pls";

    /////////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////// IAPDeclaration SETTINGS /////////////////////////////////////

    public final static String IAP_FETCH_MASS_FROM_SCALES  		= "mass.scales";
    public final static String IAP_ALLOW_MANUAL_MASS_ENTRY 		= "mass.manual";
    public final static String IAP_ONTRACK_DISABLED			    = "ontrack.disabled";
    public final static String IAP_COMPANY_USERNAME        		= "username";
    public final static String IAP_SDID_URL                		= "sdid.url";
    public final static String IAP_ONTRACK_URL             		= "declarationOnTrackUrl";
    public final static String IAP_REPEATED_ALERT_TIME_MINS		= "repeated.alert.time";
    public final static String IAP_ALERT_TIME_AFTER_BOOT_MINS 	= "after.boot.alert.time";
    public final static String IAP_ALERT_ENABLED		   		= "alert.enabled";
    public final static String IAP_LAST_DEC_TIME		   		= "last.dec.time";
    public final static String IAP_ALERT_FIRST_TIME_AFTER_HRS	= "first.time.alert.after";

    /////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////// MassManagement SETTINGS /////////////////////////////////////

    public final static String MM_AVAILABLE_UNITS  			= "mm.available.units";
    public final static String MM_SHOW_LOCATIONS_BUTTONS	= "mm.show.locations.buttons";
    public final static String MM_MAX_HISTORY_RECORDS		= "mm.max.history.records";
    public final static String MM_FETCH_MASS_FROM_SCALES    = "mm.mass.scales";
    public final static String MM_ALLOW_MANUAL_MASS_ENTRY 	= "mm.mass.manual";

    /////////////////////////////// IAPDeclaration SETTINGS /////////////////////////////////////


    /////////////////////////////// Sentinel SETTINGS /////////////////////////////////////

    public final static String SENTINEL_COMPANY_CODE            = "company.code";
    public final static String SENTINEL_DRIVER_RULESETS         = "drive.rulesets";
    public final static String SENTINEL_MOVEMENT_ALERT_INTERVAL = "alert.interval";
    public final static String SENTINEL_SERVICE_URL     	    = "service.url";
    public final static String SENTINEL_CHECKPOINT_URL          = "checkpoint.url";
    public final static String SENTINEL_CHECKPOINT_INTERVAL     = "checkpoint.interval";
    public final static String SENTINEL_SYNC_INTERVAL           = "sync.interval";
    public final static String SENTINEL_CHECKPOINT_PORT         = "checkpoint.port";
    public final static String SENTINEL_ENABLED_FOR_DRIVER      = "sentinel.enabled.for.driver";

    /////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////// Messaging SETTINGS /////////////////////////////////////

    public final static String MESSAGING_CLEANUP_DAYS_THRESHOLD  = "purge.threshold.date";
    public final static String MESSAGING_CLEANUP_COUNT_THRESHOLD = "purge.threshold.count";
    public final static String MESSAGING_ONTRACK_DISABLED		 = "ontrack.disabled";

    public final static String MESSAGING_SERVICE_URL             = "service.url";
    public final static String MESSAGING_LISTENER_PORT           = "service.port";
    public final static String MESSAGING_BACKEND_URL             = "backend.url";
    public final static String MESSAGING_BACKEND_PORT     	     = "backend.port";

    /////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////// SpeedAssist SETTINGS /////////////////////////////////////

    public final static String SPEEDASSIST_PREDEFINED_SPEEDS      = "speed.predefined";
    public final static String SPEEDASSIST_ACTIVATE_SPEED_ASSIST  = "activate_speed_assist";
    public final static String SPEEDASSIST_PRIORITISED_APPS       = "apps.prioritised";
    public final static String SPEEDASSIST_IS_DEMO     	          = "demo.mode";
    public final static String SPEEDASSIST_DISABLE_SIGNPOSTED     = "signposted.disable";
    public final static String SPEEDASSIST_NEVER_START            = "never_start";
    public final static String SPEEDASSIST_DEMO_SPEED_SAMPLE      = "demo.samples";
    public final static String SPEEDASSIST_IDLE_TIME              = "idle.time";
    public final static String SPEEDASSIST_SPEED_LIMIT_OFFSET     = "speed.offset";
    public final static String SPEEDASSIST_OVER_SPEED_TIME_FRAME  = "overspeed.delta";
    public final static String SPEEDASSIST_GPS_REGAIN_ALERT       = "alert.gps";

    /////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////// SpeedEvents SETTINGS /////////////////////////////////////

    public final static String SPEEDEVENT_LEADIN_TIME     	      = "speedevent.leadin";
    public final static String SPEEDEVENT_LEADOUT_TIME            = "speedevent.leadout";
    public final static String SPEEDEVENT_IDLE_TIME               = "se_idle_time";
    public final static String SPEEDEVENT_EVENT_TIMEOUT           = "speedevent.timeout";
    public final static String SPEEDEVENT_ACTIVATE_SPEED_EVENTS   = "activate_speed_events";
    public final static String SPEEDEVENT_SPEED_LIMIT             = "speedevent.threshold";
    public final static String SPEEDEVENT_SPEED_LIMIT_OFFSET      = "speedevent.offset";
    public final static String SPEEDEVENT_OVER_SPEED_TIME_FRAME   = "speedevent.duration";
    public final static String SPEEDEVENT_DEMO_LIMIT 			  = "speedevent.demo";
    public final static String SPEEDEVENT_DISABLE_SIGNPOSTED      = "signposted.disable";
    public final static String SPEEDEVENT_IGNORE_POWER		      = "speedevent.ignore.power";

    /////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////// QuickJobs SETTINGS /////////////////////////////////////

    public final static String QUICKJOBS_SHOW_DELAY       = "show.delay";
    public final static String QUICKJOBS_SHOW_PICKUP      = "show.pickup";
    public final static String QUICKJOBS_SHOW_DELIVERY    = "show.delivery";
    public final static String QUICKJOBS_SHOW_ALL_JOBS    = "show.jobs";
    public final static String QUICKJOBS_ATTRIBUTE_LIST   = "attributes";
    public final static String QUICKJOBS_CURRENT_TRIP_ID  = "current.trip.id";

    /////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////// SmartJobs SETTINGS /////////////////////////////////////

    public final static String SMARTJOBS_REASONS_LIST               = "reasons_list";
    public final static String SMARTJOBS_OVERSTAY_DURATION          = "overstay.duration";
    public final static String SMARTJOBS_STALE_DATA_THRESHOLD       = "stale.data.threshold";
    public final static String SMARTJOBS_WORKFLOW_IDS               = "workflow.ids";
    public final static String SMARTJOBS_PICKUP_VARIANCE_CODES      = "variance.pickup";
    public final static String SMARTJOBS_LOAD_VARIANCE_CODES        = "variance.load";
    public final static String SMARTJOBS_VARIANCE_DELAY_PICKUP      = "variance.pickup.delay";
    public final static String SMARTJOBS_VARIANCE_DELAY_DELIVERY    = "variance.delivery.delay";
    public final static String SMARTJOBS_DELIVERY_VARIANCE_CODES    = "variance.delivery";
    public final static String SMARTJOBS_PICKUP_CANCEL_VARIANCE_CODES      = "variance.pickup.cancellation";
    public final static String SMARTJOBS_RUNSHEET_CANCEL_CODES      = "variance.runsheet.reject";
    public final static String SMARTJOBS_LOAD_CANCEL_VARIANCE_CODES        = "variance.load.cancellation";
    public final static String SMARTJOBS_DELIVERY_CANCEL_VARIANCE_CODES    = "variance.delivery.cancellation";
    public final static String SMARTJOBS_IMAGE_SIZE    				= "image.size";
    public final static String SMARTJOBS_IMAGE_QUALITY    			= "image.quality";


    /////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////// Pretrip SETTINGS /////////////////////////////////////

    public final static String PRETRIP_CHECKLIST_IDS       = "checklist.ids";

    /////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////// AppManager SETTINGS /////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////// OTHER CONSTANTS ///////////////////////////////////////

    public final static String ACTION_COMMON_SETTINGS_CHANGED    = "transtech.af.android.settings.action.changed";
    public final static String EXTRA_COMMON_SETTINGS_OLD_RECORD  = "transtech.af.android.settings.extra.oldrecord";
    public final static String EXTRA_COMMON_SETTINGS_NEW_RECORD  = "transtech.af.android.settings.extra.newrecord";

    public final static String DISABLED  = "disabled";

    ////////////////////////////////////////////////////////////////////////////////////////

}
