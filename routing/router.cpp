#include "router.hpp"

namespace routing
{

string ToString(RouterType type)
{
  switch(type)
  {
  case RouterType::Vehicle: return "Vehicle";
  case RouterType::Pedestrian: return "Pedestrian";
  case RouterType::Bicycle: return "Bicycle";
  case RouterType::Taxi: return "Taxi";
  case RouterType::External: return "External";
  }
  ASSERT(false, ());
  return "Error";
}

RouterType FromString(string const & str)
{
  if (str == "vehicle")
    return RouterType::Vehicle;
  if (str == "pedestrian")
    return RouterType::Pedestrian;
  if (str == "bicycle")
    return RouterType::Bicycle;
  if (str == "taxi")
    return RouterType::Taxi;
  if (str == "external")
    return RouterType::External;

  ASSERT(false, ("Incorrect routing string:", str));
  return RouterType::Vehicle;
}
} //  namespace routing
