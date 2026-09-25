package com.example.ledger;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import java.net.URI;

@RestControllerAdvice
public class ProblemHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request){
        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setTitle("Validation failed");
        problem.setDetail("One or more request fields are invalid.");
        problem.setType(URI.create("https://example.com/problems/validation"));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(UnknownMerchantException.class)
    public ProblemDetail handleUnknownMerchant(
            UnknownMerchantException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setTitle("Unknown merchant");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://example.com/problems/unknown-merchant"));
        problem.setInstance(URI.create(request.getRequestURI()));

        return problem;
    }
}
