package com.example.store.compliance

data class BusinessLocationCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null
)

data class LocationSourceInfo(
    val sourceName: String,
    val verificationStatus: String,
    val notes: String
)

data class FetchedLocationInfo(
    val city: String,
    val localAuthority: String,
    val municipality: String,
    val wardOrZone: String,
    val areaOrLocality: String,
    val propertyClassification: String,
    val roadInfo: String,
    val localJurisdiction: String,
    val businessLocationStatus: String,
    val applicableAuthority: String,
    val importantJurisdiction: String,
    val importantLandUse: String,
    val importantRoadInfo: String,
    val otherNotes: String,
    val source: LocationSourceInfo
)

data class EditableBusinessAttributes(
    var businessLocationStatus: String = "Needs verification",
    var applicableAuthority: String = "Needs verification",
    var importantJurisdiction: String = "Needs verification",
    var importantLandUse: String = "Needs verification",
    var importantRoadInfo: String = "Needs verification",
    var city: String = "Not available",
    var localAuthority: String = "Not available",
    var municipality: String = "Not available",
    var wardOrZone: String = "Not available",
    var areaOrLocality: String = "Not available",
    var propertyClassification: String = "Not available",
    var roadInfo: String = "Not available",
    var localJurisdiction: String = "Needs verification",
    var otherNotes: String = "Not available"
)

sealed class BusinessComplianceUiState {
    data object Initial : BusinessComplianceUiState()
    data object RequestingPermission : BusinessComplianceUiState()
    data object GettingCurrentLocation : BusinessComplianceUiState()
    data object LocationObtained : BusinessComplianceUiState()
    data object FetchingInformation : BusinessComplianceUiState()
    data object InformationLoaded : BusinessComplianceUiState()
    data object PartialInformationAvailable : BusinessComplianceUiState()
    data object LocationUnavailable : BusinessComplianceUiState()
    data object PermissionDenied : BusinessComplianceUiState()
    data object LocationServicesDisabled : BusinessComplianceUiState()
    data object NetworkFailure : BusinessComplianceUiState()
    data object DataSourceUnavailable : BusinessComplianceUiState()
}
