package com.cdsap.talaiot.plugin.elasticsearch

import com.cdsap.talaiot.TalaiotExtension
import com.cdsap.talaiot.publisher.PublishersConfiguration
import groovy.lang.Closure
import org.gradle.api.Project

open class ElasticSearchExtension(project: Project) : TalaiotExtension(project) {
    /**
     * General Publisher configuration included in the build
     */
    var publishers: ElasticSearchConfiguration? = null

    fun publishers(block: PublishersConfiguration.() -> Unit) {
        publishers = ElasticSearchConfiguration(project).also(block)
    }

    fun publishers(closure: Closure<*>) {
        publishers = ElasticSearchConfiguration(project)
        closure.delegate = publishers
        closure.call()
    }


}