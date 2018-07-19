package com.xiaoma.rxcache.diskcache;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * author: mxc
 * date: 2018/7/18.
 */

public class DiskLruCacheHelper {
    private static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;

    private static ExecutorService service = null;

    /**
     * 创建DiskLruCache实例,默认版本号为当前应用版本号，缓存位置由getDiskCacheDir指定
     *
     * @param context      上下文
     * @param cacheDirName 缓存文件夹名称
     * @param maxSize      缓存最大值,单位是byte
     * @return 创建成功返回DiskLruCache实例否则返回null
     */
    public static DiskLruCache createCache(Context context, String cacheDirName, long maxSize) {
        DiskLruCache cache = null;
        try {
            cache = DiskLruCache.open(getDiskCacheDir(cacheDirName, context), getAppVersion(context), 1, maxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cache;
    }

    /**
     * 返回一个具有默认大小的DiskLruCache实例,默认大小为10Mb
     *
     * @param context
     * @param cacheDirName
     * @return 创建成功返回DiskLruCache实例否则返回null
     */
    public static DiskLruCache createCache(Context context, String cacheDirName) {
        return createCache(context, cacheDirName, DEFAULT_MAX_SIZE);
    }

    /**
     * 将图片写入缓存
     */
    public static boolean writeBitmapToCache(DiskLruCache cache, Bitmap bitmap, String url) {
        return writeBitmapToCache(cache, bitmap, url, Bitmap.CompressFormat.JPEG, 100);
    }

    /**
     * 异步地将图片写入缓存。将不会不会写入结果。
     *
     * @param cache
     * @param bitmap
     * @param url
     */
    public static void asyncWriteBitmapToCache(final DiskLruCache cache, final Bitmap bitmap, final String url) {
        if (service == null)
            service = Executors.newSingleThreadExecutor();
        service.execute(new Runnable() {
            @Override
            public void run() {
                writeBitmapToCache(cache, bitmap, url);
            }
        });
    }

    /**
     * 将图片写入缓存
     *
     * @param cache  缓存对象
     * @param bitmap 图片对象
     * @param url    用于标识bitmap的唯一名称，通常为图片url
     * @return true表示写入缓存成功否则为false
     */
    public static boolean writeBitmapToCache(DiskLruCache cache, Bitmap bitmap, String url, Bitmap.CompressFormat format, int quality) {
        if (cache == null || bitmap == null || url == null || TextUtils.isEmpty(url))
            return false;
        try {
            DiskLruCache.Editor editor = cache.edit(generateKey(url));
            if (editor != null) {
                OutputStream out = editor.newOutputStream(0);
                if (bitmap.compress(format, quality, out)) {
                    editor.commit();
                    return true;
                } else {
                    editor.abort();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 异步地将图片写入缓存。将不会不会写入结果。
     */
    public static void asyncWriteBitmapToCache(final DiskLruCache cache, final Bitmap bitmap, final String url, final Bitmap.CompressFormat format, final int quality) {
        if (service == null)
            service = Executors.newSingleThreadExecutor();
        service.execute(new Runnable() {
            @Override
            public void run() {
                writeBitmapToCache(cache, bitmap, url, format, quality);
            }
        });
    }

    /**
     * 异步地将inputStram流写入缓存，将不会返回写入结果。
     */
    public static void asyncWriteStreamToCache(final DiskLruCache cache, final InputStream in, final String url) {
        if (service == null)
            service = Executors.newSingleThreadExecutor();
        service.execute(new Runnable() {
            @Override
            public void run() {
                asyncWriteStreamToCache(cache, in, url);
            }
        });
    }

    /**
     * 将inputStram流写入缓存
     *
     * @param cache
     * @param in
     * @param url
     * @return
     */
    public static boolean writeStreamToCache(DiskLruCache cache, InputStream in, String url) {
        if (cache == null || in == null || url == null || TextUtils.isEmpty(url))
            return false;
        DiskLruCache.Editor editor = null;
        try {
            editor = cache.edit(generateKey(url));
            if (editor != null) {
                OutputStream out = editor.newOutputStream(0);
                BufferedInputStream bin = new BufferedInputStream(in);
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = bin.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
                editor.commit();
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 异步地将文件写入缓存，将不会返回写入结果。
     */
    public static void asyncWriteFileToCache(final DiskLruCache cache, final File file, final String url) {
        if (service == null)
            service = Executors.newSingleThreadExecutor();
        service.execute(new Runnable() {
            @Override
            public void run() {
                writeFileToCache(cache, file, url);
            }
        });
    }

    /**
     * 将文件写入缓存
     *
     * @return true表示写入成功否则写入失败
     */
    public static boolean writeFileToCache(DiskLruCache cache, File file, String url) {
        if (cache == null || file == null || url == null || !file.exists() || TextUtils.isEmpty(url)) {
            return false;
        }
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return writeStreamToCache(cache, fin, url);
    }

    /**
     * 异步地将字符串写入缓存,将不会返回写入结果
     */
    public static void asyncWriteStringToCache(final DiskLruCache cache, final String str, final String url) {
        if (service == null)
            service = Executors.newSingleThreadExecutor();
        service.execute(new Runnable() {
            @Override
            public void run() {
                writeStringToCache(cache, str, url);
            }
        });
    }

    /**
     * 将字符串写入缓存
     *
     * @param cache
     * @param str
     * @param url
     * @return
     */
    public static boolean writeStringToCache(DiskLruCache cache, String str, String url) {
        if (cache == null || str == null || url == null || TextUtils.isEmpty(url) || TextUtils.isEmpty(str)) {
            return false;
        }
        DiskLruCache.Editor editor = null;
        try {
            int lastIndex = url.lastIndexOf("&");
            String cacheTime = url.substring(lastIndex + 1);
            editor = cache.edit(generateKey(url.substring(0, lastIndex)));
            if (editor != null) {
                OutputStream out = editor.newOutputStream(0);
                out.write(("cacheTime:" + cacheTime + "\n").getBytes());
                out.write(str.getBytes());
                out.flush();
            }
            editor.commit();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 停止内部正在写缓存的线程，
     * 这将导致部分写缓存任务不能进行。
     */
    public static void stop() {
        if (service != null)
            service.shutdownNow();
    }

    /**
     * 根据url获取缓存，并将结果以String形式返回
     *
     * @param cache
     * @param url
     * @return 成功则返回String否则返回null
     */
    public static String readCacheToString(DiskLruCache cache, String url) {
        if (cache == null || url == null || TextUtils.isEmpty(url))
            return null;

        String key = generateKey(url);
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = cache.get(key);
            if (snapshot != null) {
                InputStream in = snapshot.getInputStream(0);
                StringBuilder builder = new StringBuilder(1024 * 2);
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = in.read(buffer)) != -1) {
                    builder.append(new String(buffer, 0, len));
                }
                return builder.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据url获取缓存，并将缓存以InputStream形式返回
     *
     * @param cache DiskLruCache实例
     * @param url   缓存名
     * @return 命中则返回InputStream流否则返回null
     */
    public static InputStream readCacheToInputStream(DiskLruCache cache, String url) {
        if (cache == null || url == null || TextUtils.isEmpty(url))
            return null;
        String key = generateKey(url);
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = cache.get(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (snapshot != null)
            return snapshot.getInputStream(0);
        return null;
    }


    /**
     * 根据url获取缓存，并将缓存以Bitmap形式返回
     *
     * @param cache
     * @param url
     * @return 成功返回bitmap，否则返回null
     */
    public static Bitmap readCacheToBitmap(DiskLruCache cache, String url) {
        InputStream in = readCacheToInputStream(cache, url);
        if (in != null)
            return BitmapFactory.decodeStream(in);
        return null;
    }

    /**
     * 获取缓存文件路径(优先选择sd卡)
     *
     * @param cacheDirName 缓存文件夹名称
     * @param context      上下文
     * @return
     */
    public static File getDiskCacheDir(String cacheDirName, Context context) {
        String cacheDir;

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !Environment.isExternalStorageRemovable()) {
            cacheDir = getExternalCacheDir(context);
            if (cacheDir == null)//部分机型返回了null
                cacheDir = getInternalCacheDir(context);
        } else {
            cacheDir = getInternalCacheDir(context);
        }
        /*int lastIndex = cacheDirName.lastIndexOf("&");
        File dirFile = new File(cacheDir);
        if (dirFile.isDirectory() && lastIndex != -1) {
            File[] files = dirFile.listFiles();
            for (File file : files) {
                if (file != null && file.getName().contains(cacheDirName.substring(0, lastIndex))) {
                    return file;
                }
            }
        }*/

        File dir = new File(cacheDir, cacheDirName);
        if (!dir.exists())
            dir.mkdirs();
        return dir;
    }


    public static long getDiskCacheTime(String cacheDirName, Context context) {
        String cacheDir;
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !Environment.isExternalStorageRemovable()) {
                cacheDir = getExternalCacheDir(context);
                if (cacheDir == null)//部分机型返回了null
                    cacheDir = getInternalCacheDir(context);
            } else {
                cacheDir = getInternalCacheDir(context);
            }
            int lastIndex = cacheDirName.lastIndexOf("&");
            File dirFile = new File(cacheDir);
            if (dirFile.isDirectory() && lastIndex != -1) {
                File[] files = dirFile.listFiles();
                for (File file : files) {
                    if (file != null && file.getName().contains(cacheDirName.substring(0, lastIndex))) {
                        return Long.parseLong(cacheDirName.substring(lastIndex + 1));
                    }
                }
            }
        } catch (Exception e) {

        }
        return -1;
    }

    /**
     * 获取当前app版本号
     *
     * @param context 上下文
     * @return 当前app版本号
     */
    public static int getAppVersion(Context context) {
        PackageManager manager = context.getPackageManager();
        int code = 1;
        try {
            code = manager.getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return code;
    }


    /**
     * 根据指定的url移除指定缓存
     * Note:请不要是使用DiskLruCache.remove()
     *
     * @param cache
     * @param url
     * @return
     */
    public static boolean remove(DiskLruCache cache, String url) {
        if (cache == null || url == null || TextUtils.isEmpty(url)) {
            return false;
        }
        try {
            return cache.remove(generateKey(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 根据原始键生成新键，以保证键的名称的合法性
     *
     * @param key 原始键，通常是url
     * @return
     */
    public static String generateKey(String key) {
        String cacheKey;
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(key.getBytes());
            cacheKey = bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1)
                builder.append('0');
            builder.append(hex);
        }
        return builder.toString();
    }

    private static String getExternalCacheDir(Context context) {
        File dir = context.getExternalCacheDir();
        if (dir == null)
            return null;
        if (!dir.exists())
            dir.mkdirs();
        return dir.getPath();
    }

    private static String getInternalCacheDir(Context context) {
        File dir = context.getCacheDir();
        if (!dir.exists())
            dir.mkdirs();
        return dir.getPath();
    }
}
