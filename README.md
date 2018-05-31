# Trasher

A small script to clean up my downloads folder written in Clojure. Only works on Mac OS X.

The script will trash everything under `~/Downloads` that has not been opened for longer than a year.  

## Build

To build the binary, here are some instructions.

- Install [lein](https://leiningen.org/)
- Download [GraalVM](http://www.graalvm.org/downloads/) for your machine. You will need the EE version.
- Set `JAVA_HOME` to the GraalVM home directory, e.g.

```sh
export JAVA_HOME=~/Downloads/graalvm-1.0.0-rc1/Contents/Home
```
    
- Set the `PATH` to use GraalVM's binaries, e.g.

```sh
export PATH=$PATH:~/Downloads/graalvm-1.0.0-rc1/Contents/Home/bin
```

- Create the uberjar:

```sh
lein uberjar
```

- Finally, create the binary:

``` sh
native-image -jar target/trasher-0.1.0-SNAPSHOT-standalone.jar -H:Name="trasher"
```

## Schedule Job

Here is how to schedule a daily job for executing the binary.

- Change the username, binary path and schedule in the `./ams.geurs.trasher.plist` file.

- Copy the file into `~/Library/LaunchAgents/`

- Load the job into launchd:

```sh
launchctl load ~/Library/LaunchAgents/ams.geurs.trasher.plist
```