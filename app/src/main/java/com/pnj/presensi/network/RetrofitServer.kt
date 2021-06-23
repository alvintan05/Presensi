package com.pnj.presensi.network

import com.pnj.presensi.utils.Constant
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitServer {

    private val loggingInterceptor: HttpLoggingInterceptor =
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    private val okHttpClientAzure = OkHttpClient.Builder().apply {
        addInterceptor(
            Interceptor { chain ->
                val builder = chain.request().newBuilder()
                builder.header("Ocp-apim-subscription-key", Constant.API_KEY_AZURE)
                return@Interceptor chain.proceed(builder.build())
            }
        )
        addInterceptor(loggingInterceptor)
    }.build()

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constant.BASE_URL_API)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    private fun getRetrofitAzure(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constant.BASE_URL_AZURE)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClientAzure)
            .build()
    }

    val apiRequest: ApiRequest = getRetrofit().create(ApiRequest::class.java)
    val azureRequest: AzureRequest = getRetrofitAzure().create(AzureRequest::class.java)

}
