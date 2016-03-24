
#include "routing/route.hpp"
#include "routing/external_router.hpp"

namespace
{
double constexpr kMwmLoadedProgress = 10.0f;
double constexpr kPointsFoundProgress = 15.0f;
double constexpr kPathFoundProgress = 70.0f;
} //  namespace

#define INTERRUPT_WHEN_CANCELLED(DELEGATE) \
  do                               \
  {                                \
    if (DELEGATE.IsCancelled())    \
      return Cancelled;            \
  } while (false)


namespace routing
{

ExternalRouter::ExternalRouter(IRouter * router, Index * index, TCountryFileFn const & countryFileFn) :
    m_realRouter(router), m_pIndex(index), m_indexManager(countryFileFn, *index)
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
/*
    my::HighResTimer timer(true);

    TRoutingMappingPtr startMapping = m_indexManager.GetMappingByPoint(startPoint);
    TRoutingMappingPtr targetMapping = m_indexManager.GetMappingByPoint(finalPoint);

    if (!startMapping->IsValid())
    {
      IRouter::ResultCode const code = startMapping->GetError();
      if (code != IRouter::ResultCode::NoError)
      {
        string const name = startMapping->GetCountryName();
        if (name.empty())
          return IRouter::ResultCode::StartPointNotFound;
        route.AddAbsentCountry(name);
        return code;
      }
      return IRouter::ResultCode::StartPointNotFound;
    }

    if (!targetMapping->IsValid())
    {
      IRouter::ResultCode const code = targetMapping->GetError();
      if (code != IRouter::ResultCode::NoError)
      {
        string const name = targetMapping->GetCountryName();
        if (name.empty())
          return IRouter::ResultCode::EndPointNotFound;
        route.AddAbsentCountry(name);
        return code;
      }
      return IRouter::ResultCode::EndPointNotFound;
    }

    MappingGuard startMappingGuard(startMapping);
    MappingGuard finalMappingGuard(targetMapping);
    UNUSED_VALUE(startMappingGuard);
    UNUSED_VALUE(finalMappingGuard);
    LOG(LINFO, ("Duration of the MWM loading", timer.ElapsedNano()));
    timer.Reset();

    delegate.OnProgress(kMwmLoadedProgress);

    LOG(LINFO, ("Duration of the start/stop points lookup", timer.ElapsedNano()));
    timer.Reset();

    // Manually load facade to avoid unmaping files we routing on.
    startMapping->LoadFacade();

    INTERRUPT_WHEN_CANCELLED(delegate);

    //ITS: Send routing request...

    // 5. Restore route.
    Route::TTurns turnsDir;
    Route::TTimes times;
    vector<m2::PointD> points;

    if (DoExternalRouting(startMapping, delegate, points, turnsDir, times) != NoError)
    {
      LOG(LWARNING, ("External routing request failed!"));
      return IRouter::ResultCode::RouteNotFound;
    }
    route.SetGeometry(points.begin(), points.end());
    route.SetTurnInstructions(turnsDir);
    route.SetSectionTimes(times);

    return IRouter::ResultCode::NoError;
  */
    return m_realRouter->CalculateRoute(startPoint, startDirection, finalPoint, delegate, route);
}

void ExternalRouter::ClearState()
{
    m_realRouter->ClearState();
}

}
