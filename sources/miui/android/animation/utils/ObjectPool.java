package miui.android.animation.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/* loaded from: classes.dex */
public class ObjectPool {
    private static final long DELAY = 5000;
    private static final int MAX_POOL_SIZE = 10;
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final ConcurrentHashMap<Class<?>, Cache> sCacheMap = new ConcurrentHashMap<>();

    /* loaded from: classes.dex */
    public interface IPoolObject {
        void clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Cache {
        final ConcurrentHashMap<Object, Boolean> mCacheRecord;
        final ConcurrentLinkedQueue<Object> pool;
        final Runnable shrinkTask;

        private Cache() {
            this.pool = new ConcurrentLinkedQueue<>();
            this.mCacheRecord = new ConcurrentHashMap<>();
            this.shrinkTask = new Runnable() { // from class: miui.android.animation.utils.ObjectPool.Cache.1
                @Override // java.lang.Runnable
                public void run() {
                    Cache.this.shrink();
                }
            };
        }

        <T> T acquireObject(Class<T> cls, Object... objArr) {
            T t = (T) this.pool.poll();
            if (t != null) {
                this.mCacheRecord.remove(t);
                return t;
            }
            if (cls != null) {
                return (T) ObjectPool.createObject(cls, objArr);
            }
            return t;
        }

        void releaseObject(Object obj) {
            if (this.mCacheRecord.putIfAbsent(obj, true) != null) {
                return;
            }
            this.pool.add(obj);
            ObjectPool.sMainHandler.removeCallbacks(this.shrinkTask);
            if (this.pool.size() > 10) {
                ObjectPool.sMainHandler.postDelayed(this.shrinkTask, ObjectPool.DELAY);
            }
        }

        void shrink() {
            Object obj;
            while (this.pool.size() > 10 && (obj = this.pool.poll()) != null) {
                this.mCacheRecord.remove(obj);
            }
        }
    }

    private ObjectPool() {
    }

    public static <T> T acquire(Class<T> cls, Object... objArr) {
        return (T) getObjectCache(cls, true).acquireObject(cls, objArr);
    }

    public static void release(Object obj) {
        if (obj == null) {
            return;
        }
        Class<?> clz = obj.getClass();
        if (obj instanceof IPoolObject) {
            ((IPoolObject) obj).clear();
        } else if (obj instanceof Collection) {
            ((Collection) obj).clear();
        } else if (obj instanceof Map) {
            ((Map) obj).clear();
        }
        Cache cache = getObjectCache(clz, false);
        if (cache != null) {
            cache.releaseObject(obj);
        }
    }

    private static Cache getObjectCache(Class<?> clz, boolean create) {
        ConcurrentHashMap<Class<?>, Cache> concurrentHashMap = sCacheMap;
        Cache cache = concurrentHashMap.get(clz);
        if (cache == null && create) {
            Cache cache2 = new Cache();
            Cache prev = concurrentHashMap.putIfAbsent(clz, cache2);
            return prev != null ? prev : cache2;
        }
        return cache;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Object createObject(Class<?> clz, Object... args) {
        try {
            Constructor<?>[] ctrs = clz.getDeclaredConstructors();
            for (Constructor<?> ctr : ctrs) {
                if (ctr.getParameterTypes().length == args.length) {
                    ctr.setAccessible(true);
                    return ctr.newInstance(args);
                }
            }
            return null;
        } catch (Exception e) {
            Log.w(CommonUtils.TAG, "ObjectPool.createObject failed, clz = " + clz, e);
            return null;
        }
    }
}
