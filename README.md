# Apna TV Open

An Android TV / Google TV M3U playlist player starter project.

## Current MVP
- Android TV launcher entry and remote-friendly focusable controls
- Load a remote M3U/M3U8 playlist over HTTP(S)
- Parse channel names and `group-title` metadata
- Search channel names and groups
- Play compatible HTTP(S) streams using AndroidX Media3 ExoPlayer
- Remember the last playlist URL on the device
- GitHub Actions workflow to build a debug APK

## Build an APK
Open the repository in Android Studio with JDK 17 and Android SDK 35 installed, then build the `app` configuration. Or open the **Actions** tab, select **Android APK**, and run the workflow manually. A successful workflow uploads `apna-tv-debug-apk` as an artifact.

## Install
Download `app-debug.apk` from the workflow artifact, transfer it to your Android TV, and install it. You may need to allow installation from the file manager used to open the APK.

## Current limitations
This first MVP supports remote HTTP(S) M3U playlists. Local file import, Xtream Codes, Stalker Portal, EPG, favorites, parental controls, and DRM-protected streams are not implemented yet. Playback depends on the stream's codec and provider restrictions.

Only use playlists and streams you are authorized to access.
