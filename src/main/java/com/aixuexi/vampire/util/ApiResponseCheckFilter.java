package com.aixuexi.vampire.util;

import com.alibaba.dubbo.rpc.*;
import com.gaosi.api.common.to.ApiResponse;

/**
 * @author zhouxiong
 * on 2017/12/29 12:01.
 */
public class ApiResponseCheckFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result = invoker.invoke(invocation);
        Object resultValue = result.getValue();
        if (result instanceof RpcResult) {
            RpcResult rpcResult = (RpcResult) result;
            if (null == resultValue){
                rpcResult.setException(new NullPointerException("result is null."));
            }else if (resultValue instanceof ApiResponse){
                ApiResponse apiResponse =(ApiResponse)resultValue;
                if (apiResponse.isNotSuccess()){
                    rpcResult.setException(new RuntimeException(apiResponse.getMessage()));
                }
            }else if (resultValue instanceof com.aixuexi.thor.response.ApiResponse){
                com.aixuexi.thor.response.ApiResponse apiResponse =(com.aixuexi.thor.response.ApiResponse)resultValue;
                if (apiResponse.isNotSuccess()){
                    rpcResult.setException(new RuntimeException(apiResponse.getMessage()));
                }
            }

        }
        return result;
    }
}
