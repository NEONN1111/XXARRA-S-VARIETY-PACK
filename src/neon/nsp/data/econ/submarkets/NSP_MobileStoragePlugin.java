package neon.nsp.data.econ.submarkets;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;

public class NSP_MobileStoragePlugin extends BaseSubmarketPlugin {
    private final int maxShipStorage = 2;
    private final float maxWeaponsInStorage = 1000f;
    private static final Color QUOTE_TEXT = new Color(150, 150, 200); // Example color

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    @Override
    public void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addPara(
                "Most weapons and fighter LPCs can be stored here, up to " + (int)maxWeaponsInStorage +
                        " units. They will be available if you use the ship's refitting function.",
                5f
        ).setHighlight("weapons", " fighter LPCs", String.valueOf((int)maxWeaponsInStorage));

        tooltip.addPara(
                "The ship can also store up to " + maxShipStorage + " frigates. They will not be considered mothballed, " +
                        "and can be deployed directly to combat if this ship is deployed.",
                5f
        ).setHighlight(String.valueOf(maxShipStorage));

        tooltip.addPara("They will also be automatically recovered if destroyed.", 5f);
        tooltip.addPara("\"Wow, it really is bigger on the inside!\"", 10f).setColor(QUOTE_TEXT);
    }

    @Override
    public boolean isIllegalOnSubmarket(String commodityId, SubmarketPlugin.TransferAction action) {
        return true; // Don't allow commodity storage
    }

    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
        if (action == SubmarketPlugin.TransferAction.PLAYER_BUY) return false;

        // Calculate current cargo space used
        float current = 0f;
        if (stack != null) {
            current += stack.getCargoSpace();
        }

        for (CargoStackAPI existingStack : submarket.getCargo().getStacksCopy()) {
            current += existingStack.getCargoSpace();
        }

        // Check if we've reached capacity
        if (current >= maxWeaponsInStorage) return true;

        // Only allow weapons and fighter wings
        if (stack != null && (stack.isWeaponStack() || stack.isFighterWingStack())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, SubmarketPlugin.TransferAction action) {
        if (action == SubmarketPlugin.TransferAction.PLAYER_BUY) return false;
        if (member == null || member.getHullSpec() == null) return true;

        // Only allow frigates
        if (member.getHullSpec().getHullSize().ordinal() > ShipAPI.HullSize.FRIGATE.ordinal()) return true;

        // Check storage capacity
        int stored = submarket.getCargo().getMothballedShips().getMembersListCopy().size();
        return stored >= maxShipStorage;
    }

    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }

    @Override
    public String getIllegalTransferText(FleetMemberAPI member, SubmarketPlugin.TransferAction action) {
        if (member == null || member.getHullSpec() == null) return "Invalid ship.";

        // Modified: Check if this is the storage ship itself by checking memory flag
        if (submarket.getMarket() != null && submarket.getMarket().getPrimaryEntity() != null) {
            String storageShipName = submarket.getMarket().getMemoryWithoutUpdate().getString("$nsp_storageShipName");
            if (storageShipName != null && storageShipName.equals(member.getShipName())) {
                return "You're not really going to try to store a ship inside of itself, are you?";
            }
        }

        if (member.getHullSpec().getHullSize().ordinal() > ShipAPI.HullSize.FRIGATE.ordinal()) {
            return "Too large to store.";
        }

        return "Storage is full.";
    }

    @Override
    public String getIllegalTransferText(CargoStackAPI stack, SubmarketPlugin.TransferAction action) {
        return "Cannot store this type of cargo.";
    }

    @Override
    public float getTariff() {
        return 0f;
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return true;
    }

    @Override
    public boolean isFreeTransfer() {
        return true;
    }

    @Override
    public String getBuyVerb() {
        return "Take";
    }

    @Override
    public String getSellVerb() {
        return "Leave";
    }
}