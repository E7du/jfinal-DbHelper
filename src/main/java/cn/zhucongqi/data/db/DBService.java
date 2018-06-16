/**
 * 
 */
package cn.zhucongqi.data.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jfinal.log.Logger;

import cn.zhucongqi.data.IDataRecordService;
import cn.zhucongqi.dbhelper.DbHelperKit;
import cn.zhucongqi.dbhelper.DbHelperTx;
import cn.zhucongqi.dbhelper.DbTx;
import cn.zhucongqi.models.DataRecordModel;
import cn.zhucongqi.sqlp.parser.SqlPkit;
import cn.zhucongqi.sqlp.parser.TablePkit;
import cn.zhuongqi.data.redis.RedisCacheService;

/**
 * @author Jobsz:Jobsz
 *
 */
public class DBService implements IDataRecordService{

	private static final long serialVersionUID = 7248288212037585941L;
	
	protected Logger log = Logger.getLogger(this.getClass());
	protected RedisCacheService mainRedisService = new RedisCacheService();
	// generate id
	protected String id = null;
	
	protected DbHelperKit db = null;
	
	public DBService() {
		this.db = DbHelperKit.use();
	}
	
	public DBService(String cfgName) {
		this.db = DbHelperKit.use(cfgName);
	}

	/**
	 * 获取数据库的id
	 * @return
	 */
	public String getGenerateId() {
		return this.id;
	}
	
	/**
	 * 保存数据  <br/>
	 * 使用 {@link #getGenerateId()}获取generateid
	 */
	@Override
	public boolean save(final Object data) {
		List<Object> params = new ArrayList<Object>();
		String selectSql = SqlPkit.isDataExist(data, params);
		Map<String, Object> obj = null;
		if (null != selectSql) {
			obj = this.db.queryFirst(selectSql, params.toArray());	
		}
		
		if (null != obj && obj.size() != 0) {
			//数据已经存在
			this.log.error("data exist!!!");
			return false;
		}
		
		boolean ret = DbHelperTx.execute(new DbTx() {
			private String sqlStatement = null;
			public void sql() {
				//数据不存在,写入
				List<Object> params = new ArrayList<Object>();
				String saveSql = SqlPkit.save(data, params);
				this.sqlStatement = saveSql;
				DBService.this.log.debug("write sql=>"+saveSql+", params=>"+params);
				DBService.this.id = DBService.this.db.insert(saveSql, params.toArray()).toString();
				if (null != DBService.this.id && !"".equals(DBService.this.id)) {
					TablePkit.setPrimaryKeyValue(data, DBService.this.id);
					DataRecordModel<?> _data = (DataRecordModel<?>) data;
					DBService.this.mainRedisService.save(_data.fill());
				}
			}
			@Override
			public void error(Exception e) {
				e.printStackTrace();
				DBService.this.log.error("操作{ "+this.sqlStatement+" }出错了=>"+e.getLocalizedMessage());
			}
		});
		return ret;
	}

	@Override
	public boolean update(final Object data) {
		boolean ret =  DbHelperTx.execute(new DbTx() {
			private String sqlStatement = null;
			@Override
			public void sql() {
				List<Object> params = new ArrayList<Object>();
				this.sqlStatement = SqlPkit.update(data, params);
				int affectSize = DBService.this.db.update(this.sqlStatement, params.toArray());
				if (affectSize != 0) {
					// 修改成功，获取数据库中最新的数据放到缓存中
					DataRecordModel<?> _data = (DataRecordModel<?>) data;
					DBService.this.mainRedisService.save(_data.fill());
				}
			}
			
			@Override
			public void error(Exception e) {
				e.printStackTrace();
				DBService.this.log.error("操作{ "+this.sqlStatement+" }出错了=>"+e.getLocalizedMessage());
			}
		});
		return ret;
	}

	@Override
	public boolean delete(final Object condition) {
		boolean ret = DbHelperTx.execute(new DbTx() {
			private String sqlStatement = null;
			@Override
			public void sql() {
				List<Object> params = new ArrayList<Object>();
				this.sqlStatement = SqlPkit.delete(condition, params);
				int affectSize = DBService.this.db.update(this.sqlStatement, params.toArray());
				if (affectSize != 0) {
					DBService.this.mainRedisService.delete(condition);
				}
			}
			
			@Override
			public void error(Exception e) {
				e.printStackTrace();
				DBService.this.log.error("操作{ "+this.sqlStatement+" }出错了=>"+e.getLocalizedMessage());
			}
		});
		return ret;
	}

	@Override
	public Map<String, Object> findOne(Object condition) {
		if (!TablePkit.isPrimaryKeyValueNull(condition)) {
			Map<String, Object> obj = this.mainRedisService.findOne(condition);
			if (null != obj && obj.size() > 0) {
				return obj;
			}
		}		
		return (Map<String, Object>) this.queryAndSaveData(condition, SqlPkit.SELECT_FIRST);
	}
	
