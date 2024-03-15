package qiwifiless3.demo.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppError {
    private String message;
    private Integer HttpStatus;

    public AppError(String message, Integer httpStatus) {
        this.message = message;
        HttpStatus = httpStatus;
    }

}
