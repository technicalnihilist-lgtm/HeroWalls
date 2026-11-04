package com.herowalls.app.data.model

/**
 * Small in-memory hand-off so the detail screen always opens instantly with the
 * exact object that was tapped (including its credits), while Room remains the
 * source of truth for favourites and downloads.
 */
object WallpaperDirectory {
    private val map = LinkedHashMap<Long, Wallpaper>()

    fun remember(wallpapers: Collection<Wallpaper>) {
        wallpapers.forEach { map[it.pageId] = it }
        while (map.size > 400) {
            val oldest = map.keys.firstOrNull() ?: break
            map.remove(oldest)
        }
    }

    fun get(pageId: Long): Wallpaper? = map[pageId]
}
