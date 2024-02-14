package com.example.hikeapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("cgi/search.pl")
    fun searchProducts(@Query("search_terms") query: String, @Query("search_simple") simple: Int = 1, @Query("json") json: Int = 1): Call<SearchResponse>
    @GET("api/v0/product/{code}.json")
    fun getProductDetails(@Path("code") barcode: String): Call<Product>
}