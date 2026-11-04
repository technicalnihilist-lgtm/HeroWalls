package com.herowalls.app

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.herowalls.app.data.download.WallpaperDownloader
import com.herowalls.app.data.local.AppDatabase
import com.herowalls.app.data.local.LibraryRepository
import com.herowalls.app.data.remote.WikimediaApi
import com.herowalls.app.data.remote.WikimediaRepository
import com.herowalls.app.wallpaper.WallpaperSetter
import com.herowalls.app.work.HeroWallsWork
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class HeroWallsApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        HeroWallsWork.schedule(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { container.okHttpClient }
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.22)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(File(cacheDir, "image_cache"))
                .maxSizeBytes(320L * 1024 * 1024)
                .build()
        }
        .crossfade(220)
        .respectCacheHeaders(false)
        .build()
}

/** Simple manual dependency graph - no reflection, no annotation processing at runtime. */
class AppContainer(context: Context) {

    val gson: Gson = GsonBuilder().setLenient().create()

    private val httpCache = Cache(File(context.cacheDir, "http_cache"), 48L * 1024 * 1024)

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cache(httpCache)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", WallpaperDownloader.USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
            )
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://commons.wikimedia.org/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val api: WikimediaApi = retrofit.create(WikimediaApi::class.java)

    private val database: AppDatabase = AppDatabase.get(context)

    val library: LibraryRepository = LibraryRepository(database, gson)

    val wikimedia: WikimediaRepository = WikimediaRepository(api, database.cacheDao(), gson)

    val downloader: WallpaperDownloader = WallpaperDownloader(context, okHttpClient, library)

    val wallpaperSetter: WallpaperSetter = WallpaperSetter(context)
}
