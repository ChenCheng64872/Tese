import XCTest

class YouTubeUITest: BaseWriteTest {

    let appBundleId = "com.google.ios.youtube"
    let automationDelay: TimeInterval = 2.0

    func testPlayVideo() throws {
        // Read environment variables (default to 30s)
        let durationStr = ProcessInfo.processInfo.environment["DURATION"] ?? "S30"
        let durationMs = durationStringToMs(durationStr)
        
        let app = XCUIApplication(bundleIdentifier: appBundleId)
        app.launch()
        
        Thread.sleep(forTimeInterval: automationDelay)
        
        // Ensure playing: we'll try to tap the center of the screen, or look for a video cell.
        // In YouTube iOS, the first cell is usually an auto-playing video or a thumbnail.
        let firstVideo = app.cells.firstMatch
        if firstVideo.waitForExistence(timeout: 5.0) {
            firstVideo.tap()
        } else {
            // Fallback tap center
            let coordinate = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
            coordinate.tap()
        }
        
        // Wait for buffer
        Thread.sleep(forTimeInterval: 2.0)
        
        // Try to skip forward (scrubbing). On iOS, scrubbing the player is tricky without exact IDs.
        // We'll attempt to tap the center to wake controls, then tap the right side to skip forward 10s.
        let rightSide = app.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.5))
        rightSide.doubleTap() // double tap skips 10s on YouTube
        
        // Sleep for the requested duration to measure power
        print("Playing YouTube video for \(durationMs) ms...")
        Thread.sleep(forTimeInterval: TimeInterval(durationMs) / 1000.0)
        
        // Cleanup
        app.terminate()
        Thread.sleep(forTimeInterval: 1.0)
    }
    
    private func durationStringToMs(_ d: String) -> Int {
        switch d.uppercased() {
        case "S15": return 15000
        case "S30": return 30000
        case "S60": return 60000
        default: return 30000
        }
    }
}
