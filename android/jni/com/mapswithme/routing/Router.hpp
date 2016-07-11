#pragma once

#include <jni.h>

#include "routing/router.hpp"

namespace routing
{

class Router : public routing::IRouter
{
private:
    jobject m_self;

public:

    Router( jobject self );
    virtual ~Router();

    string GetName() const;
    void ClearState();
    ResultCode CalculateRoute(m2::PointD const & startPoint,
                              m2::PointD const & startDirection,
                              m2::PointD const & finalPoint, RouterDelegate const & delegate,
                              Route & route);
};

}
