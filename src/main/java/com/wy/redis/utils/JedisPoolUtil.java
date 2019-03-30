package com.wy.redis.utils;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.Properties;

/**
 * @author wangyong
 * @description
 * @date 2019-03-30
 */
@Slf4j
public class JedisPoolUtil {
    private static JedisPool jedisPool;

    static {
        initPool("config/");
    }

    /**
     * 初始化连接池
     *
     * @param configPath
     */
    public static void initPool(String configPath) {
        Properties properties = new Properties();
        try {
            properties.load(JedisPoolUtil.class.getClassLoader().getResourceAsStream(configPath + "redis.properties"));
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(Integer.parseInt(properties.getProperty("redis.pool.maxActive")));
            config.setMaxIdle(Integer.parseInt(properties.getProperty("redis.pool.maxIdle")));
            config.setMinIdle(Integer.parseInt(properties.getProperty("redis.pool.minIdle")));
            config.setMaxWaitMillis(Integer.parseInt(properties.getProperty("redis.pool.maxWait")));
            config.setTestOnBorrow(Boolean.parseBoolean(properties.getProperty("redis.pool.testOnBorrow")));
            config.setTestOnReturn(Boolean.parseBoolean(properties.getProperty("redis.pool.testOnReturn")));
            config.setTestWhileIdle(Boolean.parseBoolean(properties.getProperty("redis.pool.testWhileIdle")));
            String host = properties.getProperty("redis.host");
            Integer port = Integer.parseInt(properties.getProperty("redis.port"));
            Integer timeout = Integer.parseInt(properties.getProperty("redis.timeout"));
            jedisPool = new JedisPool(config, host, port, timeout);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("redis初始化完成！");
    }

    /**
     * 从连接池中获取一个连接
     *
     * @return
     */
    public static Jedis getConn() {
        return jedisPool.getResource();
    }

    /**
     * 关闭连接池
     */
    public static void closePool(){
        jedisPool.close();
    }


    public static String set(String key, String value) {
        Jedis jedis = jedisPool.getResource();
        jedis.select(0);
        String result = jedis.set(key, value);
        jedis.close();
        return result;
    }

    public static String get(String key) {
        Jedis jedis = jedisPool.getResource();
        jedis.select(0);
        String result = jedis.get(key);
        jedis.close();
        return result;
    }

    public static void main(String[] args) {
        initPool("config/");
        System.out.println(set("key", "value"));
        System.out.println(get("key"));
        jedisPool.close();
    }


}
