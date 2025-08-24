#### jpackage command line:

jpackage --input my-kafka-tool-main/target/my-kafka-tool-main-0.1.1.5-SNAPSHOT --name MyKafkaTool --main-jar my-kafka-tool-main-0.1.1.5-SNAPSHOT.jar --main-class io.github.nhtuan10.mykafkatool.MyKafkaToolLauncher --type dmg

jpackage --input my-kafka-tool-launcher/target/my-kafka-tool-launcher-0.1.1.5-SNAPSHOT/lib --name MyKafkaTool --main-jar my-kafka-tool-launcher-0.1.1.5-SNAPSHOT.jar --main-class io.github.nhtuan10.mykafkatool.launcher.ModularLauncher --type dmg

jpackage --input my-kafka-tool-launcher/target/my-kafka-tool-launcher-0.1.1.5-SNAPSHOT/lib --name MyKafkaTool --main-jar my-kafka-tool-launcher-0.1.1.5-SNAPSHOT.jar --main-class io.github.nhtuan10.mykafkatool.launcher.ModularLauncher --type dmg --java-options  "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"