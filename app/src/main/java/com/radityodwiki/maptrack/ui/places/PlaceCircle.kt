package com.radityodwiki.maptrack.ui.places

import com.radityodwiki.maptrack.ui.map.circleRing
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

const val PLACE_CIRCLE_COLOR = "#1A73E8"

/** Place radius as a translucent fill with an outline, sharing one GeoJSON source. */
fun addPlaceCircleLayers(style: Style, source: GeoJsonSource) {
    style.addSource(source)
    style.addLayer(
        FillLayer("${source.id}-fill", source.id).withProperties(
            PropertyFactory.fillColor(PLACE_CIRCLE_COLOR),
            PropertyFactory.fillOpacity(0.15f),
        ),
    )
    style.addLayer(
        LineLayer("${source.id}-line", source.id).withProperties(
            PropertyFactory.lineColor(PLACE_CIRCLE_COLOR),
            PropertyFactory.lineWidth(2f),
        ),
    )
}

fun placeCircle(latitude: Double, longitude: Double, radiusMeters: Double): Polygon =
    Polygon.fromLngLats(listOf(circleRing(latitude, longitude, radiusMeters).map { (lng, lat) -> Point.fromLngLat(lng, lat) }))
