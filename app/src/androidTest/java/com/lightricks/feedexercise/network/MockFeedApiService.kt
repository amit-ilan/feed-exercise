package com.lightricks.feedexercise.network

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Single
import java.io.BufferedReader

class MockFeedApiService(private val context: Context) : FeedApiService {
    override fun getFeed(): Single<GetFeedResponse> {
        val bufferedReader: BufferedReader =
            context.assets.open("get_feed_response.json").bufferedReader()
        val inputString = bufferedReader.use { it.readText() }
        return Single.just(makeResponse(inputString))
    }

    private fun makeResponse(jsonString: String): GetFeedResponse? {
        val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(GetFeedResponse::class.java)
        return adapter.fromJson(jsonString)
    }
}