package com.example.redis;

import com.example.redis.lock.RedisLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisLockTest {

	@Autowired
	private RedisLock redisLock;
	@Autowired
	private RedisTemplate redisTemplate;

	@Test
	public void test() throws InterruptedException {
		//线程1获取锁
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				redisLock.lock("lock:key:3", "123", 2, 60);
				//redisTemplate.opsForValue().set("lock:key", "lock:value", 5000);
			}
		});
		//线程2获取锁
//		Thread t2 = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				redisLock.lock("lock:key:1", "lock:value1", 2, 20);
//			}
//		});
		//先启动线程1
		t.start();
		//主线程休眠几秒
		Thread.sleep(3000);
		//释放锁
		redisLock.unlock("lock:key:3", "123");
//		Thread.sleep(12000);
//		t2.start();
//		Thread.sleep(3000);
//		Object object = redisTemplate.hasKey("lock:key:3");
//		System.out.println(object);
	}

}
