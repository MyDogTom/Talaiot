package com.cdsap.talaiot.publisher

import com.cdsap.talaiot.configuration.*
import com.cdsap.talaiot.entities.ExecutionReport
import com.cdsap.talaiot.logger.LogTracker
import com.cdsap.talaiot.request.SimpleRequest
import java.util.concurrent.Executor

class HybridPublisher(
    private val hybridPublisherConfiguration: HybridPublisherConfiguration,
    private val logTracker: LogTracker,
    /**
     * Executor to schedule a task in Background
     */
    private val executor: Executor
) : Publisher {

    private val TAG = "HybridPublisher"

    override fun publish(report: ExecutionReport) {
        logTracker.log(TAG, "================")
        logTracker.log(TAG, "HybridPublisher")
        logTracker.log(TAG, "publishBuildMetrics: ${hybridPublisherConfiguration.publishBuildMetrics}")
        logTracker.log(TAG, "publishTaskMetrics: ${hybridPublisherConfiguration.publishTaskMetrics}")
        logTracker.log(TAG, "================")

        if (validate()) {
            hybridPublisherConfiguration.buildPublisher?.let {
                it.publishTaskMetrics = false
                getPublisher(it)?.publish(report)
            }

            hybridPublisherConfiguration.taskPublisher?.let {

                it.publishBuildMetrics = false
                getPublisher(it)?.publish(report)
            }
        }
    }

    private fun validate(): Boolean {
        if (hybridPublisherConfiguration.buildPublisher == null && hybridPublisherConfiguration.taskPublisher == null) {
            logTracker.error("HybridPublisher-Error: BuildPublisher and TaskPublisher are null. Not publisher will be executed ")
            return false
        }
        return true
    }


    private fun getPublisher(publisherConfiguration: PublisherConfiguration): Publisher? =
        when (publisherConfiguration) {
            is InfluxDbPublisherConfiguration -> {
                InfluxDbPublisher(
                    publisherConfiguration,
                    logTracker,
                    executor
                )

            }
            is PushGatewayPublisherConfiguration -> {
                PushGatewayPublisher(
                    publisherConfiguration,
                    logTracker,
                    SimpleRequest(logTracker),
                    executor
                )
            }

            is ElasticSearchPublisherConfiguration -> {
                ElasticSearchPublisher(
                    publisherConfiguration,
                    logTracker,
                    executor
                )
            }

            is CustomPublisherConfiguration -> {
                publisherConfiguration.publisher
            }

            else -> {
                logTracker.error(
                    "HybridPublisher: Not supported Publisher. Current Publishers supported by HybridPublisher: " +
                            "InfluxDbPublisher, PushGatewayPublisherConfiguration and ElasticSearchPublisherConfiguration"
                )
                null
            }
        }

}