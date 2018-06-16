/**
 * 
 */
package cn.zhucongqi.sqlp.parser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.zhucongqi.annotation.sqlp.Table;

/**
 * @author Jobsz
 *
 */
public class SqlPkit {

	private static final String select = "SELECT";
	private static final String update = "UPDATE";
	private static final String set = "SET";
	private static final String delete = "DELETE";
	private static final String insert = "INSERT INTO";
	private static final String values = "VALUES";
	private static final String from = "FROM";
	private static final String where = "WHERE";
	private static final String in = "IN";
	private static final String and = "AND";
	private static final String or = "OR";
	private static final String limit = "LIMIT";
	private static final String space = " ";
	private static final String defaulteq = "1 = 1";
	private static final String orderBy = "ORDER BY";
	private static final String desc = "DESC";
	
	public static final int SELECT_FIRST = 1;
	public static final int SELECT_LAST = 2;
	
	///========Common====
	
	/**
	 * 获取子字符串的数量
	 * @param str
	 * @param find
	 * @return
	 */
	private static final int getSubStringCount(String str, String find) {
		int count = 0;
		int index = -1;
		while ((index = str.indexOf(find, index)) > -1) {
			++index;
			++count;
		}
		return count;
	}
	
	/**
	 * limit 限制条件
	 * @param condition
	 * @return
	 */
	private static final String _limit(Integer... condition) {
		int len = condition.length;
		if (len > 3) {
			throw new IllegalArgumentException("wrong number of arguments for set, condition length can not great 3.");
		}
		if (0 == len) {
			throw new IllegalArgumentException("if you want to use limit , you must insert at least one parameter");
		}
		StringBuilder limitSql = new StringBuilder();
		limitSql.append(space).append(limit).append(space);
		switch (len) {
		case 1: {
			limitSql.append("1");
		}
			break;
		case 2:{
			Integer start = condition[0];
			Integer end = condition[1];
			if (start > end) {
				Integer tmp = end;
				end = start;
				start = tmp;
			}
			limitSql.append(start).append(",").append(end);
		}
			break;
		}
		
		return limitSql.toString();
	}
	
	/**
	 * 校验数据是否合法
	 * @param data
	 * @param params
	 */
	private static void validate(Object data, List<Object> params) {
		if (null == data) {
			throw new IllegalArgumentException("\nthe data can not null.");
		}
		
		if (null == params) {
			throw new IllegalArgumentException("\nthe params is null, init first.=>\nList<String> params = new ArrayList<String>().");
		}
		

		Table table = data.getClass().getAnnotation(Table.class);
		if (null == table) {
			throw new IllegalArgumentException("\nthe Table annotation cannout found in data "+data.getClass());
		}
	}
	
	/**
	 * 获取主键 Field
	 * @param data
	 * @return
	 */
	private static Field getPrimaryKeyField(Object data) {
		Field primaryKeyField = TablePkit.getPrimaryKeyField(data);
		if (null == primaryKeyField) {
			throw new IllegalArgumentException("\nthe PrimaryKey is not found in "+data.getClass()+".\nplease add @Table.PrimaryKey in primarykey field.");
		}
		return primaryKeyField;
	}
	
	/**
	 * 获取除了主键以外的其他字段
	 * @param data
	 * @return
	 */
	private static List<Field> getExcludePrimaryKeyFields(Object data) {
		return TablePkit.getHasValueFields(data, TablePkit.getExcludePrimaryKeyFields(data));
	}
	
	/**
	 * 把含有","或"|"的字段的值分析处理成 Set
	 * @param data
	 * @param fieldName
	 * @param val
	 * @param contain
	 * @return
	 */
	private static Set<String> parserFieldValToSet(Object data, String fieldName, String val, String contain) {
		String[] strValSplited = val.split(contain);
		int len = strValSplited.length;
		if ("\\|".equals(contain)) {
			contain = "|";
		}
		if (len != (SqlPkit.getSubStringCount(val, contain)+1)) {
			String template = null;
			String sqlFunc = null;
			if (",".equals(contain)) {
				template = "val0,val1,val2";
				sqlFunc = SqlPkit.in;
			}else if("\\|".equals(contain)) {
				template = "val0|val1|val2";
				contain = "|";
				sqlFunc = SqlPkit.or;
			}
			throw new IllegalArgumentException("\nthe field '"+fieldName+"' values ["+val+"] count is invalid in "+data.getClass()+".\nthe '"+contain+"' as sql '"+sqlFunc+"' logic, template is ["+template+"].");
		}
		Set<String> set = new HashSet<String>();
		set.addAll(Arrays.asList(strValSplited));
		return set;
	}
	
