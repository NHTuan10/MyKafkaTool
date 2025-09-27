#### jpackage command line:

VERSION=0.1.1.9-SNAPSHOT

jpackage --input my-kafka-tool-main/target/my-kafka-tool-main-${VERSION} --name MyKafkaTool --main-jar my-kafka-tool-main-${VERSION}.jar --type dmg

jpackage --input my-kafka-tool-launcher/target/my-kafka-tool-launcher-${VERSION} --name MyKafkaTool --main-jar my-kafka-tool-launcher-${VERSION}.jar --type dmg --java-options "-XX:+UseZGC"

jpackage --input my-kafka-tool-launcher/target/my-kafka-tool-launcher-${VERSION} --name MyKafkaTool --main-jar my-kafka-tool-launcher-${VERSION}.jar --type dmg --java-options "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -XX:+UseZGC"

jpackage --input my-kafka-tool-launcher/target/my-kafka-tool-launcher-${VERSION} --name MyKafkaTool --main-jar my-kafka-tool-launcher-${VERSION}.jar --type app-image --java-options "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -XX:+UseZGC" --win-console