package wisp.questgiver.wispLib

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import wisp.questgiver.Questgiver.game
import java.util.*

fun String.findBestMatch(toSearch: Collection<String>) = StringAutocorrect.findBestStringMatch(this, toSearch)

/**
 * Provides static functions to correct string spellings in various contexts.
 */
object StringAutocorrect {
    /**
     * From Console Command's `CommandUtils`.
     */
    fun findBestStringMatch(id: String, toSearch: Collection<String>): String? {
        var idInner = id
        if (toSearch.contains(idInner)) {
            return idInner
        }
        idInner = idInner.lowercase(Locale.getDefault())
        var bestMatch: String? = null
        var typoCorrectionThreshold = 0.9
        for (str in toSearch) {
            val distance = calcSimilarity(idInner, str.lowercase(Locale.getDefault()))
            if (distance == 1.0) {
                return str
            }
            if (distance > typoCorrectionThreshold) {
                typoCorrectionThreshold = distance
                bestMatch = str
            }
        }
        return bestMatch
    }

    /**
     * From Console Command's `CommandUtils`.
     */
    fun findBestFactionMatch(name: String): FactionAPI? {
        val nameInner = name.lowercase(Locale.getDefault())
        var bestMatch: FactionAPI? = null
        var typoCorrectionThreshold = 0.9

        // Wisp addition: Use fast hashmap lookup first to see if it's a direct match
        Global.getSector().getFaction(name)
            ?.run { return this }

        // Check IDs first in case multiple factions share the same name
        for (faction in Global.getSector().allFactions) {
            val distance = calcSimilarity(nameInner, faction.id.lowercase(Locale.getDefault()))
            if (distance == 1.0) {
                return faction
            }
            if (distance > typoCorrectionThreshold) {
                typoCorrectionThreshold = distance
                bestMatch = faction
            }
        }

        // Search again by name if no matching ID is found
        if (bestMatch == null) {
            for (faction in Global.getSector().allFactions) {
                val distance = calcSimilarity(nameInner, faction.displayName.lowercase(Locale.getDefault()))
                if (distance == 1.0) {
                    return faction
                }
                if (distance > typoCorrectionThreshold) {
                    typoCorrectionThreshold = distance
                    bestMatch = faction
                }
            }
        }

        if (bestMatch == null) {
            game.logger.w { "No faction found for text '$name'." }
        }

        return bestMatch
    }

    fun fixFactionTypos(factionList: List<String>, allowErrors: Boolean): List<String> {
        val newList: MutableList<String> = factionList.toMutableList()

        for (j in newList.indices) {
            val factionId = newList[j]
            if (Global.getSector().getFaction(factionId) == null) {
                val fixedFaction = findBestFactionMatch(factionId)
                if (fixedFaction == null) {
                    if (!allowErrors) {
                        throw RuntimeException(
                            String.format(
                                "Unable to find a matching faction for id '%s'.",
                                factionId
                            )
                        )
                    }
                } else {
                    Global.getLogger(StringAutocorrect::class.java)
                        .info(String.format("Correcting %s to %s.", factionId, fixedFaction.id))
                    newList[j] = fixedFaction.id
                }
            }
        }
        return newList
    }

    /**
     * From Console Command's `CommandUtils`.
     * Returns normalized score, with 0.0 meaning no similarity at all,
     * and 1.0 meaning full equality.
     */
    // Taken from: https://github.com/larsga/Duke/blob/master/duke-core/src/main/java/no/priv/garshol/duke/comparators/JaroWinkler.java
    fun calcSimilarity(s1: String, s2: String): Double {
        var s1Inner = s1
        var s2Inner = s2

        if (s1Inner == s2Inner) {
            return 1.0
        }

        // ensure that s1 is shorter than or same length as s2
        if (s1Inner.length > s2Inner.length) {
            val tmp = s2Inner
            s2Inner = s1Inner
            s1Inner = tmp
        }

        // (1) find the number of characters the two strings have in common.
        // note that matching characters can only be half the length of the
        // longer string apart.
        val maxdist = s2Inner.length / 2
        var c = 0 // count of common characters
        var t = 0 // count of transpositions
        var prevpos = -1
        for (ix in s1Inner.indices) {
            val ch = s1Inner[ix]

            // now try to find it in s2
            for (ix2 in Math.max(0, ix - maxdist) until Math.min(s2Inner.length, ix + maxdist)) {
                if (ch == s2Inner[ix2]) {
                    c++ // we found a common character
                    if (prevpos != -1 && ix2 < prevpos) {
                        t++ // moved back before earlier
                    }
                    prevpos = ix2
                    break
                }
            }
        }

        // we don't divide t by 2 because as far as we can tell, the above
        // code counts transpositions directly.
        // System.out.println("c: " + c);
        // System.out.println("t: " + t);
        // System.out.println("c/m: " + (c / (double) s1.length()));
        // System.out.println("c/n: " + (c / (double) s2.length()));
        // System.out.println("(c-t)/c: " + ((c - t) / (double) c));
        // we might have to give up right here
        if (c == 0) {
            return 0.0
        }

        // first compute the score
        var score = c / s1Inner.length.toDouble() + c / s2Inner.length.toDouble() + (c - t) / c.toDouble() / 3.0

        // (2) common prefix modification
        var p = 0 // length of prefix
        val last = Math.min(4, s1Inner.length)
        while (p < last && s1Inner[p] == s2Inner[p]) {
            p++
        }
        score += p * (1 - score) / 10

        // (3) longer string adjustment
        // I'm confused about this part. Winkler's original source code includes
        // it, and Yancey's 2005 paper describes it. However, Winkler's list of
        // test cases in his 2006 paper does not include this modification. So
        // is this part of Jaro-Winkler, or is it not? Hard to say.
        if (s1Inner.length >= 5  // both strings at least 5 characters long
            && c - p >= 2 // at least two common characters besides prefix
            && c - p >= ((s1Inner.length - p) / 2) // fairly rich in common chars
        ) {
            score += (1 - score) * ((c - (p + 1))
                    / ((s1Inner.length + s2Inner.length) - (2 * (p - 1))).toDouble())
        }

        // (4) similar characters adjustment
        // the same holds for this as for (3) above.
        return score
    }
}