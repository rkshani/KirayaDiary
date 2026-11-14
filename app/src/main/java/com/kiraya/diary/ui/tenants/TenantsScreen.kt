package com.kiraya.diary.ui.tenants

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kiraya.diary.R
import com.kiraya.diary.ui.components.EmptyState
import com.kiraya.diary.ui.components.KirayaTopBar

@Composable
fun TenantsScreen(modifier: Modifier = Modifier) {
    Column(modifier) {
        KirayaTopBar(title = stringResource(R.string.nav_tenants))
        EmptyState(iconRes = R.drawable.ic_tenants, title = stringResource(R.string.coming_soon))
    }
}
