build/apodesktop: APODesktop/APOD.swift APODesktop/concurrentSequence.swift
	mkdir -p build
	swiftc -g -Onone APODesktop/* -o build/apodesktop

# NOTE: if you change this, you may also need to update `PATH` in
# `launchCtlSchedule.plist`
PREFIX ?= /usr/local
$(PREFIX)/bin/apodesktop: build/apodesktop
	mkdir -pv $(PREFIX)/bin
	cp ./build/apodesktop $(PREFIX)/bin

LAUNCHAGENTS=~/Library/LaunchAgents
PLIST=./launchCtlSchedule.plist
install: build/apodesktop $(PREFIX)/bin/apodesktop

	launchctl unload -w $(LAUNCHAGENTS)/$(PLIST)
	install -d ~/Library/LaunchAgents
	install $(PLIST) ~/Library/LaunchAgents
	launchctl load -w $(LAUNCHAGENTS)/$(PLIST)
	launchctl kickstart gui/501/com.vegerot.apodesktop

uninstall:
	launchctl unload -w $(LAUNCHAGENTS)/$(PLIST)
	rm -f $(LAUNCHAGENTS)/$(PLIST)
	rm -f $(PREFIX)/bin/apodesktop

clean:
	rm -rf ./build

xcode: APODesktop/APOD.swift APODesktop/concurrentSequence.swift
	xcodebuild -scheme APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build

debug: APODesktop/APOD.swift APODesktop/concurrentSequence.swift
	xcodebuild -scheme APODesktop -project APODesktop.xcodeproj -configuration Debug CONFIGURATION_BUILD_DIR=./build

