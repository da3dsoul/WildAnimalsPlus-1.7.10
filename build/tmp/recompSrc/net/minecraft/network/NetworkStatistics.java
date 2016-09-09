package net.minecraft.network;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class NetworkStatistics
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker NETSTAT_MARKER = MarkerManager.getMarker("NETSTAT_MARKER", NetworkManager.logMarkerStat);
    private NetworkStatistics.Tracker field_152480_c = new NetworkStatistics.Tracker();
    private NetworkStatistics.Tracker field_152481_d = new NetworkStatistics.Tracker();
    private static final String __OBFID = "CL_00001897";

    public void func_152469_a(int p_152469_1_, long p_152469_2_)
    {
        this.field_152480_c.func_152488_a(p_152469_1_, p_152469_2_);
    }

    public void func_152464_b(int p_152464_1_, long p_152464_2_)
    {
        this.field_152481_d.func_152488_a(p_152464_1_, p_152464_2_);
    }

    public long func_152465_a()
    {
        return this.field_152480_c.func_152485_a();
    }

    public long func_152471_b()
    {
        return this.field_152481_d.func_152485_a();
    }

    public long func_152472_c()
    {
        return this.field_152480_c.func_152489_b();
    }

    public long func_152473_d()
    {
        return this.field_152481_d.func_152489_b();
    }

    public NetworkStatistics.PacketStat func_152477_e()
    {
        return this.field_152480_c.func_152484_c();
    }

    public NetworkStatistics.PacketStat func_152467_f()
    {
        return this.field_152480_c.func_152486_d();
    }

    public NetworkStatistics.PacketStat func_152475_g()
    {
        return this.field_152481_d.func_152484_c();
    }

    public NetworkStatistics.PacketStat func_152470_h()
    {
        return this.field_152481_d.func_152486_d();
    }

    public NetworkStatistics.PacketStat func_152466_a(int p_152466_1_)
    {
        return this.field_152480_c.func_152487_a(p_152466_1_);
    }

    public NetworkStatistics.PacketStat func_152468_b(int p_152468_1_)
    {
        return this.field_152481_d.func_152487_a(p_152468_1_);
    }

    public static class PacketStat
        {
            private final int packetId;
            private final NetworkStatistics.PacketStatData data;
            private static final String __OBFID = "CL_00001895";

            public PacketStat(int id, NetworkStatistics.PacketStatData statData)
            {
                this.packetId = id;
                this.data = statData;
            }

            public String toString()
            {
                return "PacketStat(" + this.packetId + ")" + this.data;
            }
        }

    static class PacketStatData
        {
            private final long totalBytes;
            private final int count;
            private final double averageBytes;
            private static final String __OBFID = "CL_00001893";

            private PacketStatData(long p_i1184_1_, int p_i1184_3_, double p_i1184_4_)
            {
                this.totalBytes = p_i1184_1_;
                this.count = p_i1184_3_;
                this.averageBytes = p_i1184_4_;
            }

            public NetworkStatistics.PacketStatData func_152494_a(long p_152494_1_)
            {
                return new NetworkStatistics.PacketStatData(p_152494_1_ + this.totalBytes, this.count + 1, (double)((p_152494_1_ + this.totalBytes) / (long)(this.count + 1)));
            }

            public long getTotalBytes()
            {
                return this.totalBytes;
            }

            public int getCount()
            {
                return this.count;
            }

            public String toString()
            {
                return "{totalBytes=" + this.totalBytes + ", count=" + this.count + ", averageBytes=" + this.averageBytes + '}';
            }

            PacketStatData(long p_i1185_1_, int p_i1185_3_, double p_i1185_4_, Object p_i1185_6_)
            {
                this(p_i1185_1_, p_i1185_3_, p_i1185_4_);
            }
        }

    static class Tracker
        {
            private AtomicReference[] field_152490_a = new AtomicReference[100];
            private static final String __OBFID = "CL_00001894";

            public Tracker()
            {
                for (int i = 0; i < 100; ++i)
                {
                    this.field_152490_a[i] = new AtomicReference(new NetworkStatistics.PacketStatData(0L, 0, 0.0D, null));
                }
            }

            public void func_152488_a(int p_152488_1_, long p_152488_2_)
            {
                try
                {
                    if (p_152488_1_ < 0 || p_152488_1_ >= 100)
                    {
                        return;
                    }

                    NetworkStatistics.PacketStatData packetstatdata;
                    NetworkStatistics.PacketStatData packetstatdata1;

                    do
                    {
                        packetstatdata = (NetworkStatistics.PacketStatData)this.field_152490_a[p_152488_1_].get();
                        packetstatdata1 = packetstatdata.func_152494_a(p_152488_2_);
                    }
                    while (!this.field_152490_a[p_152488_1_].compareAndSet(packetstatdata, packetstatdata1));
                }
                catch (Exception exception)
                {
                    if (NetworkStatistics.LOGGER.isDebugEnabled())
                    {
                        NetworkStatistics.LOGGER.debug(NetworkStatistics.NETSTAT_MARKER, "NetStat failed with packetId: " + p_152488_1_, exception);
                    }
                }
            }

            public long func_152485_a()
            {
                long i = 0L;

                for (int j = 0; j < 100; ++j)
                {
                    i += ((NetworkStatistics.PacketStatData)this.field_152490_a[j].get()).getTotalBytes();
                }

                return i;
            }

            public long func_152489_b()
            {
                long i = 0L;

                for (int j = 0; j < 100; ++j)
                {
                    i += (long)((NetworkStatistics.PacketStatData)this.field_152490_a[j].get()).getCount();
                }

                return i;
            }

            public NetworkStatistics.PacketStat func_152484_c()
            {
                int i = -1;
                NetworkStatistics.PacketStatData packetstatdata = new NetworkStatistics.PacketStatData(-1L, -1, 0.0D, null);

                for (int j = 0; j < 100; ++j)
                {
                    NetworkStatistics.PacketStatData packetstatdata1 = (NetworkStatistics.PacketStatData)this.field_152490_a[j].get();

                    if (packetstatdata1.totalBytes > packetstatdata.totalBytes)
                    {
                        i = j;
                        packetstatdata = packetstatdata1;
                    }
                }

                return new NetworkStatistics.PacketStat(i, packetstatdata);
            }

            public NetworkStatistics.PacketStat func_152486_d()
            {
                int i = -1;
                NetworkStatistics.PacketStatData packetstatdata = new NetworkStatistics.PacketStatData(-1L, -1, 0.0D, null);

                for (int j = 0; j < 100; ++j)
                {
                    NetworkStatistics.PacketStatData packetstatdata1 = (NetworkStatistics.PacketStatData)this.field_152490_a[j].get();

                    if (packetstatdata1.count > packetstatdata.count)
                    {
                        i = j;
                        packetstatdata = packetstatdata1;
                    }
                }

                return new NetworkStatistics.PacketStat(i, packetstatdata);
            }

            public NetworkStatistics.PacketStat func_152487_a(int p_152487_1_)
            {
                return p_152487_1_ >= 0 && p_152487_1_ < 100 ? new NetworkStatistics.PacketStat(p_152487_1_, (NetworkStatistics.PacketStatData)this.field_152490_a[p_152487_1_].get()) : null;
            }
        }
}