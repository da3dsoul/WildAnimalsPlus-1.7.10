package net.minecraft.network.play.server;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.IOException;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;

public class S2BPacketChangeGameState extends Packet
{
    public static final String[] MESSAGE_NAMES = new String[] {"tile.bed.notValid", null, null, "gameMode.changed"};
    private int state;
    private float field_149141_c;
    private static final String __OBFID = "CL_00001301";

    public S2BPacketChangeGameState() {}

    public S2BPacketChangeGameState(int stateIn, float p_i45194_2_)
    {
        this.state = stateIn;
        this.field_149141_c = p_i45194_2_;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public void readPacketData(PacketBuffer data) throws IOException
    {
        this.state = data.readUnsignedByte();
        this.field_149141_c = data.readFloat();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer data) throws IOException
    {
        data.writeByte(this.state);
        data.writeFloat(this.field_149141_c);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void processPacket(INetHandlerPlayClient handler)
    {
        handler.handleChangeGameState(this);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void processPacket(INetHandler handler)
    {
        this.processPacket((INetHandlerPlayClient)handler);
    }

    @SideOnly(Side.CLIENT)
    public int func_149138_c()
    {
        return this.state;
    }

    @SideOnly(Side.CLIENT)
    public float func_149137_d()
    {
        return this.field_149141_c;
    }
}