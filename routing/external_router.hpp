#pragma once

#include "routing/router.hpp"
#include "routing/routing_mapping.hpp"

namespace routing
{

class ExternalRouter : public IRouter
{
public:
    ExternalRouter(IRouter * router, Index * index, TCountryFileFn const & countryFileFn);

    virtual string GetName() const override;

    ResultCode CalculateRoute(m2::PointD const & startPoint, m2::PointD const & startDirection,
                              m2::PointD const & finalPoint, RouterDelegate const & delegate,
                              Route & route) override;

    virtual void ClearState() override;

private:
    IRouter * m_realRouter;
    Index const * m_pIndex;
    RoutingIndexManager m_indexManager;
};

}
