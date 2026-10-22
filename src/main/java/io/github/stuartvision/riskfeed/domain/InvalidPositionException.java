package io.github.stuartvision.riskfeed.domain;

/** A record that can't be loaded. The batch step skips it and records the reason. */
public class InvalidPositionException extends RuntimeException {

    public InvalidPositionException(String reason) {
        super(reason);
    }
}
