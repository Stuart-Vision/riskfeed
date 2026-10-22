package io.github.stuartvision.riskfeed.batch;

import java.time.LocalDateTime;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.ExitStatus;

/** The outcome of one import, built from Spring Batch's execution metadata. */
public record ImportSummary(
        long importId,
        String fileName,
        String status,
        long read,
        long loaded,
        long rejected,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String error) {

    static ImportSummary of(JobExecution execution) {
        long read = 0, loaded = 0, rejected = 0;
        for (StepExecution step : execution.getStepExecutions()) {
            read += step.getReadCount() + step.getReadSkipCount();
            loaded += step.getWriteCount();
            rejected += step.getReadSkipCount() + step.getProcessSkipCount() + step.getWriteSkipCount();
        }
        String error = execution.getAllFailureExceptions().stream()
                .findFirst()
                .map(t -> t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage())
                .orElse(null);
        String status = execution.getExitStatus().getExitCode();
        if (ExitStatus.COMPLETED.getExitCode().equals(status) && rejected > 0) {
            status = "COMPLETED_WITH_REJECTIONS";
        }
        return new ImportSummary(execution.getId(), execution.getJobParameters().getString("fileName"), status,
                read, loaded, rejected, execution.getStartTime(), execution.getEndTime(), error);
    }

    /** Exit code for schedulers: 0 clean, 3 loaded with rejections, 1 failed. */
    public int exitCode() {
        return switch (status) {
            case "COMPLETED" -> 0;
            case "COMPLETED_WITH_REJECTIONS" -> 3;
            default -> 1;
        };
    }
}
