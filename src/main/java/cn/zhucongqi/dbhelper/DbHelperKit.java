/**
 * 
 */
package cn.zhucongqi.dbhelper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.ActiveRecordException;
import com.jfinal.plugin.activerecord.Config;
import com.jfinal.plugin.activerecord.DbKit;

/**
 * @author Jobsz:Jobsz
 *
 */
public class DbHelperKit {

	private final Config config;
	private static final Map<String, DbHelperKit> map = new HashMap<String, DbHelperKit>();
	private Logger log = Logger.getLogger(this.getClass());
	
	private DbHelperKit() {
		if (DbKit.getConfig() == null)
			throw new RuntimeException("The main config is null, initialize ActiveRecordPlugin first");
		this.config = DbKit.getConfig();
	}
	
	private DbHelperKit(String configName) {
		this.config = DbKit.getConfig(configName);
		if (this.config == null)
			throw new IllegalArgumentException("Config not found by configName: " + configName);
	}
	
	public static DbHelperKit use(String configName) {
		DbHelperKit result = map.get(configName);
		if (result == null) {
			result = new DbHelperKit(configName);
			map.put(configName, result);
		}
		return result;
	}
	
	public static DbHelperKit use() {
		if (DbKit.getConfig() == null)
			throw new RuntimeException("The main config is null, initialize ActiveRecordPlugin first");
		return use(DbKit.getConfig().getName());
	}

	private synchronized byte[] getBlobData(String colName, InputStream stream) {
		BufferedInputStream bis = new BufferedInputStream(stream);
		byte[] bytes = (byte[]) null;
		try {
			bytes = new byte[bis.available()];
			bis.read(bytes);
		} catch (IOException ex) {
			this.log.error("SFSObject serialize error. Failed reading BLOB data for column: "
							+ colName);
			throw new ActiveRecordException(ex);
		} finally {
			IOUtils.closeQuietly(bis);
		}
		return bytes;
	}
	
	private synchronized List<Map<String, Object>> resultSet2List(ResultSet rset) {
		List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
		try {
			while (rset.next()) {
				list.add(resultSet2Map(rset));
			}
		} catch (SQLException e) {
			this.log.error("resultSet2array 失败,"
					+ e.getMessage());
			throw new ActiveRecordException(e);
		}
		return list;
	}

	private synchronized Map<String, Object> resultSet2Map(ResultSet rset) {
		Map<String, Object> data = new HashMap<String, Object>();
		try {
			ResultSetMetaData metaData = rset.getMetaData();
			if (rset.isBeforeFirst()) {
				rset.next();
			}
			for (int col = 1; col <= metaData.getColumnCount(); col++) {
				String colName = metaData.getColumnLabel(col);
				int type = metaData.getColumnType(col);

				Object rawDataObj = rset.getObject(col);
				if (rawDataObj == null) {
					continue;
				}
				
				// double,float,decimal,real,int, bigint = > UtfString
				// tinyint, smallint, bit => int
				if (type == Types.NULL) {//0
					data.put(colName, null);
				} else if (type == Types.BOOLEAN) {//16
					data.put(colName, rset.getBoolean(col));
				} else if (type == Types.DATE) {//91
					data.put(colName, String.valueOf(rset.getDate(col).getTime()));
				} else if ((type == Types.FLOAT) 
						|| (type == Types.DECIMAL)
						|| (type == Types.DOUBLE)
						|| (type == Types.REAL)) {// 6 \ 3 \ 8 \ 7
					data.put(colName, String.valueOf(rset.getDouble(col)));
				} else if ((type == Types.TINYINT) 
						|| (type == Types.SMALLINT)
						|| (type == Types.BIT)) {// -6  \ 5 \ -7
					data.put(colName, rset.getInt(col));
				} else if ((type == Types.INTEGER)) {// 4 (long to utfstring)
					data.put(colName, String.valueOf(rset.getInt(col)));
				} else if ((type == Types.LONGVARCHAR) 
						|| (type == Types.VARCHAR) 
						|| (type == Types.CHAR)) {// -1 \ 12 \ 1
					data.put(colName, rset.getString(col));
				} else if ((type == Types.NVARCHAR) 
						|| (type == Types.LONGNVARCHAR) 
						|| (type == Types.NCHAR)) {// -9 \ -16 \ -15
					data.put(colName, rset.getNString(col));
				} else if (type == Types.TIMESTAMP) {// 93
					data.put(colName, String.valueOf(rset.getTimestamp(col).getTime()));
				} else if (type == Types.BIGINT) {// -5
					data.put(colName, String.valueOf(rset.getLong(col)));
				} else if (type == Types.LONGVARBINARY) {// -4
					byte[] binData = getBlobData(colName,
							rset.getBinaryStream(col));
					if (binData != null) {
						data.put(colName, binData);
					}
				} else if (type == Types.BLOB) {
					Blob blob = rset.getBlob(col);
					data.put(colName,
							blob.getBytes(0L, (int) blob.length()));
				} else {
					this.log.error("Skipping Unsupported SQL TYPE: " + type
									+ ", Column:" + colName);
				}
			}
			
		} catch (Exception e) {
			this.log.error("resultSet2object 失败," + e.getMessage());
			throw new ActiveRecordException(e);
		}
		return data;
	}
	
	public synchronized Map<String, Object> queryFirst(String sql, Object[] params) {
		List<Map<String, Object>> datas = this.query(sql, params);
		if (datas == null || datas.size() == 0) {
			return new HashMap<String, Object>();
		}
		return datas.get(0);
	}
	
	public synchronized List<Map<String, Object>> query(String sql, Object[] params) {
		List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = this.config.getConnection();
			stmt = conn.prepareStatement(sql);
			if (null != params) {
				this.config.getDialect().fillStatement(stmt, params);	
			}
			ResultSet resultSet = stmt.executeQuery();
			if (resultSet != null) {
				datas = this.resultSet2List(resultSet);
			}
		} catch (SQLException e) {
			this.log.error("executeQuery=>[\""
					+ sql + "\"]失败," + e.getMessage());
			throw new ActiveRecordException(e);
		} finally {
			this.config.close(stmt, conn);
		}
		return datas;
	}

	public synchronized int update(String sql, Object[] params) {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = this.config.getConnection();
			stmt = conn.prepareStatement(sql);
			if (null != params) {
				this.config.getDialect().fillStatement(stmt, params);	
			}
			return stmt.executeUpdate();
		} catch (SQLException e) {
			this.log.error("executeUpdate=>[\""
					+ sql + "\"]失败," + e.getMessage());
			throw new ActiveRecordException(e);
		}finally {
			config.close(stmt, conn);
		}
	}

	public synchronized Object insert(String sql, Object[] params) {
		Object idObj = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = config.getConnection();
			stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			if (null != params) {
				this.config.getDialect().fillStatement(stmt, params);	
			}
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next())
				idObj = generatedKeys.getObject(1);
			else {
				throw new ActiveRecordException("INSERT failed: " + stmt.toString());
			}
		}  catch (SQLException e) {
			this.log.error("executeInsert=>[\""
					+ sql + "\"]失败," + e.getMessage());
			throw new ActiveRecordException(e);
		}finally {
			config.close(stmt, conn);
		}
		return idObj;
	}
}
