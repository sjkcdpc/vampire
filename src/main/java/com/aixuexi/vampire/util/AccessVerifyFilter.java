package com.aixuexi.vampire.util;

import com.gaosi.api.common.util.CollectionUtils;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;

/**
 * 验证角色
 * Created by gaoxinzhong on 2017/6/8.
 */
public class AccessVerifyFilter implements Filter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.debug("accessVerifyFilter init.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        List<String> roles = UserSessionHandler.getRoles();
        if (CollectionUtils.isNotEmpty(roles) && (roles.contains("super_manager") || roles.contains("manage"))) {
            chain.doFilter(request, response);
            return;
        }
    }

    @Override
    public void destroy() {
        logger.debug("accessVerifyFilter destroy.");
    }
}
