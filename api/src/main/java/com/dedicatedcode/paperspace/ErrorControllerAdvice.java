package com.dedicatedcode.paperspace;

import com.dedicatedcode.paperspace.web.UnknownPageException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ErrorControllerAdvice {

    @ExceptionHandler(UnknownBinaryException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleBinaryNotFound(UnknownBinaryException ex) {
        return new ErrorResponse(ex.getMessage(), 404);
    }

    @ExceptionHandler(UnknownPageException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlePageNotFound(UnknownPageException ex) {
        return new ErrorResponse(ex.getMessage(), 404);
    }
}
