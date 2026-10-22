package io.github.stuartvision.riskfeed;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param inbox folder where uploaded files are stored before they are imported
 * @param chunkSize records written per database transaction
 */
@ConfigurationProperties("riskfeed")
public record RiskFeedProperties(Path inbox, int chunkSize) {
}
