package neon.nsp.data.scripts.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.magiclib.util.MagicUI
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*

class nsp_mk1CombatPlugin : BaseEveryFrameCombatPlugin() {

    val fullString = "T H R E A T  D E T E C T E D"

    val soundID = "nsp_threat_detected"
    val soundPitch = 1f
    val soundVolume = 0.7f
    val textColor = Color.RED
    val minTimer = 1f
    val maxTimer = 5f
    val maxIntensity = 1.5f
    var isThreat = false
    var init = false
    val timer = IntervalUtil(minTimer, maxTimer)
    var jitterTimer = 0f
    var midJitter = 0f
    var intensity = 1f
    val fadeJitterOutAt = 3f

    val CHAR_DELAY = 0.35f

    data class TextCrawlData(
        var currDex: Int = 0,
        var currString: String = "",
        var isComplete: Boolean = false,
        var charTimer: Float = 0f,
        var lingerTimer: Float = 0f,
        var drawText: Boolean = false,
        var hasStarted: Boolean = false
    )

    val shipTextCrawls = mutableMapOf<ShipAPI, TextCrawlData>()

    companion object {
        val HullIDList = listOf(
            "nsp_dominatormk1", "nsp_legionmk1", "onslaught_mk1"
        )
    }

    val foundShipsList = mutableListOf<ShipAPI>()

    override fun advance(amount: Float, events: List<InputEventAPI>?) {
        if (Global.getCombatEngine().isPaused) return

        if (!init) {
            init = true
            val playerFleet = Global.getSector().playerFleet
            if (playerFleet != null && playerFleet.battle != null) {
                val enemyFleet = getFleetFromBattle(playerFleet.battle)
                if (enemyFleet != null && enemyFleet.faction != null && enemyFleet.faction.id == Factions.THREAT) {
                    isThreat = true
                    for (ship in Global.getCombatEngine().ships) {
                        if (HullIDList.contains(ship.hullSpec.baseHullId)) {
                            foundShipsList.add(ship)
                            foundShipsList.addAll(ship.childModulesCopy)
                        }
                    }
                    jitterTimer = timer.intervalDuration * 1.8f
                    midJitter = jitterTimer / 2f
                }
            }
        }

        if (init && !isThreat) {
            Global.getCombatEngine().removePlugin(this)
            return
        }

        timer.advance(amount)

        jitterTimer -= amount

        val toadd = if (jitterTimer > fadeJitterOutAt) {
            amount / midJitter
        } else {
            -amount / fadeJitterOutAt
        }
        intensity = (intensity + toadd).coerceIn(0.01f, maxIntensity)

        val viewport = Global.getCombatEngine().viewport

        for (ship in foundShipsList) {

            if (jitterTimer > 0f) {
                ship.setJitterUnder(ship, textColor, intensity, 5, 8f)
            }

            var data = shipTextCrawls[ship]

            if (data == null && timer.intervalElapsed() && ship.parentStation == null && !ship.isHulk) {
                Global.getSoundPlayer().playSound(soundID, soundPitch, soundVolume, ship.location, ship.velocity)
                data = TextCrawlData(drawText = true, hasStarted = true)
                shipTextCrawls[ship] = data
            }

            if (data != null && data.drawText && data.hasStarted) {

                if (!data.isComplete) {
                    data.charTimer += amount

                    if (data.charTimer >= CHAR_DELAY) {
                        data.charTimer = 0f

                        val next = fullString.getOrNull(data.currDex)

                        if (next != null) {
                            data.currString += next
                            data.currDex++
                        }

                        if (data.currDex >= fullString.length) {
                            data.isComplete = true
                            data.lingerTimer = 0f
                        }
                    }
                } else {
                    data.lingerTimer += amount

                    if (data.lingerTimer >= 20f) {
                        data.drawText = false
                        shipTextCrawls.remove(ship)
                        continue
                    }
                }

                if (viewport != null && viewport.isNearViewport(ship.location, ship.collisionRadius)) {
                    // Draw main text above ship
                    val textWorldPos = Vector2f(ship.location.x, ship.location.y + ship.collisionRadius)
                    val screenX = viewport.convertWorldXtoScreenX(textWorldPos.x)
                    val screenY = viewport.convertWorldYtoScreenY(textWorldPos.y)
                    MagicUI.addText(null, data.currString, textColor, Vector2f(screenX, screenY), true)

                    // Draw glitch text around the ship
                    for (i in 0 until MathUtils.getRandomNumberInRange(4, 8)) {
                        val randomPos = MathUtils.getRandomPointInCircle(ship.location, ship.collisionRadius)
                        val screenX2 = viewport.convertWorldXtoScreenX(randomPos.x)
                        val screenY2 = viewport.convertWorldYtoScreenY(randomPos.y)
                        MagicUI.addText(null, "!", textColor, Vector2f(screenX2, screenY2), true)
                    }
                }
            }
        }

        if (jitterTimer <= 0f && shipTextCrawls.isEmpty()) {
            Global.getCombatEngine().removePlugin(this)
        }
    }

    fun getFleetFromBattle(battle: BattleAPI): CampaignFleetAPI? {
        var bestFallback: CampaignFleetAPI? = null
        for (fleet in battle.nonPlayerSide) {
            bestFallback = fleet
        }
        return bestFallback
    }
}
