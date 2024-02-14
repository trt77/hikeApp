package com.example.hikeapp


data class Product(
    val code: String,
    val product: ProductDetails,
    val status: Int,
    val status_verbose: String
)


data class ProductDetails(
    val product_name: String,
    val nutrition_grades: String,
    val nutriments: Nutriments,
    val nutriscore_data: NutriscoreData?
)

data class Nutriments(
    val carbohydrates: Double,
    val carbohydrates_100g: Double,
    val carbohydrates_unit: String,
    val carbohydrates_value: Double,
    val energy: Int,
    val energy_kcal: Int,
    val energy_kcal_100g: Int,
    val energy_kcal_unit: String,
    val sugars: Double,
    val sugars_100g: Double,
    val sugars_unit: String,
    val sugars_value: Double
    // Add other nutrients as required
)

data class NutriscoreData(
    val energy: Int,
    val energy_points: Int,
    val energy_value: Int,
    val sugars_points: Int,
    val sugars_value: Double
    // Add other computation data as required
)

data class SearchResponse(
    val count: Int,
    val products: List<ProductDetails>,
    val page: Int,
    val page_size: Int,
    val status: Int,
    val status_verbose: String
)