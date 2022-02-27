package com.wxl52d41.validator.exception;

public class CustomException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CustomException(String message){
        super(message);
    }

    public CustomException(Throwable cause)
    {
        super(cause);
    }

    public CustomException(String message, Throwable cause)
    {
        super(message,cause);
    }
}
