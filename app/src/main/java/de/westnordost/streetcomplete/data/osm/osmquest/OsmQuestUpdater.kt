package de.westnordost.streetcomplete.data.osm.osmquest

import android.util.Log
import de.westnordost.countryboundaries.isInAny
import de.westnordost.countryboundaries.CountryBoundaries
import de.westnordost.countryboundaries.intersects
import de.westnordost.osmapi.map.MapDataWithGeometry
import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.map.data.Element
import de.westnordost.osmapi.map.data.LatLon
import de.westnordost.osmapi.map.data.OsmLatLon
import de.westnordost.streetcomplete.ApplicationConstants
import de.westnordost.streetcomplete.data.osm.elementgeometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.elementgeometry.ElementPolylinesGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.ElementKey
import de.westnordost.streetcomplete.data.osm.mapdata.OsmElementSource
import de.westnordost.streetcomplete.data.quest.QuestType
import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.util.contains
import de.westnordost.streetcomplete.util.enlargedBy
import de.westnordost.streetcomplete.util.measuredLength
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.FutureTask
import javax.inject.Inject

/** Does one API call to get all the map data and generates quests from that. Calls getApplicableElements
 *  on all the quest types */
class OsmQuestUpdater @Inject constructor(
    private val osmElementSource: OsmElementSource,
    private val questTypeRegistry: QuestTypeRegistry,
    private val osmQuestController: OsmQuestController,
    private val countryBoundariesFuture: FutureTask<CountryBoundaries>,
    private val blacklistedPositionsSource: BlacklistedPositionsSource,
    private val blacklistedElementsSource: BlacklistedElementsSource
) : CoroutineScope by CoroutineScope(Dispatchers.Default), OsmElementSource.ElementUpdatesListener {

    private val questTypes get() = questTypeRegistry.all.filterIsInstance<OsmElementQuestType<*>>()

    init {
        osmElementSource.addQuestStatusListener(this)
    }

    override fun onDeleted(type: Element.Type, id: Long) {
        osmQuestController.deleteAllForElement(type, id)
    }

    override fun onUpdated(element: Element, geometry: ElementGeometry) {
        val paddedBounds = geometry.getBounds().enlargedBy(ApplicationConstants.QUEST_FILTER_PADDING)
        val lazyMapData by lazy { osmElementSource.getMapDataWithGeometry(paddedBounds) }

        val quests = mutableListOf<OsmQuest>()
        val truncatedBlacklistedPositions = blacklistedPositionsSource.getAllPositions(paddedBounds).map { it.truncateTo5Decimals() }.toSet()
        val blacklistedElements = blacklistedElementsSource.getAll().toSet()

        for (questType in questTypes) {
            val appliesToElement = questType.isApplicableTo(element)
                ?: questType.getApplicableElements(lazyMapData).contains(element)

            if (!appliesToElement) continue
            if (!mayCreateQuest(questType, element, geometry, blacklistedElements, truncatedBlacklistedPositions, null)) continue

            quests.add(OsmQuest(questType, element.type, element.id, geometry))
        }

        osmQuestController.updateForElement(element.type, element.id, quests)
    }

    override fun onUpdateForBBox(bbox: BoundingBox, mapDataWithGeometry: MapDataWithGeometry) {
        var time = System.currentTimeMillis()

        val quests = ConcurrentLinkedQueue<OsmQuest>()
        val truncatedBlacklistedPositions = blacklistedPositionsSource.getAllPositions(bbox).map { it.truncateTo5Decimals() }.toSet()
        val blacklistedElements = blacklistedElementsSource.getAll().toSet()
        val countryBoundaries = countryBoundariesFuture.get()

        runBlocking {
            for (questType in questTypes) {
                launch(Dispatchers.Default) {
                    val questTypeName = questType.getName()
                    if (!countryBoundaries.intersects(bbox, questType.enabledInCountries)) {
                        Log.d(TAG, "$questTypeName: Skipped because it is disabled for this country")
                    } else {
                        val questTime = System.currentTimeMillis()
                        for (element in questType.getApplicableElements(mapDataWithGeometry)) {
                            val geometry = mapDataWithGeometry.getGeometry(element.type, element.id)
                            if (!mayCreateQuest(questType, element, geometry, blacklistedElements, truncatedBlacklistedPositions, bbox)) continue
                            quests.add(OsmQuest(questType, element.type, element.id, geometry!!))
                        }

                        val questSeconds = System.currentTimeMillis() - questTime
                        Log.d(TAG, "$questTypeName: Found ${quests.size} quests in ${questSeconds}ms")
                    }
                }
            }
        }
        val secondsSpentAnalyzing = (System.currentTimeMillis() - time) / 1000
        Log.i(TAG,"Created ${quests.size} quests in ${secondsSpentAnalyzing}s")
        time = System.currentTimeMillis()

        osmQuestController.updateForBBox(quests, bbox)

        for (questType in questTypes) {
            questType.cleanMetadata()
        }

        val secondsSpentPersisting = (System.currentTimeMillis() - time) / 1000
        Log.i(TAG,"Persisted ${quests.size} quests in ${secondsSpentPersisting}s")

    }

    private fun mayCreateQuest(
        questType: OsmElementQuestType<*>,
        element: Element,
        geometry: ElementGeometry?,
        blacklistedElements: Set<ElementKey>,
        blacklistedPositions: Set<LatLon>,
        downloadedBoundingBox: BoundingBox?
    ): Boolean {
        // invalid geometry -> can't show this quest, so skip it
        val pos = geometry?.center ?: return false

        // outside downloaded area: skip
        if (downloadedBoundingBox != null && !downloadedBoundingBox.contains(pos)) return false

        // do not create quests whose marker is at/near a blacklisted position
        if (blacklistedPositions.contains(pos.truncateTo5Decimals()))  return false

        // do not create quests in countries where the quest is not activated
        val countries = questType.enabledInCountries
        if (!countryBoundariesFuture.get().isInAny(pos, countries))  return false

        // do not create quests of blacklisted elements
        val elementKey = ElementKey(element.type, element.id)
        if (blacklistedElements.contains(elementKey)) return false

        // do not create quests that refer to geometry that is too long for a surveyor to be expected to survey
        if (geometry is ElementPolylinesGeometry) {
            val totalLength = geometry.polylines.sumByDouble { it.measuredLength() }
            if (totalLength > MAX_GEOMETRY_LENGTH_IN_METERS) {
                return false
            }
        }
        return true
    }

    companion object {
        private const val TAG = "QuestUpdater"
    }
}

private fun QuestType<*>.getName() = javaClass.simpleName

// the resulting precision is about ~1 meter (see #1089)
private fun LatLon.truncateTo5Decimals() = OsmLatLon(latitude.truncateTo5Decimals(), longitude.truncateTo5Decimals())

private fun Double.truncateTo5Decimals() = (this * 1e5).toInt().toDouble() / 1e5

const val MAX_GEOMETRY_LENGTH_IN_METERS = 600