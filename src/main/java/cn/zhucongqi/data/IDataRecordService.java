/**
 * 
 */
package cn.zhucongqi.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 数据服务接口
 * @author Jobsz:Jobsz
 */
public interface IDataRecordService extends Serializable {

	/**
	 * 保存数据
	 * @param data
	 * @return
	 */
	public boolean save(Object data);
	
	/**
	 * 更新数据
	 * @param data
	 * @return
	 */
	public boolean update(Object data);
	
	/**
	 * 删除数据
	 * @param condition
	 * @return
	 */
	public boolean delete(Object condition);

	/**
	 * 找一个Map数据
	 * @param condition
	 * @return
	 */
	public Map<String, Object> findOne(Object condition);
	
	/**
	 * 找最后一个数据
	 * @param condition
	 * @return
	 */
	public Map<String, Object> findLast(Object condition);
	
	/**
	 * 基于某条件查找所有数据
	 * @param condition
	 * @return
	 */
	public List<Map<String, Object>> findAll(Object condition);
	
	/**
	 * 分页
	 * @param condition
	 * @param pageNumber
	 * @param pageSize
	 * @return
	 */
	public List<Map<String, Object>> paginate(Object condition, Integer pageNumber, Integer pageSize);
	
	/**
	 * 自定义方法获取数据
	 * @param sql
	 * @return
	 */
	public List<Map<String, Object>> userQuerySql(String sql, Object[] params);
	
	/**
	 * 自定义方法插入数据
	 * @param sql
	 * @note DBService.id 为 id
	 * @return
	 */
	public boolean userInsertSql(String sql, Object[] params);
	
	/**
	 * 自定义方法更新/删除数据
	 * @param sql
	 * @return
	 */
	public boolean userUpdateSql(String sql, Object[] params);
}