	/**
	 * 分析字段值: ","分割数据使用 SQL 的 IN,"|"使用 SQL 的 OR
	 * @param data
	 * @param fieldName
	 * @param val
	 * @param params
	 * @return
	 */
	private static String parserFieldVal(Object data, String fieldName, Object val, List<Object> params) {
		StringBuilder sql = new StringBuilder();
		boolean flag = true;
		String column = TablePkit.getColumnName(data, fieldName);
		String originType = TablePkit.getColumnOriginType(data, fieldName);
		boolean isIntLike = originType.equals(Table.ColumnOriginType.INT) 
								|| originType.equals(Table.ColumnOriginType.BIGINT) 
								|| originType.equals(Table.ColumnOriginType.LONG);
		if (val instanceof String) {
			String strVal = val.toString();
			if (strVal.contains("，")) {
				throw new IllegalArgumentException("\nthe values ["+val+"], Can not use '，'split.");
			}
			if (strVal.contains("&")) {
				throw new IllegalArgumentException("\nthe values ["+val+"], Can not use '&' split in one field.");
			}
			if (strVal.contains("|") && strVal.contains("&")) {
				throw new IllegalArgumentException("\nthe values ["+val+"], Can not use '|' and '&' split at same time.");
			}
			// "," split use "IN"
			int len = 0;
			int idx = 0;
			Set<String> set = null;
			if (strVal.contains(",")) {
				sql.append(column);
				sql.append(SqlPkit.space);
				sql.append(SqlPkit.in);
				sql.append(SqlPkit.space);
				sql.append("(");
				
				//去除重复的参数
				set = SqlPkit.parserFieldValToSet(data, fieldName, strVal, ",");
				
				idx = 0;
				//更新新的长度
				len = set.size();
				for (String _field : set) {
					sql.append("?");
					if (idx != len-1) {
						sql.append(",");
						sql.append(SqlPkit.space);
					}
					params.add(_field);
					idx++;
				}
				sql.append(")");
			} 
			// "|", split use "OR"
			else if(strVal.contains("|")) {
				//去除重复的参数
				set = SqlPkit.parserFieldValToSet(data, fieldName, strVal, "\\|");
				
				idx = 0;
				len = set.size();
				for (String _field : set) {
					sql.append(column);
					sql.append(SqlPkit.space);
					sql.append("=");
					sql.append(SqlPkit.space);
					sql.append("?");
					if (idx != len-1) {
						sql.append(SqlPkit.space);
						sql.append(SqlPkit.or);
						sql.append(SqlPkit.space);
					}
					params.add(_field);
					idx++;
				}
			}
			// start ">" or "<"
			else if (((strVal.startsWith(">") 
					&& SqlPkit.getSubStringCount(strVal, ">")==1)
					|| (strVal.startsWith("<")
					&& SqlPkit.getSubStringCount(strVal, "<")==1))
					&& isIntLike) {
				sql.append(column);
				sql.append(SqlPkit.space);
				sql.append((val.toString().charAt(0)));
				sql.append(SqlPkit.space);
				sql.append("?");
				StringBuilder valStr = new StringBuilder();
				valStr.append(val);
				params.add(valStr.deleteCharAt(0).toString());
			}
			else{
				flag = false;
			}
		}
		if (!flag) {
			sql.append(column);
			sql.append(SqlPkit.space);
			sql.append("=");
			sql.append(SqlPkit.space);
			sql.append("?");
			params.add(val);
		}
		return sql.toString();
	}
	
