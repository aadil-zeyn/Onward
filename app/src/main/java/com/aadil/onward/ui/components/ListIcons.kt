package com.aadil.onward.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Schema
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aadil.onward.ui.theme.Onward

/** The icons a list can wear, by the key saved with it. */
object ListIcons {
    val all: List<Pair<String, ImageVector>> = listOf(
        "inbox" to Icons.Rounded.Inbox,
        "heart" to Icons.Rounded.Favorite,
        "home" to Icons.Rounded.Home,
        "cart" to Icons.Rounded.ShoppingCart,
        "book" to Icons.Rounded.AutoStories,
        "fitness" to Icons.Rounded.FitnessCenter,
        "plane" to Icons.Rounded.Flight,
        "money" to Icons.Rounded.Payments,
        "idea" to Icons.Rounded.Lightbulb,
        "star" to Icons.Rounded.Star,
        "work" to Icons.Rounded.Work,
        "tree" to Icons.Rounded.AccountTree,
        "graphql" to Icons.Rounded.Hub,
        "database" to Icons.Rounded.Storage,
        "graph" to Icons.Rounded.Schema,
        "code" to Icons.Rounded.Code,
        "bug" to Icons.Rounded.BugReport,
        "cloud" to Icons.Rounded.Cloud,
        "rocket" to Icons.Rounded.RocketLaunch,
        "bolt" to Icons.Rounded.Bolt,
        "flag" to Icons.Rounded.Flag,
        "shapes" to Icons.Rounded.Category,
    )
    private val byKey = all.toMap()

    fun of(key: String): ImageVector = byKey[key] ?: Icons.Rounded.Inbox
}

/** A rounded tile washed in [color] with the list's icon on top. */
@Composable
fun IconTile(icon: ImageVector, color: Long, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val c = Onward.colors
    Box(
        modifier
            .size(size)
            .background(c.wash(color), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.tint(color), modifier = Modifier.size(size * 0.5f))
    }
}
