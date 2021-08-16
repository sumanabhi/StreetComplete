package de.westnordost.streetcomplete.quests.shop_type

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.meta.*
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import java.time.LocalDate

class CheckShopType : OsmElementQuestType<ShopTypeAnswer> {

    private val disusedShops by lazy { """
        nodes, ways, relations with (
          shop = vacant
          or ${isKindOfShopExpression("disused")}
        ) and (
          older today -1 years
          or ${LAST_CHECK_DATE_KEYS.joinToString(" or ") { "$it < today -1 years" }}
        )
    """.toElementFilterExpression() }

    /* elements tagged like "shop=ice_cream + disused:amenity=bank" should not appear as quests.
     *  This is arguably a tagging mistake, but that mistake should not lead to all the tags of
     *  this element being cleared when the quest is answered */
    private val shops by lazy { """
        nodes, ways, relations with ${isKindOfShopExpression()}
    """.toElementFilterExpression() }

    override val commitMessage = "Check if vacant shop is still vacant"
    override val wikiLink = "Key:disused:"
    override val icon = R.drawable.ic_quest_check_shop

    override fun getTitle(tags: Map<String, String>) = R.string.quest_shop_vacant_type_title

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> =
        mapData.filter { isApplicableTo(it) }

    override fun isApplicableTo(element: Element): Boolean =
        disusedShops.matches(element) && !shops.matches(element)

    override fun createForm() = ShopTypeForm()

    override fun applyAnswerTo(answer: ShopTypeAnswer, changes: StringMapChangesBuilder) {
        val otherCheckDateKeys = LAST_CHECK_DATE_KEYS.filterNot { it == SURVEY_MARK_KEY }
        for (otherCheckDateKey in otherCheckDateKeys) {
            changes.deleteIfExists(otherCheckDateKey)
        }
        when (answer) {
            is IsShopVacant -> {
                changes.addOrModify(SURVEY_MARK_KEY, LocalDate.now().toCheckDateString())
            }
            is ShopType -> {
                if (!answer.tags.containsKey("shop")) {
                    changes.deleteIfExists("shop")
                }

                changes.deleteIfExists(SURVEY_MARK_KEY)

                for ((key, _) in changes.getPreviousEntries()) {
                    // also deletes all "disused:" keys
                    val isOkToRemove =
                        KEYS_THAT_SHOULD_NOT_BE_REMOVED_WHEN_SHOP_IS_REPLACED.none { it.matches(key) }
                    if (isOkToRemove && !answer.tags.containsKey(key)) {
                        changes.delete(key)
                    }
                }

                for ((key, value) in answer.tags) {
                    changes.addOrModify(key, value)
                }
            }
        }
    }

}
