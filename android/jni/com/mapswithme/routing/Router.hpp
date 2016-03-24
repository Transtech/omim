#pragma once

#include "../routing/router.hpp"

namespace routing
{

class Router : public IRouter
{
private:
    jobject m_self;

public:

    Router();
    virtual ~Router();

    string GetName() const;
    void ClearState();
    ResultCode CalculateRoute(m2::PointD const & startPoint,
                              m2::PointD const & startDirection,
                              m2::PointD const & finalPoint, RouterDelegate const & delegate,
                              Route & route);
};

}
