
#include "Router.hpp"
#include "../core/jni_helper.h"

namespace routing
{

Router::Router(jobject obj)
{
    JNIEnv * env = jni::GetEnv();
    ASSERT ( env, () );

    m_self = env->NewGlobalRef(obj);
    ASSERT ( m_self, () );
}

Router::~Router()
{
    JNIEnv * env = jni::GetEnv();
    ASSERT ( env, () );

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

void ClearState()
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

ResultCode CalculateRoute(m2::PointD const & startPoint,
                          m2::PointD const & startDirection,
                          m2::PointD const & finalPoint, RouterDelegate const & delegate,
                          Route & route)
{
    return routing::ResultCode::InternalError;
}


}
