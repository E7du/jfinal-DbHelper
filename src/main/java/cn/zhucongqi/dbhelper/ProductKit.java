/**
 * 
 */
package cn.zhucongqi.dbhelper;

/**
 * @author Jobsz:Jobsz
 *
 */
public class ProductKit {

	public static String PRODUCT_NAME = null;
	
	public static String getProductName() {
		String name = ProductKit.PRODUCT_NAME;
		if (name == null || "".equals(name)) {
			throw (new IllegalArgumentException("Please use `ProductKit.PRODUCT_NAME = \"name\"`set PRODUCT_NAME"));
		}
		return name;
	}
	
}
