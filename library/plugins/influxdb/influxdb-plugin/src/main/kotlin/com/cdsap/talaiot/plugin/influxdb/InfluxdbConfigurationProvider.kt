package com.cdsap.talaiot.plugin.influxdb

import com.cdsap.talaiot.logger.LogTrackerImpl
import com.cdsap.talaiot.provider.PublisherConfigurationProvider
import com.cdsap.talaiot.publisher.Publisher
import com.cdsap.talaiot.publisher.influxdb.InfluxDbPublisher
import org.gradle.api.Project
import java.util.concurrent.Executors

class InfluxdbConfigurationProvider(
    val project: Project
) : PublisherConfigurationProvider {
    override fun get(): List<Publisher> {
        val publishers = mutableListOf<Publisher>()
        val talaiotExtension = project.extensions.getByName("talaiot") as InfluxdbExtension

        talaiotExtension.publishers?.apply {
            influxDbPublisher?.let { publisherConfig ->
                publishers.add(
                    InfluxDbPublisher(
                        publisherConfig,
                        LogTrackerImpl(talaiotExtension.logger),
                        Executors.newSingleThreadExecutor()
                    )
                )
            }
            publishers.addAll(customPublishers)
        }
        return publishers
    }
}
