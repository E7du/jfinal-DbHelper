/**
 * 
 */
package cn.zhucongqi.annotation.sqlp;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库表的注解
 * @author Jobsz
 */
@Target(java.lang.annotation.ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Table {
	
	/**
	 * 数据库中表的 name
	 * @return
	 */
	String name();
	
	/**
	 * 数据库表适用的别名
	 * @return
	 */
	String as() default "";
	
	/**
	 * 所在的数据库
	 * @return
	 */
	String catalog() default "";
	
	/**
	 * 所在的缓存name
	 * @return
	 */
	String cache() default "default";

	/**
	 * 数据库表每一列的注解
	 * @author Jobsz
	 */
	@Target(java.lang.annotation.ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	public @interface Column {
		
		/**
		 * 列的 name
		 * @return
		 */
		String name();
		
		/**
		 * 列的别名
		 * @return
		 */
		String as() default "";
		
		/**
		 * 不可以为 null
		 * @return
		 */
		boolean nullable() default true;
		
		/**
		 * 原始类型
		 * @return
		 */
		String originType();
	}
	
	/**
	 * 数据库表唯一键的注解
	 * @author Jobsz
	 */
	@Target(java.lang.annotation.ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	public static @interface PrimaryKey{
	}
	
	/**
	 * 数据库表联合唯一键的注解
	 * @author Jobsz
	 */
	@Target(java.lang.annotation.ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	public static @interface UnionUniqueSubKey{
	}
	
	/**
	 * 数据库表唯一键的注解
	 * @author Jobsz
	 */
	@Target(java.lang.annotation.ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	public static @interface UniqueKey{
	}

	/**
	 * 字段额原始类型
	 * @author Jobsz:Jobsz
	 */
	public static class ColumnOriginType {
		public static final String STRING = "string";
		public static final String BIGINT = "bigint";
		public static final String LONG = "long";
		public static final String INT = "int";
	}
}

