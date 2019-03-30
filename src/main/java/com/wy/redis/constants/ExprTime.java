package com.wy.redis.constants;

/**
 * @author wangyong
 * @description redis过期时间,以秒为单位
 * @date 2019-03-30
 */
public interface ExprTime {
    /**
     * 一个小时
     */
    int EXRP_HOUR = 60 * 60;
    /**
     * 半天
     */
    int EXRP_HALF_DAY = 60 * 60 * 12;
    /**
     * 一天
     */
    int EXRP_DAY = 60 * 60 * 24;
    /**
     * 一个月
     */
    int EXRP_MONTH = 60 * 60 * 24 * 30;
    /**
     * 一周
     */
    int EXRP_WEEK = 7 * 60 * 60 * 24;
}
