# Installation instructions

## macOS

### CLI

There are several ways to build APODesktop.  The simplest is by running `make`

1. simply run

```sh
$ make install
```

Now your wallpaper will be automatically updated every morning at 10:00AM!

You can also manually update your wallpaper at any time by running `apodesktop`

The macOS program uses [NASA's APOD API](https://science.nasa.gov/wp-json/wp/v2/apod-basic)
without an API key. It requests one recent entry per connected screen plus two
spare entries, skips non-images and entries without an image URL, and assigns
successfully downloaded images newest first. Screens without an available image
keep their current wallpaper.

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

2. Profit?

A systemd user timer updates the wallpaper daily once your graphical session
has started. Linux uses the same keyless NASA API, requests one recent entry per
monitor plus two spares (some entries are videos), and gives the newest image to
the primary monitor. On GNOME it combines one image per monitor into a single
spanned wallpaper in `~/.local/share/apodesktop/`. In other X sessions (such as
i3) it uses `feh`. It needs `python3`, Pillow, and `xrandr` (plus `feh` outside
GNOME).

### Uninstallation

1. simply run

```sh
$ cd gnu+X+linux/
$ make uninstall
```

2. Unprofit?

## Windows

1. Install the .NET 10 SDK and open PowerShell in the repository.
2. Run:

```pwsh
> .\windows\APODesktop\scheduleApodDaily.ps1
```

The script builds a Release version in `%LOCALAPPDATA%\Programs\APODesktop`
and registers a daily wallpaper update at noon. Run the installed
`APODesktop.exe` to update immediately. Like macOS, Windows uses NASA's
keyless APOD API, assigns images newest first, and skips failed downloads
and non-images.

### Uninstallation

1. Open Task Scheduler
2. On the left-hand sidebar click "Task Scheduler Library"
3. Right-click "APOD-Update Wallpaper daily"
4. Click "delete"

## Android

1. Build and install the app on your Android device or emulator:

```sh
$ make android-install
```

Alternatively, open the `android/` folder in Android Studio, build, and run the app.

2. Launch the APODesktop application on your device and turn on the "Enable Daily Wallpaper" switch to schedule the daily APOD wallpaper worker.

Android uses the same keyless NASA API. It requests six recent entries, skips
videos and entries without an image URL, and uses the newest image for the home
screen and the preceding image for the lock screen. The preview displays the
newest image and converts NASA's HTML explanation to readable text.

### Uninstallation

1. Uninstall the APODesktop app from your device.

## TODO

- detect when the user plugs in a new monitor and update the wallpaper
