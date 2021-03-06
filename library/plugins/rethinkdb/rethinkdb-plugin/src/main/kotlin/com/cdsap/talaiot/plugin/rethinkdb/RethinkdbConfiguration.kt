package com.cdsap.talaiot.plugin.rethinkdb

import com.cdsap.talaiot.publisher.PublishersConfiguration
import com.cdsap.talaiot.publisher.rethinkdb.RethinkDbPublisher
import com.cdsap.talaiot.publisher.rethinkdb.RethinkDbPublisherConfiguration
import groovy.lang.Closure
import org.gradle.api.Project

class  RethinkdbConfiguration(project: Project) : PublishersConfiguration(project) {

    var rethinkDbPublisher: RethinkDbPublisherConfiguration? = null

    /**
     * Configuration accessor within the [PublishersConfiguration] for the [RethinkDbPublisher]
     *
     * @param configuration Configuration block for the [RethinkDbPublisherConfiguration]
     */
    fun rethinkDbPublisher(configuration: RethinkDbPublisherConfiguration.() -> Unit) {
        rethinkDbPublisher = RethinkDbPublisherConfiguration().also(configuration)
    }

    /**
     * Configuration accessor within the [PublishersConfiguration] for the [RethinkDbPublisher]
     *
     * @param closure closure for the [RethinkDbPublisherConfiguration]
     */
    fun rethinkDbPublisher(closure: Closure<*>) {
        rethinkDbPublisher = RethinkDbPublisherConfiguration()
        closure.delegate = rethinkDbPublisher
        closure.call()
    }
}