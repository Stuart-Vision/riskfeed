package io.github.stuartvision.riskfeed;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/** The REST API end to end: upload, status, rejections, exposures and errors. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-${random.uuid};MODE=MSSQLServer",
        "riskfeed.inbox=target/test-inbox"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RiskFeedApiTest {

    @Autowired MockMvc mvc;

    @Test
    void uploadThenQuery() throws Exception {
        var file = new MockMultipartFile("file", "positions-2026-10-21.csv", "text/csv",
                Files.readAllBytes(Path.of("samples/positions-2026-10-21.csv")));

        String body = mvc.perform(multipart("/api/imports").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("positions-2026-10-21.csv"))
                .andExpect(jsonPath("$.status").value("COMPLETED_WITH_REJECTIONS"))
                .andExpect(jsonPath("$.loaded").value(8))
                .andExpect(jsonPath("$.rejected").value(4))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.importId");

        mvc.perform(get("/api/imports/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaded").value(8));

        mvc.perform(get("/api/imports/{id}/rejections", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].recordNo").value(9))
                .andExpect(jsonPath("$[0].reason").value(containsString("not a valid LEI")));

        mvc.perform(get("/api/exposures").param("asOfDate", "2026-10-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].counterpartyLei").value("213800QWERTYUIOP1289"))
                .andExpect(jsonPath("$[0].grossUsd").value(3986250.75));

        // Uploading the same file again is a conflict, reported as problem+json.
        mvc.perform(multipart("/api/imports").file(file))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Already imported"));
    }

    @Test
    void rejectsUnsupportedAndEmptyFiles() throws Exception {
        mvc.perform(multipart("/api/imports").file(new MockMultipartFile("file", "positions.txt", "text/plain", "x".getBytes())))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/imports").file(new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0])))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownImportsAndBadParameters() throws Exception {
        mvc.perform(get("/api/imports/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/imports/999999/rejections")).andExpect(status().isNotFound());
        mvc.perform(get("/api/exposures").param("asOfDate", "21-10-2026")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/exposures").param("asOfDate", "2020-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
