package de.werner.cookmore

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri

@Composable
fun AboutComposable(
    modifier: Modifier,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val fontSize by viewModel.font_size.collectAsStateWithLifecycle()

    // Header
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(65.dp)
                .padding(bottom = 4.dp)
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "About",
                modifier = Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.headlineLarge,
//                fontSize = fontSize.sp
            )
        }
        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ########################################
            item {
                Text(stringResource(R.string.policy_link), style = MaterialTheme.typography.titleMedium,
                    fontSize = (fontSize + 4).sp)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Link", fontSize = fontSize.sp)
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://wernerth94.github.io/CookMore/".toUri())
                        context.startActivity(intent)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.link_2),
                            contentDescription = "Profile"
                        )
                    }
                }
            }


            // ########################################
            item {
                Text(stringResource(R.string.icon_artists), style = MaterialTheme.typography.titleMedium,
                    fontSize = (fontSize + 4).sp,
                    modifier = Modifier.padding(top=20.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Royyan Wijaya", fontSize = fontSize.sp)
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://www.flaticon.com/free-icon/pan_6059516".toUri())
                        context.startActivity(intent)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.link_2),
                            contentDescription = "Profile"
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("edt.im", fontSize = fontSize.sp)
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://www.flaticon.com/free-icon/robot_12637629".toUri())
                        context.startActivity(intent)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.link_2),
                            contentDescription = "Profile"
                        )
                    }
                }
            }
        }
    }
}