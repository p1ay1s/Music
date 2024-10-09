package com.niki.common.repository.dataclasses

/**
 * 热门 - 包含热门大类的结果
 */
data class HotPlaylistResponse(
    var code: Int,
    var tags: List<Tag> = listOf()
)

/**
 * 单个大类的信息
 * @param id 通过 id 可以获取当前大类的歌曲
 */
data class Tag(
    var id: String = "",
    var name: String = "",
)

/**
 * 分类 - TODO 目前还没有在使用
 */
data class CatePlaylistResponse(
    var code: Int,
    var all: Sub = Sub(),
    var sub: List<Sub> = listOf()
)

/**
 * 应该和 Tag 类差不多
 */
data class Sub(
    var name: String = "",
    var resourceCount: Int = 0,
    var category: String = ""
)

/**
 * 最流行 - 包含流行歌单的结果
 */
data class TopPlaylistResponse(
    var more: Boolean,
    var code: Int,
    var playlists: List<Playlist> = listOf()
)

/**
 * 单个歌单的信息
 * @param id 通过 id 可以获取当前大类的歌曲
 */
data class Playlist(
    var name: String = "",
    var id: String = "",
    var coverImgUrl: String = "",
    var description: String = "",
    var tags: List<String> = listOf(),
    var creator: Creator = Creator()
)

/**
 * 歌单的作者
 */
data class Creator(
    var nickname: String = "",
    var avatarUrl: String = ""
)