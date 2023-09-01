# Installation instructions

## macOS

### CLI

There are several ways to build APODesktop.  The simplest is by running `make`

1. simply run

```sh
make install
```

You can also build APODesktop with Xcode at your peril

1.

```sh
xcodebuild -scheme APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build
```

or

```sh
xcodebuild -target APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build
```

Now your wallpaper will be automatically updated every morning at 10:00AM!

You can also manually update your wallpaper at any time by running `apodesktop`

### Xcode

1. Click run button
2. Profit???

## other operating systems

lul stop being poor
