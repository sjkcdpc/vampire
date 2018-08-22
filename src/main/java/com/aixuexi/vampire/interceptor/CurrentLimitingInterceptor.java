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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 接口限流Interceptor
 * Created by lvxiaodong on 2018/8/21.
 */
public class CurrentLimitingInterceptor implements HandlerInterceptor {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MyJedisService myJedisService;

    /**
     * 7秒周期的key
     */
    private static final String KEY_CURRENTLIMIT_SMALL = new CacheKeyManager(MicroServiceKey.REVOLVER).of(RevolverKey.CURRENTLIMIT).of("SMALL").getKey();
    private static final String KEY_CURRENTLIMIT_SMALL_FLAG = new CacheKeyManager(MicroServiceKey.REVOLVER).of(RevolverKey.CURRENTLIMIT).of("SMALL").of("FLAG").getKey();
    /**
     * 11秒周期的key
     */
    private static final String KEY_CURRENTLIMIT_BIG = new CacheKeyManager(MicroServiceKey.REVOLVER).of(RevolverKey.CURRENTLIMIT).of("BIG").getKey();
    private static final String KEY_CURRENTLIMIT_BIG_FLAG = new CacheKeyManager(MicroServiceKey.REVOLVER).of(RevolverKey.CURRENTLIMIT).of("BIG").of("FLAG").getKey();
    /**
     * 3分钟周期的key
     */
    private static final String KEY_CURRENTLIMIT_TOTAL = new CacheKeyManager(MicroServiceKey.REVOLVER).of(RevolverKey.CURRENTLIMIT).of("TOTAL").getKey();

    @Value("${smallLimit}")
    private Integer smallLimit;

    @Value("${smallCycle}")
    private Integer smallCycle;

    @Value("${bigLimit}")
    private Integer bigLimit;

    @Value("${bigCycle}")
    private Integer bigCycle;

    @Value("${totalLimit}")
    private Integer totalLimit;

    @Value("${totalCycle}")
    private Integer totalCycle;

    /**
     * 接口限流拦截
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("CurrentLimitingInterceptor prehandle.");
        // 请求接口
        String requestURI = request.getRequestURI();
        // 请求ip
        String remoteAddr = IpUtil.getIpAdrress(request);
        // 请求userId
        Integer userId = UserHandleUtil.getUserId();

        String suffix = StringUtils.join(new Object[]{userId, IpUtil.ipToLong(remoteAddr), requestURI}, '.');
        String totalKey = KEY_CURRENTLIMIT_TOTAL + "." + suffix;
        String smallKey = KEY_CURRENTLIMIT_SMALL + "." + suffix;
        String smallFlagKey = KEY_CURRENTLIMIT_SMALL_FLAG + "." + suffix;
        String bigKey = KEY_CURRENTLIMIT_BIG + "." + suffix;
        String bigFlagKey = KEY_CURRENTLIMIT_BIG_FLAG + "." + suffix;

        initKey(suffix);

        Long totalTimes = 0l;
        String totalTimesStr = myJedisService.get(totalKey);
        if(StringUtils.isNotBlank(totalTimesStr)){
            totalTimes = Long.parseLong(totalTimesStr);
        }

        if(totalTimes > totalLimit){
            // 如果超过阈值，就返回错误提醒
            handleResponse(response);
            return false;
        }

        Long smallTimes = myJedisService.incrBy(smallKey, 1);
        Long bigTimes = myJedisService.incrBy(bigKey, 1);
        if(smallTimes > smallLimit){
            totalTimes = incrTimes(totalTimes, smallKey, smallFlagKey, totalKey, smallCycle);
        }

        if(bigTimes > bigLimit){
            totalTimes = incrTimes(totalTimes, bigKey, bigFlagKey, totalKey, bigCycle);
        }

        if(totalTimes > totalLimit){
            // 如果超过阈值，就返回错误提醒
            handleResponse(response);
            return false;
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
     * @param flagKey
     * @param totalKey
     * @param cycle
     * @return
     */
    private long incrTimes(long totalTimes, String key, String flagKey, String totalKey, Integer cycle){
        // 判断key是否变为-1，变为-1，重新设置过期时间(测试过程中发现redis有这个现象)
        Long ttl = myJedisService.ttl(key);
        Long FlagTtl = myJedisService.ttl(flagKey);

        if(ttl == -1 || FlagTtl == -1){
            myJedisService.setex(key, "0", cycle);
            myJedisService.setex(flagKey, "0", cycle);
            return totalTimes;
        }

        // 保证每个周期内只累加一次
        String bigFlag = myJedisService.get(flagKey);
        if("0".equals(bigFlag)){
            totalTimes = myJedisService.incr(totalKey);

            myJedisService.set(flagKey, "1");
        }
        return totalTimes;
    }

    /**
     * 初始化key
     * @param suffix
     */
    private void initKey(String suffix){
        String totalKey = KEY_CURRENTLIMIT_TOTAL + "." + suffix;
        String smallKey = KEY_CURRENTLIMIT_SMALL + "." + suffix;
        String smallFlagKey = KEY_CURRENTLIMIT_SMALL_FLAG + "." + suffix;
        String bigKey = KEY_CURRENTLIMIT_BIG + "." + suffix;
        String bigFlagKey = KEY_CURRENTLIMIT_BIG_FLAG + "." + suffix;

        // 初始化过期时间
        if(!myJedisService.exists(totalKey)){
            commonInit(totalKey, null, totalCycle);
        }

        if(!myJedisService.exists(smallKey)){
            commonInit(smallKey, smallFlagKey, smallCycle);
        }

        if(!myJedisService.exists(bigKey)){
            commonInit(bigKey, bigFlagKey, bigCycle);
        }
    }

    private void commonInit(String key, String flagKey, Integer cycle){
        if(StringUtils.isNotBlank(flagKey)){
            // 伴生key，用于标记该周期内是否已累加
            myJedisService.setex(flagKey,"0", cycle);
        }
        myJedisService.setex(key, "0", cycle);
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
