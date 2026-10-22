package io.github.stuartvision.riskfeed.batch;

import java.nio.file.Path;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import io.github.stuartvision.riskfeed.RiskFeedProperties;
import io.github.stuartvision.riskfeed.domain.FxRates;
import io.github.stuartvision.riskfeed.domain.InvalidPositionException;
import io.github.stuartvision.riskfeed.domain.Position;
import io.github.stuartvision.riskfeed.domain.PositionRepository;
import io.github.stuartvision.riskfeed.domain.PositionValidator;
import io.github.stuartvision.riskfeed.domain.RawPosition;
import io.github.stuartvision.riskfeed.domain.RejectionRepository;

import jakarta.persistence.EntityManagerFactory;

/**
 * The import job: one chunk-oriented step that reads a position file, validates
 * and converts each record, and writes valid positions in chunks. Bad records are
 * skipped and saved as rejections; they never stop the rest of the file.
 */
@Configuration
public class ImportJobConfig {

    public static final String JOB_NAME = "positionImportJob";

    @Bean
    Job positionImportJob(JobRepository jobRepository, Step importPositionsStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                // A failed run is not resumed half-way: the file is fixed and imported again.
                .preventRestart()
                .start(importPositionsStep)
                .build();
    }

    @Bean
    Step importPositionsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            RiskFeedProperties properties, PositionFileReader positionReader,
            PositionProcessor positionProcessor, JpaItemWriter<Position> positionWriter,
            RejectionListener rejectionListener) {
        return new StepBuilder("importPositions", jobRepository)
                .<RawPosition, Position>chunk(properties.chunkSize(), transactionManager)
                .reader(positionReader)
                .processor(positionProcessor)
                .writer(positionWriter)
                .faultTolerant()
                .skip(InvalidPositionException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(Integer.MAX_VALUE)
                .listener(rejectionListener)
                .build();
    }

    @Bean
    @StepScope
    PositionFileReader positionReader(@Value("#{jobParameters['file']}") String file) {
        return new PositionFileReader(Path.of(file));
    }

    @Bean
    @StepScope
    PositionProcessor positionProcessor(PositionValidator validator, FxRates fxRates, PositionRepository positions,
            @Value("#{stepExecution.jobExecutionId}") Long importId) {
        return new PositionProcessor(validator, fxRates, positions, importId);
    }

    @Bean
    @StepScope
    RejectionListener rejectionListener(RejectionRepository rejections,
            @Value("#{stepExecution.jobExecutionId}") Long importId) {
        return new RejectionListener(rejections, importId);
    }

    @Bean
    JpaItemWriter<Position> positionWriter(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Position>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(true)
                .build();
    }
}
