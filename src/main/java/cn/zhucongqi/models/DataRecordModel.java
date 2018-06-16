/**
 * 
 */
package cn.zhucongqi.models;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import cn.zhucongqi.annotation.sqlp.Table;
import cn.zhucongqi.sqlp.parser.TablePkit;

/**
 * RedisModel
 * 
 * @author Jobsz
 */
public abstract class DataRecordModel<M extends DataRecordModel<?>> extends WrapHashMap {

	private static final long serialVersionUID = -5894864049393413126L;

	public DataRecordModel() {
	}

	private void init(Map<String, Object> object) {
		if (null != object && object.size() != 0) {
			this.empty();
			// set field value
			List<Field> fields = TablePkit.getFields(this);
			String as = null;
			for (Field field : fields) {
				as = TablePkit.getColumnAs(field);
				if (object.containsKey(as)) {
					String val = object.get(as).toString();
					TablePkit.setFieldValue(this, field, val.toString());
				}
			}
			this.pushAll(object);
		}
	}
	
	@SuppressWarnings("unchecked")
	public M parser(Map<String, Object> object) {
		this.init(object);
		return (M)this;
	}
	
	/**
	 * 填充数据
	 */
	@SuppressWarnings("unchecked")
	public M fill() {
		List<Field> fields = TablePkit.getHasValueFields(this);
		String originType = null;
		String as = null;
		Object val = null;
		for (Field field : fields) {
			originType = TablePkit.getColumnOriginType(field);
			as = TablePkit.getColumnAs(field);
			val = TablePkit.getFieldValue(this, field).toString();
			if (originType.equals(Table.ColumnOriginType.BIGINT)
					|| originType.equals(Table.ColumnOriginType.LONG)
					|| originType.equals(Table.ColumnOriginType.STRING)) {
				;//BIGINT LONG STRING 不做转型处理直接push
			} else if (originType.equals(Table.ColumnOriginType.INT)) {
				val = Integer.valueOf(val.toString());
			}
			this.push(as, val);
		}
		return (M)this;
	}
}
