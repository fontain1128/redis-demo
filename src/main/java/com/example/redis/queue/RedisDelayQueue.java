package com.example.redis.queue;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

/**
 *
 * redis实现延迟队列
 * (场景：争取锁失败如果放入普通队列，消息又会立即取出消费，此时可能还会争取锁失败，起不到延迟作用)
 * 1、采用zet实现
 * 2、通过zadd添加(score即延迟时间),zrange取出,zrem移除
 * 思考
 * 1、考虑zrange和zrem不是原子性(多个线程zrange之后只能有一个线程zrem成功，通过lua内置脚本解决)
 * 2、如果消息在执行过程中执行失败，可以考虑保存数据库，不建议再推送队列，因为可能还会执行失败。
 */
@Slf4j
public class RedisDelayQueue {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * delay queue impl
     * @param key
     * @param value
     * @param score
     * @param retryTime
     * @return
     */
    public boolean sendDelayQueue(String key, String value, Long score, int retryTime){
        int i = 0;
        boolean result = redisTemplate.opsForZSet().add(key, value, score);
        if (result){
            return result;
        }else{
            while (i++ < retryTime){
                if (!result){
                    result = redisTemplate.opsForZSet().add(key, value, score);
                }else {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * get from delay queue
     * to do keep zrange and zet command
     * @param key
     * @return
     */
    public String getMsgFromDelayQueue(String key){
        Long currentTime = System.currentTimeMillis();
        Set<String> valueSet = redisTemplate.opsForZSet().range(key, 0, currentTime);
        String msg = null;
        if (valueSet != null){
            msg = valueSet.iterator().next();
            try{
                Long result = redisTemplate.opsForZSet().remove(key, msg);
                if(result < 0){
                    return null;
                }
            }catch (Exception e){
                log.error("msg remove exception {},{}", key, msg);
            }
        }
        return msg;
    }
}
