package com.aixuexi.vampire.manager;

import com.aixuexi.thor.except.ExceptionCode;
import com.gaosi.api.basicdata.model.bo.BookVersionBo;
import com.gaosi.api.basicdata.model.bo.ExamAreaBo;
import com.gaosi.api.basicdata.model.dto.AddressDTO;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ruanyanjie on 2018/7/3.
 */
@Service("basicDataManager")
public class BasicDataManager {

    private final Logger logger = LoggerFactory.getLogger(BasicDataManager.class);
    @Resource
    private CacheManager cacheManager;

    /**
     * 根据区ID查询省市区地址
     * @param areaIds
     * @return
     */
    public Map<Integer,AddressDTO> getAddressByAreaIds(List<Integer> areaIds) {
        try {
            ImmutableMap<Integer, AddressDTO> all = cacheManager.getCacheBuilderAddress().getAll(areaIds);
            Map<Integer,AddressDTO> addressDTOMap = new HashMap<>(all);
            return addressDTOMap;
        } catch (Exception e) {
            logger.error("查询省市区地址异常 areaIds : {}", areaIds);
            throw new BusinessException(ExceptionCode.UNKNOWN, "查询省市区地址异常");
        }
    }

    /**
     * 根据教材版本ID查询教材版本
     * @param bookVersionIds
     * @return
     */
    public Map<Integer, BookVersionBo> getBookVersionByIds(List<Integer> bookVersionIds) {
        try {
            ImmutableMap<Integer, BookVersionBo> all = cacheManager.getCacheBookVersion().getAll(bookVersionIds);
            Map<Integer, BookVersionBo> bookVersionBoMap = new HashMap<>(all);
            return bookVersionBoMap;
        } catch (Exception e) {
            logger.error("获取教材版本异常 bookVersionIds : {} ",bookVersionIds);
            throw new BusinessException(ExceptionCode.UNKNOWN,"获取教材版本异常");
        }
    }

    /**
     * 根据考区ID查询考区
     * @param examAreaIds
     * @return
     */
    public Map<Integer, ExamAreaBo> getExamAreaByIds(List<Integer> examAreaIds) {
        try {
            ImmutableMap<Integer, ExamAreaBo> all = cacheManager.getCacheExamArea().getAll(examAreaIds);
            Map<Integer, ExamAreaBo> examAreaBoMap = new HashMap<>(all);
            return examAreaBoMap;
        } catch (Exception e) {
            logger.error("获取考区异常 examAreaIds : {} ",examAreaIds);
            throw new BusinessException(ExceptionCode.UNKNOWN,"获取考区异常");
        }
    }
}
