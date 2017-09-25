#pragma once

#include "processor.hpp"

namespace search
{
class ProcessorOnline : public Processor
{
public:

  ProcessorOnline(Index const & index, CategoriesHolder const & categories,
            vector<Suggest> const & suggests, storage::CountryInfoGetter const & infoGetter);

  void Init(bool viewportSearch);
  void SetQuery(string const & query);
  inline void SetOnResults(SearchParams::TOnResults const & onResults) { m_onResults = onResults; }

  inline bool IsEmptyQuery() const { return (m_prefix.empty() && m_tokens.empty()); }
  void Search(SearchParams const & params, m2::RectD const & viewport);

  // Tries to generate a (lat, lon) result from |m_query|.
  void SearchCoordinates();

  void InitParams(QueryParams & params) {};
  void InitEmitter();
  
  void ConfigureOnlineSearch(string url, string apiKey) {
    m_url = url;
    m_apiKey = apiKey;
  }

protected:
  
  bool SearchOnline(string const & query, string & result);
  void EmitFromJson(char const * jsonStr);

  Emitter m_emitter;

private:
  string m_url;
  string m_apiKey;

  string m_lastQuery;
  string m_lastHttpResult;
};
}  // namespace search
