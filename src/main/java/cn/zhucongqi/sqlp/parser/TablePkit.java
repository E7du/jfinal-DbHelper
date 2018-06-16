/**
 * 
 */
package cn.zhucongqi.sqlp.parser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import cn.zhucongqi.annotation.sqlp.Table;

/**
 * @author Jobsz:Jobsz
 *
 */
public class TablePkit {
	
	
	///==================
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static List<Field> getFields(Object data, Class annoClazz) {
		TablePkit.validateData(data);
		List<Field> fields = new ArrayList<Field>();
		Field[] declaredFields = data.getClass().getDeclaredFields();
		for (Field field : declaredFields) {
			if (null != field.getAnnotation(annoClazz)) {
				fields.add(field);
			}
		}
		return fields;
	}
	
	private static void validateData(Object data) {
		if (null == data) {
			throw (new IllegalArgumentException("data is null!"));
		}
	}
	
	///========Table=====

	/**
	 * 获取数据的Table注解
	 * @param data
	 * @return
	 */
	public static Table getTable(Object data) {
		TablePkit.validateData(data);
		return data.getClass().getAnnotation(Table.class);
	}
	
	/**
	 * 获取表名
	 * @param data
	 * @return
	 */
	public static String getTableName(Object data) {
		TablePkit.validateData(data);
		Table table = TablePkit.getTable(data);
		if (null != table) {
			return table.name();
		}
		return "";
	}
	
	/**
	 * 获取数据库
	 * @param data
	 * @return
	 */
	public static String getTableCatalog(Object data) {
		TablePkit.validateData(data);
		Table table = TablePkit.getTable(data);
		if (null != table) {
			return table.catalog();
		}
		return "";
	}
	
	/**
	 * 获取表别名
	 * @param data
	 * @return
	 */
	public static String getTableAs(Object data) {
		TablePkit.validateData(data);
		Table table = TablePkit.getTable(data);
		if (null != table) {
			return table.as();
		}
		return "";
	}

	///=======Table Cache
	
	/**
	 * 数据缓存name
	 * @param data
	 * @return
	 */
	public static String getTableCache(Object data) {
		TablePkit.validateData(data);
		Table table = TablePkit.getTable(data);
		if (null != table) {
			return table.cache();
		}
		return "";
	}
	
	///=======Field====
	
	/**
	 * 获取所有的属性
	 * @param data
	 * @return
	 */
	public static List<Field> getFields(Object data) {
		TablePkit.validateData(data);
		return TablePkit.getFields(data, Table.Column.class);
	}
	
	/**
	 * 获取主键属性
	 * @param data
	 * @return
	 */
	public static Field getPrimaryKeyField(Object data) {
		TablePkit.validateData(data);
		List<Field> primaryKeyFields = TablePkit.getFields(data, Table.PrimaryKey.class);
		if (null == primaryKeyFields || primaryKeyFields.size() == 0) {
			return null;
		}
		return primaryKeyFields.get(0);
	}
	
	/**
	 * 处理主键以外的其他属性
	 * @param data
	 * @return
	 */
	public static List<Field> getExcludePrimaryKeyFields(Object data) {
		TablePkit.validateData(data);
		List<Field> excludePrimaryKeyFields = TablePkit.getFields(data);
		Field primaryKeyField = TablePkit.getPrimaryKeyField(data);
		excludePrimaryKeyFields.remove(primaryKeyField);
		return excludePrimaryKeyFields;
	}
	
	/**
	 * 获取uniquekey的属性
	 * @param data
	 * @return
	 */
	public static List<Field> getUniqueKeyFields(Object data) {
		TablePkit.validateData(data);
		return TablePkit.getFields(data, Table.UniqueKey.class);
	}
	
	/**
	 * 获取union unique key的属性
	 * @param data
	 * @return
	 */
	public static List<Field> getUnionUniqueKeyFields(Object data) {
		TablePkit.validateData(data);
		return TablePkit.getFields(data, Table.UnionUniqueSubKey.class);
	}
	
	/**
	 * 获取值不为空得Fieds
	 * @param data
	 * @param fields
	 * @return
	 */
	public static List<Field> getHasValueFields(Object data, List<Field> fields) {
		TablePkit.validateData(data);
		List<Field> _fields = new ArrayList<Field>();
		for (Field field : fields) {
			if (null != TablePkit.getFieldValue(data, field)) {
				_fields.add(field);
			}
		}
		return _fields;
	}
	
	/**
	 * 获取值不为空得Fieds
	 * @param data
	 * @param fields
	 * @return
	 */
	public static List<Field> getHasValueFields(Object data) {
		TablePkit.validateData(data);
		return TablePkit.getHasValueFields(data, TablePkit.getFields(data));
	}
	
