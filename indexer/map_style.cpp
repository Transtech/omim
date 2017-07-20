#include "indexer/map_style.hpp"

#include "base/assert.hpp"

MapStyle const kDefaultMapStyle = MapStyleClear;

MapStyle MapStyleFromSettings(std::string const & str)
{
  // MapStyleMerged is service style. It's unavailable for users.
  if (str == "MapStyleClear")
    return MapStyleClear;
  else if (str == "MapStyleDark")
    return MapStyleDark;

  else if (str == "MapStyleClearBD")
    return MapStyleClearBD;
  else if (str == "MapStyleDarkBD")
    return MapStyleDarkBD;

  else if (str == "MapStyleClearCrane")
    return MapStyleClearCrane;
  else if (str == "MapStyleDarkCrane")
    return MapStyleDarkCrane;

  return kDefaultMapStyle;
}

std::string MapStyleToString(MapStyle mapStyle)
{
  switch (mapStyle)
  {
  case MapStyleDark:
    return "MapStyleDark";
  case MapStyleClear:
    return "MapStyleClear";
  case MapStyleMerged:
    return "MapStyleMerged";

  case MapStyleClearBD:
      return "MapStyleClearBD";
  case MapStyleDarkBD:
      return "MapStyleDarkBD";

  case MapStyleClearCrane:
      return "MapStyleClearCrane";
  case MapStyleDarkCrane:
      return "MapStyleDarkCrane";

  case MapStyleCount:
    break;
  }
  ASSERT(false, ());
  return std::string();
}

std::string DebugPrint(MapStyle mapStyle)
{
  return MapStyleToString(mapStyle);
}
