#include <iostream>
#include "testing/testing.hpp"

#include "generator/generator_tests_support/test_feature.hpp"

#include "search/viewport_search_callback.hpp"
#include "search/mode.hpp"
#include "search/search_integration_tests/helpers.hpp"
#include "search/search_tests_support/test_results_matching.hpp"
#include "search/search_tests_support/test_search_request.hpp"

#include "base/macros.hpp"

using namespace generator::tests_support;
using namespace search::tests_support;

namespace search
{
    namespace
    {
        class TestDelegate : public ViewportSearchCallback::Delegate
        {
        public:
            TestDelegate(bool & mode) : m_mode(mode) {}
            
            // ViewportSearchCallback::Delegate overrides:
            void RunUITask(function<void()> /* fn */) override {}
            void SetHotelDisplacementMode() override { m_mode = true; }
            bool IsViewportSearchActive() const override { return true; }
            void ShowViewportSearchResults(Results const & /* results */) override {}
            void ClearViewportSearchResults() override {}
            
        private:
            bool & m_mode;
        };
        
        class SearchRequest : public TestDelegate, public TestSearchRequest
        {
        public:
            SearchRequest(TestSearchEngine & engine, string const & query,
                                     m2::RectD const & viewport, bool & mode)
            : TestDelegate(mode)
            , TestSearchRequest(engine, query, "en" /* locale */, Mode::Viewport, viewport)
            {
                SetCustomOnResults(
                                   ViewportSearchCallback(static_cast<ViewportSearchCallback::Delegate &>(*this),
                                                          bind(&SearchRequest::OnResults, this, _1)));
            }
        };
        
        class OnlineSearchTest : public SearchTest
        {
        };
        
        double const kDX[] = {-0.01, 0, 0, 0.01};
        double const kDY[] = {0, -0.01, 0.01, 0};
        
        static_assert(ARRAY_SIZE(kDX) == ARRAY_SIZE(kDY), "Wrong deltas lists");
        
        UNIT_CLASS_TEST(OnlineSearchTest, Smoke)
        {
            {
                bool mode = false;
                SearchRequest request(m_engine, "transtech, melb", m2::RectD(m2::PointD(-1.5, -1.5), m2::PointD(-0.5, -0.5)), mode);
                request.Run();
                      
                cout << "result: " << request.Results().size() << endl;
                for (auto &r: request.Results()) {
                    cout << r.GetString() << "|" << r.GetAddress() << "|" << r.GetFeatureCenter() << endl;
                }
            }
          
        }
    }  // namespace
}  // namespace search
