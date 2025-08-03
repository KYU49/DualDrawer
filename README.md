# DualDrawer
A Jetpack Compose module for Android apps that provides drawers opening from both the left and right sides.

English follows Japanese.

# 概要
Jetpack ComposeでAndroidアプリを開発する際、Compose Material標準のModalDrawerには以下のような仕様がある。
* 標準のModalDrawerは左から右にしか開くことができない。
* CompositionLocalのLayoutDirectionをRtlを設定することで一応右から左にも開けるが、いくつか問題が出る。
* どちらにせよ、左右両方にDrawerを設定することはできない。
このModuleを使うことで、左右両方、もしくは任意の一方にDrawerを設定することができる。

# 使い方
* 以下でModuleをローカルに保存。
```bash
git clone https://github.com/KYU49/DualDrawer.git
```
* Android Studioの「File」→「New」→「Immport module...」からDualDrawerのディレクトリを指定。
* `YourProject\app\build.gradle.kts` (appレベルのbuild.gradle)に以下を追記。
```kotlin
plugins {
    ...
}
android{
    ...
}
dependencies {

    ...

    implementation(project(":DualDrawer"))
}
```
* Composableなfun内で以下のように使用。
```kotlin
import icu.ejapon.dual_drawer.DualDrawer
import icu.ejapon.dual_drawer.DrawerValue
import icu.ejapon.dual_drawer.rememberCustomDrawerState

@Composable
fun MainScreen (modifier: Modifier = Modifier){
    Surface(modifier = modifier){
        DualDrawer(
            drawerState = drawerState,
            rightDrawerContent = {
                Text(
                    text = "Hello World!"
                )
            },
            leftDrawerContent = null,   // nullで無効になる(default: null)。
            content = {
                Text(
                    text = "Hello World!"
                )

            }
        )
    }
}
```

# Licenseについて
このModuleはCompose MaterialのModalDrawerを改造して作っています。コード内にもその旨が記載されていますが、もし記載方法が不十分であった場合は修正いたしますので、ご指摘いただけると幸いです。

以下、同じ内容の英語。

**Attention!**
**English text is translated by ChatGPT.**

# Overview

When developing Android apps with Jetpack Compose, the default `ModalDrawer` from Compose Material has the following limitations:

* The standard `ModalDrawer` only opens from left to right.
* While it is technically possible to make it open from right to left by setting the `LayoutDirection` to `Rtl` using `CompositionLocal`, this approach introduces several issues.
* In any case, it's not possible to set drawers on both sides simultaneously.

This module allows you to set drawers on **either side**, or **both left and right**, flexibly.

---

# Usage

* Clone the module locally:

```bash
git clone https://github.com/KYU49/DualDrawer.git
```

* In Android Studio, go to **File** → **New** → **Import Module...**, and select the `DualDrawer` directory.

* Add the following to your `YourProject/app/build.gradle.kts` (your app-level Gradle file):

```kotlin
plugins {
    ...
}
android {
    ...
}
dependencies {
    ...

    implementation(project(":DualDrawer"))
}
```

* Use it in a composable function like this:

```kotlin
import icu.ejapon.dual_drawer.DualDrawer
import icu.ejapon.dual_drawer.DrawerValue
import icu.ejapon.dual_drawer.rememberCustomDrawerState

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier) {
        DualDrawer(
            drawerState = drawerState,
            rightDrawerContent = {
                Text(text = "Hello World!")
            },
            leftDrawerContent = null,  // Pass null to disable (default: null)
            content = {
                Text(text = "Hello World!")
            }
        )
    }
}
```

---

# About the License

This module is based on modifications to the `ModalDrawer` component from Compose Material.
Although the source code includes comments indicating this, if the attribution is insufficient in any way, please let me know and I will make corrections.

