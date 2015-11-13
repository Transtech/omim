#pragma once

#include "std/array.hpp"
#include "std/string.hpp"

// This file is autogenerated while exporting sounds.csv from the google table.
// It contains the list of languages which can be used by TTS.
// It shall be included to Android(jni) and iOS part to get the languages list.

namespace routing
{
namespace turns
{
namespace sound
{
array<pair<string, string>, 28> const kLanguageList =
{{
  {"en", "English"},
  {"ru", "Русский"},
  {"es", "Español"},
  {"de", "Deutsch"},
  {"fr", "Français"},
  {"zh-Hant", "中文繁體"},
  {"zh-Hans", "中文简体"},
  {"pt", "Português"},
  {"th", "ภาษาไทย"},
  {"tr", "Türkçe"},
  {"ar", "العربية"},
  {"cs", "Čeština"},
  {"da", "Dansk"},
  {"el", "Ελληνικά"},
  {"fi", "Suomi"},
  {"hi", "हिंदी"},
  {"hr", "Hrvatski"},
  {"hu", "Magyar"},
  {"id", "Indonesia"},
  {"it", "Italiano"},
  {"ja", "日本語"},
  {"ko", "한국어"},
  {"nl", "Nederlands"},
  {"pl", "Polski"},
  {"ro", "Română"},
  {"sk", "Slovenčina"},
  {"sv", "Svenska"},
  {"sw", "Kiswahili"}
}};
}  // namespace sound
}  // namespace turns
}  // namespace routing

