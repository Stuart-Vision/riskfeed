package io.github.stuartvision.riskfeed.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.github.stuartvision.riskfeed.batch.AlreadyImportedException;

/** Errors as RFC 9457 problem details ({@code application/problem+json}). */
@RestControllerAdvice
public class ApiErrors extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AlreadyImportedException.class)
    ProblemDetail alreadyImported(AlreadyImportedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Already imported");
        return problem;
    }
}
