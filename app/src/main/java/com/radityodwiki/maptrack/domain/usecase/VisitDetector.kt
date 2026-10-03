package com.radityodwiki.maptrack.domain.usecase

import com.radityodwiki.maptrack.domain.model.LocationPoint
import com.radityodwiki.maptrack.domain.model.Visit

/** Detects visits in the points of one trip (PRD §38 Fase 2). Points are expected in recordedAt order. */
object VisitDetector {

    fun detect(points: List<LocationPoint>): List<Visit> {
        if (points.size < 2) return emptyList()

        val visits = mutableListOf<Cluster>()
        var cluster = Cluster(points.first())
        for (point in points.drop(1)) {
            if (cluster.distanceTo(point.latitude, point.longitude) <= PlaceDetectionConfig.VISIT_RADIUS_METERS) {
                cluster.add(point)
            } else {
                if (cluster.isVisit()) visits += cluster
                cluster = Cluster(point)
            }
        }
        if (cluster.isVisit()) visits += cluster

        return mergeConsecutive(visits).map { it.toVisit() }
    }

    /** Absorbs a visit into the previous one when both are close in time and space, smoothing stray fixes. */
    private fun mergeConsecutive(visits: List<Cluster>): List<Cluster> {
        val merged = mutableListOf<Cluster>()
        for (visit in visits) {
            val previous = merged.lastOrNull()
            if (previous != null &&
                visit.firstAt - previous.lastAt <= PlaceDetectionConfig.MERGE_GAP_MS &&
                previous.distanceTo(visit.centerLatitude, visit.centerLongitude) <= PlaceDetectionConfig.MERGE_DISTANCE_METERS
            ) {
                previous.absorb(visit)
            } else {
                merged += visit
            }
        }
        return merged
    }

    /** Points within the radius of their running mean center. */
    private class Cluster(first: LocationPoint) {
        private val tripId = first.tripId
        private var latitudeSum = first.latitude
        private var longitudeSum = first.longitude
        private var count = 1
        val firstAt = first.recordedAt
        var lastAt = first.recordedAt
            private set

        val centerLatitude get() = latitudeSum / count
        val centerLongitude get() = longitudeSum / count

        fun distanceTo(latitude: Double, longitude: Double): Double =
            GeoDistance.meters(centerLatitude, centerLongitude, latitude, longitude)

        fun add(point: LocationPoint) {
            latitudeSum += point.latitude
            longitudeSum += point.longitude
            count++
            lastAt = point.recordedAt
        }

        fun absorb(other: Cluster) {
            latitudeSum += other.latitudeSum
            longitudeSum += other.longitudeSum
            count += other.count
            lastAt = other.lastAt
        }

        fun isVisit() = lastAt - firstAt >= PlaceDetectionConfig.MIN_VISIT_DURATION_MS

        fun toVisit() = Visit(tripId, firstAt, lastAt, centerLatitude, centerLongitude, count)
    }
}
