package io.github.cfsima.modernsafe.navigation

import kotlinx.serialization.Serializable

@Serializable
data class AskPasswordRoute(val isLocal: Boolean = true)

@Serializable
data object CategoryListRoute

@Serializable
data class PassListRoute(val categoryId: Long)

@Serializable
data class PassViewRoute(
    val id: Long,
    val categoryId: Long,
    val rowIds: List<Long> = emptyList(),
    val position: Int = -1
)

@Serializable
data class PassEditRoute(
    val id: Long = -1L,
    val categoryId: Long = -1L
)

@Serializable
data object PassGenRoute

@Serializable
data object SearchRoute

@Serializable
data object ChangePassRoute

@Serializable
data object SettingsRoute

@Serializable
data class RestoreRoute(
    val filePath: String? = null,
    val isFirstTime: Boolean = false
)

@Serializable
data object RestoreFirstTimeRoute

@Serializable
data object HelpRoute

@Serializable
data object LogOffRoute
