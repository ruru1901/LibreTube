package com.github.libretube.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.libretube.R

enum class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    Home("home", R.string.startpage, R.drawable.ic_home),
    Subscriptions("subscriptions", R.string.subscriptions, R.drawable.ic_subscriptions)
}