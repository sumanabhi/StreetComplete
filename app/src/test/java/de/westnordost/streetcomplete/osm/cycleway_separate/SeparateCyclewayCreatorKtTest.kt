package de.westnordost.streetcomplete.osm.cycleway_separate

import de.westnordost.streetcomplete.osm.cycleway_separate.SeparateCycleway.*
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryAdd
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryChange
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryDelete
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapEntryModify
import de.westnordost.streetcomplete.osm.nowAsCheckDateString
import org.assertj.core.api.Assertions
import org.junit.Test

class SeparateCyclewayCreatorKtTest {
    @Test fun `apply none`() {
        verifyAnswer(
            mapOf("highway" to "footway"),
            NONE,
            arrayOf(StringMapEntryAdd("bicycle", "no"))
        )
    }

    @Test fun `apply none re-tags cycleway and adds foot=yes if foot was not yes before`() {
        verifyAnswer(
            mapOf("highway" to "cycleway", "foot" to "no"),
            NONE,
            arrayOf(
                StringMapEntryAdd("bicycle", "no"),
                StringMapEntryModify("highway", "cycleway", "path"),
                StringMapEntryModify("foot", "no", "yes")
            )
        )
    }

    @Test fun `apply none re-tags cycleway to footway if foot is designated`() {
        verifyAnswer(
            mapOf("highway" to "cycleway", "foot" to "designated"),
            NONE,
            arrayOf(
                StringMapEntryAdd("bicycle", "no"),
                StringMapEntryModify("highway", "cycleway", "footway")
            )
        )
    }

    @Test fun `apply none removes sidewalk and segregated tags`() {
        verifyAnswer(
            mapOf(
                "highway" to "footway",
                "segregated" to "yes",
                "sidewalk" to "both",
                "sidewalk:left" to "yes",
                "sidewalk:right" to "yes",
                "sidewalk:both" to "yes",
            ),
            NONE,
            arrayOf(
                StringMapEntryAdd("bicycle", "no"),
                StringMapEntryDelete("segregated", "yes"),
                StringMapEntryDelete("sidewalk", "both"),
                StringMapEntryDelete("sidewalk:left", "yes"),
                StringMapEntryDelete("sidewalk:right", "yes"),
                StringMapEntryDelete("sidewalk:both", "yes"),
            )
        )
    }

    @Test fun `apply allowed`() {
        verifyAnswer(
            mapOf("highway" to "footway"),
            ALLOWED,
            arrayOf(StringMapEntryAdd("bicycle", "yes"))
        )
    }

    @Test fun `apply allowed re-tags cycleway and adds foot=yes if foot was not yes before`() {
        verifyAnswer(
            mapOf("highway" to "cycleway", "foot" to "no"),
            ALLOWED,
            arrayOf(
                StringMapEntryAdd("bicycle", "yes"),
                StringMapEntryModify("highway", "cycleway", "path"),
                StringMapEntryModify("foot", "no", "yes")
            )
        )
    }

    @Test fun `apply allowed re-tags cycleway to footway if foot is designated`() {
        verifyAnswer(
            mapOf("highway" to "cycleway", "foot" to "designated"),
            ALLOWED,
            arrayOf(
                StringMapEntryAdd("bicycle", "yes"),
                StringMapEntryModify("highway", "cycleway", "footway")
            )
        )
    }

    @Test fun `apply allowed removes sidewalk and segregated tags`() {
        verifyAnswer(
            mapOf(
                "highway" to "footway",
                "segregated" to "yes",
                "sidewalk" to "both",
                "sidewalk:left" to "yes",
                "sidewalk:right" to "yes",
                "sidewalk:both" to "yes",
            ),
            ALLOWED,
            arrayOf(
                StringMapEntryAdd("bicycle", "yes"),
                StringMapEntryDelete("segregated", "yes"),
                StringMapEntryDelete("sidewalk", "both"),
                StringMapEntryDelete("sidewalk:left", "yes"),
                StringMapEntryDelete("sidewalk:right", "yes"),
                StringMapEntryDelete("sidewalk:both", "yes"),
            )
        )
    }

    @Test fun `apply non-designated`() {
        verifyAnswer(
            mapOf("highway" to "footway"),
            NON_DESIGNATED,
            arrayOf(StringMapEntryAdd("check_date:bicycle", nowAsCheckDateString()))
        )
    }

    @Test fun `apply non-designated does not change bicycle tag unless it is designated`() {
        verifyAnswer(
            mapOf("highway" to "footway", "bicycle" to "designated"),
            NON_DESIGNATED,
            arrayOf(StringMapEntryDelete("bicycle", "designated"))
        )
        verifyAnswer(
            mapOf("highway" to "footway", "bicycle" to "yes"),
            NON_DESIGNATED,
            arrayOf(StringMapEntryAdd("check_date:bicycle", nowAsCheckDateString()))
        )
        verifyAnswer(
            mapOf("highway" to "footway", "bicycle" to "no"),
            NON_DESIGNATED,
            arrayOf(StringMapEntryAdd("check_date:bicycle", nowAsCheckDateString()))
        )
    }

