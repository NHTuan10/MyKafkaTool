VERSION=0.1.1.8-SNAPSHOT
java -jar -XX:+UseZGC -DisDeployed=false my-kafka-tool-launcher-${VERSION}.jar
# -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005