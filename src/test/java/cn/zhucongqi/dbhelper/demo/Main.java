/**
 * 
 */
package cn.zhucongqi.dbhelper.demo;

import java.util.Map;

import org.apache.log4j.PropertyConfigurator;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallFilter;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.plugin.redis.RedisPlugin;

import cn.zhucongqi.dbhelper.ProductKit;
import cn.zhucongqi.dbhelper.demo.model.User;
import cn.zhucongqi.dbhelper.demo.redisservice.UserService;

/**
 * @author Jobsz:Jobsz
 *
 */
public class Main {

	private static void initDb() {
		DruidPlugin dp = new DruidPlugin("jdbc:mysql://192.168.1.250/zcq", "dev", "123");
		dp.setInitialSize(10);
		dp.setMaxActive(10);
		dp.addFilter(new StatFilter());
		WallFilter wall = new WallFilter();
		wall.setDbType("mysql");
		dp.addFilter(wall);
		ActiveRecordPlugin arp = new ActiveRecordPlugin("zcq", dp);
		arp.setShowSql(true);
		dp.start();
		arp.start();
	}
	
	private static void initRedis() {
		RedisPlugin redis = new RedisPlugin("default", "192.168.1.250", 6379, 1500, "redisadmin");
		redis.start();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		Main.initRedis();
		Main.initDb();
		
		ProductKit.PRODUCT_NAME = "zcq";
		
		User user = new User();
		user.name = "Jobsz";
		
		//save
		UserService userService = new UserService();
		boolean ret = userService.save(user);
		System.out.println("save =="+ret);
		
		//find id = 6
		user.id = "6";
		Map<String, Object> find = userService.findOne(user);
		System.out.println("find =="+find);
		//find to User
		User otherUser = user.parser(find);
		System.out.println("other user == "+otherUser);
		//findall
		Object all = userService.findAll(user);
		System.out.println("findall =="+all);
		//update Jobsz to newName
		user.name = "newName";
		ret = userService.update(user);
		System.out.println("update =="+ret);
		//delete id = 10
		user.id = "10";
		ret = userService.delete(user);
		System.out.println("delete=="+ret);
	}
}
