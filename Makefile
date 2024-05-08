build/apodesktop: APODesktop/APOD.swift APODesktop/concurrentSequence.swift
	mkdir -p build
	swiftc APODesktop/* -o build/apodesktop

PREFIX ?= /usr/local
$(PREFIX)/bin/apodesktop: build/apodesktop
	cp ./build/apodesktop $(PREFIX)/bin

LAUNCHAGENTS=~/Library/LaunchAgents
PLIST=./launchCtlSchedule.plist
install: build/apodesktop $(PREFIX)/bin/apodesktop
	launchctl unload -w $(LAUNCHAGENTS)/$(PLIST)
	install $(PLIST) ~/Library/LaunchAgents
	launchctl load -w $(LAUNCHAGENTS)/$(PLIST)
	$(PREFIX)/bin/apodesktop

clean:
	rm -rf ./build

xcode: APODesktop/APOD.swift APODesktop/concurrentSequence.swift
	xcodebuild -scheme APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build

debug: APODesktop/APOD.swift APODesktop/concurrentSequence.swift
	xcodebuild -scheme APODesktop -project APODesktop.xcodeproj -configuration Debug CONFIGURATION_BUILD_DIR=./build

