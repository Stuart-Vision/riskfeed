package io.github.stuartvision.riskfeed.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.github.stuartvision.riskfeed.RiskFeedProperties;
import io.github.stuartvision.riskfeed.batch.ImportService;
import io.github.stuartvision.riskfeed.batch.ImportSummary;
import io.github.stuartvision.riskfeed.batch.PositionReaders;
import io.github.stuartvision.riskfeed.domain.CounterpartyExposure;
import io.github.stuartvision.riskfeed.domain.PositionRepository;
import io.github.stuartvision.riskfeed.domain.Rejection;
import io.github.stuartvision.riskfeed.domain.RejectionRepository;

@RestController
@RequestMapping("/api")
public class RiskFeedController {

    private final ImportService imports;
    private final PositionRepository positions;
    private final RejectionRepository rejections;
    private final RiskFeedProperties properties;

    public RiskFeedController(ImportService imports, PositionRepository positions, RejectionRepository rejections,
            RiskFeedProperties properties) {
        this.imports = imports;
        this.positions = positions;
        this.rejections = rejections;
        this.properties = properties;
    }

    /** Uploads a CSV or XML position file and imports it. */
    @PostMapping("/imports")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportSummary upload(@RequestParam("file") MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "" : Path.of(file.getOriginalFilename()).getFileName().toString();
        if (!PositionReaders.isSupported(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .csv and .xml files are supported");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }
        // Stored under a generated name: the uploaded name is never used as a path.
        Files.createDirectories(properties.inbox());
        Path stored = properties.inbox().resolve(UUID.randomUUID() + name.substring(name.lastIndexOf('.')));
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, stored, StandardCopyOption.REPLACE_EXISTING);
        }
        return imports.importFile(stored, name);
    }

    @GetMapping("/imports/{id}")
    public ImportSummary importStatus(@PathVariable long id) {
        return imports.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No import " + id));
    }

    @GetMapping("/imports/{id}/rejections")
    public List<RejectionView> rejections(@PathVariable long id) {
        imports.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No import " + id));
        return rejections.findByImportIdOrderByRecordNo(id).stream().map(RejectionView::of).toList();
    }

    /** Net and gross USD exposure per counterparty on a business date, largest first. */
    @GetMapping("/exposures")
    public List<CounterpartyExposure> exposures(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return positions.exposuresOn(asOfDate);
    }

    public record RejectionView(int recordNo, String tradeId, String reason) {
        static RejectionView of(Rejection r) {
            return new RejectionView(r.getRecordNo(), r.getTradeId(), r.getReason());
        }
    }
}
