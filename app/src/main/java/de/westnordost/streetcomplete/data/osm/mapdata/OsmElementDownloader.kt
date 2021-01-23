package de.westnordost.streetcomplete.data.osm.mapdata

import android.util.Log
import de.westnordost.osmapi.common.errors.OsmQueryTooBigException
import de.westnordost.osmapi.map.MutableMapData
import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.map.data.OsmLatLon
import de.westnordost.osmapi.map.handler.MapDataHandler
import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.data.MapDataApi
import de.westnordost.streetcomplete.data.download.Downloader
import de.westnordost.streetcomplete.util.enlargedBy
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/** Takes care of downloading all note and osm quests */
class OsmElementDownloader @Inject constructor(
    private val mapDataApi: MapDataApi,
    private val osmElementController: OsmElementController
) : Downloader {

    @Synchronized override fun download(bbox: BoundingBox, cancelState: AtomicBoolean) {
        if (cancelState.get()) return

        val time = System.currentTimeMillis()

        val mapData = MutableMapData()
        val expandedBBox = bbox.enlargedBy(ApplicationConstants.QUEST_FILTER_PADDING)
        getMapAndHandleTooBigQuery(expandedBBox, mapData)
        /* The map data might be filled with several bboxes one after another if the download is
           split up in several, so lets set the bbox back to the bbox of the complete download */
        mapData.handle(expandedBBox)

        val seconds = (System.currentTimeMillis() - time) / 1000
        Log.i(TAG,"Downloaded ${mapData.nodes.size} nodes, ${mapData.ways.size} ways and ${mapData.relations.size} relations in ${seconds}s")

        osmElementController.updateForBBox(bbox, mapData)
    }

    private fun getMapAndHandleTooBigQuery(bounds: BoundingBox, mapDataHandler: MapDataHandler) {
        try {
            mapDataApi.getMap(bounds, mapDataHandler)
        } catch (e : OsmQueryTooBigException) {
            for (subBounds in bounds.splitIntoFour()) {
                getMapAndHandleTooBigQuery(subBounds, mapDataHandler)
            }
        }
    }

    companion object {
        private const val TAG = "OsmElementDownload"
    }
}

private fun BoundingBox.splitIntoFour(): List<BoundingBox> {
    val center = OsmLatLon((maxLatitude + minLatitude) / 2, (maxLongitude + minLongitude) / 2)
    return listOf(
        BoundingBox(minLatitude,     minLongitude,     center.latitude, center.longitude),
        BoundingBox(minLatitude,     center.longitude, center.latitude, maxLongitude),
        BoundingBox(center.latitude, minLongitude,     maxLatitude,     center.longitude),
        BoundingBox(center.latitude, center.longitude, maxLatitude,     maxLongitude)
    )
}