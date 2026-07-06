require 'xcodeproj'

project_path = 'UserEnergyIOS/UserEnergyIOS.xcodeproj'
project = Xcodeproj::Project.open(project_path)

target = project.targets.find { |t| t.name == 'UserEnergyIOSUITests' }
unless target
  puts "Could not find target UserEnergyIOSUITests"
  exit 1
end

group = project.main_group.find_subpath(File.join('UserEnergyIOSUITests'), true)

files = [
  'BaseWriteTest.swift', 'InstagramUITest.swift', 'SignalUITest.swift',
  'TelegramUITest.swift', 'TextMessage.swift', 'TikTokUITest.swift',
  'WhatsAppUITest.swift', 'YouTubeUITest.swift'
]

files.each do |file|
  unless group.files.any? { |f| f.path == file }
    file_ref = group.new_reference(file)
    target.add_file_references([file_ref])
    puts "Added #{file}"
  end
end

project.save
puts "Successfully added files to Xcode project via Ruby!"
