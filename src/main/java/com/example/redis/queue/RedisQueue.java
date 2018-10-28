package com.example.redis.queue;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import redis.clients.jedis.Jedis;

/**
 * redis实现消息队列
 * 1、采用lpush/rpush,lpop/rpop(轮询任务拉高qps,sleep会造成延迟)
 * 2、采用blpop/brpop(一直阻塞(连接闲置时间过长会报异常))
 * 思考
 * 1、redis作为消息队列是否100%可靠
 *   redis设计队列也没有ack机制，消息可能丢失
 *   延迟队列zrem之后执行突然中断，消息会丢失
 */
public class RedisQueue {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * sendmessage
     * @param key
     * @param value
     * @return
     */
    public Long sendMessage(String key, String value){
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * jedis block阻塞命令
     * @param key
     * @return
     */
    public List<String> getMessageForBlock(String key){
        List<String> msgList = (List<String>) redisTemplate.execute(new RedisCallback() {
            @Nullable
            @Override
            public Object doInRedis(RedisConnection connection)
                throws DataAccessException {
                Jedis jedis = (Jedis)connection.getNativeConnection();
                List<String> msgList = null;
                try{
                    msgList = jedis.brpop(0, key);
                }catch (Exception e){
                    //to do block命令闲置一段时间会断开连接，捕获异常

                }
                return msgList;
            }
        });
        return msgList;
    }

    /**
     * rightPop获取message
     * @param key
     * @return
     */
    public String getMessage(String key){
        return (String) redisTemplate.opsForList().rightPop(key);
    }


}
