package neon.nsp.data.scripts.util

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.state.AppDriver
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.combat.CombatState
import org.lazywizard.lazylib.ext.minus
import org.lazywizard.lazylib.ext.rotate
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.util.vector.Vector2f
import neon.nsp.data.scripts.util.ReflectionUtils.getFieldsMatching
import neon.nsp.data.scripts.util.ReflectionUtils.invoke
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class PaperdollUIPanelAdder: BaseEveryFrameCombatPlugin() {
    private val noHPColor = Color(200, 30, 30, 255)
    private val fullHPColor = Color(120, 230, 0, 255)

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val state = AppDriver.getInstance().currentState
        if (state !is CombatState) return
        val shipInfo = state.invoke("getShipInfo") as UIPanelAPI

        val uiElements = shipInfo.getChildrenCopy()
        if (uiElements.any { it is CustomPanelAPI && it.plugin is PaperdollPanelPlugin }) return // return if added

        val shipField = shipInfo.getFieldsMatching(fieldAssignableTo = ShipAPI::class.java)[0]
        val ship = shipField.get(shipInfo) as ShipAPI? ?: return

        val hullId = ship.hullSpec.baseHullId

        // Check if the ship is one of the allowed hull types
        if (hullId !in setOf("onslaught_mk1", "nsp_legionmk1", "nsp_dominatormk1")) return

        shipInfo.CustomPanel(200f, 200f) { plugin ->
            anchorInBottomLeftOfParent()
            val center = Vector2f(centerX, centerY)

            plugin.render { alpha ->
                initRendering()

                val currentShip = shipField.get(shipInfo) as ShipAPI? ?: return@render
                val targetWidth = ( currentShip.hullSize.ordinal / 5f ) * 170f
                val moduleScaleFactor = min(targetWidth / max(currentShip.spriteAPI.width, currentShip.spriteAPI.height), 2f)

                val shipSprite = currentShip.spriteAPI
                val shipOffset = Vector2f(shipSprite.centerX - shipSprite.width/2, shipSprite.centerY - shipSprite.height/2).rotate(currentShip.facing - 90f)
                val shipSpriteLocation = currentShip.location - shipOffset

                for(module in currentShip.childModulesCopy){
                    if (module.hitpoints <= 0f) continue

                    val moduleSprite = module.spriteAPI
                    val moduleOffset = Vector2f(moduleSprite.centerX - moduleSprite.width/2, moduleSprite.centerY - moduleSprite.height/2).rotate(module.facing - 90f)
                    val moduleSpriteLocation = module.location - moduleOffset

                    val offset = (shipSpriteLocation - moduleSpriteLocation).scale(moduleScaleFactor) as Vector2f
                    val paperDollLocation = center - offset

                    val armorHealthLevel = with(module.armorGrid) {
                        (armorAtCell(weakestArmorRegion()!!)!! + module.hitpoints) / (armorRating + module.maxHitpoints)
                    }

                    val sprite = Global.getSettings().getSprite(module.hullSpec.spriteName).apply {
                        angle = module.facing - 90f
                        color = interpolateColorNicely(noHPColor, fullHPColor, armorHealthLevel)
                        alphaMult = 0.75f * alpha
                        setSize(width * moduleScaleFactor, height * moduleScaleFactor)
                    }

                    sprite.renderAtCenter(paperDollLocation.x, paperDollLocation.y)
                }

                glPopAttrib()
            }
        }
    }

    private fun initRendering(){
        // Save GL state (includes texture, blend, matrix modes, texenv, etc.)
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        // Use GL_COMBINE to allow arbitrary uniform colors
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE)

        // RGB Combine -> Use Primary Color (from glColor, set internally by spriteAPI.render based on spriteAPI.color)
        glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_REPLACE)
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_RGB, GL_PRIMARY_COLOR)
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR)

        // Alpha Combine -> Modulate Texture Alpha * Primary Color Alpha (from glColor)
        glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_MODULATE)
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_ALPHA, GL_TEXTURE)
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA)
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC1_ALPHA, GL_PRIMARY_COLOR)
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, GL_SRC_ALPHA)
    }
}

// Marker class to identify that our paperdoll panel has been added
class PaperdollPanelPlugin : CustomUIPanelPlugin {
    override fun advance(amount: Float) {}
    override fun positionChanged(position: PositionAPI?) {}
    override fun processInput(events: MutableList<InputEventAPI>?) {}
    override fun buttonPressed(buttonId: Any?) {
        TODO("Not yet implemented")
    }

    override fun renderBelow(alphaMult: Float) {}
    override fun render(alphaMult: Float) {}
}