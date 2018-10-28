package com.example.redis.lock;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

/**
 * redis锁实现
 * 思考：
 * redis获取锁失败怎么处理(延时队列)
 */
@Component
@Slf4j
public class RedisLock {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String COMPARE_AND_DELETE =
        "if redis.call('get',KEYS[1]) == ARGV[1]\n" +
            "then\n" +
            "    return redis.call('del',KEYS[1])\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    /**
     * 获取锁
     * @param key redis key
     * @param value redis value
     * @param retryTimes redis重试次数
     * @param timeOut redis超时
     * @return true or false
     */
    public boolean lock(String key, String value, int retryTimes, long timeOut) {
        boolean result = setOfRedis(key, value, timeOut);
        while (!result && retryTimes-- > 1){
            result = setOfRedis(key, value, timeOut);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("redis lock interupted exception");
            }
        }
        return result;
    }

    /**
     * 在spring data中通过execute函数执行redis命令
     * 1、获取jedisconnection
     * 2、redis2.6.0提供了set和expire命令一起执行，保证原子性
     * @param key
     * @param value
     * @param timeOut
     * @return
     */
    private boolean setOfRedis(String key, String value, long timeOut) {
        try {
            Object result = (Object) redisTemplate.execute(new RedisCallback() {
                @Nullable
                @Override
                public Object doInRedis(RedisConnection connection)
                    throws DataAccessException {
                    Jedis jedis = (Jedis) connection.getNativeConnection();
                    return jedis.set(key, value, "NX", "EX", timeOut);
                }
            });
            return !(result == null);
        } catch (Exception e) {
            log.error("redis lock exception", e);
            return false;
        }
    }

    /**
     * 释放锁
     * @param key
     * @return
     */
    public boolean unlock(String key, String value){
        Integer result = (Integer) redisTemplate.execute(
            new DefaultRedisScript(COMPARE_AND_DELETE, String.class), Collections.singletonList(key), value);
        log.info("unlock result:{}", result);
        if (result == null || result == 0){
            log.info("unlock failue,{},{}:", key, value);
            return false;
        }
        return true;
    }




}
