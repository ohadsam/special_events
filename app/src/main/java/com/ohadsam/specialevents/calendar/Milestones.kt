package com.ohadsam.specialevents.calendar

enum class Tier { NONE, NOTABLE, MAJOR }

/** Named gifts of wedding anniversaries (the traditional list the owner asked for: 25 silver, 50 gold, ...). */
enum class AnniversaryGift(val years: Int) {
    PAPER(1), COTTON(2), LEATHER(3), FLOWERS(4), WOOD(5), SUGAR(6), WOOL(7), BRONZE(8), POTTERY(9), TIN(10), STEEL(11), SILK(12),
    LACE(13), IVORY(14), CRYSTAL(15), CHINA(20), SILVER(25), PEARL(30), CORAL(35), RUBY(40), SAPPHIRE(45), GOLD(50), EMERALD(55),
    DIAMOND(60), PLATINUM(70),
}

enum class SpecialAge { FIRST, ADULT_18, AGE_21, DECADE, JUBILEE_50, LONG_LIFE_120 }

/** A noteworthy recurrence: [number] years, how much to make of it ([tier]), and the name key for the UI. */
data class Milestone(val number: Int, val tier: Tier, val gift: AnniversaryGift? = null, val specialAge: SpecialAge? = null)

object Milestones {
    private const val LONG_LIFE = 120
    private const val JUBILEE = 50
    private const val FIRST_BIRTHDAY = 1
    private const val ADULT = 18
    private const val TWENTY_ONE = 21
    private const val DECADE = 10
    private const val FIRST_YAHRZEIT = 1
    private val MAJOR_ANNIVERSARIES = setOf(10, 20, 25, 30, 40, 50, 60, 70)

    /** Round and special birthdays: 1, 18, 21, every decade from 10 (30+ are major), 50 = jubilee, 120. */
    fun forBirthday(age: Int): Milestone? = when {
        age == LONG_LIFE -> Milestone(age, Tier.MAJOR, specialAge = SpecialAge.LONG_LIFE_120)
        age == JUBILEE -> Milestone(age, Tier.MAJOR, specialAge = SpecialAge.JUBILEE_50)
        age >= 30 && age % DECADE == 0 -> Milestone(age, Tier.MAJOR, specialAge = SpecialAge.DECADE)
        age == DECADE || age == 20 -> Milestone(age, Tier.NOTABLE, specialAge = SpecialAge.DECADE)
        age == ADULT -> Milestone(age, Tier.NOTABLE, specialAge = SpecialAge.ADULT_18)
        age == TWENTY_ONE -> Milestone(age, Tier.NOTABLE, specialAge = SpecialAge.AGE_21)
        age == FIRST_BIRTHDAY -> Milestone(age, Tier.NOTABLE, specialAge = SpecialAge.FIRST)
        else -> null
    }

    /** Wedding anniversaries: every traditionally named year is notable; 10, 20, 25, 30, 40, 50, 60, 70 are major. */
    fun forAnniversary(years: Int): Milestone? {
        val gift = AnniversaryGift.entries.firstOrNull { it.years == years } ?: return if (years > 0 && years % DECADE == 0) Milestone(years, Tier.NOTABLE) else null
        return Milestone(years, if (years in MAJOR_ANNIVERSARIES) Tier.MAJOR else Tier.NOTABLE, gift = gift)
    }

    /** Yahrzeit: the first year (שנה) is major, every tenth year notable. */
    fun forYahrzeit(years: Int): Milestone? = when {
        years == FIRST_YAHRZEIT -> Milestone(years, Tier.MAJOR)
        years > 0 && years % DECADE == 0 -> Milestone(years, Tier.NOTABLE)
        else -> null
    }
}
