package neon.nsp.data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class NSPSandevistan2 {

    // Gold color constants matching your Temporal Jaunt system
    public static final Color GOLD_AFTERIMAGE_COLOR = new Color(255, 196, 19, 100);

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
                color, // Now uses the passed color parameter instead of hardcoded Color.ORANGE
                true,
                0f,
                fadein, // Use the fadein parameter
                duration, // Use the duration parameter
                fadeout, // Use the fadeout parameter
                0f,
                0.1f,
                0.1f,
                1f, // This should probably be fadeout, but keeping original structure
                CombatEngineLayers.BELOW_SHIPS_LAYER
        );
    }

    // Convenience method for gold afterimages with Temporal Jaunt styling
    public static void goldAfterimage(ShipAPI ship, Float fadein, Float duration, Float fadeout){
        afterimage(ship, GOLD_AFTERIMAGE_COLOR, fadein, duration, fadeout);
    }

    // Convenience method for gold afterimages with default timing
    public static void goldAfterimage(ShipAPI ship){
        afterimage(ship, GOLD_AFTERIMAGE_COLOR, 0.1f, 0.5f, 0.3f);
    }
}