    @Test fun `apply non-designated re-tags cycleway and adds foot=yes if foot was not yes before`() {
        verifyAnswer(
            mapOf("highway" to "cycleway", "foot" to "no"),
            NON_DESIGNATED,
            arrayOf(
                StringMapEntryModify("highway", "cycleway", "path"),
                StringMapEntryModify("foot", "no", "yes")
            )
        )
    }

    @Test fun `apply non-designated re-tags cycleway to footway if foot is designated`() {
        verifyAnswer(
            mapOf("highway" to "cycleway", "foot" to "designated"),
            NON_DESIGNATED,
            arrayOf(
                StringMapEntryModify("highway", "cycleway", "footway")
            )
        )
    }

    @Test fun `apply non-designated removes sidewalk and segregated tags`() {
        verifyAnswer(
            mapOf(
                "highway" to "footway",
                "segregated" to "yes",
                "sidewalk" to "both",
                "sidewalk:left" to "yes",
                "sidewalk:right" to "yes",
                "sidewalk:both" to "yes",
            ),
            NON_DESIGNATED,
            arrayOf(
                StringMapEntryDelete("segregated", "yes"),
                StringMapEntryDelete("sidewalk", "both"),
                StringMapEntryDelete("sidewalk:left", "yes"),
                StringMapEntryDelete("sidewalk:right", "yes"),
                StringMapEntryDelete("sidewalk:both", "yes"),
            )
        )
    }

