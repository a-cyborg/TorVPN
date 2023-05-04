# TorVPN

*WARNING* This is experimental software, do not rely on it for anything other than testing and development. It may leak information and should not be relied on for anything sensitive!

## Install a debug version from CI

A debug version of the app is built in the project's CI, which you can install and run on your device.

### Download the latest CI artifact
If you go to the [pipelines](https://gitlab.torproject.org/tpo/applications/vpn/-/pipelines) page, look for the most recent successful pipeline, and then on the right side you will find a `Download artifacts` button. Click that to download the `artifacts.zip` file.

You will then need to unzip this artifacts.zip file on your computer. The android package is `outputs/apk/debug/app-debug.apk`. 

### Install adb
In order to install this unsigned package, you will need to have the command-line tool `adb` installed on your computer. On a Debian, or Debian-derived machine, you can `sudo apt install adb` to install it. If using something else, [follow a tutorial](https://www.xda-developers.com/install-adb-windows-macos-linux/) to get it installed.

### Enable Developer mode
On your Android device, you will need to have `Developer Mode` enabled. To do this, you have to follow a little bit of a secret pathway:

1. Open Settings on your phone
2. Then go to `About phone` (near the bottom of the list)
4. Tap `Build number` (bottom of list) *seven* times in a row.
5. When you have finished you will see a message saying it is enabled.
6. Now go back to the main Settings screen and you should see a new Developer options menu. On Google Pixel phones and some other devices, you might need to navigate to Settings > System to find the Developer options menu.
7. Go in there and enable the USB debugging option.

### Connect to your phone
Attach your phone to your computer via a USB cable. 

You should get a pop-up on your phone asking you if you want the device to connect. You will need to accept it (possibly a few times, unless you tell it to remember your device).

Then go to a shell and type `adb devices`, you should see something similar to the following:

```
List of devices attached
1e778e25        device
```

The number on the left will be unique to your device.

### Install the package
While in the directory that you unzipped the archive, install the application with `adb install app-debug.apk`.

If this does not work, you may need to enable installation of apps from 'unknown sources' (unsigned apps):
`adb shell settings put secure install_non_market_apps 1`

Once you have installed the application, you should be able to find it in your normal list of applications.

## Build and run a debug version
To build the app the Android SDK is required and the command line tool ADB is recommended.

For now the build and installation steps are as easy as:
1. Get the repository:
```
    git clone https://gitlab.torproject.org/tpo/applications/vpn.git`
    cd vpn
```
2. Build the app: `./gradlew assembleDebug`
3. Install it on your phone: `adb -d install -t app/build/outputs/apk/debug/app-debug.apk`

Have fun!
