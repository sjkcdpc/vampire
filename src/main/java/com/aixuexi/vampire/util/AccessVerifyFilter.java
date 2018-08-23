package com.aixuexi.vampire.util;

import com.gaosi.api.common.util.CollectionUtils;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 验证角色
 * Created by gaoxinzhong on 2017/6/8.
 */
public class AccessVerifyFilter implements Filter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String ignoreUrlRegex;

    public void setIgnoreUrlRegex(String ignoreUrlRegex) {
        this.ignoreUrlRegex = ignoreUrlRegex;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.debug("accessVerifyFilter init.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String requestURI = req.getRequestURI();
        if (Pattern.matches(ignoreUrlRegex, requestURI)) {
            chain.doFilter(request, response);
        } else {
            List<String> roles = UserSessionHandler.getRoles();
            if (CollectionUtils.isNotEmpty(roles) && (roles.contains("super_manager") || roles.contains("manage"))) {
                chain.doFilter(request, response);
            }
        }
    }

    @Override
    public void destroy() {
        logger.debug("accessVerifyFilter destroy.");
    }
}
