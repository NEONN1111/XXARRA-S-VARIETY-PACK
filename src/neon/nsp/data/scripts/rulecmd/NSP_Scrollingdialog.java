package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NSP_Scrollingdialog extends BaseCommandPlugin {

    @Override
    public boolean execute(
            String ruleId,
            InteractionDialogAPI dialog,
            List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap
    ) {
        String optionID = params.get(0).getString(memoryMap); // cs_V1PasswordInput2
        int gotten = 1;
        ArrayList<String> list = new ArrayList<>();
        
        while (gotten < params.size()) { // if we have four paragraphs we want to add for example, params size would be 5. if we have one, it'd be size 2
            list.add(params.get(gotten).getString(memoryMap));
            gotten++;
        }
        
        Global.getSector().addScript(new NSPScrollingdialog(dialog, optionID, list));
        return true;
    }
    
    public static class NSPScrollingdialog implements EveryFrameScript {
        private InteractionDialogAPI dialog;
        private String dialogOption;
        private ArrayList<String> list;
        
        private boolean done = false;
        private int typed = 0;
        private int currdex = 0;
        private String currentpara;
        private String lastpara = null;
        private final IntervalUtil adder = new IntervalUtil(0.07f, 0.07f);
        private final IntervalUtil afterPause = new IntervalUtil(1.5f, 1.5f);
        
        public NSPScrollingdialog(InteractionDialogAPI dialog, String dialogOption, ArrayList<String> list) {
            this.dialog = dialog;
            this.dialogOption = dialogOption;
            this.list = list;
            this.currentpara = list.get(0);
            
            TextPanelAPI textPanel = dialog.getTextPanel();
            textPanel.setFontOrbitron();
            textPanel.clear();
            dialog.getOptionPanel().clearOptions();
        }
        
        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            TextPanelAPI textPanel = dialog.getTextPanel();
            textPanel.advance(10f); // remove text fade-in
            
            if (lastpara == null || !lastpara.equals(currentpara)) {
                lastpara = currentpara;
                textPanel.addPara("", Color.WHITE);
            }
            
            // Check if we've typed all characters in current paragraph
            if (typed >= currentpara.length()) {
                afterPause.advance(amount);
                if (afterPause.intervalElapsed()) {
                    currdex++;
                    if (currdex >= list.size()) {
                        textPanel.setFontInsignia();
                        dialog.getOptionPanel().addOption("Continue", dialogOption);
                        Global.getSector().removeScript(this);
                        done = true;
                        return;
                    } else {
                        currentpara = list.get(currdex);
                        typed = 0;
                    }
                }
                return;
            }
            
            adder.advance(amount);
            if (adder.intervalElapsed()) {
                char nextChar = currentpara.charAt(typed);
                textPanel.appendToLastParagraph(String.valueOf(nextChar));
                typed++;
            }
        }
    }
}