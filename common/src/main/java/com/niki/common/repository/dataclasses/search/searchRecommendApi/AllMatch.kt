package com.niki.common.repository.dataclasses.search.searchRecommendApi

data class AllMatch(
    val alg: String,
    val feature: String,
    val keyword: String, // to show
    val lastKeyword: String,
    val type: Int
)