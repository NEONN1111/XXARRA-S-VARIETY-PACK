package data.scripts.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;

public class NSP_PackedShips extends BaseSpecialItemPlugin {
    private String memberID = null;
    private FleetMemberAPI member = null;

    @Override
    public void performRightClickAction() {
        if (member == null) return;

        Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
        Global.getSector().getCampaignUI().getMessageDisplay()
                .addMessage("Retrieved the " + member.getShipName() + ".");
    }

    @Override
    public void render(float x, float y, float w, float h,
                       float alphaMult, float glowMult,
                       SpecialItemRendererAPI renderer) {
        super.render(x, y, w, h, alphaMult, glowMult, renderer);
    }

    @Override
    public void init(CargoStackAPI stack) {
        memberID = stack.getSpecialDataIfSpecial().getData();

        // Find the ship in the storage market
        MarketAPI storageMarket = Global.getSector().getEntityById("ds_nexusStorage").getMarket();
        SubmarketAPI storageSubmarket = storageMarket.getSubmarket(Submarkets.SUBMARKET_STORAGE);

        for (FleetMemberAPI ship : storageSubmarket.getCargo().getMothballedShips().getMembersListCopy()) {
            if (ship.getId().equals(memberID)) {
                member = ship;
                break;
            }
        }
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded,
                              CargoTransferHandlerAPI transferHandler, Object stackSource) {
        tooltip.addPara("i packed this ship into your butthole", 5f);

        if (member == null) return;

        tooltip.addTitle(member.getShipName()).setColor(Color.PINK);

        if (expanded) {
            TooltipMakerAPI img = tooltip.beginImageWithText(member.getHullSpec().getSpriteName(), 128f);
            img.addTitle(member.getShipName()).setColor(Color.PINK);
        }
    }

    @Override
    public String getName() {
        if (member != null) {
            return "Packed " + member.getHullSpec().getNameWithDesignationWithDashClass();
        }
        return "";
    }

    @Override
    public boolean isTooltipExpandable() {
        return true;
    }

    @Override
    public boolean hasRightClickAction() {
        return true;
    }

    @Override
    public int getPrice(MarketAPI market, SubmarketAPI submarket) {
        if (member == null) return 0;
        return Math.round(member.getBaseValue());
    }

    @Override
    public boolean shouldRemoveOnRightClickAction() {
        return true;
    }
}