	private static final String selectOrDelete(String sqlFunc, Object data, List<Object> params) {
		// 校验数据
		SqlPkit.validate(data, params);
		
		if (null == sqlFunc || "".equals(sqlFunc)) {
			sqlFunc = SqlPkit.select;
		}

		StringBuilder sql = new StringBuilder();
		int idx = 0;
		int len = 0;
		if (sqlFunc.equals(SqlPkit.select)) {
			sql.append(SqlPkit.select);
			sql.append(SqlPkit.space);
			//parser fields
			List<Field> fields = TablePkit.getFields(data);
			len = fields.size();
			for (Field field : fields) {
				sql.append(TablePkit.getColumnName(field));
				sql.append(space);
				sql.append(TablePkit.getColumnAs(field));
//				sql.append(field.getName());
				if (idx != len - 1) {
					sql.append(",");
					sql.append(space);
				}
				idx++;
			}
		}else if(sqlFunc.equals(SqlPkit.delete)){
			sql.append(SqlPkit.delete);
		}
		
		//获取表信息和 primarykey 信息
		Table table = data.getClass().getAnnotation(Table.class);
		Field primaryKeyField = SqlPkit.getPrimaryKeyField(data);
		String pkField = primaryKeyField.getName();
		String pk = (String) TablePkit.getFieldValue(data, pkField);

		sql.append(SqlPkit.space);
		sql.append(SqlPkit.from);
		sql.append(SqlPkit.space);
		sql.append(table.name());
		
		// common 数据
		len = 0;
		idx = 0;
		//当 primarykey 存在时直接适用 primarykey 即可
		if (null != pk && !"0".equals(pk) ) {
			sql.append(SqlPkit.space);
			sql.append(SqlPkit.where);
			sql.append(SqlPkit.space);
			if (sqlFunc.equals(SqlPkit.select)) {
				sql.append(SqlPkit.defaulteq);
				sql.append(SqlPkit.space);
				sql.append(SqlPkit.and);
				sql.append(SqlPkit.space);
			}
			if (pk.contains(",") || pk.contains("|")) {
				sql.append(SqlPkit.parserFieldVal(data, pkField, pk, params));
			}else{
				sql.append(TablePkit.getColumnName(primaryKeyField));
				sql.append(SqlPkit.space);
				sql.append("=");
				sql.append(SqlPkit.space);
				sql.append("?");
				params.add(pk);
			}
		}else{
			List<Field> fields = SqlPkit.getExcludePrimaryKeyFields(data);
			List<Object> fieldValues = TablePkit.getFieldValues(data, fields);
			if (null == fieldValues || fieldValues.size() == 0) {
				sql.append(SqlPkit.space);
				sql.append(SqlPkit.where);
				sql.append(SqlPkit.space);
				sql.append(SqlPkit.defaulteq);
				sql.append(";");
				return sql.toString();
			}
			len = fields.size();
			sql.append(SqlPkit.space);
			sql.append(SqlPkit.where);
			sql.append(SqlPkit.space);
			if (len != 0) {
				if (sqlFunc.equals(SqlPkit.select)) {
					sql.append(SqlPkit.defaulteq);
					sql.append(SqlPkit.space);
					sql.append(SqlPkit.and);
					sql.append(SqlPkit.space);
				}
				idx = 0;
				for (Field field : fields) {
					String fieldName = field.getName();
					Object fieldVal = TablePkit.getFieldValue(data, fieldName);
					sql.append(SqlPkit.parserFieldVal(data, fieldName, fieldVal, params));
					if (idx != len-1) {
						sql.append(SqlPkit.space);
						sql.append(SqlPkit.and);
						sql.append(SqlPkit.space);
					}
					idx++;
				}
			}
		}
		sql.append(";");
		
		return sql.toString();
	}
	
	/**
	 * 获取联合主键
	 * @author FengHuan
	 * @param obj
	 * @return
	 */
	private static List<Field> getUnionUniqueSubKey(Object data) {
		List<Field> _fields = TablePkit.getUnionUniqueKeyFields(data);
		List<Field> unionUniqueKeyList = new ArrayList<Field>();
		for (Field field : _fields) {
			Object value = TablePkit.getFieldValue(data, field);
			 if (null == value || (value instanceof String && "".equals(value))) {
				 throw new IllegalArgumentException("\nthe value of union unique key can't be null");
			 }
			 unionUniqueKeyList.add(field);
		}
		if (unionUniqueKeyList.size() == 0) {
			throw new IllegalArgumentException("\nthe UnionUniqueSubKey is not found in "+data.getClass()+".\nplease add @Table.UnionUniqueSubKey in UnionUniqueSubKey field.");
		}
		
		return unionUniqueKeyList;
	}
	
