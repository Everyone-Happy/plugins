package io.flutter.plugins.videoplayer;

import android.content.Context;

import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;

/**
 * Created by bby
 * Date: 2020/10/20.
 */
public class ExoPlayerCache {

    private static SimpleCache sDownloadCache;

    public static SimpleCache getInstance(Context context, long maxCacheSize) {
        if (sDownloadCache == null) {
            sDownloadCache = new SimpleCache(new File(context.getCacheDir(), "exoCache"), new LeastRecentlyUsedCacheEvictor(maxCacheSize));
        }
        return sDownloadCache;
    }


}
