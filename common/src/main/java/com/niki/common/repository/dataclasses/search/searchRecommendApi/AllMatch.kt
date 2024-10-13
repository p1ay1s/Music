package com.niki.common.repository.dataclasses.search.searchsuggest

data class AllMatch(
    val alg: String,
    val feature: String,
    val keyword: String,
    val lastKeyword: String,
    val type: Int
)