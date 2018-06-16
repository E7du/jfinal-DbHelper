/**
 * 
 */
package cn.zhuongqi.data.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jfinal.log.Logger;
import com.jfinal.plugin.redis.Cache;

import cn.zhucongqi.data.IDataRecordService;
import cn.zhucongqi.dbhelper.ProductKit;
import cn.zhucongqi.models.DataRecordModel;
import cn.zhucongqi.sqlp.parser.TablePkit;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author Jobsz:Jobsz
 *
 */
public class RedisCacheService implements IDataRecordService {

	private static final long serialVersionUID = 7248288212037585941L;

	//=====PARAMS=====
	
	protected Logger log = Logger.getLogger(this.getClass());
	private static final String REDIS_BASE_REGION = ProductKit.getProductName()+":";
	public static final String MIDDLE_BASE_REGION = REDIS_BASE_REGION+"middle:";
	public static final String ALL_BASE_REGION = RedisCacheService.REDIS_BASE_REGION+"all:";
	
	private String lst = ":lst";
	private String data = ":data:";
	
	//=====DATA===
	
	private RedisCache dataCache = null;
	protected RedisCache getCache(Object data) {
		if (null == this.dataCache) {
			this.dataCache = new RedisCache(data);
		}
		return this.dataCache;
	}

	public boolean dataCacheExist(Object object) {
		String setKey = this.geSetKey(object);
		return this.isExist(setKey, this.getCache(object));
	}
	
	public boolean dataCacheExist(String key, Object object) {
		return this.isExist(key, this.getCache(object));
	}
	
	//=====BIS=====
	
	private RedisCache bisCache = null;
	protected RedisCache getCache(String cacheName) {
		if (null == this.bisCache) {
			this.bisCache = new RedisCache(cacheName);
		}
		return this.bisCache;
	}
	
	public boolean bisCacheExist(String key, String cacheName) {
		return this.isExist(key, this.getCache(cacheName));
	}
	
	//=====COMMON===
	
	private boolean isExist(String key, RedisCache cache) {
		try {
			if (null == cache || (null != cache && null == cache.getCache())) {
				this.log.debug("key { "+key+" } not exist in Cache!");
				return false;
			}
		} catch (JedisException e) {
			e.printStackTrace();
			this.log.error("Redis操作失败 { "+e.getLocalizedMessage()+" }");
			return false;
		}
		return cache.exists(key);
	}
	
	/**
	 * region:tablename:lst
	 * @param data
	 * @return
	 */
	protected String geSetKey(Object data) {
		//table name 
		String tablename = TablePkit.getTableName(data);
		this.log.debug("getSetKey tablename { "+tablename+" }");
		//list key
		return (new StringBuilder().append(RedisCacheService.ALL_BASE_REGION).append(tablename).append(this.lst).toString());
	}
	
	/**
	 * region:tablename:data:primarykey
	 * @param data
	 * @return
	 */
	protected String getHashKey(Object data) {
		//primaryKey
		String primaryKey = this.getPrimaryKey(data);
		return this.getHashKey(data, primaryKey);
	}
	
	/**
	 * region:tablename:data:primarykey
	 * @param data
	 * @return
	 */
	protected String getHashKey(Object data, String primaryKey) {
		//table name 
		String tablename = TablePkit.getTableName(data);
		this.log.debug("getHashKey primaryKey { "+primaryKey+" }, tablename { "+tablename+" }");
		//hash key
		return (new StringBuilder().append(RedisCacheService.ALL_BASE_REGION).append(tablename).append(this.data).append(primaryKey).toString());
	}
	
	/**
	 * 获取主键
	 * @param data
	 * @return
	 */
	private String getPrimaryKey(Object data) {
		String primaryKey = TablePkit.getPrimaryKeyValue(data).toString();
		if (null == primaryKey || "".equals(primaryKey)) {
			throw new IllegalArgumentException("\nthe primarykey is null in "+data.getClass());
		}
		this.log.debug("getPrimaryKey primaryKey { "+primaryKey+" }");
		return primaryKey;
	}
	
	/**
	 * 写入数据到hash
	 * @param data
	 */
	private void saveData(Cache cache, Object data) {
		if (data instanceof DataRecordModel<?>) {
			//put to hash
			String hashKey = this.getHashKey(data);
			this.log.debug("save hashKey { "+hashKey+" }");
			DataRecordModel<?> _data = (DataRecordModel<?>) data;
			cache.hmset(hashKey, _data);
		}
	}

