package com.cdsap.talaiot

import com.cdsap.talaiot.entities.NodeArgument
import com.cdsap.talaiot.logger.LogTrackerImpl
import com.cdsap.talaiot.provider.MetricsProvider
import com.cdsap.talaiot.provider.PublishersProvider
import com.cdsap.talaiot.publisher.TalaiotPublisherImpl
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.GradleInternal
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import org.gradle.internal.logging.LoggingManagerInternal
import org.gradle.internal.logging.events.StyledTextOutputEvent
import org.gradle.internal.scan.eob.BuildScanEndOfBuildNotifier
import org.gradle.internal.scan.eob.DefaultBuildScanEndOfBuildNotifier
import org.gradle.internal.scan.time.BuildScanBuildStartedTime
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.invocation.DefaultGradle
import org.gradle.util.GradleVersion
import javax.annotation.Nullable


/**
 * Custom listener that combines the BuildListener and TaskExecutionListener. For each Task we need to record information
 * like duration or state, it's helped by the TalaiotTracker to track the information.
 * Once the build is finished it will publish the data for all the publishers included in the configuration
 */
class TalaiotListener(
    /**
     * Gradle project required to access the properties
     */
    val project: Project,
    /**
     * Extension with the main configuration of the plugin
     */
    private val extension: TalaiotExtension
) : BuildListener, TaskExecutionListener {

    private val talaiotTracker = TalaiotTracker()
    private var start: Long = 0L
    private var configurationEnd: Long? = null

    override fun settingsEvaluated(settings: Settings) {
    }

    override fun buildFinished(result: BuildResult) {
        if (shouldPublish()) {
            val end = System.currentTimeMillis()
            val logger = LogTrackerImpl(extension.logger)

            val scanLink = forceGradleScanPublishing(result)

            TalaiotPublisherImpl(
                extension,
                logger,
                MetricsProvider(project),
                PublishersProvider(project, logger)
            ).publish(
                taskLengthList = talaiotTracker.taskLengthList,
                success = result.success(),
                startMs = start,
                configuraionMs = configurationEnd,
                endMs = end,
                scanLink = scanLink
            )
        }
    }

    /**
     * Be warned: super hacky
     * The link output happens after the build actually already finishes
     */
    private fun forceGradleScanPublishing(result: BuildResult): String? {
        val services = (result.gradle as GradleInternal).services

        val endOfBuildNotifier = when {
            GradleVersion.current() > GradleVersion.version("5.0") -> {
                services.get(
                    DefaultBuildScanEndOfBuildNotifier::class.java
                ) as DefaultBuildScanEndOfBuildNotifier
            }
            else -> {
                /**
                 * Previously the code registered a regular buildListener
                 * This is unsupported currently
                 */

                return null
            }
        }

        val loggingManager = (project.gradle as GradleInternal).services.get(LoggingManagerInternal::class.java)

        var shouldCaptureNext = false
        var link: String? = null
        loggingManager.addOutputEventListener {
            if (it is StyledTextOutputEvent) {
                if (it.spans.any { span -> span.text.contains("Publishing build scan") }) {
                    shouldCaptureNext = true
                } else if (shouldCaptureNext) {
                    shouldCaptureNext = false
                    link = it.spans.map { it.text }.joinToString(separator = "").trim()
                }
            }
        }

        endOfBuildNotifier.fireBuildComplete(result.failure)
        preventDoubleScanReport(endOfBuildNotifier)

        return link
    }

    private fun preventDoubleScanReport(endOfBuildNotifier: DefaultBuildScanEndOfBuildNotifier) {
        val listenerField = findListenerField(endOfBuildNotifier)
        listenerField.isAccessible = true
        listenerField.set(endOfBuildNotifier, null)
    }

    private fun findListenerField(endOfBuildNotifier: DefaultBuildScanEndOfBuildNotifier) =
        endOfBuildNotifier::class.java.getDeclaredField("listener")

    /**
     * it checks if the executions has to be published, checking the  main ignoreWhen configuration and the
     * state of the tracker
     */
    private fun shouldPublish() = ((extension.ignoreWhen == null || extension.ignoreWhen?.shouldIgnore() == false)
            && talaiotTracker.isTracking)

    override fun projectsLoaded(gradle: Gradle) {
    }

    override fun buildStarted(gradle: Gradle) {
        //This never gets called because we're registering after the build has already started
    }

    override fun projectsEvaluated(gradle: Gradle) {
        start = assignBuildStarted(gradle)

        configurationEnd = System.currentTimeMillis()
        gradle.startParameter.taskRequests.forEach {
            it.args.forEach { task ->
                talaiotTracker.queue.add(NodeArgument(task, 0, 0))
            }
        }
        if (talaiotTracker.queue.isNotEmpty()) {
            talaiotTracker.initNodeArgument()
        }
    }

    private fun assignBuildStarted(gradle: Gradle): Long {
        val buildStartedTimeService = (gradle as GradleInternal).services.get(BuildScanBuildStartedTime::class.java)
        return buildStartedTimeService?.let { it.buildStartedTime } ?: System.currentTimeMillis()
    }

    override fun beforeExecute(task: Task) {
        talaiotTracker.startTrackingTask(task)
    }

    override fun afterExecute(task: Task, state: TaskState) {
        val currentWorkerLease =
            (task.project.gradle as DefaultGradle).services.get(WorkerLeaseService::class.java).currentWorkerLease
        val workerName = currentWorkerLease.displayName
        talaiotTracker.finishTrackingTask(task, state, workerName)
    }
}

private fun BuildResult.success() = when {
    failure != null -> false
    else -> true
}