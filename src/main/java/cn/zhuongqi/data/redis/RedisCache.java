/**
 * 
 */
package cn.zhuongqi.data.redis;

import com.jfinal.kit.StrKit;
import com.jfinal.plugin.redis.Cache;
import com.jfinal.plugin.redis.Redis;

import cn.zhucongqi.sqlp.parser.TablePkit;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author Jobsz:Jobsz
 *
 */
public class RedisCache {
	
	private Cache cache = null;
	
	public RedisCache(Object data) {
		String cacheName = TablePkit.getTableCache(data);
		this.init(cacheName);
	}
	
	public RedisCache(String cacheName) {
		this.init(cacheName);
	}
	
	private void init(String cacheName) {
		if (StrKit.isBlank(cacheName) 
				|| (StrKit.notBlank(cacheName) && "default".equals(cacheName))) {
			this.cache = Redis.use();	
		} else {
			this.cache = Redis.use(cacheName);
		}
	}
	
	public Cache getCache() throws JedisException {
		if (null == this.cache) {
			throw (new JedisConnectionException("Redis is not running..."));
		}
		return this.cache;
	}

	public boolean exists(String key) {
		return this.getCache() == null ? false : this.cache.exists(key);
	}
	
//	public boolean running() {
//		if (null == this.cache) {
//			return false;
//		}
//		// check redis running
//		if (!this.cache.ping().toLowerCase().equals("pong")) {
//			return false;
//		}
//		return true;
//	}
}
