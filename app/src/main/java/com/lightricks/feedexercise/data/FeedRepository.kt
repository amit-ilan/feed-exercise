package com.lightricks.feedexercise.data

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemEntity
import com.lightricks.feedexercise.network.FeedApi
import com.lightricks.feedexercise.network.GetFeedResponse
import com.lightricks.feedexercise.network.TemplatesMetadataItem
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers


private const val BASE_URL: String =
    "https://assets.swishvideoapp.com/Android/demo/catalog/thumbnails/"

/**
 * This is our data layer abstraction. Users of this class don't need to know
 * where the data actually comes from (network, database or somewhere else).
 */
class FeedRepository(
    private val feedApiService: FeedApi,
    private val feedDatabase: FeedDatabase
) {

    private val feedItems: LiveData<List<FeedItem>> =
        Transformations.map(feedDatabase.feedItemDao().getAll()) {
            it.toFeedItems()
        }

    fun getFeedItems(): LiveData<List<FeedItem>> = feedItems

    @SuppressLint("CheckResult")
    fun refresh(): Completable {
        return feedApiService.service.getFeed()
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { feedResponse ->
                handleResponse(feedResponse)
            }
    }

    private fun handleResponse(feedResponse: GetFeedResponse): Completable {
        val feedItemEntityList = mutableListOf<FeedItemEntity>()
        for (item in feedResponse.templatesMetadata) {
            feedItemEntityList.add(toFeedItemEntity(item))
        }
        return saveItemsToDB(feedItemEntityList)
    }

    /**
     * Convert templatesMetadataItem to FeedItemEntity
     */
    private fun toFeedItemEntity(templatesMetadataItem: TemplatesMetadataItem): FeedItemEntity {
        return FeedItemEntity(
            templatesMetadataItem.id,
            templatesMetadataItem.templateThumbnailURI,
            templatesMetadataItem.isPremium
        )
    }

    private fun saveItemsToDB(items: MutableList<FeedItemEntity>): Completable {
        return feedDatabase.feedItemDao().insertAll(items)
    }


    private fun List<FeedItemEntity>.toFeedItems(): List<FeedItem> {
        return map {
            FeedItem(it.id, BASE_URL + it.thumbnailUrl, it.isPremium)
        }
    }
}