	@Override
	public Map<String, Object> findLast(Object condition) {
		Map<String, Object> obj = this.mainRedisService.findLast(condition);
		if (null != obj && obj.size() > 0) {
			return obj;
		}
		return (Map<String, Object>) this.queryAndSaveData(condition, SqlPkit.SELECT_LAST);
	}
	
	private Map<String, Object> queryAndSaveData(Object data, int pos) {
		List<Object> params = new ArrayList<Object>();
		String findOneSql = null;
		if (pos == SqlPkit.SELECT_FIRST) {
			findOneSql = SqlPkit.selectFirst(data, params);
		}else if(pos == SqlPkit.SELECT_LAST) {
			findOneSql = SqlPkit.selectLast(data, params);
		}
		this.log.debug("queryAndSaveData-sql { pos="+((pos==SqlPkit.SELECT_FIRST)?"first":"last")+", sql="+findOneSql+", params="+params+" } ");
		DataRecordModel<?> _data = (DataRecordModel<?>)data;
		Map<String, Object> obj = this.db.queryFirst(findOneSql, params.toArray());
		if (null != obj && obj.size() > 0) {
			_data = _data.parser(obj);
			this.mainRedisService.save(_data);	
		}
		return obj;
	}
	
	public void saveAllToRedis(List<Map<String, Object>> list, Object condition) {
		int size = list.size();
		Map<String, Object> obj = null;
		DataRecordModel<?> d = null;
		try {
			Object data = condition.getClass().newInstance();
			d = (DataRecordModel<?>)data;
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			this.log.error("saveAllToRedis Error { "+e.getLocalizedMessage()+" }");
		}
		this.log.debug("saveAllObject{ "+d.getClass()+" } ToRedis");
		for (int i = 0; i < size; i++) {
			obj = list.get(i);
			this.mainRedisService.save(d.parser(obj));
		}
	}

	@Override
	public List<Map<String, Object>> findAll(Object condition) {
		List<Map<String, Object>> data = this.mainRedisService.findAll(condition);
		if (null != data && data.size() > 0) {
			return data;
		}

		List<Object> params = new ArrayList<Object>();
		String findAllSql = SqlPkit.select(condition, params);
		data = this.db.query(findAllSql, params.toArray());
		if (null != data && data.size() == 0) {
			return data;
		}
		this.saveAllToRedis(data, condition);
		return data;
	}

	@Override
	public List<Map<String, Object>> paginate(Object condition, Integer pageNumber, Integer pageSize) {
		if (pageNumber < 1) {
			pageNumber = 1;
		}
		if (pageSize < 0) {
			pageSize = 10;
		}
		
		List<Map<String, Object>> data = this.mainRedisService.paginate(condition, pageNumber, pageSize);
		if (null != data && data.size() > 0) {
			return data;
		}

		List<Object> params = new ArrayList<Object>();
		String paginateSql = SqlPkit.select(condition, params, (pageNumber-1)*pageSize, pageSize);
		data = this.db.query(paginateSql, params.toArray());
		 if (null != data && data.size() == 0) {
		    	return data;
		  }
		this.saveAllToRedis(data, condition);
		return data;
	}

	@Override
	public List<Map<String, Object>> userQuerySql(final String sql, final Object[] params) {
		if (null == sql) {
			throw new IllegalArgumentException("the sql parameter can't be null");
		}
		return this.db.query(sql, params);
	}
	
	@Override
	public boolean userInsertSql(final String sql, final Object[] params) {
		if (null == sql) {
			throw new IllegalArgumentException("the sql parameter can't be null");
		}
		
		return DbHelperTx.execute(new DbTx() {
			public void sql() {
				DBService.this.log.debug("write sql=>"+sql+", params=>"+params);
				String id = DBService.this.db.insert(sql, params).toString();
				if (null != id && !"".equals(id)) {
					DBService.this.id = id;
				}
			}
			@Override
			public void error(Exception e) {
				e.printStackTrace();
				DBService.this.log.error("操作{ "+sql+" }出错了=>"+e.getLocalizedMessage());
			}
		});
	}

	@Override
	public boolean userUpdateSql(final String sql, final Object[] params) {
		if (null == sql) {
			throw new IllegalArgumentException("the sql parameter can't be null");
		}

		return DbHelperTx.execute(new DbTx() {
			public void sql() {
				DBService.this.log.debug("write sql=>"+sql+", params=>"+params);
				DBService.this.db.update(sql, params);
			}
			@Override
			public void error(Exception e) {
				e.printStackTrace();
				DBService.this.log.error("操作{ "+sql+" }出错了=>"+e.getLocalizedMessage());
			}
		});
	}
}
