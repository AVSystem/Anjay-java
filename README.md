# Anjay-java [<img align="right" height="50px" src="https://avsystem.github.io/Anjay-doc/_images/avsystem_logo.png">](http://www.avsystem.com/)

[![Maven Central](https://img.shields.io/maven-central/v/com.avsystem.anjay/anjay-android?label=maven%20central%3A%20anjay-android)](https://search.maven.org/artifact/com.avsystem.anjay/anjay-android)
[![Maven Central](https://img.shields.io/maven-central/v/com.avsystem.anjay/anjay-java?label=maven%20central%3A%20anjay-java)](https://search.maven.org/artifact/com.avsystem.anjay/anjay-java)

## ⚠️ Archived Repository

**This repository is no longer actively maintained by the team. It is provided as is for reference purposes only.**
**We do not accept issues, pull requests, or provide support.**
**If you are still interested in this project or have questions, feel free to contact us. https://avsystem.com/contact/**

---

## About

This project provides almost 1:1 API bindings between [Anjay](https://github.com/AVSystem/Anjay)
(written in C) and Java, thus making it possible to run Anjay clients on
[Android](https://www.android.com/) for example.

This is a preview release and we don't provide any guarantees about API stability
or library reliability.

## Using in Android Studio

This project is released to Maven Central repository as
`com.avsystem.anjay.anjay-android`. Add `mavenCentral()` in `repositories`
section of your build script and `implementation 'com.avsystem.anjay:anjay-android:2.+'` in `dependencies` to use it.

## Using anjay-java package

Although the `anjay-java` package is released on Maven Central repository, it
requires native shared library to be available. See the Compilation guide for
details how to build the native library.

## Compilation guide

First of all, remember to update all submodules using
`git submodule update --init --recursive`.

### Build library

```sh
./gradlew :library:build
```

The `jar` file is placed in `library/build/libs` directory. Note that it doesn't
include the native library, which can be found in `library/build/cmake`
directory.

### Build and run demo

```sh
./gradlew :demo:build
java -Djava.library.path=library/build/cmake/ -jar demo/build/libs/demo.jar
```

By default, the client attempts to connect to `coap://127.0.0.1:5683`, but it
can be customized using command line arguments. Use `--help` to see all
available options.

### Building for Android

```sh
ANDROID_SDK_ROOT=<path to Android SDK> ./gradlew -Pandroid :library:build
```

The `aar` files are in `library/build/outputs/aar` directory.

### Running tests

```sh
./gradlew :testing:check
```
