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

protected:
  
  bool SearchOnline(string const & query, string & result);
  void EmitFromJson(char const * jsonStr);

  Emitter m_emitter;
};
}  // namespace search
