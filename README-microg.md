# Purpose of this fork

Currently, many contact-tracing applications rely on the Google-Apple Exposure
Notification framework, known as GAEN. On Android, GAEN is part of the Google
Play services, in order to integrate with the OS at a low level.  As a
consequence, if a phone runs an OS that does not have Google Play services
installed, contact-tracing applications will not work. This is the case for
example for:

* Commercial OSes that do not (or cannot) come with Google Play services
  installed (e.g. Huawei).
* Open Source OSes (e.g. LineageOS).

The purpose of this fork is to remedy the situation by creating a version of
the DP3T-SDK that does not depend on the GAEN libraries for Exposure
Notification.

The primary user of this library is the SwissCovid application, but other
applications that use the DP3T-SDK should be easily adaptable, and the approach
is applicable as well to applications that do not use the DP3T-SDK but
perhaps access the GAEN libraries directly.

# Approach

After examining various possibilities, we have settled to base our approach on
the [microG](https://microg.org/) project.  Indeed, microG aims to provide a
free re-implementation of the complete Google Play services, in a way that
makes it completely transparent to the applications using them.  In order to
achieve this, microG uses some clever tricks which make its installation
non-trivial for end-users.  Furthermore, for our project, we only need the
subset of microG that re-implements the GAEN functionality.

Consequently, we are reusing the relevant parts of microG related to GAEN, but
bundling them as libraries with the app instead of having them installed as a
separate set of services.  This keeps the goal of requiring very minimal
modifications on the final application: indeed, the source code remains the
same since the GAEN API is preserved, and only build-related changes are needed
to ensure that the GAEN calls are handled by the libraries instead of the
Google Play services.

# Implementation

These are the changes that were done to the original DP3T-SDK in order to use microG as libraries:

* Copy the base modules from microG and the ones related to GAEN:
    * play-services-base
    * play-services-base-api
    * play-services-base-core
    * play-services-basement
    * play-services-tasks
    * play-services-nearby
    * play-services-nearby-api
    * play-services-nearby-core
    * play-services-nearby-core-proto

  These are included as new modules in the DP3T-SDK Android Studio project, and
  will each produce their separate library (`*.aar`) during the build.

* Make some minor modifications to the microG code allowing it to handle calls
  originating from the same process.

* Modify the build files (`build.gradle`, `settings.gradle`) replacing the
  dependencies on Google Play services with dependencies on the included microG
  modules.

The changes required on the Calibration app in order to use the modified SDK are:

* Modify the build files (`build.gradle`, `settings.gradle`) adding the
  dependencies on the included microG modules.

* Require the `ACCESS_FINE_LOCATION` and `ACCESS_BACKGROUND_LOCATION`
  permissions in the manifest file. These are needed in order to have access to
  BLE scanning.

* Add some code at the startup of the main activity to request those
  permissions from the user.

* Declare the main activity as receiver of the `EXPOSURE_NOTIFICATION_SETTINGS`
  intent in the manifest file. This is required to satisfy the way the SDK
  checks whether GAEN is available.

And finally, the changes required on the SwissCovid app in order to use the modified SDK are:

* Modify the build files (`build.gradle`) to take the SDK libraries locally
  instead of the official ones from Maven, as well as to add some more required
  dependencies.

* Require the `ACCESS_FINE_LOCATION` and `ACCESS_BACKGROUND_LOCATION`
  permissions in the manifest file. These are needed in order to have access to
  BLE scanning.

* Add some code at the startup of the main activity to request those
  permissions from the user.

* Add a dummy activity and declare it as receiver of the
  `EXPOSURE_NOTIFICATION_SETTINGS` intent in the manifest file. This is
  required to satisfy the way the SDK checks whether GAEN is available.

# Build

Automatically built releases of the SDK and SwissCovid app are available on
GitHub, respectively at:

* https://github.com/c4dt/dp3t-sdk-android/releases
* https://github.com/c4dt/dp3t-app-android-ch/releases

In order to build the SDK and the SwissCovid application manually, follow these
steps (these are the same as the workflows for GitHub actions):

* Install the latest version of the Android SDK (FIXME: or is the JDK only needed?).

* Checkout the `microg-nearby` branch of the SDK and SwissCovid app forks:
```
$ git clone -b microg-nearby git@github.com:c4dt/dp3t-sdk-android.git
$ git clone -b microg-nearby git@github.com:c4dt/dp3t-app-android-ch.git
```

* Build the SDK:
```
$ cd dp3t-sdk-android/dp3t-sdk
$ ./gradlew assembleRelease
$ cd ..
```

* Make a zip archive containing the SDK libraries:
```
$ zip --junk-paths sdk-libs $( find . -name 'play-*.aar' -o -name 'sdk-production*.aar' -o -name 'play-*.jar'
```

* Build the app:
```
$ cd ../dp3t-app-android-ch

# Uncompress the SDK archive
$ rm -f ./app/libs/*
$ unzip ../dp3t-sdk-android/sdk-libs.zip -d ./app/libs/

# For the following step, you need to use your keystore file location and
# passwords, or create a new one. Further information is available at
# https://developer.android.com/studio/publish/app-signing .
# Otherwise, you can use the `assembleProdDebug` target instead to build a
# debug app.
$ ./gradlew assembleProdRelease -PkeystoreFile=<keystoreFile> -PkeystorePassword=<keystorePassword> -PkeyAliasPassword=<keyAliasPassword>
```

If all proceeds without errors, the final APK can then be found at `./app/build/outputs/apk/prod/release/app-prod-release.apk`.

# Debug

In order to verify the proper execution of the application, the following can be useful:

* Connect two (or more) phones to your development machine (ensure they are
  recognized by `adb devices`).
* Install the application on all the phones.
* Capture the logs of each phone:
```
$ adb -s <phone1_device> logcat --format color | tee -a phone1.logcat
$ adb -s <phone2_device> logcat --format color | tee -a phone2.logcat
...
```
* Monitor the exposure notification events between the phones:
```
$ tail -F *logcat | awk '/^==>/ {filename=$2; next} {print filename ":" $0}' | grep ExposureNotification:
```

# Notes

To update the relevant microG libraries:

```
for m in play-services-{base,base-api,base-core,basement,nearby,nearby-api,nearby-core,nearby-core-proto,tasks}
do
    echo $m
    rm -rf ./dp3t-sdk/$m
    cp -r ../android_packages_apps_GmsCore/$m ./dp3t-sdk/
    echo "/build" > ./dp3t-sdk/$m/.gitignore
done
```

To build for a phone with an OS that has Play Services, the declaration of the
`com.google.android.gms.nearby.exposurenotification.EXPOSURE_CALLBACK`
permission must be removed, because it is already declared in GMS:

```
$ sed -i '/<permission/,+3d' dp3t-sdk/play-services-nearby-core/src/main/AndroidManifest.xml
```
