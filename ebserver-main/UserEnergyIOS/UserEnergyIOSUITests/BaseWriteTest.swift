import XCTest

class BaseWriteTest: XCTestCase {
    
    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.
        
        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }
    
    /// Helper function to type text in a more controlled manner if standard typeText() is too fast.
    func typeTextSlowly(_ text: String, into element: XCUIElement, delay: TimeInterval = 0.05) {
        element.tap()
        for char in text {
            element.typeText(String(char))
            Thread.sleep(forTimeInterval: delay)
        }
    }
}
