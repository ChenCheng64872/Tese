import XCTest

class TikTokUITest: BaseWriteTest {

    let appBundleId = "com.zhiliaoapp.musically"
    let automationDelay: TimeInterval = 2.0

    func testRuntest() throws {
        let env = ProcessInfo.processInfo.environment
        let watchDurationMs = Double(env["DURATION_MS"] ?? "120000") ?? 120000.0
        let autoScroll = (env["AUTOSCROLL"] ?? "true").lowercased() == "true"
        let scrollEveryMs = Double(env["AUTOSCROLL_INTERVAL_MS"] ?? "15000") ?? 15000.0
        
        let app = XCUIApplication(bundleIdentifier: appBundleId)
        app.launch()
        
        Thread.sleep(forTimeInterval: automationDelay)
        
        let startTime = Date().timeIntervalSince1970
        let endTime = startTime + (watchDurationMs / 1000.0)
        let midPointTime = startTime + (watchDurationMs / 1000.0) / 2.0
        
        var lastScrollTime = startTime
        var hasPaused = false
        var hasResumed = false
        
        while Date().timeIntervalSince1970 < endTime {
            let currentTime = Date().timeIntervalSince1970
            
            // Handle AutoScroll
            if autoScroll && (currentTime - lastScrollTime) * 1000.0 >= scrollEveryMs {
                app.swipeUp()
                lastScrollTime = currentTime
            }
            
            // Mid-run Pause
            if !hasPaused && currentTime >= midPointTime {
                let center = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
                center.tap()
                hasPaused = true
            }
            
            // Mid-run Resume (pause for half the scroll interval)
            if hasPaused && !hasResumed && (currentTime - midPointTime) * 1000.0 >= (scrollEveryMs / 2.0) {
                let center = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
                center.tap()
                hasResumed = true
            }
            
            Thread.sleep(forTimeInterval: 0.5)
        }
        
        app.terminate()
        Thread.sleep(forTimeInterval: 1.0)
    }
}
