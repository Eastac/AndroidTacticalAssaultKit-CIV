# PluginTemplate: Build, Package, Transfer, Install, and Use (Step-by-step)

This guide explains how to:
1. Build/package the plugin APK.
2. Move it to another ATAK device.
3. Install and load it in ATAK.
4. Use the heading toggle features added by this plugin.

---

## 1) What this plugin does

The plugin UI provides:
- A **Use XML/GPS heading only** switch (controls `useOnlyGPSBearing`).
- Automatic protection against magnetic mode when enabling (switches from `MAGNETIC_UP` to `TRACK_UP` if needed).
- A **Restore previous ATAK heading values** button that restores the previous `useOnlyGPSBearing` and `compass_mapmode` values.

---

## 2) Prerequisites on your build machine

- Android SDK installed.
- Java version compatible with this Gradle/AGP setup (JDK 11 is commonly used for AGP 4.2.x projects).
- Access to the TAK dev repo/artifacts if your environment requires it.
- A `local.properties` file in `plugin-examples/plugintemplate/`.

### 2.1 Create `local.properties`

From the template in this repo:

```bash
cd plugin-examples/plugintemplate
cp template.local.properties local.properties
```

Then edit `local.properties` and set at least:

- `sdk.dir`
- `takrepo.url`
- `takrepo.user`
- `takrepo.password`
- `takdev.plugin` (if using local takdev jar)
- signing keys (required by this project):
  - `takDebugKeyFile`
  - `takDebugKeyFilePassword`
  - `takDebugKeyAlias`
  - `takDebugKeyPassword`
  - (optionally release equivalents)

> Note: The build file throws **"No signing key configured!"** if key properties are missing.

---

## 3) Build/package the plugin APK

From repo root:

```bash
cd plugin-examples/plugintemplate
./gradlew clean :app:assembleCivDebug
```

If you need a different flavor, swap `Civ` for `Mil`, `Gov`, etc. Example:

```bash
./gradlew :app:assembleMilDebug
```

### 3.1 Find the built APK

Use:

```bash
find app/build/outputs/apk -name "*.apk"
```

The generated APK name follows ATAK plugin naming conventions from `archivesBaseName` and flavor/build type.

---

## 4) Move the APK to the other ATAK instance/device

Pick one method:

### Method A: ADB push (USB)

```bash
adb devices
adb push app/build/outputs/apk/civ/debug/<your-plugin>.apk /sdcard/Download/
```

### Method B: Direct copy

- Copy the APK to the other device using email, secure file transfer, SD card, or MDM tooling.
- Place it somewhere easy to browse (e.g., `Download`).

---

## 5) Install on the target ATAK device

### 5.1 Android install step

- On the target device, open the APK from Files app, and install it.
- If prompted, allow app installs from that source (Unknown Apps).

Alternative via ADB:

```bash
adb install -r <your-plugin>.apk
```

### 5.2 Load/enable in ATAK

In ATAK, go to plugin/app management and load the plugin if it is not auto-loaded.
Typical places:
- **Settings → Tools/Plugins**
- **TAK Apps & Plugins**
- **Sideloaded plugins**

If ATAK asks, choose **Load Plugins**.

---

## 6) Use the plugin on the target device

1. Open ATAK.
2. Open the plugin tool entry named **Plugin Template**.
3. In the plugin panel:
   - Turn on **Use XML/GPS heading only** to set `useOnlyGPSBearing=true`.
   - If ATAK is currently in magnetic-up, plugin moves map mode to track-up.
   - Use **Restore previous ATAK heading values** to revert both heading toggle and prior map mode.
4. Confirm status text reflects current values.

---

## 7) Verification checklist

- Plugin appears in ATAK plugin/tool list.
- Switch toggles and remains persisted after closing/reopening panel.
- Restore button returns to previous map mode and previous `useOnlyGPSBearing` state.

---

## 8) Troubleshooting

### Build error: `Unsupported class file major version 65`

- You are likely using too new a JDK for this Gradle/Groovy/AGP combination.
- Set `JAVA_HOME` to a compatible JDK (typically JDK 11 for AGP 4.2.2 projects).

### Plugin installs but doesn’t show in ATAK

- Confirm plugin APK matches ATAK flavor compatibility.
- In ATAK, manually run plugin load flow from **TAK Apps & Plugins** / **Load Plugins**.
- Restart ATAK after install.

### Install blocked

- Enable install from unknown source for your file manager/browser.
- Verify APK signature and version code policy in your environment.