	/**
	 * 获取唯一主键
	 * @author FengHuan
	 * @param obj
	 * @return
	 */
	private static List<Field> getUniqueKey(Object data) {
		List<Field> _fields = TablePkit.getUniqueKeyFields(data);
		List<Field> uniqueKeyList = new ArrayList<Field>();
		for (Field field : _fields) {
			 Object value = TablePkit.getFieldValue(data, field);
			 if (null == value || (value instanceof String && "".equals(value))) {
				 throw new IllegalArgumentException("\nthe value of unique key can't be null");
			 }
			 uniqueKeyList.add(field);
		}
		
		if (uniqueKeyList.size() == 0) {
			throw new IllegalArgumentException("\nthe UniqueKey is not found in "+data.getClass()+".\nplease add @Table.UniqueKey in UniqueKey field.");
		}
		
		return uniqueKeyList;
	}
	
	/**
	 * 插入前查询是否数据已经存在
	 * @author FengHuan
	 * @param data
	 * @param params
	 * @return
	 */
	private static final String isDataExist(Object data, List<Object> params, String andor) {
		//校验数据
		SqlPkit.validate(data, params);
		
		if (null == andor ||
				(null != andor && !andor.equals(SqlPkit.and) 
						&& !andor.equals(SqlPkit.or))) {
			throw new IllegalArgumentException("\nthe SqlOperation '"+andor+"' is invalid in "+data.getClass()+".");
		}
		
		StringBuilder sql = new StringBuilder(SqlPkit.select);
		sql.append(SqlPkit.space);
		sql.append("*");
		sql.append(SqlPkit.space);
		sql.append(SqlPkit.from);
		sql.append(SqlPkit.space);
		Table table = data.getClass().getAnnotation(Table.class);
		sql.append(table.name());
		sql.append(SqlPkit.space);
		sql.append(SqlPkit.where);
		sql.append(SqlPkit.space);
		sql.append(SqlPkit.defaulteq);
		sql.append(SqlPkit.space);
		sql.append(SqlPkit.and);
		sql.append(SqlPkit.space);
		List<Field> keyList = new ArrayList<Field>();
		if (SqlPkit.and.equals(andor)) {
			keyList = SqlPkit.getUnionUniqueSubKey(data);
		} else {
			keyList = SqlPkit.getUniqueKey(data);
		}
		
		int len = keyList.size();
		int idx = 0;
		for (Field field : keyList) {
			String unionKeyName = field.getName();
			String unionKeyVal = (String) TablePkit.getFieldValue(data, unionKeyName);
			sql.append(SqlPkit.parserFieldVal(data, unionKeyName,
					unionKeyVal, params));
			if (idx != (len - 1)) {
				sql.append(SqlPkit.space);
				sql.append(andor);
				sql.append(SqlPkit.space);	
			}
			idx++;
		}
		sql.append(SqlPkit._limit(1));
		sql.append(";");
		
		return sql.toString();
	}
	
	/**
	 * 查找数据
	 * @param data
	 * @param params
	 * @return
	 */
	public static final String select(Object data, List<Object> params) {
		return SqlPkit.selectOrDelete(SqlPkit.select, data, params);
	}
	
	/**
	 * 查找第一条
	 * @param data
	 * @param params
	 * @return
	 */
	public static final String selectFirst(Object data, List<Object> params) {
		StringBuilder sql = new StringBuilder(SqlPkit.select(data, params));
		String _sql = sql.toString();
		if (_sql.endsWith(";")) {
			sql.deleteCharAt(sql.length()-1);
			sql.append(SqlPkit._limit(1));
			sql.append(";");
		}
		return sql.toString();
	}
	
