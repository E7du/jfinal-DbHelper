/**
 * 
 */
package cn.zhucongqi.dbhelper;

/**
 * 数据库事务处理
 * @author Jobsz
 */
public class DbHelperTx {
	
	/**
	 * 处理数据库操作，包含事务处理
	 * @param tx
	 */
	public static boolean execute(DbTx tx) {
		return tx.execute();
	}
}
