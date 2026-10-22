package io.github.stuartvision.riskfeed.batch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

/** Runs and looks up position imports. */
@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final JobLauncher jobLauncher;
    private final Job positionImportJob;
    private final JobExplorer jobExplorer;

    public ImportService(JobLauncher jobLauncher, Job positionImportJob, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.positionImportJob = positionImportJob;
        this.jobExplorer = jobExplorer;
    }

    /**
     * Imports a file and waits for the result. The file's SHA-256 is the job's
     * identifying parameter, so importing the same content twice is refused even
     * if the file was renamed.
     */
    public ImportSummary importFile(Path file, String displayName) {
        String checksum = sha256(file);
        JobParameters params = new JobParametersBuilder()
                .addString("checksum", checksum, true)
                .addString("file", file.toAbsolutePath().toString(), false)
                .addString("fileName", displayName, false)
                .toJobParameters();
        try {
            JobExecution execution = jobLauncher.run(positionImportJob, params);
            ImportSummary summary = ImportSummary.of(execution);
            log.info("Import {} of {}: {} (read {}, loaded {}, rejected {})", summary.importId(), displayName,
                    summary.status(), summary.read(), summary.loaded(), summary.rejected());
            return summary;
        } catch (JobInstanceAlreadyCompleteException | JobRestartException e) {
            throw new AlreadyImportedException("This file (SHA-256 " + checksum.substring(0, 12) + "...) was already imported");
        } catch (JobExecutionAlreadyRunningException e) {
            throw new AlreadyImportedException("This file is being imported right now");
        } catch (JobParametersInvalidException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<ImportSummary> find(long importId) {
        return Optional.ofNullable(jobExplorer.getJobExecution(importId))
                .filter(e -> ImportJobConfig.JOB_NAME.equals(e.getJobInstance().getJobName()))
                .map(ImportSummary::of);
    }

    static String sha256(Path file) {
        try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), MessageDigest.getInstance("SHA-256"))) {
            in.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(in.getMessageDigest().digest());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
