package dev.railroadide.switchboard.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Cache<K, V> {
    private final static Duration DEFAULT_TTL = Duration.ofHours(1);

    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private Duration ttl = DEFAULT_TTL;

    public Cache() {
    }

    public Cache(Duration ttl) {
        setTtl(ttl);
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        if (ttl == null)
            throw new IllegalArgumentException("TTL cannot be null.");

        this.ttl = ttl;
    }

    public void put(K key, V value, long ttlMillis) {
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        cache.put(key, new CacheEntry<>(value, expiresAt));
    }

    public void put(K key, V value) {
        put(key, value, ttl.toMillis());
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && entry.isActive())
            return entry.value();

        cache.remove(key);
        return null;
    }

    public void clear() {
        cache.clear();
    }

    public boolean containsKey(K key) {
        CacheEntry<V> entry = cache.get(key);
        return entry != null && entry.isActive();
    }

    public V get(K key, Supplier<V> valueFetcher) {
        V value = get(key);
        if (value != null)
            return value;

        value = valueFetcher.get();
        if (value != null)
            put(key, value);

        return value;
    }

    public record CacheEntry<T>(T value, long expiresAt) {
        public boolean isActive() {
            return System.currentTimeMillis() < expiresAt;
        }
    }
}
