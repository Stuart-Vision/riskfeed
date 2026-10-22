package io.github.stuartvision.riskfeed.cli;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import io.github.stuartvision.riskfeed.batch.AlreadyImportedException;
import io.github.stuartvision.riskfeed.batch.ImportService;
import io.github.stuartvision.riskfeed.batch.ImportSummary;

/**
 * Command-line import for schedulers such as Control-M, Airflow or cron:
 * {@code java -jar riskfeed.jar --spring.main.web-application-type=none --import=positions.csv}
 *
 * <p>Exit codes: 0 loaded cleanly, 3 loaded with rejected records, 2 already imported, 1 failed.
 */
@Component
public class ImportCommand implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(ImportCommand.class);

    private final ImportService imports;
    private int exitCode;

    public ImportCommand(ImportService imports) {
        this.imports = imports;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("import")) {
            return;
        }
        Path file = Path.of(args.getOptionValues("import").getFirst());
        if (!Files.isReadable(file)) {
            log.error("Cannot read {}", file);
            exitCode = 1;
            return;
        }
        try {
            ImportSummary summary = imports.importFile(file, file.getFileName().toString());
            exitCode = summary.exitCode();
        } catch (AlreadyImportedException e) {
            log.warn(e.getMessage());
            exitCode = 2;
        } catch (RuntimeException e) {
            log.error("Import failed", e);
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
