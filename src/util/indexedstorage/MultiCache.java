package util.indexedstorage;

import java.util.concurrent.atomic.AtomicInteger;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

public class MultiCache {

	//private static final int CACHE_SIZE = 5000;
	//private static final int CACHE_SIZE = 10000; // too large ???
	private static final int CACHE_SIZE = 1000;

	private static final ResourcePools r = ResourcePoolsBuilder.heap(CACHE_SIZE).build();	
	private static final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
	@SuppressWarnings("rawtypes")
	private static final CacheConfigurationBuilder<IdKey, Object> config = CacheConfigurationBuilder.newCacheConfigurationBuilder(IdKey.class, Object.class, r);
	@SuppressWarnings("rawtypes")
	private static final Cache<IdKey, Object> cache = cacheManager.createCache("MultiCache", config);	
	
	private static final AtomicInteger ids = new AtomicInteger();

	private MultiCache() {}

	private static class IdKey<K> {
		public final int id;
		public final K key;
		public IdKey(int id, K key) {
			this.id = id;
			this.key = key;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IdKey<?> other = (IdKey<?>) obj;
			if (id != other.id)
				return false;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}		
	}

	public static class TypedCache<K,V> {

		public final String name;
		public final int id;

		public TypedCache(String name) {
			this.name = name;
			this.id = ids.getAndIncrement();
		}

		public void put(K key, V value) {
			IdKey<K> idKey = new IdKey<K>(id, key);
			cache.put(idKey, value);
		}

		public V get(K key) {
			IdKey<K> idKey = new IdKey<K>(id, key);
			return (V) cache.get(idKey);
		}
	}
}
