package com.aixuexi.vampire.test;

import com.aixuexi.vampire.util.ExpressUtil;
import com.gaosi.api.revolver.vo.ExpressVo;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * Created by ruanyanjie on 2017/9/27.
 */
public class ConfigPropertyTest extends BaseTest {

    @Resource
    private ExpressUtil expressUtil;

    @Test
    public void fun1(){
        System.out.println(expressUtil.getExpressVos().get(0).getTips());
        System.out.println(expressUtil.getSplitTips());
    }
}