	/**
	 * 查找最后一条
	 * @param data
	 * @param params
	 * @return
	 */
	public static final String selectLast(Object data, List<Object> params) {
		StringBuilder sql = new StringBuilder(SqlPkit.select(data, params));
		String _sql = sql.toString();
		if (_sql.endsWith(";")) {
			sql.deleteCharAt(sql.length()-1);
			//获取表信息和 primarykey 信息
			Field primaryKeyField = SqlPkit.getPrimaryKeyField(data);
			String pkField = primaryKeyField.getName();
			sql.append(SqlPkit.space);
			sql.append(SqlPkit.orderBy);
			sql.append(SqlPkit.space);
			sql.append(pkField);
			sql.append(SqlPkit.space);
			sql.append(SqlPkit.desc);
			sql.append(SqlPkit._limit(1));
			sql.append(";");
		}
		return sql.toString();
	}
	
	/**
	 * 更新数据
	 * @param data
	 * @param params
	 * @return
	 */
	public static final String update(Object data, List<Object> params) {
		SqlPkit.validate(data, params);
		//获取表信息和 primarykey 信息
		Field primaryKeyField = SqlPkit.getPrimaryKeyField(data);
		String pkField = primaryKeyField.getName();
		String pk = (String) TablePkit.getFieldValue(data, pkField);	
		
		if (null == pk || "0".equals(pk)) {
			throw new IllegalArgumentException("\nthe primarykey value is null in "+data.getClass()+". Can not use SQL 'UPDATE'.");
		}

		Table table = data.getClass().getAnnotation(Table.class);

		StringBuilder sql = new StringBuilder();
		sql.append(SqlPkit.update);
		sql.append(SqlPkit.space);
		sql.append(table.name());
		sql.append(SqlPkit.space);
		sql.append(SqlPkit.set);
		sql.append(SqlPkit.space);

		List<Field> fields = SqlPkit.getExcludePrimaryKeyFields(data);
		int len = fields.size();
		if (len == 0) {
			throw new IllegalArgumentException("\nthe fields value is null in "+data.getClass()+".No more data need update.");
		}
		int idx = 0;
		String fieldName = null;
		Object fieldVal = null;
		Table.Column col = null;
		String originType = null;
		boolean isIntLike = false;
		for (Field field : fields) {
			fieldName = field.getName();
			fieldVal = TablePkit.getFieldValue(data, fieldName);
			
			//校验待写入数据是否合法
			col = field.getAnnotation(Table.Column.class);	
			originType = TablePkit.getColumnOriginType(data, fieldName);
			isIntLike = originType.equals(Table.ColumnOriginType.INT) 
							|| originType.equals(Table.ColumnOriginType.BIGINT)
							|| originType.equals(Table.ColumnOriginType.LONG);
			if (null != col 
					&& isIntLike) {
				try {
					Long.parseLong(fieldVal.toString());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("\nthe originType of field '"+fieldName+"' in "+data.getClass()+" is '"+col.originType()+"'.");
				}
			}
			sql.append(TablePkit.getColumnName(data, fieldName));
			sql.append(SqlPkit.space);
			sql.append("=");
			sql.append(SqlPkit.space);
			sql.append("?");
			params.add(fieldVal);
			if (idx != len-1) {
				sql.append(",");
			}
			sql.append(SqlPkit.space);
			idx++;
		}
		
		sql.append(SqlPkit.where);
		sql.append(SqlPkit.space);
		sql.append(pkField);
		sql.append(SqlPkit.space);
		sql.append("=");
		sql.append(SqlPkit.space);
		sql.append("?");
		params.add(pk);
		sql.append(";");
		return sql.toString();
	}
	
