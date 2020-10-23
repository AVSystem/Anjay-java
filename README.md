# Anjay-java [<img align="right" height="50px" src="https://avsystem.github.io/Anjay-doc/_images/avsystem_logo.png">](http://www.avsystem.com/)

## About

This project provides almost 1:1 API bindings between [Anjay](https://github.com/AVSystem/Anjay)
(written in C) and Java, thus making it possible to run Anjay clients on
[Android](https://www.android.com/) for example.

This is a preview release and we don't provide any guarantees about API stability
or library reliability.

## Usage

First of all, remember to update all submodules using `git submodule update --init --recursive`.

### CMake

The library, example client and documentation can be built from command line as follows:
```sh
$ cmake .
$ make -j
```

To run the example client execute:
```sh
$ java -Djava.library.path=$PWD/ -jar demo.jar
```

By default, the client attempts to connect to `coap://127.0.0.1:5683`, but it
can be customized using command line arguments. Use `--help` to see all available
options.

### Android Studio

1. Start with creating a simple base project (_Empty activity_ for example),
   using Java and API 26 or newer.
2. In the left navigator right click on the _app_ and select
   _Link C++ Project with Gradle_, set _Build System_ to _CMake_ and choose
   _CMakeLists.txt_ file from the root project directory.
3. As of today's version of mbed TLS (2.23.0) there's a problem preventing it
   from compiling. A crude and simple workaround for this is to edit
   application's `build.gradle` as below:
   ```
   android {
       // ...
       defaultConfig {
           // ...
           externalNativeBuild {
               cmake {
                   // Workaround for bad casts within mbed TLS
                   cFlags "-Wno-pointer-sign"
               }
           }
       }
       // ...
   }
   ```
4. The next step is to add Anjay's Java sources to your project so that you can
   actually use the bindings. The simplest way to do it is to create a symlink
   from `bindings/main/java/com/avsystem` to `app/src/main/java/com/avsystem`.
5. Now you can check
   `bindings/src/main/java/com/avsystem/anjay/demo/DemoClient.java` to see
   how to use Anjay's Java bindings.
