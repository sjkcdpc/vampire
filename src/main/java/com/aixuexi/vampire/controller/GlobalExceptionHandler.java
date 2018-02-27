package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;

import com.gaosi.api.vulcan.bean.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author ruanyanjie
 * @createTime 20171018
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResultData handleBusinessException(BusinessException e) {
        logger.warn("handleBusinessException : " + e.getMessage(), e);
        return ResultData.failed(e.getMessage());
    }

}
