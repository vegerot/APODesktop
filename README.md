# Installation instructions

## macOS

### CLI

There are several ways to build APODesktop.  The simplest is by running `make`

1. simply run

```sh
$ make install
```

Now your wallpaper will be automatically updated every morning at 10:00AM!

The service also runs as a daemon that monitors for display changes. When you plug in or unplug a monitor, the wallpaper will be updated automatically.

You can also manually update your wallpaper at any time by running `apodesktop`

<details>
    <summary>You can also build APODesktop with Xcode at your peril</summary>

1.

```sh
$ cd macOS/
$ xcodebuild -scheme APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build
```

or

```sh
$ cd macOS/
$ xcodebuild -target APODesktop -project APODesktop.xcodeproj -configuration Release CONFIGURATION_BUILD_DIR=./build
```

### Xcode

1. Click run button
2. Profit???
</details>



### Uninstallation

1. run

```sh
$ cd macOS/
$ make uninstall
```

2. Unprofit?

## GNU+Linux

1. simply run

```sh
$ make install
```

The service runs as a daemon that monitors for display changes. When you plug in or unplug a monitor, the wallpaper will be updated automatically.

2. Profit?

### Uninstallation

1. simply run

```sh
$ cd gnu+X+linux/
$ make uninstall
```

2. Unprofit?

## Windows

1. Open Developer Powershell (Powershell with `msbuild`)
2. run

```pwsh
> sl windows\APODesktop
> msbuild .
> .\scheduleApodDaily.ps1
```

The scheduled task runs the application in daemon mode, which monitors for display changes. When you plug in or unplug a monitor, the wallpaper will be updated automatically.

3. Profit?

### Uninstallation

1. Open Task Scheduler
2. On the left-hand sidebar click "Task Scheduler Library"
3. Right-click "APOD-Update Wallpaper daily"
4. Click "delete"

## TODO

- ~~detect when the user plugs in a new monitor and update the wallpaper~~ ✅ Implemented!
