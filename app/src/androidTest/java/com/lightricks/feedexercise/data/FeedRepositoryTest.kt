package com.lightricks.feedexercise.data

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lightricks.feedexercise.database.FeedDatabase
import com.lightricks.feedexercise.database.FeedItemDao
import com.lightricks.feedexercise.database.FeedItemEntity
import com.lightricks.feedexercise.network.MockFeedApiService
import junit.framework.Assert.assertEquals
import org.hamcrest.Matchers.greaterThan
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FeedRepositoryTest {

    private lateinit var feedDao: FeedItemDao
    private lateinit var db: FeedDatabase
    private lateinit var mockFeedApiService: MockFeedApiService

    @Before
    fun createDependencies() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, FeedDatabase::class.java
        ).build()
        feedDao = db.feedItemDao()
        mockFeedApiService = MockFeedApiService(context)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Test
    fun writeToDBReadFeedItems() {
        val feedItemEntities = mutableListOf<FeedItemEntity>()
        feedItemEntities.add(FeedItemEntity("1", "temp", true))
        val repository = FeedRepository(mockFeedApiService, db)
        feedDao.insertAll(feedItemEntities).blockingAwait()
        val res = repository.getFeedItems().blockingObserve()
        if (res != null) {
            assertEquals(feedItemEntities.size, res.size)
        }
    }

    @Test
    fun checkDBAfterRefresh() {
        val repository = FeedRepository(mockFeedApiService, db)
        val observer = repository.refresh().test()
        observer.awaitTerminalEvent()
        observer.assertComplete()
        observer.assertNoErrors()
        assertThat(feedDao.getCount(), greaterThan(0))
    }
}

private fun <T> LiveData<T>.blockingObserve(): T? {
    var value: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(t: T) {
            value = t
            latch.countDown()
            removeObserver(this)
        }
    }

    observeForever(observer)
    latch.await(5, TimeUnit.SECONDS)
    return value
}
