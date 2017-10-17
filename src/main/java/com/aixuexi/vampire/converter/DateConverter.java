package com.aixuexi.vampire.converter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author zhouxiong
 * on 2017/8/14 17:01.
 */
public class DateConverter implements Converter<String, Date> {
    private final Logger logger = LoggerFactory.getLogger(DateConverter.class);

    @Override
    public Date convert(String source) {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        if(StringUtils.isNotBlank(source)){
            try {
                return sdf.parse(source);
            } catch (ParseException e) {
                logger.error("",e);
            }
        }
        return null;
    }
}
