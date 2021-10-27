package de.westnordost.streetcomplete.quests.defibrillator

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.mapdata.filter
import de.westnordost.streetcomplete.quests.AbstractQuestAnswerFragment
import de.westnordost.streetcomplete.quests.AnswerItem

class AddIsDefibrillatorForm : AbstractQuestAnswerFragment<Boolean>() {

    override val buttonPanelAnswers = listOf(
        AnswerItem(R.string.quest_generic_hasFeature_no) { applyAnswer(false) },
        AnswerItem(R.string.quest_generic_hasFeature_yes) { applyAnswer(true) }
    )

    override suspend fun addInitialMapMarkers() {
        getMapData().filter("nodes with emergency = defibrillator").forEach {
            putMarker(it, R.drawable.ic_pin_defibrillator)
        }
    }
}
