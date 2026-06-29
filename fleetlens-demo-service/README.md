# fleetlens-demo-service

A minimal stand-in "order-service" so FleetLens has something real to monitor out of the box. It exposes `/actuator/env` for the Config Auditor, produces JSON order events to the `order-events` Kafka topic for the Schema Drift detector, and consumes those same events with an artificial per-message delay so consumer lag grows over time for the Memory Profiler's heap/lag correlation panel.

Run it with JMX enabled so the Memory Profiler can attach (the `start.ps1`/`start.sh` scripts at the repo root already do this):

```
java -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -Dcom.sun.management.jmxremote.rmi.port=9010 \
     -Djava.rmi.server.hostname=localhost \
     -jar fleetlens-demo-service/target/fleetlens-demo-service.jar
```

To see schema drift fire, restart the service with `DEMO_SCHEMA_VARIANT=v2` — the producer switches to a shape that drops `status` (breaking removal), changes `amount` from a number to a string (breaking type change), and adds `currency` (non-breaking addition).
