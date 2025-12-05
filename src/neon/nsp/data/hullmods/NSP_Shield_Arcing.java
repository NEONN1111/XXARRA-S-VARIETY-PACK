package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Map;

public class NSP_Shield_Arcing extends BaseHullMod {

    //min and max distance between arcs, its angle so bigger ships will have bigger distance in SU
    float maxAngle = 40f,
            minAngle = 10f;

    public void advanceInCombat(ShipAPI ship, float amount) {
        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        if (ship.getShield() == null) return;
        String id = ship.getId();

        //time between arcs first min second max
        IntervalUtil timer = new IntervalUtil(1.5f, 1.5f);

        if (customCombatData.get("NSP_Shield_Arcing" + id) instanceof IntervalUtil)
            timer = (IntervalUtil) customCombatData.get("NSP_Shield_Arcing" + id);

        if (ship.getShield().isOff()) return;

        timer.advance(amount);
        customCombatData.put("NSP_Shield_Arcing" + id, timer);
        if (timer.intervalElapsed()){
            if (ship.getShield().getActiveArc() < minAngle) return;

            float arcDistance = MathUtils.getRandomNumberInRange(minAngle, maxAngle);
            float StartAngle = MathUtils.getRandomNumberInRange(0, ship.getShield().getActiveArc() - arcDistance);
            float shieldStartAngle = ship.getShield().getFacing() + (ship.getShield().getActiveArc() * 0.5f);
            float arcStartAngle = shieldStartAngle - StartAngle;


            Vector2f arcStart = MathUtils.getPointOnCircumference(ship.getShieldCenterEvenIfNoShield(),ship.getShield().getRadius(),arcStartAngle);
            Vector2f arcEnd = MathUtils.getPointOnCircumference(ship.getShieldCenterEvenIfNoShield(),ship.getShield().getRadius(),arcStartAngle - arcDistance);

            Global.getCombatEngine().spawnEmpArcVisual(arcStart,ship,arcEnd,ship,10,
                    new Color(255,200,100,255),
                    new Color(255, 255, 255,255));

        }

    }

}
