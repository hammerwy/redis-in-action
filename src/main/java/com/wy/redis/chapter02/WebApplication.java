package com.wy.redis.chapter02;

import com.wy.redis.utils.JedisPoolUtil;
import redis.clients.jedis.Jedis;

/**
 * @author wangyong
 * @description
 * @date 2019-04-01
 */
public class WebApplication {

    private Jedis connection = JedisPoolUtil.getConn();

    {
        connection.select(0);
    }

    /**
     * 获取名为"login："的hash中，sub-key为token的用户名
     * @param token
     * @return
     */
    public String checkToken(String token){
        return connection.hget("login:", token);
    }

    public void updateToken(String token, String user, String item){

    }
}
