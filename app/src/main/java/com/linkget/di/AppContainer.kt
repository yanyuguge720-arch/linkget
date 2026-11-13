package com.linkget.di

import android.content.Context
import androidx.room.Room
import com.linkget.core.ImageLoader
import com.linkget.core.InMemoryCookieJar
import com.linkget.data.db.AppDatabase
import com.linkget.data.download.DownloadRepository
import com.linkget.data.download.MediaPublisher
import com.linkget.data.download.ResumableFileDownloader
import com.linkget.data.parser.ApiWorkParser
import com.linkget.data.parser.BilibiliWorkParser
import com.linkget.data.parser.DouyinWorkParser
import com.linkget.data.parser.KuaishouWorkParser
import com.linkget.data.parser.ParseCoordinator
import com.linkget.data.parser.ToutiaoWorkParser
import com.linkget.data.parser.XiaohongshuWorkParser
import com.linkget.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-written dependency graph; small enough that a DI framework would add noise. */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(InMemoryCookieJar())
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    val settings = SettingsRepository(appContext)

    val parser = ParseCoordinator(
        settings = settings,
        douyin = DouyinWorkParser(client, settings = settings),
        bilibili = BilibiliWorkParser(client, settings),
        xiaohongshu = XiaohongshuWorkParser(client),
        kuaishou = KuaishouWorkParser(client),
        toutiao = ToutiaoWorkParser(client),
        api = ApiWorkParser(client)
    )

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "linkget.db"
    ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()

    val dao get() = database.downloadDao()
    val historyDao get() = database.historyDao()

    val downloads = DownloadRepository(
        context = appContext,
        dao = dao,
        fileDownloader = ResumableFileDownloader(client),
        publisher = MediaPublisher(appContext),
        settings = settings
    )

    /** Clipboard text read on ON_RESUME, consumed by ParseViewModel. */
    val clipboardReads = MutableSharedFlow<String?>(replay = 1, extraBufferCapacity = 8)

    val imageLoader = ImageLoader(client)
}
