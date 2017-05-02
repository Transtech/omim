#include "processor_online.hpp"
#include "search/latlon_match.hpp"
#include "geometry/mercator.hpp"
#include "platform/http_client.hpp"
#include "coding/url_encode.hpp"

#include "base/assert.hpp"
#include "base/logging.hpp"
#include "base/macros.hpp"
#include "base/scope_guard.hpp"
#include "base/stl_add.hpp"
#include "base/stl_helpers.hpp"
#include "base/string_utils.hpp"

#include "std/algorithm.hpp"
#include "std/function.hpp"
#include "std/iterator.hpp"
#include "std/limits.hpp"

#include "3party/jansson/myjansson.hpp"

namespace search
{
ProcessorOnline::ProcessorOnline(Index const & index, CategoriesHolder const & categories,
                     vector<Suggest> const & suggests,
                     storage::CountryInfoGetter const & infoGetter)
  : Processor(index, categories, suggests, infoGetter)
{
}

void ProcessorOnline::Init(bool viewportSearch)
{
  m_tokens.clear();
  m_prefix.clear();
}

void ProcessorOnline::SetQuery(string const & query)
{
  m_query = query;
}

void ProcessorOnline::Search(SearchParams const & params, m2::RectD const & viewport)
{
  SetQuery(params.m_query);
  SetOnResults(params.m_onResults);
  
  InitEmitter();
  if (m_query.size() < 3) {
    m_emitter.Finish(true);
    return;
  }

  try
  {
    if (params.m_onStarted)
      params.m_onStarted();

    SearchCoordinates();

    string httpResult;
    SearchOnline(m_query, httpResult);
    
    if (!httpResult.empty()) {
      EmitFromJson(httpResult.c_str());
    }
  }
  catch (CancelException const &)
  {
    LOG(LDEBUG, ("Search has been cancelled."));
  }

  m_emitter.Finish(IsCancelled());
}

void ProcessorOnline::SearchCoordinates()
{
  double lat, lon;
  if (!MatchLatLonDegree(m_query, lat, lon))
    return;
  
  m_emitter.AddResultNoChecks(m_ranker.MakeResult(PreResult2(lat, lon)));
  m_emitter.Emit();
}
  
bool ProcessorOnline::SearchOnline(string const & query, string & result)
{
  LOG(LDEBUG, ("SearchOnline: " + query));
  platform::HttpClient request(m_url + UrlEncode(query));
    
  if (!m_apiKey.empty())
    request.SetRawHeader("Authorization", "Token token=\"" + m_apiKey + "\"");
  
  if (request.RunHttpRequest() && !request.WasRedirected() && request.ErrorCode() == 200) {
    result = request.ServerResponse();
    return true;
  }
  else {
      m_emitter.SetError(request.ErrorCode());
  }
  
  return false;
}
  
void ProcessorOnline::EmitFromJson(char const * jsonStr)
{
  try
  {
    my::Json root(jsonStr);
    if (json_is_array(root.get())) {
      size_t const size = json_array_size(root.get());
      if (size > 0) {
        for (size_t i = 0; i < size; i++) {
          json_t* obj = json_array_get(root.get(), i);
        
          string name;
          my::FromJSONObjectOptionalField(obj, "name", name);
          
          string addr_number, addr_street, addr_suburb, addr_state, addr_postcode, addr_country;

          json_t * addressObj = json_object_get(obj, "address");
          if (addressObj != NULL) {
            my::FromJSONObjectOptionalField(addressObj, "number", addr_number);
            my::FromJSONObjectOptionalField(addressObj, "street", addr_street);
            my::FromJSONObjectOptionalField(addressObj, "suburb", addr_suburb);
            my::FromJSONObjectOptionalField(addressObj, "state", addr_state);
            my::FromJSONObjectOptionalField(addressObj, "postcode", addr_postcode);
            my::FromJSONObjectOptionalField(addressObj, "country", addr_country);
          }
          
          string address = addr_number + ", " + addr_street + ", " + addr_suburb;
          
          double lat = 0.0, lng = 0.0;

          json_t * gpsObj = json_object_get(obj, "GPS");
          if (gpsObj != NULL) {
            my::FromJSONObject(gpsObj, "Lat", lat);
            my::FromJSONObject(gpsObj, "Lng", lng);
          }

          m_emitter.AddResultNoChecks(Result(MercatorBounds::FromLatLon(lat, lng), name, address));
        }

        m_emitter.Emit();
      }
      else {
        LOG(LINFO, (string("Result is empty: ") + jsonStr));
      }
    }
    else {
      LOG(LINFO, (string("Result is not an array: ") + jsonStr));
    }
  }
  catch (my::Json::Exception const & e)
  {
    LOG(LWARNING, (e.Msg()));
  }
}

void ProcessorOnline::InitEmitter() { m_emitter.Init(m_onResults); }

}  // namespace search
