package qiwifiless3.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReaderException.class)
    public ResponseEntity<AppError> handleException(ReaderException e) {
        return new ResponseEntity<>(new AppError(e.getMessage(), HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WriterException.class)
    public ResponseEntity<AppError> handleException(WriterException e) {
        return new ResponseEntity<>(new AppError(e.getMessage(), HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
    }

}
