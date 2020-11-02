package io.flutter.plugins.videoplayer

import android.net.Uri
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSourceException
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheUtil
import io.flutter.plugins.videoplayer.BuildConfig.DEBUG
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Created by bby
 * Date: 2020/10/20.
 */
abstract class BaseVideoPlayer {


    private val WORKER_COUNT_PRELOAD = 3

    /** 忽略集合，处于此集合中的链接将不再缓存 */
    private val ignoreSet = Collections.synchronizedSet(HashSet<String>())
    /** 等待集合，处于此集合中的表示任务已提交，等待被缓存 */
    private val waitQueue = Collections.synchronizedSet(HashSet<Future<*>>())
    /** 加载集合，处于此集合中的表示正在缓存中 */
    private val loadQueue = Collections.synchronizedSet(HashSet<String>())
    /** 处理缓存任务的线程池 */
    private val executors = Executors.newFixedThreadPool(WORKER_COUNT_PRELOAD)

    public fun preloadMedia(uris: List<Uri>, byteSize: Long) {
        // 清除已加载完成的task
        waitQueue.removeAll { it.isDone }
        // 尝试暂停未加载的task
        waitQueue.forEach { it.cancel(false) }

        uris.forEach {
            val task = executors.submit {
                val key = CacheUtil.generateKey(it)
                // 如果在忽略集合中存在，则无需再次加载
                if (ignoreSet.contains(key)) return@submit
                // 如果在加载队列中存在，则无需再次加载
                if (loadQueue.contains(key)) return@submit

                loadQueue.add(key)
                val spec = DataSpec(it, 0, byteSize, key)
                try {
                    CacheUtil.cache(spec, getCache(), getDataSource(), null, null)
                } catch (ex: HttpDataSource.InvalidResponseCodeException) {
                    // Http 416，表示Range越界，加入到忽略集合中，不再缓存
                    if ((ex.cause as? DataSourceException)?.reason == DataSourceException.POSITION_OUT_OF_RANGE) {
                        ignoreSet.add(ex.dataSpec.key)
                    }
                    // IO错误有可能造成缓存异常，因此直接删掉
                    CacheUtil.remove(getCache(), key)
                } catch (ex: IOException) {
                    // IO错误有可能造成缓存异常，因此直接删掉
                    CacheUtil.remove(getCache(), key)
                } catch (ex: Exception) {
                    if (DEBUG) ex.printStackTrace()
                } finally {
                    loadQueue.remove(key)
                }
            }
            waitQueue.add(task)
        }
    }


    open fun preload(urls: String, byteSize: Long) {
        val list = ArrayList<Uri>()
        urls.split(",").forEach {
            if (it.isNotEmpty()) {
                list.add(Uri.parse(it))
            }
        }
        preloadMedia(list, byteSize)
    }

    abstract fun getCache(): Cache?

    abstract fun getDataSource(): DataSource?
}