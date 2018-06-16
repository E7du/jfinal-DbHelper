/**
 * 
 */
package cn.zhucongqi.dbhelper.demo.model;

import cn.zhucongqi.annotation.sqlp.Table;
import cn.zhucongqi.models.DataRecordModel;

/**
 * @author Jobsz:Jobsz
 *
 * SQL: 
 * CREATE TABLE `user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
 *
 */
@Table(name = User.TABLE)
public class User extends DataRecordModel<User> {

	private static final long serialVersionUID = 7098160284590588258L;

	public static final String TABLE = "user";
	public static final String TABLE_AS = "u";
	
	// SQL Fields
	public static final String ID = "id";
	public static final String NAME = "name";
	//SQL Field As
	public static final String ID_AS = "id";
	public static final String NAME_AS = "name";
	
	@Table.PrimaryKey
	@Table.Column(name = User.ID, as = User.ID_AS, originType = Table.ColumnOriginType.BIGINT)
	public String id;
	
	@Table.Column(name = User.NAME, as = User.NAME_AS, originType = Table.ColumnOriginType.STRING)
	public String name;
}
