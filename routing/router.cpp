#include "router.hpp"

namespace routing
{

string ToString(RouterType type)
{
  switch(type)
  {
    case RouterType::Vehicle: return "Vehicle";
    case RouterType::Pedestrian: return "Pedestrian";
    case RouterType::Truck: return "Truck";
  }
  ASSERT(false, ());
  return "Error";
}

} //  namespace routing
