package net.minecraft.client.network;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SideOnly(Side.CLIENT)
public class OldServerPinger
{
    private static final Splitter PING_RESPONSE_SPLITTER = Splitter.on('\u0000').limit(6);
    private static final Logger logger = LogManager.getLogger();
    /** A list of NetworkManagers that have pending pings */
    private final List pingDestinations = Collections.synchronizedList(new ArrayList());
    private static final String __OBFID = "CL_00000892";

    public void ping(final ServerData server) throws UnknownHostException
    {
        ServerAddress serveraddress = ServerAddress.func_78860_a(server.serverIP);
        final NetworkManager networkmanager = NetworkManager.provideLanClient(InetAddress.getByName(serveraddress.getIP()), serveraddress.getPort());
        this.pingDestinations.add(networkmanager);
        server.serverMOTD = "Pinging...";
        server.pingToServer = -1L;
        server.playerList = null;
        networkmanager.setNetHandler(new INetHandlerStatusClient()
        {
            private boolean field_147403_d = false;
            private static final String __OBFID = "CL_00000893";
            public void handleServerInfo(S00PacketServerInfo packetIn)
            {
                ServerStatusResponse serverstatusresponse = packetIn.func_149294_c();

                if (serverstatusresponse.getServerDescription() != null)
                {
                    server.serverMOTD = serverstatusresponse.getServerDescription().getFormattedText();
                }
                else
                {
                    server.serverMOTD = "";
                }

                if (serverstatusresponse.getProtocolVersionInfo() != null)
                {
                    server.gameVersion = serverstatusresponse.getProtocolVersionInfo().getName();
                    server.version = serverstatusresponse.getProtocolVersionInfo().getProtocol();
                }
                else
                {
                    server.gameVersion = "Old";
                    server.version = 0;
                }

                if (serverstatusresponse.getPlayerCountData() != null)
                {
                    server.populationInfo = EnumChatFormatting.GRAY + "" + serverstatusresponse.getPlayerCountData().getOnlinePlayerCount() + "" + EnumChatFormatting.DARK_GRAY + "/" + EnumChatFormatting.GRAY + serverstatusresponse.getPlayerCountData().getMaxPlayers();

                    if (ArrayUtils.isNotEmpty(serverstatusresponse.getPlayerCountData().getPlayers()))
                    {
                        StringBuilder stringbuilder = new StringBuilder();
                        GameProfile[] agameprofile = serverstatusresponse.getPlayerCountData().getPlayers();
                        int i = agameprofile.length;

                        for (int j = 0; j < i; ++j)
                        {
                            GameProfile gameprofile = agameprofile[j];

                            if (stringbuilder.length() > 0)
                            {
                                stringbuilder.append("\n");
                            }

                            stringbuilder.append(gameprofile.getName());
                        }

                        if (serverstatusresponse.getPlayerCountData().getPlayers().length < serverstatusresponse.getPlayerCountData().getOnlinePlayerCount())
                        {
                            if (stringbuilder.length() > 0)
                            {
                                stringbuilder.append("\n");
                            }

                            stringbuilder.append("... and ").append(serverstatusresponse.getPlayerCountData().getOnlinePlayerCount() - serverstatusresponse.getPlayerCountData().getPlayers().length).append(" more ...");
                        }

                        server.playerList = stringbuilder.toString();
                    }
                }
                else
                {
                    server.populationInfo = EnumChatFormatting.DARK_GRAY + "???";
                }

                if (serverstatusresponse.getFavicon() != null)
                {
                    String s = serverstatusresponse.getFavicon();

                    if (s.startsWith("data:image/png;base64,"))
                    {
                        server.setBase64EncodedIconData(s.substring("data:image/png;base64,".length()));
                    }
                    else
                    {
                        OldServerPinger.logger.error("Invalid server icon (unknown format)");
                    }
                }
                else
                {
                    server.setBase64EncodedIconData((String)null);
                }

                FMLClientHandler.instance().bindServerListData(server, serverstatusresponse);
                networkmanager.scheduleOutboundPacket(new C01PacketPing(Minecraft.getSystemTime()), new GenericFutureListener[0]);
                this.field_147403_d = true;
            }
            public void handlePong(S01PacketPong packetIn)
            {
                long i = packetIn.func_149292_c();
                long j = Minecraft.getSystemTime();
                server.pingToServer = j - i;
                networkmanager.closeChannel(new ChatComponentText("Finished"));
            }
            /**
             * Invoked when disconnecting, the parameter is a ChatComponent describing the reason for termination
             */
            public void onDisconnect(IChatComponent reason)
            {
                if (!this.field_147403_d)
                {
                    OldServerPinger.logger.error("Can\'t ping " + server.serverIP + ": " + reason.getUnformattedText());
                    server.serverMOTD = EnumChatFormatting.DARK_RED + "Can\'t connect to server.";
                    server.populationInfo = "";
                    OldServerPinger.this.tryCompatibilityPing(server);
                }
            }
            /**
             * Allows validation of the connection state transition. Parameters: from, to (connection state). Typically
             * throws IllegalStateException or UnsupportedOperationException if validation fails
             */
            public void onConnectionStateTransition(EnumConnectionState oldState, EnumConnectionState newState)
            {
                if (newState != EnumConnectionState.STATUS)
                {
                    throw new UnsupportedOperationException("Unexpected change in protocol to " + newState);
                }
            }
            /**
             * For scheduled network tasks. Used in NetHandlerPlayServer to send keep-alive packets and in
             * NetHandlerLoginServer for a login-timeout
             */
            public void onNetworkTick() {}
        });

        try
        {
            networkmanager.scheduleOutboundPacket(new C00Handshake(5, serveraddress.getIP(), serveraddress.getPort(), EnumConnectionState.STATUS), new GenericFutureListener[0]);
            networkmanager.scheduleOutboundPacket(new C00PacketServerQuery(), new GenericFutureListener[0]);
        }
        catch (Throwable throwable)
        {
            logger.error(throwable);
        }
    }