	/**
	 * 获取Field
	 * @param data
	 * @param fieldName
	 * @return
	 */
	public static Field getField(Object data, String fieldName) {
		TablePkit.validateData(data);
		try {
			return data.getClass().getField(fieldName);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 获取属性值
	 * @param data
	 * @param field
	 * @return
	 */
	public static Object getFieldValue(Object data, Field field) {
		TablePkit.validateData(data);
		if (null == field) {
			return null;
		}
		
		try {
			return field.get(data);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 获取属性值
	 * @param data
	 * @param fieldName
	 * @return
	 */
	public static Object getFieldValue(Object data, String fieldName) {
		TablePkit.validateData(data);
		Field field = TablePkit.getField(data, fieldName);
		return TablePkit.getFieldValue(data, field);
	}
	
	/**
	 * 获取所有域的值
	 * @author FengHuan
	 * @param data
	 * @param fields
	 * @return
	 */
	public static List<Object> getFieldValues(Object data, List<Field> fields) {
		TablePkit.validateData(data);
		List<Object> fieldValues = new ArrayList<Object>();
		for (Field field : fields) {
			 Object fieldValue = TablePkit.getFieldValue(data, field);
			 fieldValues.add(fieldValue);
		}
		return fieldValues;
	}
	
	/**
	 * 获取主键的值
	 * @param data
	 * @return
	 */
	public static Object getPrimaryKeyValue(Object data) {
		TablePkit.validateData(data);
		Field field = TablePkit.getPrimaryKeyField(data);
		return TablePkit.getFieldValue(data, field);
	}
	
	///=====Columns
	
	/**
	 * 获取列的name
	 * @param field
	 * @return
	 */
	public static String getColumnName(Field field) {
		if (null == field) {
			return "";
		}
		Table.Column column = field.getAnnotation(Table.Column.class);
		if (null != column) {
			return column.name();
		}
		return null;
	}
	
	/**
	 * 获取列的name
	 * @param data
	 * @param fieldName
	 * @return
	 */
	public static String getColumnName(Object data, String fieldName) {
		TablePkit.validateData(data);
		Field field = TablePkit.getField(data, fieldName);
		return TablePkit.getColumnName(field);
	}
	
	/**
	 * 获取列的原始类型
	 * @param field
	 * @return
	 */
	public static String getColumnOriginType(Field field) {
		if (null == field) {
			return Table.ColumnOriginType.INT;
		}
		Table.Column column = field.getAnnotation(Table.Column.class);
		if (null != column) {
			return column.originType();
		}
		return Table.ColumnOriginType.INT;
	}
	
	/**
	 * 获取列的原始类型
	 * @param data
	 * @param fieldName
	 * @return
	 */
	public static String getColumnOriginType(Object data, String fieldName) {
		TablePkit.validateData(data);
		Field field = TablePkit.getField(data, fieldName);
		return TablePkit.getColumnOriginType(field);
	}
	
	/**
	 * 获取列的as
	 * @param field
	 * @return
	 */
	public static String getColumnAs(Field field) {
		if (null == field) {
			return null;
		}
		Table.Column column = field.getAnnotation(Table.Column.class);
		if (null == column) {
			return null;
		}
		return column.as();
	}
	
	/**
	 * 获取列的as
	 * @param field
	 * @return
	 */
	public static String getColumnAs(Object data, String fieldName) {
		TablePkit.validateData(data);
		Field field = TablePkit.getField(data, fieldName);
		return TablePkit.getColumnAs(field);
	}
	
	/**
	 * 给某字段赋值
	 * @param data
	 * @param field
	 * @param value
	 */
	public static void setFieldValue(Object data, Field field, Object value) {
		TablePkit.validateData(data);
		try {
			field.set(data, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 给某字段赋值
	 * @param data
	 * @param field
	 * @param value
	 */
	public static void setFieldValue(Object data, String fieldName, Object value) {
		TablePkit.validateData(data);
		Field field = TablePkit.getField(data, fieldName);
		TablePkit.setFieldValue(data, field, value);
	}

	/**
	 * 给主键赋值
	 * @param data
	 * @param field
	 * @param value
	 */
	public static void setPrimaryKeyValue(Object data, Object value) {
		TablePkit.validateData(data);
		Field primayKey = TablePkit.getPrimaryKeyField(data);
		TablePkit.setFieldValue(data, primayKey, value);
	}
	
	/**
	 * 判断所有字段参数是否为null
	 * @param data
	 * @return true:参数为空,false:参数有值;
	 */
	public static boolean isParameterValuesNull(Object data) {
		TablePkit.validateData(data);
		List<Field> fields = TablePkit.getHasValueFields(data);
		return fields.size() != 0 ? false : true;
	}
	
	/**
	 * 判断主键值是否为null
	 * @param data
	 * @return true:主键值有空,false:主键值非空;
	 */
	public static boolean isPrimaryKeyValueNull(Object data) {
		Object keyValue = TablePkit.getPrimaryKeyValue(data);
		return keyValue != null ? false : true;
	}
}
