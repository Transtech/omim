# Map library tests.

TARGET = search_online_tests
CONFIG += console warn_on
CONFIG -= app_bundle
TEMPLATE = app

ROOT_DIR = ../..

DEPENDENCIES = generator_tests_support search_tests_support indexer_tests_support generator \
               routing search storage stats_client indexer platform editor geometry coding base \
               tess2 protobuf jansson succinct pugixml opening_hours

include($$ROOT_DIR/common.pri)

QT *= core

macx-* {
  QT *= gui widgets # needed for QApplication with event loop, to test async events (downloader, etc.)
  LIBS *= "-framework IOKit" "-framework QuartzCore" "-framework Cocoa" "-framework SystemConfiguration"
}
win32*|linux* {
  QT *= network
}

SOURCES += \
    ../../testing/testingmain.cpp \
    helpers.cpp \
    online_search_test.cpp \

HEADERS += \
    helpers.hpp \
