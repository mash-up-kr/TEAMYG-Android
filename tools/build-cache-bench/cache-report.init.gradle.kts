import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.initialization.Settings
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskExecutionResult
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import java.io.File

abstract class TaskOutcomeRecorder :
    BuildService<TaskOutcomeRecorder.Params>, OperationCompletionListener, AutoCloseable {

    interface Params : BuildServiceParameters {
        val csv: RegularFileProperty
    }

    private val rows = StringBuilder()

    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent) return
        val result = event.result
        // getSkipMessage() 가 돌려주는 "NO-SOURCE" 는 하이픈이다. 원본 대조를 위해 정규화하지 않는다.
        val outcome = when (result) {
            is TaskSuccessResult -> when {
                result.isFromCache -> "FROM_CACHE"
                result.isUpToDate -> "UP_TO_DATE"
                else -> "EXECUTED"
            }
            is TaskSkippedResult -> result.skipMessage
            is TaskFailureResult -> "FAILED"
            else -> "UNKNOWN"
        }
        // 왜 실행됐는지를 남긴다. S3 에서 EXECUTED 로 남은 태스크의 사유가 판단 재료다.
        // 쉼표·따옴표·개행은 CSV 구조를 깬다. 개행이 섞이면 한 행이 두 행으로 쪼개져
        // 이후 집계의 컬럼 정렬이 통째로 밀린다.
        val reasons = (result as? TaskExecutionResult)?.executionReasons.orEmpty()
            .joinToString(";") {
                it.replace(',', ' ').replace('"', '\'').replace('\n', ' ').replace('\r', ' ')
            }
        rows.append(event.descriptor.taskPath).append(',')
            .append(outcome).append(',')
            .append(result.endTime - result.startTime).append(',')
            .append('"').append(reasons).append('"').append('\n')
    }

    override fun close() {
        val target = parameters.csv.get().asFile
        target.parentFile.mkdirs()
        target.writeText("task_path,outcome,duration_ms,execution_reasons\n" + rows)
        // 데몬 온도 편향을 사후에 확인하려면 어느 데몬이 이 빌드를 돌렸는지 알아야 한다.
        File(target.path + ".daemon").writeText(ProcessHandle.current().pid().toString())
    }
}

val csvPath = providers.gradleProperty("cacheReport.csv")
    .getOrElse(File(gradle.startParameter.currentDir, "build/task-outcomes.csv").absolutePath)

val recorder = gradle.sharedServices.registerIfAbsent(
    "taskOutcomeRecorder",
    TaskOutcomeRecorder::class,
) { parameters.csv.set(File(csvPath)) }

gradle.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(recorder)

// 측정이 개발자의 ~/.gradle 캐시를 쓰거나 더럽히지 않게 한다.
val benchCacheDir = providers.gradleProperty("cacheReport.cacheDir").orNull
if (benchCacheDir != null) {
    gradle.settingsEvaluated(
        Action<Settings> {
            buildCache.local.directory = File(benchCacheDir)
        },
    )
}
