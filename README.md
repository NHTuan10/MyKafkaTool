#### jpackage command line:

jpackage --input my-kafka-tool-main/target/my-kafka-tool-main-0.1.1.5-SNAPSHOT --name MyKafkaTool --main-jar my-kafka-tool-main-0.1.1.5-SNAPSHOT.jar --type dmg

jpackage --input my-kafka-tool-launcher/target/my-kafka-tool-launcher-0.1.1.5-SNAPSHOT --name MyKafkaTool --main-jar my-kafka-tool-launcher-0.1.1.5-SNAPSHOT.jar --type dmg --java-options "-XX:+UseZGC"

jpackage --input my-kafka-tool-launcher/target/my-kafka-tool-launcher-0.1.1.5-SNAPSHOT --name MyKafkaTool --main-jar my-kafka-tool-launcher-0.1.1.5-SNAPSHOT.jar --type dmg --java-options "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -XX:+UseZGC"

jpackage --input my-kafka-tool-launcher/target/my-kafka-tool-launcher-0.1.1.5-SNAPSHOT --name MyKafkaTool --main-jar my-kafka-tool-launcher-0.1.1.5-SNAPSHOT.jar --type app-image --java-options "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -XX:+UseZGC" --win-console