package br.com.antigravity.fiscalapi.shared;

import br.com.antigravity.fiscalapi.operational.OperationalRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError badRequest(BadRequestException ex, HttpServletRequest request) {
        return error(request, "bad_request", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(NotFoundException ex, HttpServletRequest request) {
        return error(request, "not_found", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError conflict(ConflictException ex, HttpServletRequest request) {
        return error(request, "conflict", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError forbidden(ForbiddenException ex, HttpServletRequest request) {
        return error(request, "forbidden", ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError validation(Exception ex, HttpServletRequest request) {
        String message = ex instanceof MethodArgumentNotValidException manve
            ? manve.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "))
            : ex.getMessage();
        return error(request, "validation_error", message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError generic(Exception ex, HttpServletRequest request) {
        return error(request, "internal_error", ex.getMessage());
    }

    private ApiError error(HttpServletRequest request, String code, String message) {
        OperationalRequestContext.attachError(request, code, message);
        return new ApiError(code, message, OperationalRequestContext.requestId(request));
    }

    public record ApiError(String code, String message, String requestId) {
    }
}
