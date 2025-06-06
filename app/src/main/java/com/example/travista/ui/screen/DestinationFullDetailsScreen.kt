package com.example.travista.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.travista.R
import com.example.travista.data.detinationfulldetails.getPriceLevelText
import com.example.travista.data.getApikey
import com.example.travista.ui.viewmodel.DestinationFullDetailsViewModel
import com.example.travista.utils.isNetworkAvailable

@Composable
fun DestinationFullDetailsScreen(
    placeId: String,
    apiKey: String = getApikey(),
    viewModel: DestinationFullDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val destinationState by viewModel.destinationFullDetails.collectAsState()
    val scrollState = rememberScrollState()

    val showLoading = destinationState == null && isNetworkAvailable(context)

    // ✅ Only call API if not already loaded and internet is available
    LaunchedEffect(placeId, apiKey, destinationState) {
        if (destinationState == null && isNetworkAvailable(context)) {
            viewModel.fetchDestinationFullDetails(placeId, apiKey)
        } else if (!isNetworkAvailable(context) && destinationState == null) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
        }
    }

    if (showLoading) {
        LoadingLottieAnimation()
    } else {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            when {
                destinationState == null -> {
                    CircularProgressIndicator()
                }

                destinationState!!.isFailure -> {
                    Text("Error: ${destinationState!!.exceptionOrNull()?.message}")
                }

                destinationState!!.isSuccess -> {
                    val destinationFullDetails = destinationState!!.getOrNull()
                    destinationFullDetails?.let {
                        val defaultImageUrls = listOf(
                            "https://plus.unsplash.com/premium_vector-1739976218147-36f2581a0bbd?q=80&w=2148&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
                        )

                        val photoUrls = it.photos?.mapNotNull { photo ->
                            photo.photoReference?.let { reference ->
                                "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=$reference&key=$apiKey"
                            }
                        } ?: emptyList()

                        if (photoUrls.isEmpty()) {
                            ImageCarousel(imageUrls = defaultImageUrls)
                        } else {
                            ImageCarousel(imageUrls = photoUrls)
                        }

                        Column(Modifier.padding(8.dp)) {
                            Text("${it.name}", fontWeight = Bold, fontSize = 30.sp)
                            Text("${it.address}")
                            Text("Rating: ${it.rating}")
                            Text("Total Ratings: ${it.totalRatings}")
                            Text("Website: ${it.website}")
                            Text("Call: ${it.phoneNumber}")
                            Text("Price Level: ${getPriceLevelText(it.priceLevel)}")

                            if (it.openingHours?.isOpenNow == true) {
                                Text("Open Now: Yes\n", fontWeight = Bold)
                            } else {
                                Text("Open Now: No\n", fontWeight = Bold)
                            }

                            if (it.openingHours != null) {
                                Text("Open Hours:", fontWeight = Bold)
                                Text("${it.openingHours.weekdayText?.joinToString("\n")}")
                            } else {
                                Text("Open Hours: Not Available", fontWeight = Bold)
                            }

                            Text("Tags:", fontWeight = Bold)
                            Text("${it.types?.joinToString("\n")}")

                            Button(
                                onClick = {
                                    val mapsUrl = it.googleMapsUrl
                                    val intent = Intent(Intent.ACTION_VIEW, mapsUrl?.toUri())
                                    intent.setPackage("com.google.android.apps.maps")
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, mapsUrl?.toUri())
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text("Open in Google Maps", fontSize = 16.sp)
                            }

                            Button(
                                onClick = {
                                    val mapsUrl =
                                        "https://www.google.com/maps/dir/?api=1&destination=${
                                            Uri.encode(it.name + "," + it.address)
                                        }"
                                    val intent = Intent(Intent.ACTION_VIEW, mapsUrl.toUri())
                                    intent.setPackage("com.google.android.apps.maps")
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, mapsUrl.toUri()))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text("Get Directions", fontSize = 16.sp)
                            }

                            Spacer(Modifier.padding(30.dp))

                            if (it.reviews.isNullOrEmpty()) {
                                Text("No reviews available", fontWeight = Bold)
                            } else {
                                Text(text = "Reviews:", fontWeight = Bold, fontSize = 24.sp)
                                Spacer(Modifier.padding(8.dp))

                                it.reviews.forEach { review ->
                                    Row(
                                        Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Absolute.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(review.profileImageUrl)
                                                .crossfade(true)
                                                .error(R.drawable.default_background_image)
                                                .build(),
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Text(" ${review.authorName}", fontWeight = Bold)
                                    }
                                    Text("Rating: ${review.rating}\n", fontWeight = Bold)
                                    Text("${review.reviewText}")
                                    HorizontalDivider(modifier = Modifier.padding(12.dp))
                                }

                                Button(
                                    onClick = {
                                        val mapsUrl = it.googleMapsUrl
                                        val intent = Intent(Intent.ACTION_VIEW, mapsUrl?.toUri())
                                        intent.setPackage("com.google.android.apps.maps")
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(intent)
                                        } else {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, mapsUrl?.toUri()))
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Text("See all reviews", fontSize = 16.sp)
                                }

                                Spacer(Modifier.padding(30.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun ShowHotelDetails() {
    // Preview content if needed
}
