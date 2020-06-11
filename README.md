# panopticon-example

A sample Scala app, fully equipped to be monitored with [Panopticon](https://github.com/ScalaConsultants/panopticon-tui)

Base app is created using [zio-akka-quickstart](https://github.com/ScalaConsultants/zio-akka-quickstart.g8) template. To see all the changes you'd have to make to wire-up panopticon, check out [this PR](https://github.com/ScalaConsultants/panopticon-example/pull/1).

For JMX integration to work, launch with following JVM options:

```
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.rmi.port=9010
-Dcom.sun.management.jmxremote.local.only=false
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

To connect panopticon to this app, run it the following way:

```bash
panopticon-tui \
  --tick-rate 2000 \
  --zio-zmx localhost:6789 \
  --jmx localhost:9010 \
  --db-pool-name myDb \
  --actor-tree http://localhost:8080/panopticon/actor-tree \
  --actor-system-status http://localhost:8080/panopticon/actor-system-status
  --dead-letters http://localhost:8080/panopticon/dead-letters
```