    @Test fun `apply non-segregated`() {
        verifyAnswer(
            mapOf("highway" to "footway"),
            NON_SEGREGATED,
            arrayOf(
                StringMapEntryAdd("bicycle", "designated"),
                StringMapEntryAdd("segregated", "no"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "footway", "foot" to "yes"),
            NON_SEGREGATED,
            arrayOf(
                StringMapEntryAdd("bicycle", "designated"),
                StringMapEntryModify("foot", "yes", "designated"),
                StringMapEntryAdd("segregated", "no"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "cycleway"),
            NON_SEGREGATED,
            arrayOf(
                StringMapEntryAdd("foot", "designated"),
                StringMapEntryAdd("segregated", "no"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "cycleway", "bicycle" to "yes"),
            NON_SEGREGATED,
            arrayOf(
                StringMapEntryModify("bicycle", "yes", "designated"),
                StringMapEntryAdd("foot", "designated"),
                StringMapEntryAdd("segregated", "no"),
            )
        )
    }

    @Test fun `apply non-segregated removes sidewalk tags`() {
        verifyAnswer(
            mapOf(
                "highway" to "footway",
                "sidewalk" to "both",
                "sidewalk:left" to "yes",
                "sidewalk:right" to "yes",
                "sidewalk:both" to "yes",
            ),
            NON_SEGREGATED,
            arrayOf(
                StringMapEntryAdd("bicycle", "designated"),
                StringMapEntryAdd("segregated", "no"),
                StringMapEntryDelete("sidewalk", "both"),
                StringMapEntryDelete("sidewalk:left", "yes"),
                StringMapEntryDelete("sidewalk:right", "yes"),
                StringMapEntryDelete("sidewalk:both", "yes"),
            )
        )
    }

    @Test fun `apply segregated`() {
        verifyAnswer(
            mapOf("highway" to "footway"),
            SEGREGATED,
            arrayOf(
                StringMapEntryAdd("bicycle", "designated"),
                StringMapEntryAdd("segregated", "yes"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "footway", "foot" to "yes"),
            SEGREGATED,
            arrayOf(
                StringMapEntryAdd("bicycle", "designated"),
                StringMapEntryModify("foot", "yes", "designated"),
                StringMapEntryAdd("segregated", "yes"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "cycleway"),
            SEGREGATED,
            arrayOf(
                StringMapEntryAdd("foot", "designated"),
                StringMapEntryAdd("segregated", "yes"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "cycleway", "bicycle" to "yes"),
            SEGREGATED,
            arrayOf(
                StringMapEntryModify("bicycle", "yes", "designated"),
                StringMapEntryAdd("foot", "designated"),
                StringMapEntryAdd("segregated", "yes"),
            )
        )
    }

    @Test fun `apply segregated removes sidewalk tags`() {
        verifyAnswer(
            mapOf(
                "highway" to "footway",
                "sidewalk" to "both",
                "sidewalk:left" to "yes",
                "sidewalk:right" to "yes",
                "sidewalk:both" to "yes",
            ),
            SEGREGATED,
            arrayOf(
                StringMapEntryAdd("bicycle", "designated"),
                StringMapEntryAdd("segregated", "yes"),
                StringMapEntryDelete("sidewalk", "both"),
                StringMapEntryDelete("sidewalk:left", "yes"),
                StringMapEntryDelete("sidewalk:right", "yes"),
                StringMapEntryDelete("sidewalk:both", "yes"),
            )
        )
    }

    @Test fun `apply exclusive`() {
        verifyAnswer(
            mapOf("highway" to "cycleway"),
            EXCLUSIVE,
            arrayOf(
                StringMapEntryAdd("foot", "no"),
                StringMapEntryModify("highway", "cycleway", "cycleway"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "footway"),
            EXCLUSIVE,
            arrayOf(
                StringMapEntryAdd("foot", "no"),
                StringMapEntryModify("highway", "footway", "cycleway"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "path", "foot" to "yes"),
            EXCLUSIVE,
            arrayOf(
                StringMapEntryModify("foot", "yes", "no"),
                StringMapEntryModify("highway", "path", "cycleway"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "cycleway", "bicycle" to "yes"),
            EXCLUSIVE,
            arrayOf(
                StringMapEntryAdd("foot", "no"),
                StringMapEntryModify("bicycle", "yes", "designated"),
                StringMapEntryModify("highway", "cycleway", "cycleway"),
            )
        )
    }

    @Test fun `apply exclusive removes sidewalk and segregated tags`() {
        verifyAnswer(
            mapOf(
                "highway" to "cycleway",
                "sidewalk" to "both",
                "segregated" to "yes",
                "sidewalk:left" to "yes",
                "sidewalk:right" to "yes",
                "sidewalk:both" to "yes",
            ),
            EXCLUSIVE,
            arrayOf(
                StringMapEntryAdd("foot", "no"),
                StringMapEntryModify("highway", "cycleway", "cycleway"),
                StringMapEntryDelete("sidewalk", "both"),
                StringMapEntryDelete("segregated", "yes"),
                StringMapEntryDelete("sidewalk:left", "yes"),
                StringMapEntryDelete("sidewalk:right", "yes"),
                StringMapEntryDelete("sidewalk:both", "yes"),
            )
        )
    }

    @Test fun `apply exclusive with sidewalk`() {
        verifyAnswer(
            mapOf("highway" to "cycleway"),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryAdd("sidewalk", "yes"),
                StringMapEntryModify("highway", "cycleway", "cycleway"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "cycleway", "foot" to "yes"),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryAdd("sidewalk", "yes"),
                StringMapEntryDelete("foot", "yes"),
                StringMapEntryModify("highway", "cycleway", "cycleway"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "footway"),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryAdd("sidewalk", "yes"),
                StringMapEntryModify("highway", "footway", "cycleway"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "path"),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryAdd("sidewalk", "yes"),
                StringMapEntryModify("highway", "path", "cycleway"),
            )
        )
        verifyAnswer(
            mapOf("highway" to "path", "bicycle" to "yes"),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryAdd("sidewalk", "yes"),
                StringMapEntryModify("bicycle", "yes", "designated"),
                StringMapEntryModify("highway", "path", "cycleway"),
            )
        )
    }

    @Test fun `apply exclusive with sidewalk does not touch sidewalk tags if a sidewalk is already tagged`() {
        verifyAnswer(
            mapOf("highway" to "cycleway", "sidewalk" to "both"),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryModify("highway", "cycleway", "cycleway"),
                StringMapEntryAdd("check_date:bicycle", nowAsCheckDateString())
            )
        )
        verifyAnswer(
            mapOf("highway" to "cycleway", "sidewalk:left" to "yes"),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryModify("highway", "cycleway", "cycleway"),
                StringMapEntryAdd("check_date:bicycle", nowAsCheckDateString())
            )
        )
        verifyAnswer(
            mapOf("highway" to "cycleway", "sidewalk:right" to "separate"),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryModify("highway", "cycleway", "cycleway"),
                StringMapEntryAdd("check_date:bicycle", nowAsCheckDateString())
            )
        )
        verifyAnswer(
            mapOf("highway" to "cycleway", "sidewalk:both" to "even invalid"),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryModify("highway", "cycleway", "cycleway"),
                StringMapEntryAdd("check_date:bicycle", nowAsCheckDateString())
            )
        )
    }

    @Test fun `apply exclusive with sidewalk removes segregated tags`() {
        verifyAnswer(
            mapOf(
                "highway" to "cycleway",
                "sidewalk" to "both",
                "segregated" to "yes"
            ),
            EXCLUSIVE_WITH_SIDEWALK,
            arrayOf(
                StringMapEntryModify("highway", "cycleway", "cycleway"),
                StringMapEntryDelete("segregated", "yes"),
            )
        )
    }
}

private fun verifyAnswer(tags: Map<String, String>, answer: SeparateCycleway, expectedChanges: Array<StringMapEntryChange>) {
    val cb = StringMapChangesBuilder(tags)
    answer.applyTo(cb)
    val changes = cb.create().changes
    Assertions.assertThat(changes).containsExactlyInAnyOrder(*expectedChanges)
}