    private void tryCompatibilityPing(final ServerData server)
    {
        final ServerAddress serveraddress = ServerAddress.func_78860_a(server.serverIP);
        ((Bootstrap)((Bootstrap)((Bootstrap)(new Bootstrap()).group(NetworkManager.eventLoops)).handler(new ChannelInitializer()
        {
            private static final String __OBFID = "CL_00000894";
            protected void initChannel(Channel p_initChannel_1_)
            {
                try
                {
                    p_initChannel_1_.config().setOption(ChannelOption.IP_TOS, Integer.valueOf(24));
                }
                catch (ChannelException channelexception1)
                {
                    ;
                }

                try
                {
                    p_initChannel_1_.config().setOption(ChannelOption.TCP_NODELAY, Boolean.valueOf(false));
                }
                catch (ChannelException channelexception)
                {
                    ;
                }

                p_initChannel_1_.pipeline().addLast(new ChannelHandler[] {new SimpleChannelInboundHandler()
                {
                    private static final String __OBFID = "CL_00000895";
                    public void channelActive(ChannelHandlerContext p_channelActive_1_) throws Exception
                    {
                        super.channelActive(p_channelActive_1_);
                        ByteBuf bytebuf = Unpooled.buffer();

                        try
                        {
                            bytebuf.writeByte(254);
                            bytebuf.writeByte(1);
                            bytebuf.writeByte(250);
                            char[] achar = "MC|PingHost".toCharArray();
                            bytebuf.writeShort(achar.length);
                            char[] achar1 = achar;
                            int i = achar.length;
                            int j;
                            char c0;

                            for (j = 0; j < i; ++j)
                            {
                                c0 = achar1[j];
                                bytebuf.writeChar(c0);
                            }

                            bytebuf.writeShort(7 + 2 * serveraddress.getIP().length());
                            bytebuf.writeByte(127);
                            achar = serveraddress.getIP().toCharArray();
                            bytebuf.writeShort(achar.length);
                            achar1 = achar;
                            i = achar.length;

                            for (j = 0; j < i; ++j)
                            {
                                c0 = achar1[j];
                                bytebuf.writeChar(c0);
                            }

                            bytebuf.writeInt(serveraddress.getPort());
                            p_channelActive_1_.channel().writeAndFlush(bytebuf).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                        }
                        finally
                        {
                            bytebuf.release();
                        }
                    }
                    protected void channelRead0(ChannelHandlerContext p_channelRead0_1_, ByteBuf p_channelRead0_2_)
                    {
                        short short1 = p_channelRead0_2_.readUnsignedByte();

                        if (short1 == 255)
                        {
                            String s = new String(p_channelRead0_2_.readBytes(p_channelRead0_2_.readShort() * 2).array(), Charsets.UTF_16BE);
                            String[] astring = (String[])Iterables.toArray(OldServerPinger.PING_RESPONSE_SPLITTER.split(s), String.class);

                            if ("\u00a71".equals(astring[0]))
                            {
                                int i = MathHelper.parseIntWithDefault(astring[1], 0);
                                String s1 = astring[2];
                                String s2 = astring[3];
                                int j = MathHelper.parseIntWithDefault(astring[4], -1);
                                int k = MathHelper.parseIntWithDefault(astring[5], -1);
                                server.version = -1;
                                server.gameVersion = s1;
                                server.serverMOTD = s2;
                                server.populationInfo = EnumChatFormatting.GRAY + "" + j + "" + EnumChatFormatting.DARK_GRAY + "/" + EnumChatFormatting.GRAY + k;
                            }
                        }

                        p_channelRead0_1_.close();
                    }
                    public void exceptionCaught(ChannelHandlerContext p_exceptionCaught_1_, Throwable p_exceptionCaught_2_)
                    {
                        p_exceptionCaught_1_.close();
                    }
                    protected void channelRead0(ChannelHandlerContext p_channelRead0_1_, Object p_channelRead0_2_)
                    {
                        this.channelRead0(p_channelRead0_1_, (ByteBuf)p_channelRead0_2_);
                    }
                }
                                                                         });
            }
        })).channel(NioSocketChannel.class)).connect(serveraddress.getIP(), serveraddress.getPort());
    }

    public void pingPendingNetworks()
    {
        List list = this.pingDestinations;

        synchronized (this.pingDestinations)
        {
            Iterator iterator = this.pingDestinations.iterator();

            while (iterator.hasNext())
            {
                NetworkManager networkmanager = (NetworkManager)iterator.next();

                if (networkmanager.isChannelOpen())
                {
                    networkmanager.processReceivedPackets();
                }
                else
                {
                    iterator.remove();

                    if (networkmanager.getExitMessage() != null)
                    {
                        networkmanager.getNetHandler().onDisconnect(networkmanager.getExitMessage());
                    }
                }
            }
        }
    }

    public void clearPendingNetworks()
    {
        List list = this.pingDestinations;

        synchronized (this.pingDestinations)
        {
            Iterator iterator = this.pingDestinations.iterator();

            while (iterator.hasNext())
            {
                NetworkManager networkmanager = (NetworkManager)iterator.next();

                if (networkmanager.isChannelOpen())
                {
                    iterator.remove();
                    networkmanager.closeChannel(new ChatComponentText("Cancelled"));
                }
            }
        }
    }
}