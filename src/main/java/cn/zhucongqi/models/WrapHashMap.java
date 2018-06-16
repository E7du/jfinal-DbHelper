/**
 * 
 */
package cn.zhucongqi.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Jobsz:Jobsz
 * @param <K>
 */
public abstract class WrapHashMap extends HashMap<Object, Object> {

	private static final long serialVersionUID = 2810694869413106913L;

	protected Object push(Object key, Object value) {
		return super.put(key, value);
	}
	
	protected void pushAll(Map<?, ?> m) {
		super.putAll(m);
	}

	public Object pull(Object key) {
		return super.get(key);
	}
	
	protected void empty() {
		super.clear();
	}

	@Deprecated
	public Object put(String key, Object value) {
		throw (new IllegalArgumentException("Can not use default push func"));
	}

	@Deprecated
	public Object get(String key) {
		throw (new IllegalArgumentException("Can not use default get func"));
	}

	@Deprecated
	public void putAll(Map<?, ?> m) {
		throw (new IllegalArgumentException("Can not use default putAll func"));
	}

	@Deprecated
	public Object remove(Object key) {
		throw (new IllegalArgumentException("Can not use default remove func"));
	}

	@Deprecated
	public void clear() {
		throw (new IllegalArgumentException("Can not use default clear func"));
	}

	public Object getKeyByValue(Object value) {
		Object key = null;
		if (!this.containsValue(value)) {
			return key;
		}
		for (Entry<Object, Object> e : this.entrySet()) {
			if (e.getValue().equals(value)) {
				key = e.getKey();
			}
		}
		return key;
	}
}
