package com.mapswithme.maps.routing;

import android.app.Activity;
import android.util.Log;

/**
 * Class GraphHopperRouter
 * <p/>
 * Created by agough on 30/03/16 9:51 AM
 */
public class GraphHopperRouter implements IRouter
{
    private static final String TAG = "Maps_GraphHopperRouter";

    private static final String ACTION_ROUTE_REQUEST = "transtech.AF.Android.route.ACTION_ROUTE_REQUEST";
    private static final String ACTION_ROUTE_RESPONSE = "transtech.AF.Android.route.ACTION_ROUTE_RESPONSE";

    private Activity context;

    public GraphHopperRouter( Activity activity )
    {
        this.context = activity;
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

    @Override
    public Route calculateRoute( double startLat, double startLon, double finishLat, double finishLon )
    {
        Log.i( TAG, "Received routing request starting from (" + startLat + ", " + startLon + ") to (" + finishLat + ", " + finishLon + ")" );

//        Intent intent = new Intent( ACTION_ROUTE_REQUEST );
//        intent.putExtra( "startLat", startLat );
//        intent.putExtra( "startLng", startLon );
//        intent.putExtra( "finishLat", startLat );
//        intent.putExtra( "finishLng", startLon );
//
//        context.startService( intent );

        Route result = new Route();
        //NB: All field in the route object MUST be non-null as the JNI layer expects it!!

        result.path = new Route.Position[40];
        int i = 0;
        result.path[i++] = new Route.Position(-37.84685802059995, 145.06428146800118);
        result.path[i++] = new Route.Position(-37.84754994170852,145.07047001429953);
        result.path[i++] = new Route.Position(-37.84853546727901,145.07950700997205);
        result.path[i++] = new Route.Position(-37.8499631848178,145.09152237530475);
        result.path[i++] = new Route.Position(-37.85024090521576,145.09403359355107);
        result.path[i++] = new Route.Position(-37.8503867503342,145.09561125402774);
        result.path[i++] = new Route.Position(-37.850672666371004,145.09821020286256);
        result.path[i++] = new Route.Position(-37.850810688379646,145.0988889507701);
        result.path[i++] = new Route.Position(-37.85081962907656,145.0992031790138);
        result.path[i++] = new Route.Position(-37.85076300466276,145.10001845881385);
        result.path[i++] = new Route.Position(-37.850663911938604,145.1007627718321);
        result.path[i++] = new Route.Position(-37.850548055407735,145.1020256452715);
        result.path[i++] = new Route.Position(-37.850348379843275,145.10391511255312);
        result.path[i++] = new Route.Position(-37.84980523250562,145.106983075447);
        result.path[i++] = new Route.Position(-37.84973799101423,145.10745395215125);
        result.path[i++] = new Route.Position(-37.849642437315936,145.10840781275348);
        result.path[i++] = new Route.Position(-37.8496254872447,145.1087320992812);
        result.path[i++] = new Route.Position(-37.8496001552701,145.10972544796152);
        result.path[i++] = new Route.Position(-37.849610213554136,145.11013839640032);
        result.path[i++] = new Route.Position(-37.84964802525151,145.11109765867363);
        result.path[i++] = new Route.Position(-37.849734265723846,145.11189040046685);
        result.path[i++] = new Route.Position(-37.8499348726109,145.11364612982376);
        result.path[i++] = new Route.Position(-37.85070749783607,145.1199321848139);
        result.path[i++] = new Route.Position(-37.851019118376506,145.1227721599364);
        result.path[i++] = new Route.Position(-37.85160529281807,145.1277415110411);
        result.path[i++] = new Route.Position(-37.85211845156815,145.13235323426917);
        result.path[i++] = new Route.Position(-37.85231738207454,145.13435241135252);
        result.path[i++] = new Route.Position(-37.85233004806183,145.13489909771604);
        result.path[i++] = new Route.Position(-37.85223188666028,145.13564285194076);
        result.path[i++] = new Route.Position(-37.85215868470427,145.13599787211413);
        result.path[i++] = new Route.Position(-37.851636585257275,145.13799351017164);
        result.path[i++] = new Route.Position(-37.85142666514427,145.13887510014047);
        result.path[i++] = new Route.Position(-37.85134079720097,145.1394961060471);
        result.path[i++] = new Route.Position(-37.851316210284445,145.1397656308062);
        result.path[i++] = new Route.Position(-37.85130615200042,145.14010239705672);
        result.path[i++] = new Route.Position(-37.851305779471375,145.1404576034946);
        result.path[i++] = new Route.Position(-37.85136855061431,145.14118794667397);
        result.path[i++] = new Route.Position(-37.851555373926956,145.14278088084117);
        result.path[i++] = new Route.Position(-37.851989183991904,145.1461418378236);
        result.path[i++] = new Route.Position(-37.85199653956342,145.14619992094944);

        result.turns = new Route.TurnItem[2];
        result.turns[0] = new Route.TurnItem();
        result.turns[0].index = 0;
        result.turns[0].direction = RoutingInfo.VehicleTurnDirection.GO_STRAIGHT.ordinal();
        result.turns[0].sourceName = "Toorak Road1";
        result.turns[0].targetName = "Toorak Road2";

        result.turns[1] = new Route.TurnItem();
        result.turns[1].index = 39;
        result.turns[1].direction = RoutingInfo.VehicleTurnDirection.REACHED_YOUR_DESTINATION.ordinal();
        result.turns[1].sourceName = "Burwood Highway 1";
        result.turns[1].targetName = "Burwood Highway 2";

        result.times = new Route.TimeItem[2];
        result.times[0] = new Route.TimeItem();
        result.times[0].index = 0;
        result.times[0].time = 0.0;

        result.times[1] = new Route.TimeItem();
        result.times[1].index = 39;
        result.times[1].time = 2616.0;

        Log.i( TAG, "Responding with prepackaged route (" + result.path.length + " path items) and (" + result.turns.length + " turns)" );
        return result;
    }
}
