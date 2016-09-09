package net.minecraft.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IChatComponent;

@SideOnly(Side.CLIENT)
public class GuiDisconnected extends GuiScreen
{
    private String reason;
    private IChatComponent message;
    private List multilineMessage;
    private final GuiScreen parentScreen;
    private static final String __OBFID = "CL_00000693";

    public GuiDisconnected(GuiScreen p_i45020_1_, String p_i45020_2_, IChatComponent p_i45020_3_)
    {
        this.parentScreen = p_i45020_1_;
        this.reason = I18n.format(p_i45020_2_, new Object[0]);
        this.message = p_i45020_3_;
    }

    /**
     * Fired when a key is typed (except F11 who toggle full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode) {}

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 120 + 12, I18n.format("gui.toMenu", new Object[0])));
        this.multilineMessage = this.fontRendererObj.listFormattedStringToWidth(this.message.getFormattedText(), this.width - 50);
    }

    protected void actionPerformed(GuiButton button)
    {
        if (button.id == 0)
        {
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY, renderPartialTicks
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, this.reason, this.width / 2, this.height / 2 - 50, 11184810);
        int k = this.height / 2 - 30;

        if (this.multilineMessage != null)
        {
            for (Iterator iterator = this.multilineMessage.iterator(); iterator.hasNext(); k += this.fontRendererObj.FONT_HEIGHT)
            {
                String s = (String)iterator.next();
                this.drawCenteredString(this.fontRendererObj, s, this.width / 2, k, 16777215);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}