	/**
	 * 添加数据
	 * @param data
	 * @param params
	 * @return
	 */
	public static final String save(Object data, List<Object> params) {
		//校验数据是否合法
		SqlPkit.validate(data, params);
		//获取表信息
		Table table = data.getClass().getAnnotation(Table.class);
		
		StringBuilder sql = new StringBuilder();
		sql.append(SqlPkit.insert);
		sql.append(SqlPkit.space);
		sql.append(table.name());
		sql.append(SqlPkit.space);
		//fields
		List<Field> fields = SqlPkit.getExcludePrimaryKeyFields(data);
		int len = fields.size();
		if (len == 0) {
			throw new IllegalArgumentException("\nthe fields value is null in "+data.getClass()+".");
		}
		
		//unique keys
		List<Field> uniqueKeys = TablePkit.getUniqueKeyFields(data);
		for (Field field : uniqueKeys) {
			Object val = TablePkit.getFieldValue(data, field);
			if (null == val || (val instanceof String) && "".equals(val.toString())) {
				throw new IllegalArgumentException("\nthe unique field value is null in "+data.getClass()+".");
			}
		}
		
		//union unique keys
		List<Field> unionUniqueKeys = TablePkit.getUnionUniqueKeyFields(data);
		for (Field field : unionUniqueKeys) {
			Object val = TablePkit.getFieldValue(data, field);
			if (null == val || (val instanceof String) && "".equals(val.toString())) {
				throw new IllegalArgumentException("\nthe unionunique field value is null in "+data.getClass()+".");
			}
		}
		
		StringBuilder saveFields = new StringBuilder();
		StringBuilder values = new StringBuilder();
		saveFields.append("(id,");
		saveFields.append(SqlPkit.space);
		values.append("(?,");
		params.add(0);
		values.append(SqlPkit.space);
		int idx = 0;
		String fieldName = null;
		Object fieldVal = null;
		Table.Column col = null;
		String originType = null;
		boolean isIntLike = false;
		for (Field field : fields) {
			fieldName = field.getName();
			fieldVal = TablePkit.getFieldValue(data, fieldName);
			
			//校验待写入数据是否合法
			col = field.getAnnotation(Table.Column.class);
			originType = TablePkit.getColumnOriginType(data, fieldName);
			isIntLike = originType.equals(Table.ColumnOriginType.INT) 
							|| originType.equals(Table.ColumnOriginType.BIGINT)
							|| originType.equals(Table.ColumnOriginType.LONG);
			if (null != col 
					&& isIntLike) {
				try {
					Long.parseLong(fieldVal.toString());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("\nthe originType of field '"+fieldName+"' in "+data.getClass()+" is '"+col.originType()+"'.");
				}
			}
			saveFields.append(TablePkit.getColumnName(data, fieldName));
			values.append("?");
			params.add(fieldVal);
			if (idx != len-1) {
				saveFields.append(",");
				saveFields.append(SqlPkit.space);

				values.append(",");
				values.append(SqlPkit.space);
			}
			idx++;
		}
		saveFields.append(")");
		values.append(")");
		sql.append(saveFields);
		sql.append(SqlPkit.space);
		sql.append(SqlPkit.values);
		sql.append(SqlPkit.space);
		sql.append(values);
		sql.append(";");
		
		return sql.toString();
	}
	
	/**
	 * 删除数据
	 * @param data 条件数据
	 * @param params
	 * @return
	 */
	public static final String delete(Object data, List<Object> params) {
		return SqlPkit.selectOrDelete(SqlPkit.delete, data, params);
	}
	
	/**
	 * 判断数据是否存在
	 * @author FengHuan
	 * @param data
	 * @param params
	 * @return
	 */
	public static final String isDataExist(Object data, List<Object> params) {
		int size = TablePkit.getUniqueKeyFields(data).size();
		if (size != 0) {
			return SqlPkit.isDataExist(data, params, SqlPkit.or); 
		}

		size = TablePkit.getUnionUniqueKeyFields(data).size();
		if (size != 0) {
			return SqlPkit.isDataExist(data, params, SqlPkit.and);
		}
		return null;
	}
	
	/**
	 * 分页查询
	 * @author FengHuan
	 * @param data
	 * @param params
	 * @param condition
	 * @return
	 */
	public static final String select(Object data, List<Object> params, Integer... condition) {
		StringBuilder paginateSql = new StringBuilder(SqlPkit.select(data, params));
		String _paginateSql = paginateSql.toString();
		if (_paginateSql.endsWith(";")) {
			paginateSql.deleteCharAt(paginateSql.length()-1);
			paginateSql.append(SqlPkit._limit(condition));
		}
		
		return paginateSql.toString();
	}
	
	
}
