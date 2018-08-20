package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;

import com.aixuexi.vampire.util.UserHandleUtil;
import com.alibaba.dubbo.common.utils.IOUtils;
import com.alibaba.fastjson.JSON;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

/**
 * @author ruanyanjie
 * @createTime 20171018
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResultData handleBusinessException(HttpServletRequest request, BusinessException e) {
        logger.warn("handleBusinessException : requestUrl : {} , requestParams : {} , userId : {}, insId : {} ",
                request.getRequestURI(), getRequestParameter(request), UserHandleUtil.getUserId(), UserHandleUtil.getInsId(), e);
        return ResultData.failed(e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public ResultData handleNullPointerException(HttpServletRequest request, NullPointerException e) {
        logger.error("handleNullPointerException : requestUrl : {} , requestParams : {} , userId : {}, insId : {} ",
                request.getRequestURI(), getRequestParameter(request), UserHandleUtil.getUserId(), UserHandleUtil.getInsId(), e);
        return ResultData.failed("操作异常(空)");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResultData handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        logger.error("handleIllegalArgumentException :  requestUrl : {} , requestParams : {} , userId : {}, insId : {} ",
                request.getRequestURI(), getRequestParameter(request), UserHandleUtil.getUserId(), UserHandleUtil.getInsId(), e);
        return ResultData.failed(e.getMessage());
    }

    @ExceptionHandler(InvocationTargetException.class)
    @ResponseBody
    public ResultData handleInvocationTargetException(HttpServletRequest request, InvocationTargetException e) {
        logger.error("handleInvocationTargetException : requestUrl : {} , requestParams : {} , userId : {}, insId : {} ",
                request.getRequestURI(), getRequestParameter(request), UserHandleUtil.getUserId(), UserHandleUtil.getInsId(), e.getTargetException());
        return ResultData.failed(e.getTargetException().getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResultData handleException(HttpServletRequest request, Exception e) {
        logger.error("handleException : requestUrl : {} , requestParams : {} , userId : {}, insId : {} ",
                request.getRequestURI(), getRequestParameter(request), UserHandleUtil.getUserId(), UserHandleUtil.getInsId(), e);
        return ResultData.failed(e.getMessage());
    }

    /**
     * 获取请求参数
     * @param request
     * @return
     */
    private String getRequestParameter(HttpServletRequest request) {
        StringBuilder requestParameter = new StringBuilder();
        try {
            requestParameter.append(JSON.toJSONString(request.getParameterMap()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
            requestParameter.append(IOUtils.read(reader));
        } catch (Exception e) {
            logger.error("getRequestParameter failed url :{} ", request.getRequestURI(), e);
        }
        return requestParameter.toString();
    }

}