	/**
	 * 找到一个数据
	 * @param condition
	 * @param primarykey
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> findOne(Object condition, String primarykey) {
		Map<String, Object> data = new HashMap<String, Object>();
		try {
			Cache _cache = this.getCache(condition).getCache();
			//find from hash
			String hashKey = this.getHashKey(condition, primarykey);
			data = _cache.hgetAll(hashKey);
			if (null == data) {
				this.log.debug("primarykey => { "+primarykey+" }, Not found!! ");
			}
		} catch (JedisException e) {
			e.printStackTrace();
			this.log.error("Redis操作失败 { "+e.getLocalizedMessage()+" }");
		}
		return data;
	}
	
	@Override
	public boolean save(Object data) {
		try {
			Cache _cache = this.getCache(data).getCache();
			//primaryKey
			String primaryKey = this.getPrimaryKey(data);
			//put to SortedSet
			String setKey = this.geSetKey(data);
			this.log.debug("save setKey { "+setKey+" }");
			_cache.zadd(setKey, Double.valueOf(primaryKey), primaryKey);
			//put to hash
			this.saveData(_cache, data);
		} catch (JedisException e) {
			e.printStackTrace();
			this.log.error("Redis操作失败 { "+e.getLocalizedMessage()+" }");
		}
		return true;
	}
	
	@Override
	@Deprecated
	public boolean update(Object data) {
//		try {
//			//put to hash
//			Cache _cache = this.getCache(data).getCache();
//			this.saveData(_cache, data);
//		} catch (JedisException e) {
//			e.printStackTrace();
//		}
		return true;
	}
	
	@Override
	public boolean delete(Object condition) {
		try {
			Cache _cache = this.getCache(condition).getCache();
			String hashKey = this.getHashKey(condition);
			//delete from hash
			this.log.debug("delete hashkey { "+hashKey+" }");
			_cache.del(hashKey);
			String setKey = this.geSetKey(condition);
			//delete from SortedSet
			this.log.debug("delete listkey { "+setKey+" }");
			//primaryKey
			String primaryKey = this.getPrimaryKey(condition);
			_cache.zrem(setKey, primaryKey);
		} catch (JedisException e) {
			e.printStackTrace();
			this.log.error("Redis操作失败 { "+e.getLocalizedMessage()+" }");
		}	
		return true;
	}
	
	@Override
	public Map<String, Object> findOne(Object condition) {
		//find from hash
		String primarykey = this.getPrimaryKey(condition);
		this.log.debug("find { "+primarykey+" }");
		return this.findOne(condition, primarykey);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> findLast(Object condition) {
		Map<String, Object> data = new HashMap<String, Object>();
		try {
			Cache _cache = this.getCache(condition).getCache();
			String setKey = this.geSetKey(condition);
			//获取最有一个member，start = -1，end = -1 => 为最后一个
			Set<String> setData = _cache.zrange(setKey, -1, -1);
			this.log.debug("setData-last-members\n{ "+setData+" }");
			if (!setData.isEmpty()) {
				TablePkit.setPrimaryKeyValue(condition, setData.iterator().next().toString());
				data = this.findOne(condition);
			}
		} catch (JedisException e) {
			e.printStackTrace();
			this.log.error("Redis操作失败 { "+e.getLocalizedMessage()+" }");
		}
		return data;
	}

	@Override
	public List<Map<String, Object>> findAll(Object condition) {
		this.log.debug("find all to list { "+condition+" }");
		return this.paginate(condition, 1, -1);
	}

	@Override
	public List<Map<String, Object>> paginate(Object condition, Integer pageNumber, Integer pageSize) {
		List<Map<String, Object>> all = new ArrayList<Map<String, Object>>();
		try {
			RedisCache cache = this.getCache(condition);
			//get data from SortedSet
			String setKey = this.geSetKey(condition);
			Set<String> primarykeys = this.paginateSortSet(cache.getCache(), setKey, pageNumber, pageSize);
			for (String primarykey : primarykeys) {
				Map<String, Object> data = this.findOne(condition, primarykey);
				if (null != data && data.size() != 0) {
					all.add(data);
				}
			}
		} catch (JedisException e) {
			e.printStackTrace();
			this.log.error("Redis操作失败 { "+e.getLocalizedMessage()+" }");
		}
		return all;
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> paginateSortSet(Cache cache, String setKey, Integer pageNumber, Integer pageSize) {
		if (!cache.exists(setKey)) {
			return (new HashSet<String>());
		}
		long size = cache.zcard(setKey);
		if (size == 0) {
			return (new HashSet<String>());
		}
		
		long start = 0L;
		long end = 0L;
		
		if (pageNumber < 0) {
			pageNumber = 1;
		}
		//all
		if (pageNumber == 1 && pageSize == -1) {
			end = -1L;
		}else{
			//part of all
			start = (pageNumber-1) * pageSize;
			end = start + pageSize - 1;
			if (end >= size) {
				end = -1;
			}
		}
		return cache.zrange(setKey, start, end);
	}

	@Override
	public List<Map<String, Object>> userQuerySql(String sql, Object[] params) {
		this.log.debug("call userQuerySql");
		throw new IllegalArgumentException("\nNot available.");
	}

	@Override
	public boolean userInsertSql(String sql, Object[] params) {
		this.log.debug("call userInsertSql");
		throw new IllegalArgumentException("\nNot available.");
	}

	@Override
	public boolean userUpdateSql(String sql, Object[] params) {
		this.log.debug("call userUpdateSql");
		throw new IllegalArgumentException("\nNot available.");
	}
}
