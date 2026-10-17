package com.ohadsam.specialevents.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MilestonesTest {
    @Test
    fun roundBirthdays() {
        assertEquals(Tier.MAJOR, Milestones.forBirthday(50)?.tier)
        assertEquals(SpecialAge.JUBILEE_50, Milestones.forBirthday(50)?.specialAge)
        assertEquals(Tier.MAJOR, Milestones.forBirthday(30)?.tier)
        assertEquals(Tier.MAJOR, Milestones.forBirthday(70)?.tier)
        assertEquals(SpecialAge.LONG_LIFE_120, Milestones.forBirthday(120)?.specialAge)
        assertEquals(SpecialAge.ADULT_18, Milestones.forBirthday(18)?.specialAge)
        assertEquals(SpecialAge.AGE_21, Milestones.forBirthday(21)?.specialAge)
        assertEquals(SpecialAge.FIRST, Milestones.forBirthday(1)?.specialAge)
        assertEquals(Tier.NOTABLE, Milestones.forBirthday(10)?.tier)
        assertNull(Milestones.forBirthday(37))
        assertNull(Milestones.forBirthday(0))
    }

    @Test
    fun namedAnniversaries() {
        assertEquals(AnniversaryGift.SILVER, Milestones.forAnniversary(25)?.gift)
        assertEquals(Tier.MAJOR, Milestones.forAnniversary(25)?.tier)
        assertEquals(AnniversaryGift.GOLD, Milestones.forAnniversary(50)?.gift)
        assertEquals(Tier.MAJOR, Milestones.forAnniversary(50)?.tier)
        assertEquals(AnniversaryGift.DIAMOND, Milestones.forAnniversary(60)?.gift)
        assertEquals(AnniversaryGift.PAPER, Milestones.forAnniversary(1)?.gift)
        assertEquals(Tier.NOTABLE, Milestones.forAnniversary(5)?.tier)
        assertEquals(Tier.NOTABLE, Milestones.forAnniversary(80)?.tier)
        assertNull(Milestones.forAnniversary(80)?.gift)
        assertNull(Milestones.forAnniversary(17))
    }

    @Test
    fun yahrzeitYears() {
        assertEquals(Tier.MAJOR, Milestones.forYahrzeit(1)?.tier)
        assertEquals(Tier.NOTABLE, Milestones.forYahrzeit(10)?.tier)
        assertNull(Milestones.forYahrzeit(3))
    }
}
