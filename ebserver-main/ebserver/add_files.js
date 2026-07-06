const xcode = require('xcode');
const fs = require('fs');

const projectPath = '../UserEnergyIOS/UserEnergyIOS.xcodeproj/project.pbxproj';
const myProj = xcode.project(projectPath);

myProj.parse(function (err) {
    if (err) {
        console.error('Error parsing project:', err);
        process.exit(1);
    }
    
    const uiTestTargetName = 'UserEnergyIOSUITests';
    const uiTestTarget = myProj.pbxTargetByName(uiTestTargetName);
    
    if (!uiTestTarget) {
        console.error('Target not found:', uiTestTargetName);
        process.exit(1);
    }

    const filesToAdd = [
        'BaseWriteTest.swift',
        'InstagramUITest.swift',
        'SignalUITest.swift',
        'TelegramUITest.swift',
        'TextMessage.swift',
        'TikTokUITest.swift',
        'WhatsAppUITest.swift',
        'YouTubeUITest.swift'
    ];

    filesToAdd.forEach(file => {
        myProj.addSourceFile(`UserEnergyIOSUITests/${file}`, { target: uiTestTarget.uuid });
    });
    
    fs.writeFileSync(projectPath, myProj.writeSync());
    console.log('Successfully added swift files to the UserEnergyIOS.xcodeproj.');
});
