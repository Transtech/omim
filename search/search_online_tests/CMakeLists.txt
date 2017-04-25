project(search_online_tests)

set(
  SRC
  helpers.cpp
  helpers.hpp
  online_search_test.cpp
)

omim_add_test(${PROJECT_NAME} ${SRC})

omim_link_libraries(
  ${PROJECT_NAME}
  generator_tests_support
  search_tests_support
  indexer_tests_support
  generator
  routing
  search
  storage
  stats_client
  indexer
  editor
  platform
  geometry
  coding
  base
  oauthcpp
  tess2
  protobuf
  jansson
  succinct
  pugixml
  opening_hours
  ${Qt5Network_LIBRARIES}
  ${LIBZ}
)

link_qt5_core(${PROJECT_NAME})
