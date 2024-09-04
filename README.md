# Installation instructions

## macOS

### CLI

There are several ways to build APODesktop.  The simplest is by running `make`

1. simply run

```sh
$ make install
```

You can also build APODesktop with Xcode at your peril

1.

```sh
$ xcodebuild -scheme APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build
```

or

```sh
$ xcodebuild -target APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build
```

Now your wallpaper will be automatically updated every morning at 10:00AM!

You can also manually update your wallpaper at any time by running `apodesktop`

### Xcode

1. Click run button
2. Profit???

### Uninstallation

1. run

```sh
$ cd macOS/
$ make uninstall
```

2. Unprofit?

## Windows

1. Open Developer Powershell (Powershell with `msbuild`)
2. run

```pwsh
$ sl windows\APODesktop
$ msbuild .
$ .\scheduleApodDaily.ps1
```

3. Profit?

### Uninstallation

1. Open Task Scheduler
2. On the left-hand sidebar click "Task Scheduler Library"
3. Right-click "APOD-Update Wallpaper daily"
4. Click "delete"

## GNU+Linux

1. simply run

```sh
$ cd gnu+X+linux/
$ make install
```

2. Profit?

### Uninstallation

1. simply run

```sh
$ cd gnu+X+linux/
$ make uninstall
```

2. Unprofit?

## TODO

- detect when the user plugs in a new monitor and update the wallpaper
