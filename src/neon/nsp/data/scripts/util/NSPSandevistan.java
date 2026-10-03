package neon.nsp.data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

//V: Why not just have them in system/hullmod code? It doesn't really serve any purpose besides spreading code files
//V: Also pretty much the same code as NSPSandevistan2
//V: Moved to respective hullmod code
@Deprecated
public class NSPSandevistan {

    public static void afterimage(ShipAPI ship, Color color, Float fadein, Float duration, Float fadeout){
        // renders additive sprite of ship below where ship currently is
        SpriteAPI sprite = ship.getSpriteAPI();
        Vector2f location = ship.getLocation();

        MagicRender.battlespace(
                Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                new Vector2f(location.getX(), location.getY()),
                new Vector2f(0f, 0f),
                new Vector2f(sprite.getWidth(), sprite.getHeight()),
                new Vector2f(0f, 0f),
                ship.getFacing() - 90f,
                0f,
                Color.RED,
                true,
                0f,
                0f,
                0f,
                0f,
                0f,
                0.1f,
                0.1f,
                fadeout,
                CombatEngineLayers.BELOW_SHIPS_LAYER
        );
    }
}
