package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import neon.nsp.data.scripts.util.NSP_Util;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

public class NSP_BlinkDriveStats extends BaseShipSystemScript {

    private static final Color COLOR1 = new Color(255, 255, 255);
    private static final Color COLOR2 = new Color(150, 50, 200);
    private static final Color COLOR3 = new Color(100, 0, 0);
    private static final Color COLOR4 = new Color(255, 50, 50);
    private static final Color COLOR5 = new Color(50, 0, 0);
    private static final Color COLOR6 = new Color(150, 50, 200);

    private boolean done = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }

        ShipAPI ship = (ShipAPI) stats.getEntity();

        if (!done) {
            done = true;
            Vector2f origin = ship.getLocation();
            int amountscalar;
            switch (ship.getHullSpec().getBaseHullId()) {
                case "nsp_luminance":
                    amountscalar = 3;
                    break;
                default:
                    amountscalar = 1;
            }
            float shipRadius = NSP_Util.effectiveRadius(ship);
            for (int i = 0; i < 6 * amountscalar; i++) {
                Vector2f point1 = MathUtils.getRandomPointInCircle(origin, shipRadius * 1.25f);
                Vector2f point2 = MathUtils.getRandomPointInCircle(origin, shipRadius * 1.25f);
                if (ship.getShield() != null && ship.getShield().isOn()) {
                    Global.getCombatEngine().spawnEmpArc(ship, point1, new SimpleEntity(point1),
                            new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f, 1000f,
                            null, 10f, COLOR1, COLOR1);
                } else {
                    switch (ship.getHullSpec().getBaseHullId()) {
                        case "nsp_luminance":
                            Global.getCombatEngine().spawnEmpArc(ship, point1, new SimpleEntity(point1),
                                    new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f,
                                    1000f, null, 10f, COLOR2, COLOR1);
                            break;
                        default:
                            Global.getCombatEngine().spawnEmpArc(ship, point1, new SimpleEntity(point1),
                                    new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f,
                                    1000f, null, 5f, COLOR5, COLOR6);
                    }
                }
                if (ship.getShield() != null && ship.getShield().isOff()) {
                    Global.getCombatEngine().spawnEmpArc(ship, point1, new SimpleEntity(point1),
                                                         new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f, 1000f,
                                                         null, 10f, COLOR1, COLOR1);
                } else {
                    switch (ship.getHullSpec().getBaseHullId()) {
                        case "nsp_luminance":
                            Global.getCombatEngine().spawnEmpArc(ship, point1, new SimpleEntity(point1),
                                                                 new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f,
                                                                 1000f, null, 10f, COLOR2, COLOR1);
                            break;
                        default:
                            Global.getCombatEngine().spawnEmpArc(ship, point1, new SimpleEntity(point1),
                                                                 new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f,
                                                                 1000f, null, 5f, COLOR5, COLOR6);

                    }
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        done = false;
    }
}
