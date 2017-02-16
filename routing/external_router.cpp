
#include "routing/route.hpp"
#include "routing/external_router.hpp"

namespace
{
//double constexpr kMwmLoadedProgress = 10.0f;
//double constexpr kPointsFoundProgress = 15.0f;
//double constexpr kPathFoundProgress = 70.0f;
} //  namespace

#define INTERRUPT_WHEN_CANCELLED(DELEGATE) \
  do                               \
  {                                \
    if (DELEGATE.IsCancelled())    \
      return IRouter::ResultCode::Cancelled;            \
  } while (false)


namespace routing
{

ExternalRouter::ExternalRouter(IRouter * router, Index * index, TCountryFileFn const & countryFileFn) :
    m_realRouter(router)
//  , m_pIndex(index)
  , m_indexManager(countryFileFn, *index)
{

}

string ExternalRouter::GetName() const
{
    return m_realRouter->GetName();
}

IRouter::ResultCode ExternalRouter::CalculateRoute(m2::PointD const & startPoint, m2::PointD const & startDirection,
                          m2::PointD const & finalPoint, RouterDelegate const & delegate,
                          Route & route)
{
    return m_realRouter->CalculateRoute(startPoint, startDirection, finalPoint, delegate, route);
}

void ExternalRouter::ClearState()
{
    m_realRouter->ClearState();
}

}
