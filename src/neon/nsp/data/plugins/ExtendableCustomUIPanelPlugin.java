package neon.nsp.data.plugins;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ExtendableCustomUIPanelPlugin extends BaseCustomUIPanelPlugin {

    private final CustomPanelAPI customPanel;

    private List<Consumer<InputEventAPI>> onClickFunctions = new ArrayList<>();
    private List<Consumer<InputEventAPI>> onClickOutsideFunctions = new ArrayList<>();
    private List<Consumer<InputEventAPI>> onClickReleaseFunctions = new ArrayList<>();
    private List<Consumer<InputEventAPI>> onHoverFunctions = new ArrayList<>();
    private List<Consumer<InputEventAPI>> onHoverEnterFunctions = new ArrayList<>();
    private List<Consumer<InputEventAPI>> onHoverExitFunctions = new ArrayList<>();
    private List<Consumer<InputEventAPI>> onHeldFunctions = new ArrayList<>();
    private List<Consumer<InputEventAPI>> onKeyDownFunctions = new ArrayList<>();
    private List<Consumer<InputEventAPI>> onKeyUpFunctions = new ArrayList<>();

    private List<Consumer<Float>> renderBelowFunctions = new ArrayList<>();
    private List<Consumer<Float>> renderFunctions = new ArrayList<>();
    private List<Consumer<Float>> advanceFunctions = new ArrayList<>();

    private float inputCaptureTopPad = 0f;
    private float inputCaptureBottomPad = 0f;
    private float inputCaptureLeftPad = 0f;
    private float inputCaptureRightPad = 0f;

    private boolean isHovering = false;
    private boolean hasClicked = false;

    public ExtendableCustomUIPanelPlugin(CustomPanelAPI customPanel) {
        this.customPanel = customPanel;
    }

    public void renderBelow(Consumer<Float> function) {
        renderBelowFunctions.add(function);
    }

    @Override
    public void renderBelow(float alphaMult) {
        for (Consumer<Float> function : renderBelowFunctions) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            function.accept(alphaMult);
            GL11.glPopMatrix();
        }
    }

    public void render(Consumer<Float> function) {
        renderFunctions.add(function);
    }

    @Override
    public void render(float alphaMult) {
        for (Consumer<Float> function : renderFunctions) {
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            function.accept(alphaMult);
            GL11.glPopMatrix();
        }
    }

    public void advance(Consumer<Float> function) {
        advanceFunctions.add(function);
    }

    @Override
    public void advance(float amount) {
        for (Consumer<Float> function : advanceFunctions) {
            function.accept(amount);
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (events == null) return;

        for (InputEventAPI event : events) {
            if (!event.isMouseEvent()) continue;

            boolean inElement = event.getX() >= (getLeft() - inputCaptureLeftPad) &&
                    event.getX() <= (getRight() + inputCaptureRightPad) &&
                    event.getY() >= (getBottom() - inputCaptureBottomPad) &&
                    event.getY() <= (getTop() + inputCaptureTopPad);

            if (inElement) {
                for (Consumer<InputEventAPI> onHover : onHoverFunctions) {
                    onHover.accept(event);
                }
                if (!isHovering) {
                    for (Consumer<InputEventAPI> onHoverEnter : onHoverEnterFunctions) {
                        onHoverEnter.accept(event);
                    }
                }
                isHovering = true;

                if (event.isMouseDownEvent()) {
                    hasClicked = true;
                    for (Consumer<InputEventAPI> onClick : onClickFunctions) {
                        onClick.accept(event);
                    }
                }
                if (event.isMouseUpEvent() && hasClicked) {
                    hasClicked = false;
                    for (Consumer<InputEventAPI> onClickRelease : onClickReleaseFunctions) {
                        onClickRelease.accept(event);
                    }
                }
                if (Mouse.isButtonDown(0)) {
                    for (Consumer<InputEventAPI> onHeld : onHeldFunctions) {
                        onHeld.accept(event);
                    }
                }
            } else {
                if (isHovering) {
                    for (Consumer<InputEventAPI> onHoverExit : onHoverExitFunctions) {
                        onHoverExit.accept(event);
                    }
                }
                isHovering = false;
                if (event.isMouseDownEvent()) {
                    for (Consumer<InputEventAPI> onClickOutside : onClickOutsideFunctions) {
                        onClickOutside.accept(event);
                    }
                }
                if (event.isMouseUpEvent()) {
                    hasClicked = false;
                }
            }
        }

        for (InputEventAPI event : events) {
            if (!event.isKeyboardEvent()) continue;

            if (event.isKeyDownEvent()) {
                for (Consumer<InputEventAPI> onKeyDown : onKeyDownFunctions) {
                    onKeyDown.accept(event);
                }
            }
            if (event.isKeyUpEvent()) {
                for (Consumer<InputEventAPI> onKeyUp : onKeyUpFunctions) {
                    onKeyUp.accept(event);
                }
            }
        }
    }

    public void onClick(Consumer<InputEventAPI> function) { onClickFunctions.add(function); }
    public void onClickRelease(Consumer<InputEventAPI> function) { onClickReleaseFunctions.add(function); }
    public void onClickOutside(Consumer<InputEventAPI> function) { onClickOutsideFunctions.add(function); }
    public void onHover(Consumer<InputEventAPI> function) { onHoverFunctions.add(function); }
    public void onHoverEnter(Consumer<InputEventAPI> function) { onHoverEnterFunctions.add(function); }
    public void onHoverExit(Consumer<InputEventAPI> function) { onHoverExitFunctions.add(function); }
    public void onHeld(Consumer<InputEventAPI> function) { onHeldFunctions.add(function); }
    public void onKeyDown(Consumer<InputEventAPI> function) { onKeyDownFunctions.add(function); }
    public void onKeyUp(Consumer<InputEventAPI> function) { onKeyUpFunctions.add(function); }

    public void setInputCapturePad(float top, float bottom, float left, float right) {
        this.inputCaptureTopPad = top;
        this.inputCaptureBottomPad = bottom;
        this.inputCaptureLeftPad = left;
        this.inputCaptureRightPad = right;
    }

    public boolean isHovering() { return isHovering; }
    public boolean hasClicked() { return hasClicked; }

    public float getWidth() { return customPanel.getPosition().getWidth(); }
    public float getHeight() { return customPanel.getPosition().getHeight(); }
    public float getX() { return customPanel.getPosition().getX(); }
    public float getY() { return customPanel.getPosition().getY(); }
    public float getLeft() { return getX(); }
    public float getBottom() { return getY(); }
    public float getTop() { return getY() + getHeight(); }
    public float getRight() { return getX() + getWidth(); }
    public float getCenterX() { return customPanel.getPosition().getCenterX(); }
    public float getCenterY() { return customPanel.getPosition().getCenterY(); }
}