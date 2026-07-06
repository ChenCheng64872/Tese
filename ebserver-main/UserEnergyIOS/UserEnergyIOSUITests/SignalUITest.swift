import XCTest

class SignalUITest: BaseWriteTest {

    let appBundleId = "org.whispersystems.signal"
    let automationDelay: TimeInterval = 2.0

    func testSendMessage() throws {
        let messageSizeString = ProcessInfo.processInfo.environment["MESSAGE_SIZE"] ?? "SHORT"
        
        guard let messageType = TextMessage(rawValue: messageSizeString.lowercased()) else {
            XCTFail("Invalid message size provided: \(messageSizeString)")
            return
        }
        
        let app = XCUIApplication(bundleIdentifier: appBundleId)
        app.launch()
        
        Thread.sleep(forTimeInterval: automationDelay)
        
        // Find the first chat cell.
        let firstChat = app.cells.firstMatch
        XCTAssertTrue(firstChat.waitForExistence(timeout: 5.0), "Could not find the first chat.")
        firstChat.tap()
        
        Thread.sleep(forTimeInterval: 1.0)
        
        // Find the message entry field.
        let messageField = app.textViews.firstMatch
        if !messageField.exists {
            let altMessageField = app.textFields.firstMatch
            if altMessageField.exists {
                altMessageField.tap()
                altMessageField.typeText(messageType.rawValue)
            } else {
                XCTFail("Could not find message input field.")
            }
        } else {
            messageField.tap()
            messageField.typeText(messageType.rawValue)
        }
        
        // Find the send button.
        let sendButton = app.buttons["Send"].firstMatch
        if sendButton.waitForExistence(timeout: 2.0) {
            sendButton.tap()
        }
        
        Thread.sleep(forTimeInterval: 0.25)
        
        app.terminate()
        Thread.sleep(forTimeInterval: 1.0)
    }
}
