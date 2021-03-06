plugins {
    id("talaiotPlugin")
}

talaiotPlugin {
    idPlugin = "com.cdsap.talaiot.plugin.pushgateway"
    artifact = "pushgateway"
    group = "com.cdsap.talaiot.plugin"
    mainClass = "com.cdsap.talaiot.plugin.pushgateway.TalaiotPushgatewayPlugin"
    version = "1.3.6-SNAPSHOT"
}

dependencies {
    implementation(project(":library:plugins:pushgateway:pushgateway-publisher"))
    implementation(project(":library:core:talaiot"))
    testImplementation(project(":library:core:talaiot-test-utils"))
    testImplementation("io.github.rybalkinsd:kohttp:0.10.0")
}
