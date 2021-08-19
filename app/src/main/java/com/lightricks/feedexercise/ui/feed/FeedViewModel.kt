package com.lightricks.feedexercise.ui.feed

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.*
import com.lightricks.feedexercise.data.FeedItem
import com.lightricks.feedexercise.data.FeedRepository
import com.lightricks.feedexercise.database.getDatabase
import com.lightricks.feedexercise.network.FeedApi
import com.lightricks.feedexercise.util.Event
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.IllegalArgumentException


/**
 * This view model manages the data for [FeedFragment].
 */
open class FeedViewModel(private val repository: FeedRepository) : ViewModel() {
    private val isLoading = MutableLiveData<Boolean>()
    private val isEmpty = MutableLiveData<Boolean>()
    private val feedItems = MediatorLiveData<List<FeedItem>>()
    private val networkErrorEvent = MutableLiveData<Event<String>>()
    private val compositeDisposable = CompositeDisposable()

    fun getIsLoading(): LiveData<Boolean> = isLoading
    fun getIsEmpty(): LiveData<Boolean> = isEmpty
    fun getFeedItems(): LiveData<List<FeedItem>> = feedItems
    fun getNetworkErrorEvent(): LiveData<Event<String>> = networkErrorEvent

    init {
        feedItems.addSource(repository.getFeedItems()) { items -> updateFeedItems(items) }
        refresh()
    }

    fun refresh() {
        isLoading.value = true
        isEmpty.value = true
        loadFeedItems()
    }

    @SuppressLint("CheckResult")
    fun loadFeedItems() {
        compositeDisposable.add(
            repository.refresh()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ handleSuccess() }, { error -> handleNetworkError(error) })
        )
    }

    override fun onCleared() {
        compositeDisposable.clear()
    }

    private fun updateFeedItems(items: List<FeedItem>) {
        feedItems.value = items
        isEmpty.value = items.isEmpty()
    }

    private fun handleSuccess() {
        isLoading.value = false
    }

    private fun handleNetworkError(error: Throwable) {
        networkErrorEvent.value = Event(error.message ?: "oops!")
    }
}

/**
 * This class creates instances of [FeedViewModel].
 * It's not necessary to use this factory at this stage. But if we will need to inject
 * dependencies into [FeedViewModel] in the future, then this is the place to do it.
 */
class FeedViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        if (!modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            throw IllegalArgumentException("factory used with a wrong class")
        }

        val repository = FeedRepository(
            FeedApi.service, getDatabase(context)
        )

        @Suppress("UNCHECKED_CAST")
        return FeedViewModel(repository) as T
    }
}