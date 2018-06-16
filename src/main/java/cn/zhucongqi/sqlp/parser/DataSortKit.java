/**
 * 
 */
package cn.zhucongqi.sqlp.parser;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.zhucongqi.annotation.sqlp.Table;


/**
 * 数据排序工具
 * @author Jobsz:Jobsz
 */
public class DataSortKit {

	public enum Order {
		DESC,
		ASC;
	}
	
	public static void sort(final Order order, List<?> data, final String sortField) {
		if (null == order || null == sortField) {
			throw new IllegalAccessError("\n the fields of order and sortField  can't be null");
		}
		DataSortKit.sort(data, Arrays.asList(order), Arrays.asList(sortField));
	}
	
	public static void sort(List<?> data, final List<Order> orders, final List<String> sortFields) {
		if (null == orders || null == sortFields) {
			throw new IllegalAccessError("\n the fields of orders and sortFields can't be null");
		}
		if (orders.size() != sortFields.size()) {
			throw new IllegalAccessError("\n the length of order and sortField don't match");
		} 
		Collections.sort(data, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				for (int i = 0 ; i < sortFields.size() ; i ++) {
					String type = TablePkit.getColumnOriginType(o1, sortFields.get(i));
					String val1 = TablePkit.getFieldValue(o1, sortFields.get(i)).toString();
					String val2 = TablePkit.getFieldValue(o2, sortFields.get(i)).toString();
					if (val1.equals(val2)) {
						continue;
					} else {
					    return _sort(val1, val2, orders.get(i), sortFields.get(i), type);
					}
				}
			 
				return 0;
			}
		});
	}
	
	private static int _sort(String val1, String val2, Order order, String sortField, String type) {
		if (Table.ColumnOriginType.INT.equals(type)
				|| Table.ColumnOriginType.BIGINT.equals(type)) {
			BigInteger int1 = new BigInteger(val1);
			BigInteger int2 = new BigInteger(val2);
			if (order.equals(Order.DESC)) {
				return int2.compareTo(int1);
			}else if(order.equals(Order.ASC)){
				return int1.compareTo(int2);
			}
		}
		if (order.equals(Order.DESC)) {
			return val2.compareTo(val1);
		}else if(order.equals(Order.ASC)){
			return val1.compareTo(val2);
		}
		return 0;
	}
}
