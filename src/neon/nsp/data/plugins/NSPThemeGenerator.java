package neon.nsp.data.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.procgen.themes.ThemeGenContext;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.ShipRecoverySpecialCreator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.SpecialCreationContext;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.util.Misc;

public class NSPThemeGenerator extends BaseThemeGenerator {

	public String getThemeId() {
		return "MISC";
	}

	@Override
	public float getWeight() {
		return 0f;
	}

	@Override
	public int getOrder() {
		return 1000000;
	}

	@Override
	public void generateForSector(ThemeGenContext context, float allowedUnusedFraction) {

		// Spawn legion_xiv_Elite derelicts
		addDerelicts(context, "nsp_cassowary_standard", 2, 3, 1, 2, Tags.THEME_RUINS);
		addDerelicts(context, "nsp_peregrine_standard", 2, 3, 1, 2, Tags.THEME_RUINS);
		addDerelicts(context, "nsp_galaxy_standard", 0, 1, 1, 1, Tags.THEME_SPECIAL);
	}

	protected void addDerelicts(ThemeGenContext context, String variant,
								int minNonSalvageable, int maxNonSalvageable,
								int minSalvageable, int maxSalvageable,
								String ... allowedThemes) {
		if (Global.getSettings().getVariant(variant) != null) {
			if (DEBUG) System.out.println("Adding " + variant + " to star systems");

			Set<String> tags = new HashSet<String>(Arrays.asList(allowedThemes));

			int numSalvageable = minSalvageable + random.nextInt(maxSalvageable - minSalvageable + 1);
			int numNonSalvageable = minNonSalvageable + random.nextInt(maxNonSalvageable - minNonSalvageable + 1);

			List<Constellation> list = new ArrayList<Constellation>(context.constellations);
			Collections.shuffle(list, random);

			List<StarSystemData> systems = new ArrayList<StarSystemData>();
			for (Constellation c : list) {
				for (StarSystemAPI system : c.getSystems()) {
					StarSystemData data = computeSystemData(system);
					systems.add(data);
				}
			}

			Collections.shuffle(systems, random);
			for (StarSystemData data  : systems) {
				boolean matches = false;
				for (String tag : data.system.getTags()) {
					if (tags.contains(tag)) {
						matches = true;
						break;
					}
				}
				if (!matches) continue;

				EntityLocation loc = pickAnyLocation(random, data.system, 70f, null);
				AddedEntity ae = addDerelictShip(data, loc, variant);
				if (ae != null) {
					if (numSalvageable > 0) {
						numSalvageable--;
						ShipRecoverySpecialCreator creator = new ShipRecoverySpecialCreator(random, 0, 0, false, null, null);
						Object specialData = creator.createSpecial(ae.entity, new SpecialCreationContext());
						if (specialData != null) {
							Misc.setSalvageSpecial(ae.entity, specialData);
						}
					} else {
						numNonSalvageable--;
						SalvageSpecialAssigner.assignSpecials(ae.entity, true);
					}
					if (DEBUG) System.out.println("      Added " + variant + " to " + data.system + "\n");
				}
				if (numSalvageable + numNonSalvageable <= 0) break;
			}

			if (DEBUG) System.out.println("Finished adding " + variant + " to star systems\n\n\n\n\n");
		}
	}
}



