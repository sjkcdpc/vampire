package com.aixuexi.vampire.interceptor;

import com.aixuexi.thor.keys.CacheKeyManager;
import com.aixuexi.thor.keys.MicroServiceKey;
import com.aixuexi.thor.redis.MyJedisService;
import com.aixuexi.thor.util.IpUtil;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.revolver.keys.RevolverKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 接口限流Interceptor
 * Created by lvxiaodong on 2018/8/21.
 */
public class CurrentLimitingInterceptor implements HandlerInterceptor {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private MyJedisService myJedisService;

    /**
     * Redis键分隔符
     */
    private static final String SPLIT = ".";

    /**
     * 7秒周期的key
     */
    private static final String KEY_CURRENTLIMIT_SMALL = new CacheKeyManager(MicroServiceKey.REVOLVER).of(RevolverKey.CURRENTLIMIT).of("SMALL").getKey();
    /**
     * 11秒周期的key
     */
    private static final String KEY_CURRENTLIMIT_BIG = new CacheKeyManager(MicroServiceKey.REVOLVER).of(RevolverKey.CURRENTLIMIT).of("BIG").getKey();
    /**
     * 3分钟周期的key
     */
    private static final String KEY_CURRENTLIMIT_TOTAL = new CacheKeyManager(MicroServiceKey.REVOLVER).of(RevolverKey.CURRENTLIMIT).of("TOTAL").getKey();

    @Value("${smallLimit}")
    private Long smallLimit;

    @Value("${smallCycle}")
    private Integer smallCycle;

    @Value("${bigLimit}")
    private Long bigLimit;

    @Value("${bigCycle}")
    private Integer bigCycle;

    @Value("${totalLimit}")
    private Long totalLimit;

    @Value("${totalCycle}")
    private Integer totalCycle;

    /**
     * 接口限流拦截
     * @param request
     * @param response
     * @param handler
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 请求接口
            String requestURI = request.getRequestURI();
            // 请求ip
            String remoteAddr = IpUtil.getIpAdrress(request);
            // 请求userId
            Integer userId  = UserHandleUtil.getUserId();

            String suffix = StringUtils.join(new Object[]{userId, IpUtil.ipToLong(remoteAddr), requestURI}, SPLIT);
            String totalKey = KEY_CURRENTLIMIT_TOTAL + SPLIT + suffix;
            String smallKey = KEY_CURRENTLIMIT_SMALL + SPLIT + suffix;
            String bigKey = KEY_CURRENTLIMIT_BIG + SPLIT + suffix;

            long totalTimes = 0L;
            String totalTimesStr = myJedisService.get(totalKey);
            if(StringUtils.isNotBlank(totalTimesStr)){
                totalTimes = Long.parseLong(totalTimesStr);
            }

            if(totalTimes > totalLimit){
                // 如果超过阈值，就返回错误提醒
                handleResponse(response);
                return false;
            }

            initKey(totalKey, smallKey, bigKey);

            Long smallTimes = myJedisService.incr(smallKey);
            Long bigTimes = myJedisService.incr(bigKey);
            if(smallTimes == smallLimit){
                // 刚刚到达阈值的时候累加
                totalTimes = incrTimes(totalTimes, smallKey, totalKey, smallCycle);
            }

            if(bigTimes == bigLimit){
                // 刚刚到达阈值的时候累加
                totalTimes = incrTimes(totalTimes, bigKey, totalKey, bigCycle);
            }

            if(totalTimes > totalLimit){
                // 如果超过阈值，就返回错误提醒
                handleResponse(response);
                return false;
            }
        } catch (Exception e){
            logger.error("CurrentLimitingInterceptor execute failed: {}", e);
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

    /**
     * 处理周期的次数
     * @param totalTimes
     * @param key
     * @param totalKey
     * @param cycle
     * @return
     */
    private long incrTimes(long totalTimes, String key, String totalKey, Integer cycle){
        // 判断key是否变为-1,-2,0，变为-1，重新设置过期时间(测试过程中发现redis有这个现象)
        Long ttl = myJedisService.ttl(key);

        if(ttl <= 0){
            myJedisService.setex(key, "0", cycle);
            return totalTimes;
        }

        totalTimes = myJedisService.incr(totalKey);
        return totalTimes;
    }

    /**
     * 初始化key
     * @param totalKey
     * @param smallKey
     * @param bigKey
     */
    private void initKey(String totalKey, String smallKey, String bigKey){

        // 初始化过期时间
        if(!myJedisService.exists(totalKey)){
            myJedisService.setex(totalKey, "0", totalCycle);
        }

        if(!myJedisService.exists(smallKey)){
            myJedisService.setex(smallKey, "0", smallCycle);
        }

        if(!myJedisService.exists(bigKey)){
            myJedisService.setex(bigKey, "0", bigCycle);
        }
    }

    /**
     * 超过阈值，返回错误信息
     * @param response
     * @throws IOException
     */
    private void handleResponse(HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write("{\"status\":0,\"errorCode\":202,\"errorMessage\":\"系统繁忙，请稍后重试\",\"body\":{}}");
    }
}
