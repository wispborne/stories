## Changelog

Version 3.1.0
- Added
    - Next quest in the Telos questline.
        - If you have finished the first Telos quest, this one will automatically start.
    - Console command: `infinite`. Runs both `infinitesupplies` and `infinitefuel`.
    - Console command: `allall`. Places all hulls, weapons, wings, and commodities into storage. Learns all blueprints and hullmods.
- Fixed
  - "Error" in the game log that wasn't actually an error.
  - Missing text in quest intel after they'd been completed (thanks to toopok4k3).
  - Various small text improvements in too many places to list.
  - Dragons: Reward is now shown upfront and the "bonus" that you always get at the end is now a surprise.
  - Depths: Karengo lying about how many credits he gives you for Depths.
  - Depths: Karengo avoiding non-existent local government during Depths.
  - Depths: Item reward now generates cargo pods if you don't immediately take everything.
  - Depths: Credit reward now scales based on distance.
  - Telos: Fixed the combat map size persisting after the flashback (hopefully).
  - Telos: "hanger" -> "hangar" in lots of places (thanks Histidine).
  - Telos: Prefers unpopulated planets for the first quest location.
  - Telos: Able to talk the pirate down multiple times so they start with 1% CR.
  - Telos: Credit reward now scales based on distance.
  - Telos: Fixed Varas sharing phase dash cooldown state.
  - Telos: Karengo can no longer walk without a vacsuit on barren planets. As far as you know.
- Changed
  - Now requires Java 17 or higher.
    - Starsector 0.98a will use it by default.
  - Firebrand sprite coloration improvements.

Version 3.0.8
- Fixed
  - Crash with Progressive Smods enabled after the Telos battle (thanks to Jimminy Crimbles). 
    - Found a different way to do what I was doing that won't bother other mods anymore.
  - Crash when starting Telos if you've started Nirvana before (introduced in 3.0.7).
- Added
  - Console command `allall` (sibling to `allweapons`, `allhulls`, `allwings`, etc).
    - Places all hulls, weapons, wings, and commodities into storage. Learns all blueprints and hullmods.

Version 3.0.7
- Fixed
  - Telos quest _required_ Nex's random mode instead of the other way around (thanks to Seamus Donohue).
- Added
  - Two new Telos weapons, a small and a medium beam.
- Changed
  - Corrected Vara's slots to 2x small turrets and 1x medium hardpoint, from 2x medium turrets.

Version 3.0.6
- Fixed
  - (Another) crash after the Telos battle (thanks to bifur).

Version 3.0.5
- Fixed
    - Riley no longer pays you just for starting a new game (thanks to ganegrei).
    - Possible crash after the Telos battle ends.
    - Another crash after the Telos battle ends (if you did not take the `[REDACTED]`).
    - Crash on Linux due to case-sensitive file system (thanks to TameFroggy).
- Added
    - New boss dreadnaught (unobtainable) in Telos quest, the Firebrand.

Version 3.0.4
- Fixed
    - Crash after the Telos battle if Nexerelin is enabled (thanks to papasan).
- Added
    - For other modders: hooks for when Karengo/Riley/the laborer are in the fleet.
        - `$wisp_perseanchronicles_isKarengoInFleet`
        - `$wisp_perseanchronicles_isLaborerInFleet`
        - `$wisp_perseanchronicles_isRileyInFleet`

Version 3.0.3
- Fixed
    - Depths was offered multiple times (thanks to Thaumaturge).
    - Riley never paid you! (thanks to sadday)
        - If you already completed the quest, you'll get paid shortly after loading your save.
- Added
    - Cameo from Sierra during Riley quest (has no gameplay effects, only dialog).

Version 3.0.2
- Fixed
    - Crash after Riley dialog that shows after 2 days (thanks to AERO).

Version 3.0.1
- Fixed
    - Fixed missing Karengo portrait in Dragons bar event.
    - Typo in Riley quest (thanks to ruddygreat).
    - Possible softlock in bar event (if you decided not to accept a quest, then relanded).
    - Was not showing player name if player didn't have a last name.
- Changed
    - Vara (frigate) system now has a linear speed falloff, rather than two stages of speed. 

Version 3.0.0 (2023-06-11)
- Added
    - Phase 1 of 3 of the new Telos (rhymes with Vell-os...) questline, which starts a month after finishing the first two Karengo quests.
        - Adds a unique new ship.
        - New music (turn up the sound!)
        - More Karengo.

- Changed
    - Improvements in the five existing quests.
        - New art (mostly AI-gen).
        - Many various dialog improvements and fixes (some thanks to the Endless Sky adaptation).
        - A hidden ending is now slightly less hidden.
        - Old bugs fixed, new bugs added.

Version 2.0.2 (2021-04-04)
- Fixed
    - Broken dialog if Nexerelin has the Freeport in hyperspace.
    - Added quests to the Accepted intel tag.

Version 2.0.1 (2021-03-28)
- Fixed
    - Crash in the bar when the Nirvana quest was offered (`sys.star must not be null`).

Version 2.0.0 (2021-03-27)
- Changed
    - 0.95a compatibility.
    - No other changes from 1.0.4

Version 1.0.4 (2021-03-17)
- Fixed
    - Don't kick player out of Depths if the music fails to play.

Version 1.0.3 (2021-03-08)
- Added
    - Config file to disable quests from being offered (located at ./perseanChroniclesSettings.json).
- Changed
    - Clarified rewards for Depths and minor wording cleanup.
    - Nirvana is now only offered at level 10 and above (due to the danger involved and time for player to grow some reputation).
- Fixed
    - Crash on some computers when playing the music for Depths.

Version 1.0.2
    - Yanked due to issues and rolled into 1.0.3

Version 1.0.1 (2020-02-26)
- Added 
    - More robust blacklisting and whitelisting. (see https://starsector.fandom.com/wiki/Category:Modding#Persean_Chronicles)
- Changed
    - Slightly different text for Dragons if ending planet is hostile.
- Fixed
    - Quests should no longer be offered from blacklisted locations. 

Version 1.0.0 (2020-02-20)
- Initial release