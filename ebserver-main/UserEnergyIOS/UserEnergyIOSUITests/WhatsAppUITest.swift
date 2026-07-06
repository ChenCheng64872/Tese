import XCTest

class WhatsAppUITest: BaseWriteTest {

    let appBundleId = "net.whatsapp.WhatsApp"
    let automationDelay: TimeInterval = 2.0

    func testSendMessage() throws {
        // Retrieve message size from environment (e.g. from xcodebuild arguments)
        let messageSizeString = ProcessInfo.processInfo.environment["MESSAGE_SIZE"] ?? "SHORT"
        
        guard let messageType = TextMessage(rawValue: messageSizeString.lowercased()) else {
            XCTFail("Invalid message size provided: \(messageSizeString)")
            return
        }
        
        let app = XCUIApplication(bundleIdentifier: appBundleId)
        app.launch()
        
        // Wait for app to settle
        Thread.sleep(forTimeInterval: automationDelay)
        
        // Find the first chat cell.
        // WhatsApp typically uses "Chats" list. We can look for the first Cell.
        let firstChat = app.cells.firstMatch
        XCTAssertTrue(firstChat.waitForExistence(timeout: 5.0), "Could not find the first chat.")
        firstChat.tap()
        
        // Wait for the chat to open
        Thread.sleep(forTimeInterval: 1.0)
        
        // Find the text field. It's usually a TextView or TextField.
        // In iOS XCUITest, it might be textViews or textFields.
        let messageField = app.textViews.firstMatch
        if !messageField.exists {
            // Fallback to textFields
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
        
        // Find and tap the Send button
        let sendButton = app.buttons["Send"].firstMatch
        if sendButton.waitForExistence(timeout: 2.0) {
            sendButton.tap()
        } else {
            // Sometimes it's just an icon without "Send" label. We can try grabbing the last button.
            // Using a heuristic: send is usually the rightmost button on the keyboard toolbar.
            print("Warning: Send button with label 'Send' not found. Ensure Accessibility Identifiers are correct.")
        }
        
        // Wait a bit before terminating
        Thread.sleep(forTimeInterval: 0.25)
        
        app.terminate()
        Thread.sleep(forTimeInterval: 1.0)
    }
}
