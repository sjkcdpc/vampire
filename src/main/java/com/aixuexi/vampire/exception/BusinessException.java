package com.aixuexi.vampire.exception;

import com.aixuexi.thor.except.BaseException;
import com.aixuexi.thor.except.ExceptionCode;

/**
 * Created by ruanyanjie on 2017/10/18.
 */
public class BusinessException extends BaseException {
    private ExceptionCode exceptionCode;

    public BusinessException(ExceptionCode exceptionCode, String message) {
        super(message);
        this.exceptionCode = exceptionCode;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return this.exceptionCode;
    }
}