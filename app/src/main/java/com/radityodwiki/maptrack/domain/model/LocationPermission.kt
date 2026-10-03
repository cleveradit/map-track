package com.radityodwiki.maptrack.domain.model

/** Location permission state. [NOT_REQUESTED] is only derived by the UI layer. */
enum class LocationPermission { GRANTED, APPROXIMATE_ONLY, DENIED, NOT_REQUESTED }
