import com.squareup.moshi.JsonClass
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.text.StringEscapeUtils.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.time.ZonedDateTime

val baseUrl = "https://www.srf.ch/"
val interceptor = HttpLoggingInterceptor()
interceptor.level = HttpLoggingInterceptor.Level.NONE
var retrofit = Retrofit.Builder()
    .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
    .baseUrl(baseUrl)
    .addConverterFactory(MoshiConverterFactory.create())
    .build()

interface SrfAPI {
    @GET("play/radio/show/86a19eb5-f627-476e-9d62-2253c610ca11/latestEpisodes")
    fun getLatestEpisodes(
        @Query("nextPageHash") nextPageHash: String?,
        @Query("maxDates") maxDates: String = "ALL"
    ): Call<MentalXEpisodesList>
}

val srfAPI = retrofit.create(SrfAPI::class.java)

data class MentalXEpisode(
    val title: String,
    val playerUrl: String,
    val date: ZonedDateTime,
    val imageUrl: String
)

val episodes = mutableListOf<MentalXEpisode>()

fetchEpisodes(null)

fun MentalXCrawler.fetchEpisodes(nextPageHash: String?) {
    val mentalXEpisodesList = srfAPI.getLatestEpisodes(nextPageHash).execute().body()
    println(mentalXEpisodesList)
    mentalXEpisodesList?.episodes?.forEach {

        println("${it.title} -> ${it.absoluteDetailUrl}")
        episodes.add(MentalXEpisode(it.title!!, it.absoluteDetailUrl!!, ZonedDateTime.parse(it.dateInISO!!), it.imageUrl!!))
    }

    val url1 = mentalXEpisodesList?.nextPageUrl
    if (!url1.isNullOrBlank()) {
        val hash = "https://any.ch$url1".toHttpUrl().queryParameter("nextPageHash")
        fetchEpisodes(hash)
    } else {
        println("finish")
    }
}

println(episodes)

val episodeTextFile = StringBuilder()
episodeTextFile.appendLine("date,image,title,playerUrl")
episodes.forEach {
    episodeTextFile.appendLine("${escapeCsv(it.date.toString())},=IMAGE(\"${it.imageUrl}\"),${escapeCsv(it.title)},${it.playerUrl}")
}

File("MentalXEpisodes.csv").writeText(episodeTextFile.toString())


@JsonClass(generateAdapter = false)
data class Episode(
    val absoluteDetailUrl: String?,
    val date: String?,
    val dateInISO: String?,
    val description: String?,
    val detailUrl: String?,
    val duration: String?,
    val expiration: Expiration?,
    val id: String?,
    val imageTitle: String?,
    val imageUrl: String?,
    val isGeoblocked: Boolean?,
    val isLivestream: Boolean?,
    val lead: String?,
    val mediaType: String?,
    val popupText: String?,
    val popupUrl: String?,
    val publishDatePhrase: String?,
    val segments: List<Any>?,
    val showId: String?,
    val showTitle: String?,
    val subtitle: String?,
    val title: String?,
    val urn: String?,
    val views: Int?
)

@JsonClass(generateAdapter = false)
data class EpisodeCount(
    val isAtLeastOne: Boolean?,
    val isDefined: Boolean?
)

@JsonClass(generateAdapter = false)
class Expiration(
)

@JsonClass(generateAdapter = false)
data class Show(
    val absoluteOverviewUrl: String?,
    val allowIndexing: Boolean?,
    val description: String?,
    val episodeCount: EpisodeCount?,
    val hasHomepageUrlAndNoLinks: Boolean?,
    val hasLinks: Boolean?,
    val id: String?,
    val imageTitle: String?,
    val imageUrl: String?,
    val lead: String?,
    val overviewUrl: String?,
    val primaryChannelId: String?,
    val title: String?,
    val urlToLatestEpisode: String?,
    val urn: String?
)

@JsonClass(generateAdapter = false)
data class MentalXEpisodesList(
    val episodes: List<Episode>?,
    val episodesAvailableForSelectedDate: Boolean?,
    val nextPageUrl: String?,
    val show: Show?
)
