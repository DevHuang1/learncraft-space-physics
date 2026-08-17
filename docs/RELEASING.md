# Automated Android Releases

The Android workflow builds a signed APK and runs the on-device Android UI and physics tests whenever Android source or workflow files change on `main`. A GitHub release is then published automatically only when a semantic-version Git tag is pushed. A manual workflow run is test-only by default; publishing from that screen requires selecting the explicit **Publish a GitHub release** option.

## Prerequisites

Both `DevHuang1/learncraft-space-physics` and `DevHuang1/orbital-physics-engine` must retain these Actions secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Use the same release keystore for every version of a given Android package so existing Samsung installations can accept updates.

## Publishing version `1.0.2`

First commit and push the release-ready code to `main`. From the local repository, create an annotated tag, then push it to both remotes:

```bash
git tag -a v1.0.2 -m "LearnCraft Space Physics 1.0.2"
git push origin v1.0.2
git push public v1.0.2
```

Each tag starts two checks: the signed APK build and the KVM-backed Android UI/physics test job. When both pass, GitHub creates a release titled **LearnCraft Space Physics signed Android build 1.0.2** and attaches `app-release.apk`.

The tag must follow `vMAJOR.MINOR.PATCH`, for example `v2.3.4`. The workflow derives Android `versionName` from the tag and generates a monotonically increasing numeric `versionCode` using `MAJOR × 1,000,000 + MINOR × 1,000 + PATCH`.

## If a release fails

Do not reuse a modified tag. Correct the problem, create the next version tag, and push it. For transient infrastructure problems, use **Re-run all jobs** from the failed Actions run. The generated test-report artifact is available for Android instrumentation-test diagnostics.

## Manual verification without a release

Open **Actions → Android APK → Run workflow** and leave **Publish a GitHub release** unchecked. This builds the signed APK and runs the Android UI/physics tests without creating another public or private GitHub release. Select that checkbox only when a manually published release is intentional; version tags remain the preferred release route.
