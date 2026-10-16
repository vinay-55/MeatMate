package com.example.store.compliance

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.io.IOException

interface LocationInformationFetcher {
    fun fetchInfo(coordinates: BusinessLocationCoordinates): FetchedLocationInfo
}

class AndroidLocationInformationFetcher(
    private val context: Context
) : LocationInformationFetcher {

    override fun fetchInfo(coordinates: BusinessLocationCoordinates): FetchedLocationInfo {
        val geocoder = Geocoder(context)
        val address = try {
            geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)?.firstOrNull()
        } catch (exception: IOException) {
            null
        } catch (exception: IllegalArgumentException) {
            null
        }

        val city = address?.locality ?: address?.subAdminArea ?: "Not available"
        val localAuthority = address?.subAdminArea ?: "Not available"
        val municipality = address?.adminArea ?: "Not available"
        val wardOrZone = address?.subLocality ?: (address?.featureName ?: "Not available")
        val areaOrLocality = address?.subLocality ?: address?.featureName ?: "Not available"
        val roadInfo = address?.thoroughfare ?: "Not available"
        val localJurisdiction = listOfNotNull(
            localAuthority.takeIf { it != "Not available" },
            municipality.takeIf { it != "Not available" }
        ).joinToString(" / ").ifEmpty { "Needs verification" }

        val propertyClassification = "Not available"
        val businessLocationStatus = if (address != null) "Business location identified" else "Needs verification"
        val applicableAuthority = if (localJurisdiction == "Needs verification") "Needs verification" else localJurisdiction
        val importantJurisdiction = localJurisdiction
        val importantLandUse = "Needs verification"
        val importantRoadInfo = roadInfo.takeIf { it != "Not available" } ?: "Needs verification"
        val otherNotes = buildString {
            if (address != null) {
                append("Reverse geocoder data available. ")
                if (!address.locality.isNullOrBlank()) append("City: ${address.locality}. ")
                if (!address.adminArea.isNullOrBlank()) append("Administrative area: ${address.adminArea}. ")
                if (!address.thoroughfare.isNullOrBlank()) append("Road: ${address.thoroughfare}. ")
            } else {
                append("Geocoder data unavailable. Manual verification required.")
            }
        }

        val sourceName = if (address != null) "Android reverse geocoding" else "Unavailable"
        val verificationStatus = if (address != null) "Needs verification" else "Not available"

        return FetchedLocationInfo(
            city = city,
            localAuthority = localAuthority,
            municipality = municipality,
            wardOrZone = wardOrZone,
            areaOrLocality = areaOrLocality,
            propertyClassification = propertyClassification,
            roadInfo = roadInfo,
            localJurisdiction = localJurisdiction,
            businessLocationStatus = businessLocationStatus,
            applicableAuthority = applicableAuthority,
            importantJurisdiction = importantJurisdiction,
            importantLandUse = importantLandUse,
            importantRoadInfo = importantRoadInfo,
            otherNotes = otherNotes,
            source = LocationSourceInfo(
                sourceName = sourceName,
                verificationStatus = verificationStatus,
                notes = "Coordinates were used to derive available location details. Government or local authority data should be added later for authoritative verification."
            )
        )
    }
}
