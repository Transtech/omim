
#include "Router.hpp"

#include "base/logging.hpp"
#include "routing/route.hpp"
#include "../core/jni_helper.hpp"

namespace
{
double constexpr kMwmLoadedProgress = 10.0f;
double constexpr kPointsFoundProgress = 15.0f;
double constexpr kPathFoundProgress = 70.0f;

jclass routeClass;
jfieldID fPathId;
jfieldID fTimesId;
jfieldID fTurnsId;

jclass posClass;
jfieldID fLat;
jfieldID fLng;

jclass turnClass;
jfieldID fIndex;
jfieldID fDirection;
jfieldID fExitNum;
jfieldID fSource;
jfieldID fTarget;
jfieldID fKeep;

jclass timeClass; //not used yet
jfieldID fIndex2;
jfieldID fTime;

} //  namespace


namespace routing
{

Router::Router(jobject obj)
{
    JNIEnv * env = jni::GetEnv();
    ASSERT( env, () );

    m_self = env->NewGlobalRef(obj);
    ASSERT( m_self, () );

    //now cache/lookup needed classes etc...
    jclass localRef = env->FindClass( "com/mapswithme/maps/routing/Route" );
    ASSERT(localRef, (jni::DescribeException()));
    routeClass = (jclass) env->NewGlobalRef(localRef);
    env->DeleteLocalRef(localRef);

    localRef = env->FindClass( "com/mapswithme/maps/routing/Route$Position" );
    ASSERT(localRef, (jni::DescribeException()));
    posClass = (jclass) env->NewGlobalRef(localRef);

    localRef = env->FindClass( "com/mapswithme/maps/routing/Route$TurnItem" );
    ASSERT(localRef, (jni::DescribeException()));
    turnClass = (jclass) env->NewGlobalRef(localRef);

    localRef = env->FindClass( "com/mapswithme/maps/routing/Route$TimeItem" );
    ASSERT(localRef, (jni::DescribeException()));
    timeClass = (jclass) env->NewGlobalRef(localRef);

    fPathId = env->GetFieldID(routeClass, "path", "[Lcom/mapswithme/maps/routing/Route$Position;");
    ASSERT(fPathId, (jni::DescribeException()));
    fTimesId = env->GetFieldID(routeClass, "times", "[Lcom/mapswithme/maps/routing/Route$TimeItem;");
    ASSERT(fTimesId, (jni::DescribeException()));
    fTurnsId = env->GetFieldID(routeClass, "turns", "[Lcom/mapswithme/maps/routing/Route$TurnItem;");
    ASSERT(fTurnsId, (jni::DescribeException()));

    fLat = env->GetFieldID(posClass, "lat", "D");
    ASSERT(fLat, (jni::DescribeException()));
    fLng = env->GetFieldID(posClass, "lng", "D");
    ASSERT(fLng, (jni::DescribeException()));

    fIndex = env->GetFieldID(turnClass, "index", "I");
    ASSERT(fIndex, (jni::DescribeException()));
    fDirection = env->GetFieldID(turnClass, "direction", "I");
    ASSERT(fDirection, (jni::DescribeException()));
    fExitNum = env->GetFieldID(turnClass, "exitNum", "I");
    ASSERT(fExitNum, (jni::DescribeException()));
    fSource = env->GetFieldID(turnClass, "sourceName", "Ljava/lang/String;");
    ASSERT(fSource, (jni::DescribeException()));
    fTarget = env->GetFieldID(turnClass, "targetName", "Ljava/lang/String;");
    ASSERT(fTarget, (jni::DescribeException()));
    fKeep = env->GetFieldID(turnClass, "keepAnyway", "Z");
    ASSERT(fKeep, (jni::DescribeException()));

    fIndex2 = env->GetFieldID(timeClass, "index", "I");
    ASSERT(fIndex2, (jni::DescribeException()));
    fTime = env->GetFieldID(timeClass, "time", "D");
    ASSERT(fTime, (jni::DescribeException()));
}

Router::~Router()
{
    JNIEnv * env = jni::GetEnv();
    ASSERT ( env, () );

    env->DeleteGlobalRef(routeClass);
    env->DeleteGlobalRef(posClass);
    env->DeleteGlobalRef(turnClass);
    env->DeleteGlobalRef(timeClass);

    env->DeleteGlobalRef(m_self);
}

string Router::GetName() const
{
    JNIEnv * env = jni::GetEnv();
    ASSERT ( env, () );

    ASSERT(m_self != nullptr, ());
    jclass const k = env->GetObjectClass(m_self);
    jmethodID methodID = env->GetMethodID(k, "getName", "()Ljava/lang/String;");
    ASSERT(m_self != nullptr, ());
    ASSERT(methodID != nullptr, ());

    string res;

    jstring name = (jstring)env->CallObjectMethod(m_self, methodID);
    ASSERT(name, ("getName() returned NULL"));

    char const * nameUtf8 = env->GetStringUTFChars(name, 0);
    if (nameUtf8 != 0)
    {
        res = nameUtf8;
        env->ReleaseStringUTFChars(name, nameUtf8);
    }
    return res;
}

void Router::ClearState()
{
    JNIEnv * env = jni::GetEnv();
    ASSERT ( env, () );

    ASSERT(m_self != nullptr, ());
    jclass const k = env->GetObjectClass(m_self);
    jmethodID methodID = env->GetMethodID(k, "clearState", "()V");
    ASSERT(m_self != nullptr, ());
    ASSERT(methodID != nullptr, ());

    env->CallVoidMethod(m_self, methodID);
}

IRouter::ResultCode Router::CalculateRoute(m2::PointD const & startPoint,
                                  m2::PointD const & startDirection,
                                  m2::PointD const & finalPoint, RouterDelegate const & delegate,
                                  Route & route)
{
//        int calculateRoute(double startLat, double startLon, double finishLat, double finishLon);

    JNIEnv * env = jni::GetEnv();
    ASSERT( env, ());
    ASSERT(m_self != nullptr, ());

    jclass const k = env->GetObjectClass(m_self);
    jmethodID calcMethodID = env->GetMethodID(k, "calculateRoute", "(DDDD)Lcom/mapswithme/maps/routing/Route;");
    ASSERT(calcMethodID, (jni::DescribeException()));

//  typedef vector<turns::TurnItem> TTurns;
//  typedef pair<uint32_t, double> TTimeItem;
//  typedef vector<TTimeItem> TTimes;
    Route::TTurns turnsDir;
    Route::TTimes times;
    vector<m2::PointD> points;

    //TODO: add delegate progress calls: eg. delegate.OnProgress(kPathFoundProgress);
    LOG(LDEBUG,("JNI Router callback CalculateRoute(",startPoint.x, ",", startPoint.y, ",", finalPoint.x, ",", finalPoint.y, ")" ));
    jobject jRoute = env->CallObjectMethod(m_self, calcMethodID, startPoint.x, startPoint.y, finalPoint.x, finalPoint.y);
    ASSERT(jRoute, (jni::DescribeException()));

    LOG(LDEBUG,("JNI Router callback CalculateRoute - result 1" ));
    jobjectArray jPositionArr = (jobjectArray) env->GetObjectField(jRoute, fPathId);
    ASSERT(jPositionArr, (jni::DescribeException()));

    LOG(LDEBUG,("JNI Router callback CalculateRoute - result 2" ));
    jobjectArray jTurnsArr = (jobjectArray) env->GetObjectField(jRoute, fTurnsId);
    ASSERT(jTurnsArr, (jni::DescribeException()));

    LOG(LDEBUG,("JNI Router callback CalculateRoute - result 3" ));
    jobjectArray jTimesArr = (jobjectArray) env->GetObjectField(jRoute, fTimesId);
    ASSERT(jTimesArr, (jni::DescribeException()));

    if( jPositionArr != nullptr )
    {
        //Extract path from route.path -> points
        int len = env->GetArrayLength(jPositionArr);
        LOG(LDEBUG,("JNI Router callback CalculateRoute - result 4 - there are", len, " position objects" ));
        for( int i = 0; i < len; ++i )
        {
            jobject jPosObj = env->GetObjectArrayElement(jPositionArr, i);
            ASSERT(jPosObj, (jni::DescribeException()));
            double lat = env->GetDoubleField(jPosObj, fLat);
            double lng = env->GetDoubleField(jPosObj, fLng);
            env->DeleteLocalRef(jPosObj);

//            LOG(LDEBUG,("JNI Router callback CalculateRoute - position object", i, " at (", x, ",", y, ")" ));
            points.push_back(MercatorBounds::FromLatLon(lat,lng));
        }
    }

    if( jTurnsArr != nullptr )
    {
        //Extract turns from route.turns -> turnsDir
        int len = env->GetArrayLength(jTurnsArr);
        LOG(LDEBUG,("JNI Router callback CalculateRoute - result 5 - there are", len, " turn objects" ));
        for( int i = 0; i < len; ++i )
        {
            jobject jTurnObj = env->GetObjectArrayElement(jTurnsArr, i);
            ASSERT(jTurnObj, (jni::DescribeException()));

            turns::TurnItem ti;
            ti.m_index = env->GetIntField(jTurnObj, fIndex);
            ti.m_turn = static_cast<turns::TurnDirection>(env->GetIntField(jTurnObj, fDirection));
            //NB: not doing lane info
            ti.m_exitNum = env->GetIntField(jTurnObj, fExitNum);
            ti.m_sourceName = jni::ToNativeString(env, (jstring) env->GetObjectField(jTurnObj, fSource));
            ti.m_targetName = jni::ToNativeString(env, (jstring) env->GetObjectField(jTurnObj, fTarget));
            ti.m_keepAnyway = env->GetBooleanField(jTurnObj, fKeep);
            env->DeleteLocalRef(jTurnObj);

            turnsDir.push_back( ti );
        }
    }

    if( jTimesArr != nullptr )
    {
        //Extract turns from route.turns -> turnsDir
        int len = env->GetArrayLength(jTimesArr);
        LOG(LDEBUG,("JNI Router callback CalculateRoute - result 6 - there are", len, "time objects" ));
        for( int i = 0; i < len; ++i )
        {
            jobject jTimeObj = env->GetObjectArrayElement(jTimesArr, i);
            ASSERT(jTimeObj, (jni::DescribeException()));

            int idx = env->GetIntField(jTimeObj, fIndex2);
            double time = env->GetIntField(jTimeObj, fTime);
            env->DeleteLocalRef(jTimeObj);

            times.push_back( Route::TTimeItem(idx, time) );
        }
    }

    LOG(LDEBUG,("JNI Router callback - all data extracted OK" ));
    route.SetGeometry(points.begin(), points.end());
    route.SetTurnInstructions(turnsDir);
    route.SetSectionTimes(times);

    env->DeleteLocalRef(jPositionArr);
    env->DeleteLocalRef(jTimesArr);
    env->DeleteLocalRef(jTurnsArr);

    return ResultCode::NoError;
}


}
