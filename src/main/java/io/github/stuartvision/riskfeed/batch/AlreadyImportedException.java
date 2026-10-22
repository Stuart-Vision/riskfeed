package io.github.stuartvision.riskfeed.batch;

/** The same file content was already imported (or is being imported right now). */
public class AlreadyImportedException extends RuntimeException {

    public AlreadyImportedException(String message) {
        super(message);
    }
}
