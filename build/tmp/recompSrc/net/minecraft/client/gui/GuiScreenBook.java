package net.minecraft.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class GuiScreenBook extends GuiScreen
{
    private static final Logger logger = LogManager.getLogger();
    private static final ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
    /** The player editing the book */
    private final EntityPlayer editingPlayer;
    private final ItemStack bookObj;
    /** Whether the book is signed or can still be edited */
    private final boolean bookIsUnsigned;
    /** Whether the book's title or contents has been modified since being opened */
    private boolean bookIsModified;
    /** Determines if the signing screen is open */
    private boolean bookGettingSigned;
    /** Update ticks since the gui was opened */
    private int updateCount;
    private int bookImageWidth = 192;
    private int bookImageHeight = 192;
    private int bookTotalPages = 1;
    private int currPage;
    private NBTTagList bookPages;
    private String bookTitle = "";
    private GuiScreenBook.NextPageButton buttonNextPage;
    private GuiScreenBook.NextPageButton buttonPreviousPage;
    private GuiButton buttonDone;
    /** The GuiButton to sign this book. */
    private GuiButton buttonSign;
    private GuiButton buttonFinalize;
    private GuiButton buttonCancel;
    private static final String __OBFID = "CL_00000744";

    public GuiScreenBook(EntityPlayer p_i1080_1_, ItemStack p_i1080_2_, boolean p_i1080_3_)
    {
        this.editingPlayer = p_i1080_1_;
        this.bookObj = p_i1080_2_;
        this.bookIsUnsigned = p_i1080_3_;

        if (p_i1080_2_.hasTagCompound())
        {
            NBTTagCompound nbttagcompound = p_i1080_2_.getTagCompound();
            this.bookPages = nbttagcompound.getTagList("pages", 8);

            if (this.bookPages != null)
            {
                this.bookPages = (NBTTagList)this.bookPages.copy();
                this.bookTotalPages = this.bookPages.tagCount();

                if (this.bookTotalPages < 1)
                {
                    this.bookTotalPages = 1;
                }
            }
        }

        if (this.bookPages == null && p_i1080_3_)
        {
            this.bookPages = new NBTTagList();
            this.bookPages.appendTag(new NBTTagString(""));
            this.bookTotalPages = 1;
        }
    }

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen()
    {
        super.updateScreen();
        ++this.updateCount;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    public void initGui()
    {
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);

        if (this.bookIsUnsigned)
        {
            this.buttonList.add(this.buttonSign = new GuiButton(3, this.width / 2 - 100, 4 + this.bookImageHeight, 98, 20, I18n.format("book.signButton", new Object[0])));
            this.buttonList.add(this.buttonDone = new GuiButton(0, this.width / 2 + 2, 4 + this.bookImageHeight, 98, 20, I18n.format("gui.done", new Object[0])));
            this.buttonList.add(this.buttonFinalize = new GuiButton(5, this.width / 2 - 100, 4 + this.bookImageHeight, 98, 20, I18n.format("book.finalizeButton", new Object[0])));
            this.buttonList.add(this.buttonCancel = new GuiButton(4, this.width / 2 + 2, 4 + this.bookImageHeight, 98, 20, I18n.format("gui.cancel", new Object[0])));
        }
        else
        {
            this.buttonList.add(this.buttonDone = new GuiButton(0, this.width / 2 - 100, 4 + this.bookImageHeight, 200, 20, I18n.format("gui.done", new Object[0])));
        }

        int i = (this.width - this.bookImageWidth) / 2;
        byte b0 = 2;
        this.buttonList.add(this.buttonNextPage = new GuiScreenBook.NextPageButton(1, i + 120, b0 + 154, true));
        this.buttonList.add(this.buttonPreviousPage = new GuiScreenBook.NextPageButton(2, i + 38, b0 + 154, false));
        this.updateButtons();
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    private void updateButtons()
    {
        this.buttonNextPage.visible = !this.bookGettingSigned && (this.currPage < this.bookTotalPages - 1 || this.bookIsUnsigned);
        this.buttonPreviousPage.visible = !this.bookGettingSigned && this.currPage > 0;
        this.buttonDone.visible = !this.bookIsUnsigned || !this.bookGettingSigned;

        if (this.bookIsUnsigned)
        {
            this.buttonSign.visible = !this.bookGettingSigned;
            this.buttonCancel.visible = this.bookGettingSigned;
            this.buttonFinalize.visible = this.bookGettingSigned;
            this.buttonFinalize.enabled = this.bookTitle.trim().length() > 0;
        }
    }

    private void sendBookToServer(boolean p_146462_1_)
    {
        if (this.bookIsUnsigned && this.bookIsModified)
        {
            if (this.bookPages != null)
            {
                String s;

                while (this.bookPages.tagCount() > 1)
                {
                    s = this.bookPages.getStringTagAt(this.bookPages.tagCount() - 1);

                    if (s.length() != 0)
                    {
                        break;
                    }

                    this.bookPages.removeTag(this.bookPages.tagCount() - 1);
                }

                if (this.bookObj.hasTagCompound())
                {
                    NBTTagCompound nbttagcompound = this.bookObj.getTagCompound();
                    nbttagcompound.setTag("pages", this.bookPages);
                }
                else
                {
                    this.bookObj.setTagInfo("pages", this.bookPages);
                }

                s = "MC|BEdit";

                if (p_146462_1_)
                {
                    s = "MC|BSign";
                    this.bookObj.setTagInfo("author", new NBTTagString(this.editingPlayer.getCommandSenderName()));
                    this.bookObj.setTagInfo("title", new NBTTagString(this.bookTitle.trim()));
                    this.bookObj.setItem(Items.written_book);
                }

                ByteBuf bytebuf = Unpooled.buffer();

                try
                {
                    (new PacketBuffer(bytebuf)).writeItemStackToBuffer(this.bookObj);
                    this.mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload(s, bytebuf));
                }
                catch (Exception exception)
                {
                    logger.error("Couldn\'t send book info", exception);
                }
                finally
                {
                    bytebuf.release();
                }
            }
        }
    }

    protected void actionPerformed(GuiButton button)
    {
        if (button.enabled)
        {
            if (button.id == 0)
            {
                this.mc.displayGuiScreen((GuiScreen)null);
                this.sendBookToServer(false);
            }
            else if (button.id == 3 && this.bookIsUnsigned)
            {
                this.bookGettingSigned = true;
            }
            else if (button.id == 1)
            {
                if (this.currPage < this.bookTotalPages - 1)
                {
                    ++this.currPage;
                }
                else if (this.bookIsUnsigned)
                {
                    this.addNewPage();

                    if (this.currPage < this.bookTotalPages - 1)
                    {
                        ++this.currPage;
                    }
                }
            }
            else if (button.id == 2)
            {
                if (this.currPage > 0)
                {
                    --this.currPage;
                }
            }
            else if (button.id == 5 && this.bookGettingSigned)
            {
                this.sendBookToServer(true);
                this.mc.displayGuiScreen((GuiScreen)null);
            }
            else if (button.id == 4 && this.bookGettingSigned)
            {
                this.bookGettingSigned = false;
            }

            this.updateButtons();
        }
    }

    private void addNewPage()
    {
        if (this.bookPages != null && this.bookPages.tagCount() < 50)
        {
            this.bookPages.appendTag(new NBTTagString(""));
            ++this.bookTotalPages;
            this.bookIsModified = true;
        }
    }

    /**
     * Fired when a key is typed (except F11 who toggle full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode)
    {
        super.keyTyped(typedChar, keyCode);

        if (this.bookIsUnsigned)
        {
            if (this.bookGettingSigned)
            {
                this.keyTypedInTitle(typedChar, keyCode);
            }
            else
            {
                this.keyTypedInBook(typedChar, keyCode);
            }
        }
    }

    /**
     * Processes keystrokes when editing the text of a book
     */
    private void keyTypedInBook(char p_146463_1_, int p_146463_2_)
    {
        switch (p_146463_1_)
        {
            case 22:
                this.pageInsertIntoCurrent(GuiScreen.getClipboardString());
                return;
            default:
                switch (p_146463_2_)
                {
                    case 14:
                        String s = this.pageGetCurrent();

                        if (s.length() > 0)
                        {
                            this.pageSetCurrent(s.substring(0, s.length() - 1));
                        }

                        return;
                    case 28:
                    case 156:
                        this.pageInsertIntoCurrent("\n");
                        return;
                    default:
                        if (ChatAllowedCharacters.isAllowedCharacter(p_146463_1_))
                        {
                            this.pageInsertIntoCurrent(Character.toString(p_146463_1_));
                        }
                }
        }
    }

    /**
     * Processes keystrokes when editing the title of a book
     */
    private void keyTypedInTitle(char p_146460_1_, int p_146460_2_)
    {
        switch (p_146460_2_)
        {
            case 14:
                if (!this.bookTitle.isEmpty())
                {
                    this.bookTitle = this.bookTitle.substring(0, this.bookTitle.length() - 1);
                    this.updateButtons();
                }

                return;
            case 28:
            case 156:
                if (!this.bookTitle.isEmpty())
                {
                    this.sendBookToServer(true);
                    this.mc.displayGuiScreen((GuiScreen)null);
                }

                return;
            default:
                if (this.bookTitle.length() < 16 && ChatAllowedCharacters.isAllowedCharacter(p_146460_1_))
                {
                    this.bookTitle = this.bookTitle + Character.toString(p_146460_1_);
                    this.updateButtons();
                    this.bookIsModified = true;
                }
        }
    }

    /**
     * Returns the entire text of the current page as determined by currPage
     */
    private String pageGetCurrent()
    {
        return this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.tagCount() ? this.bookPages.getStringTagAt(this.currPage) : "";
    }

    /**
     * Sets the text of the current page as determined by currPage
     */
    private void pageSetCurrent(String p_146457_1_)
    {
        if (this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.tagCount())
        {
            this.bookPages.setTag(this.currPage, new NBTTagString(p_146457_1_));
            this.bookIsModified = true;
        }
    }

    /**
     * Processes any text getting inserted into the current page, enforcing the page size limit
     */
    private void pageInsertIntoCurrent(String p_146459_1_)
    {
        String s1 = this.pageGetCurrent();
        String s2 = s1 + p_146459_1_;
        int i = this.fontRendererObj.splitStringWidth(s2 + "" + EnumChatFormatting.BLACK + "_", 118);

        if (i <= 118 && s2.length() < 256)
        {
            this.pageSetCurrent(s2);
        }
    }

    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY, renderPartialTicks
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(bookGuiTextures);
        int k = (this.width - this.bookImageWidth) / 2;
        byte b0 = 2;
        this.drawTexturedModalRect(k, b0, 0, 0, this.bookImageWidth, this.bookImageHeight);
        String s;
        String s1;
        int l;

        if (this.bookGettingSigned)
        {
            s = this.bookTitle;

            if (this.bookIsUnsigned)
            {
                if (this.updateCount / 6 % 2 == 0)
                {
                    s = s + "" + EnumChatFormatting.BLACK + "_";
                }
                else
                {
                    s = s + "" + EnumChatFormatting.GRAY + "_";
                }
            }

            s1 = I18n.format("book.editTitle", new Object[0]);
            l = this.fontRendererObj.getStringWidth(s1);
            this.fontRendererObj.drawString(s1, k + 36 + (116 - l) / 2, b0 + 16 + 16, 0);
            int i1 = this.fontRendererObj.getStringWidth(s);
            this.fontRendererObj.drawString(s, k + 36 + (116 - i1) / 2, b0 + 48, 0);
            String s2 = I18n.format("book.byAuthor", new Object[] {this.editingPlayer.getCommandSenderName()});
            int j1 = this.fontRendererObj.getStringWidth(s2);
            this.fontRendererObj.drawString(EnumChatFormatting.DARK_GRAY + s2, k + 36 + (116 - j1) / 2, b0 + 48 + 10, 0);
            String s3 = I18n.format("book.finalizeWarning", new Object[0]);
            this.fontRendererObj.drawSplitString(s3, k + 36, b0 + 80, 116, 0);
        }
        else
        {
            s = I18n.format("book.pageIndicator", new Object[] {Integer.valueOf(this.currPage + 1), Integer.valueOf(this.bookTotalPages)});
            s1 = "";

            if (this.bookPages != null && this.currPage >= 0 && this.currPage < this.bookPages.tagCount())
            {
                s1 = this.bookPages.getStringTagAt(this.currPage);
            }

            if (this.bookIsUnsigned)
            {
                if (this.fontRendererObj.getBidiFlag())
                {
                    s1 = s1 + "_";
                }
                else if (this.updateCount / 6 % 2 == 0)
                {
                    s1 = s1 + "" + EnumChatFormatting.BLACK + "_";
                }
                else
                {
                    s1 = s1 + "" + EnumChatFormatting.GRAY + "_";
                }
            }

            l = this.fontRendererObj.getStringWidth(s);
            this.fontRendererObj.drawString(s, k - l + this.bookImageWidth - 44, b0 + 16, 0);
            this.fontRendererObj.drawSplitString(s1, k + 36, b0 + 16 + 16, 116, 0);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @SideOnly(Side.CLIENT)
    static class NextPageButton extends GuiButton
        {
            private final boolean field_146151_o;
            private static final String __OBFID = "CL_00000745";

            public NextPageButton(int p_i1079_1_, int p_i1079_2_, int p_i1079_3_, boolean p_i1079_4_)
            {
                super(p_i1079_1_, p_i1079_2_, p_i1079_3_, 23, 13, "");
                this.field_146151_o = p_i1079_4_;
            }

            /**
             * Draws this button to the screen.
             */
            public void drawButton(Minecraft mc, int mouseX, int mouseY)
            {
                if (this.visible)
                {
                    boolean flag = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager().bindTexture(GuiScreenBook.bookGuiTextures);
                    int k = 0;
                    int l = 192;

                    if (flag)
                    {
                        k += 23;
                    }

                    if (!this.field_146151_o)
                    {
                        l += 13;
                    }

                    this.drawTexturedModalRect(this.xPosition, this.yPosition, k, l, 23, 13);
                }
            }
        }
}