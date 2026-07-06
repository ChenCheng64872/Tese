import XCTest

class InstagramUITest: BaseWriteTest {

    let appBundleId = "com.burbn.instagram"
    let automationDelay: TimeInterval = 2.0

    func testRuntest() throws {
        let env = ProcessInfo.processInfo.environment
        let watchDurationMs = Double(env["DURATION_MS"] ?? "30000") ?? 30000.0
        let scrollEveryMs = Double(env["SCROLL_INTERVAL_MS"] ?? "5000") ?? 5000.0
        let likeEveryMs = Double(env["LIKE_INTERVAL_MS"] ?? "10000") ?? 10000.0
        
        let app = XCUIApplication(bundleIdentifier: appBundleId)
        app.launch()
        
        Thread.sleep(forTimeInterval: automationDelay)
        
        let startTime = Date().timeIntervalSince1970
        let endTime = startTime + (watchDurationMs / 1000.0)
        
        var lastScrollTime = startTime
        var lastLikeTime = startTime
        
        // Event loop
        while Date().timeIntervalSince1970 < endTime {
            let currentTime = Date().timeIntervalSince1970
            
            if (currentTime - lastScrollTime) * 1000.0 >= scrollEveryMs {
                // Scroll up (swipe down on the screen to scroll feed down)
                app.swipeUp()
                lastScrollTime = currentTime
            }
            
            if (currentTime - lastLikeTime) * 1000.0 >= likeEveryMs {
                // Like by double tapping the center of the feed
                let center = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
                center.doubleTap()
                lastLikeTime = currentTime
            }
            
            Thread.sleep(forTimeInterval: 0.5) // Small sleep to prevent tight loop
        }
        
        app.terminate()
        Thread.sleep(forTimeInterval: 1.0)
    }
}
