OpenTable JVM Pause Alarm
=========================

[![Build Status](https://travis-ci.org/opentable/otj-pausedetector.svg)](https://travis-ci.org/opentable/otj-pausedetector)

Provide basic notification if the JVM is unable to keep its threads running for extended periods of time
e.g. because of GC, lack of CPU time.

Basic Usage
-----------

The alarm has very basic configuration available:
* Check interval -- how long to wait between each time check
* Alarm time -- minimum pause to report
* Action on pause -- user-supplied callback when a pause alarms
* Clock -- replace the system clock for testing purposes

```java

public class MyCoolApp {
  public static void main(String[] args) {
    try (new JvmPauseAlarm(100, 400).start()) {
      runMyCoolApp();
    }
  }
}

```

This will set up the pause alarm to check every 100ms for pauses of at least 400ms.

Example output:
```
[jvm-pause-alarm] INFO com.opentable.pausedetector.JvmPauseAlarm - Watching JVM for GC pausing.  Checking every 10ms for pauses of at least 400ms.
[jvm-pause-alarm] WARN com.opentable.pausedetector.JvmPauseAlarm - Detected pause of 1000ms!
```

For Spring, you can import `JvmPauseAlarmConfiguration` and starting / stopping the alarm
is taken care of for you.  You then use the following properties to configure it:

```
ot.jvm-pause.enabled=true
ot.jvm-pause.check-time=PT0.05s
ot.jvm-pause.pause-time=PT0.2s
```

----
Copyright (C) 2016 OpenTable, Inc.
