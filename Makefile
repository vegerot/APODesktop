build/APODesktop: APODesktop/APOD.swift APODesktop/concurrentSequence.swift
	xcodebuild -scheme APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build

clean:
	rm -rf ./build

PREFIX ?= /usr/local
install: build/APODesktop
	cp ./build/APODesktop $(PREFIX)/bin

.PHONY: always
always:
	xcodebuild -scheme APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build
