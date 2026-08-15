package com.moru.server.global.exception;


import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.response.code.BaseCode;
import com.moru.server.global.response.code.status.ErrorStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.security.authorization.AuthorizationDeniedException;

import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(
            AuthorizationDeniedException ex
    ) {
        log.warn("Request rejected. code={}, status={}",
                ErrorStatus.FORBIDDEN.getCode(), ErrorStatus.FORBIDDEN.getHttpStatus().value());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.onFailure(ErrorStatus.FORBIDDEN));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex
    ) {
        log.warn("Request rejected. code={}, status={}",
                ErrorStatus.FORBIDDEN.getCode(), ErrorStatus.FORBIDDEN.getHttpStatus().value());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.onFailure(ErrorStatus.FORBIDDEN));
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ApiResponse<Object>> globalExceptionHandler(GlobalException ex) {

        BaseCode baseCode = ex.getBaseCode();
        log.warn("Request failed. code={}, status={}",
                baseCode.getCode(), baseCode.getHttpStatus().value());

        return ResponseEntity
                .status(baseCode.getHttpStatus())
                .body(ApiResponse.onFailure(baseCode));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> businessExceptionHandler(BusinessException ex) {

        BaseCode baseCode = ex.getBaseCode();
        log.warn("Request failed. code={}, status={}",
                baseCode.getCode(), baseCode.getHttpStatus().value());

        return ResponseEntity
                .status(baseCode.getHttpStatus())
                .body(ApiResponse.onFailure(baseCode));
    }

    // @Valid 검증 실패
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing + ", " + replacement
                ));
        log.warn("Request validation failed. code={}, status={}, errorCount={}",
                ErrorStatus.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST.value(), errors.size());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, errors));
    }

    // @Validated 검증 실패 (Path Variable, Request Param)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException ex) {

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing + ", " + replacement
                ));
        log.warn("Request constraint violation. code={}, status={}, errorCount={}",
                ErrorStatus.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST.value(), errors.size());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, errors));
    }

    // @RequestParam 필수 파라미터 누락
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("Required request parameter missing. code={}, status={}",
                ErrorStatus.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST.value());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, ex.getParameterName() + " 파라미터가 필요합니다."));
    }

    // @RequestParam 타입/형식 오류
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {

        log.warn("Request parameter type mismatch. code={}, status={}",
                ErrorStatus.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST.value());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, ex.getName() + " 파라미터 형식이 올바르지 않습니다."));
    }

    // JSON 파싱 실패
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("Request body parsing failed. code={}, status={}",
                ErrorStatus.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST.value());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, "잘못된 JSON 형식입니다."));
    }

    // DB 제약조건 위반
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {

        logInternalError("data_integrity", ex);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST, "데이터 무결성 위반입니다."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {

        log.warn("Illegal request argument. code={}, status={}",
                ErrorStatus.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST.value());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST));
    }

    @ExceptionHandler(BeanCreationException.class)
    public ResponseEntity<Object> handleBeanCreationException(BeanCreationException ex) {

        logInternalError("bean_creation", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(ClassCastException.class)
    public ResponseEntity<Object> handleClassCastException(ClassCastException ex) {

        logInternalError("class_cast", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<Object> handleHttpMessageConversionException(HttpMessageConversionException ex) {

        logInternalError("message_conversion", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
    public ResponseEntity<Object> handleIncorrectResultSizeException(IncorrectResultSizeDataAccessException ex) {

        logInternalError("incorrect_result_size", ex);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST));
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Object> handleInvalidDataAccessApiUsageException(InvalidDataAccessApiUsageException ex) {

        logInternalError("invalid_data_access", ex);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST));
    }

    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<Object> handleJpaSystemException(JpaSystemException ex) {

        logInternalError("jpa_system", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleNoSuchElementException(NoSuchElementException ex) {

        log.warn("Requested element not found. code={}, status={}",
                ErrorStatus.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST.value());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(ErrorStatus.BAD_REQUEST));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Object> handleNullPointerException(NullPointerException ex) {

        logInternalError("null_pointer", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(UnsatisfiedDependencyException.class)
    public ResponseEntity<Object> handleUnsatisfiedDependencyException(UnsatisfiedDependencyException ex) {

        logInternalError("unsatisfied_dependency", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception ex) {

        logInternalError("unhandled", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR));
    }

    private void logInternalError(String category, Exception ex) {
        log.error("Internal request failure. category={}, code={}, status={}, exceptionType={}",
                category,
                ErrorStatus.INTERNAL_SERVER_ERROR.getCode(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getClass().getSimpleName());
    }
}
