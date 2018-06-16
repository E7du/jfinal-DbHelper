/**
 * 
 */
package cn.zhucongqi.dbhelper;

import java.sql.SQLException;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;

/**
 * 数据库事务
 * @author Jobsz
 */
public abstract class DbTx {

	public boolean execute() {
		return Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {
				try {
					DbTx.this.sql();
				} catch (Exception e) {
					DbTx.this.error(e);
					return false;
				}
				return true;
			}
		});
	}
	
	public abstract void sql();
	
	public abstract void error(Exception e);
}
