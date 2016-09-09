package net.minecraft.server.dedicated;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommand;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.rcon.IServer;
import net.minecraft.network.rcon.RConThreadMain;
import net.minecraft.network.rcon.RConThreadQuery;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerEula;
import net.minecraft.server.gui.MinecraftServerGui;
import net.minecraft.server.management.PreYggdrasilConverter;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.CryptManager;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SideOnly(Side.SERVER)
public class DedicatedServer extends MinecraftServer implements IServer
{
    private static final Logger logger = LogManager.getLogger();
    public final List pendingCommandList = Collections.synchronizedList(new ArrayList());
    private RConThreadQuery theRConThreadQuery;
    private RConThreadMain theRConThreadMain;
    private PropertyManager settings;
    private ServerEula eula;
    private boolean canSpawnStructures;
    private WorldSettings.GameType gameType;
    private boolean guiIsEnabled;
    public static boolean allowPlayerLogins = false;
    private static final String __OBFID = "CL_00001784";

    public DedicatedServer(File workDir)
    {
        super(workDir, Proxy.NO_PROXY);
        Thread thread = new Thread("Server Infinisleeper")
        {
            private static final String __OBFID = "CL_00001787";
            {
                this.setDaemon(true);
                this.start();
            }
            public void run()
            {
                while (true)
                {
                    try
                    {
                        while (true)
                        {
                            Thread.sleep(2147483647L);
                        }
                    }
                    catch (InterruptedException interruptedexception)
                    {
                        ;
                    }
                }
            }
        };
    }

    /**
     * Initialises the server and starts it.
     */
    protected boolean startServer() throws IOException
    {
        Thread thread = new Thread("Server console handler")
        {
            private static final String __OBFID = "CL_00001786";
            public void run()
            {
                BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(System.in));
                String s4;

                try
                {
                    while (!DedicatedServer.this.isServerStopped() && DedicatedServer.this.isServerRunning() && (s4 = bufferedreader.readLine()) != null)
                    {
                        DedicatedServer.this.addPendingCommand(s4, DedicatedServer.this);
                    }
                }
                catch (IOException ioexception1)
                {
                    DedicatedServer.logger.error("Exception handling console input", ioexception1);
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        logger.info("Starting minecraft server version 1.7.10");

        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L)
        {
            logger.warn("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }

        FMLCommonHandler.instance().onServerStart(this);

        logger.info("Loading properties");
        this.settings = new PropertyManager(new File("server.properties"));
        this.eula = new ServerEula(new File("eula.txt"));

        if (!this.eula.hasAcceptedEULA())
        {
            logger.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
            this.eula.createEULAFile();
            return false;
        }
        else
        {
            if (this.isSinglePlayer())
            {
                this.setHostname("127.0.0.1");
            }
            else
            {
                this.setOnlineMode(this.settings.getBooleanProperty("online-mode", true));
                this.setHostname(this.settings.getStringProperty("server-ip", ""));
            }

            this.setCanSpawnAnimals(this.settings.getBooleanProperty("spawn-animals", true));
            this.setCanSpawnNPCs(this.settings.getBooleanProperty("spawn-npcs", true));
            this.setAllowPvp(this.settings.getBooleanProperty("pvp", true));
            this.setAllowFlight(this.settings.getBooleanProperty("allow-flight", false));
            this.setServerResourcePack(this.settings.getStringProperty("resource-pack", ""));
            this.setMOTD(this.settings.getStringProperty("motd", "A Minecraft Server"));
            this.setForceGamemode(this.settings.getBooleanProperty("force-gamemode", false));
            this.setPlayerIdleTimeout(this.settings.getIntProperty("player-idle-timeout", 0));

            if (this.settings.getIntProperty("difficulty", 1) < 0)
            {
                this.settings.setProperty("difficulty", Integer.valueOf(0));
            }
            else if (this.settings.getIntProperty("difficulty", 1) > 3)
            {
                this.settings.setProperty("difficulty", Integer.valueOf(3));
            }

            this.canSpawnStructures = this.settings.getBooleanProperty("generate-structures", true);
            int i = this.settings.getIntProperty("gamemode", WorldSettings.GameType.SURVIVAL.getID());
            this.gameType = WorldSettings.getGameTypeById(i);
            logger.info("Default game type: " + this.gameType);
            InetAddress inetaddress = null;

            if (this.getServerHostname().length() > 0)
            {
                inetaddress = InetAddress.getByName(this.getServerHostname());
            }

            if (this.getServerPort() < 0)
            {
                this.setServerPort(this.settings.getIntProperty("server-port", 25565));
            }

            logger.info("Generating keypair");
            this.setKeyPair(CryptManager.generateKeyPair());
            logger.info("Starting Minecraft server on " + (this.getServerHostname().length() == 0 ? "*" : this.getServerHostname()) + ":" + this.getServerPort());

            try
            {
                this.getNetworkSystem().addLanEndpoint(inetaddress, this.getServerPort());
            }
            catch (IOException ioexception)
            {
                logger.warn("**** FAILED TO BIND TO PORT!");
                logger.warn("The exception was: {}", new Object[] {ioexception.toString()});
                logger.warn("Perhaps a server is already running on that port?");
                return false;
            }

            if (!this.isServerInOnlineMode())
            {
                logger.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
                logger.warn("The server will make no attempt to authenticate usernames. Beware.");
                logger.warn("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
                logger.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
            }

            if (this.convertFiles())
            {
                this.getPlayerProfileCache().func_152658_c();
            }

            if (!PreYggdrasilConverter.tryConvert(this.settings))
            {
                return false;
            }
            else
            {
                FMLCommonHandler.instance().onServerStarted();
                this.setConfigManager(new DedicatedPlayerList(this));
                long j = System.nanoTime();

                if (this.getFolderName() == null)
                {
                    this.setFolderName(this.settings.getStringProperty("level-name", "world"));
                }

                String s = this.settings.getStringProperty("level-seed", "");
                String s1 = this.settings.getStringProperty("level-type", "DEFAULT");
                String s2 = this.settings.getStringProperty("generator-settings", "");
                long k = (new Random()).nextLong();

                if (s.length() > 0)
                {
                    try
                    {
                        long l = Long.parseLong(s);

                        if (l != 0L)
                        {
                            k = l;
                        }
                    }
                    catch (NumberFormatException numberformatexception)
                    {
                        k = (long)s.hashCode();
                    }
                }

                WorldType worldtype = WorldType.parseWorldType(s1);

                if (worldtype == null)
                {
                    worldtype = WorldType.DEFAULT;
                }

                this.isAnnouncingPlayerAchievements();
                this.isCommandBlockEnabled();
                this.getOpPermissionLevel();
                this.isSnooperEnabled();
                this.setBuildLimit(this.settings.getIntProperty("max-build-height", 256));
                this.setBuildLimit((this.getBuildLimit() + 8) / 16 * 16);
                this.setBuildLimit(MathHelper.clamp_int(this.getBuildLimit(), 64, 256));
                this.settings.setProperty("max-build-height", Integer.valueOf(this.getBuildLimit()));
                if (!FMLCommonHandler.instance().handleServerAboutToStart(this)) { return false; }
                logger.info("Preparing level \"" + this.getFolderName() + "\"");
                this.loadAllWorlds(this.getFolderName(), this.getFolderName(), k, worldtype, s2);
                long i1 = System.nanoTime() - j;
                String s3 = String.format("%.3fs", new Object[] {Double.valueOf((double)i1 / 1.0E9D)});
                logger.info("Done (" + s3 + ")! For help, type \"help\" or \"?\"");

                if (this.settings.getBooleanProperty("enable-query", false))
                {
                    logger.info("Starting GS4 status listener");
                    this.theRConThreadQuery = new RConThreadQuery(this);
                    this.theRConThreadQuery.startThread();
                }

                if (this.settings.getBooleanProperty("enable-rcon", false))
                {
                    logger.info("Starting remote control listener");
                    this.theRConThreadMain = new RConThreadMain(this);
                    this.theRConThreadMain.startThread();
                }

                return FMLCommonHandler.instance().handleServerStarting(this);
            }
        }
    }

    public boolean canStructuresSpawn()
    {
        return this.canSpawnStructures;
    }

    public WorldSettings.GameType getGameType()
    {
        return this.gameType;
    }

    /**
     * Get the server's difficulty
     */
    public EnumDifficulty getDifficulty()
    {
        return EnumDifficulty.getDifficultyEnum(this.settings.getIntProperty("difficulty", 1));
    }

    /**
     * Defaults to false.
     */
    public boolean isHardcore()
    {
        return this.settings.getBooleanProperty("hardcore", false);
    }

    /**
     * Called on exit from the main run() loop.
     */
    protected void finalTick(CrashReport report) {}

    /**
     * Adds the server info, including from theWorldServer, to the crash report.
     */
    public CrashReport addServerInfoToCrashReport(CrashReport report)
    {
        report = super.addServerInfoToCrashReport(report);
        report.getCategory().addCrashSectionCallable("Is Modded", new Callable()
        {
            private static final String __OBFID = "CL_00001785";
            public String call()
            {
                String s = DedicatedServer.this.getServerModName();
                return !s.equals("vanilla") ? "Definitely; Server brand changed to \'" + s + "\'" : "Unknown (can\'t tell)";
            }
        });
        report.getCategory().addCrashSectionCallable("Type", new Callable()
        {
            private static final String __OBFID = "CL_00001788";
            public String call()
            {
                return "Dedicated Server (map_server.txt)";
            }
        });
        return report;
    }

    /**
     * Directly calls System.exit(0), instantly killing the program.
     */
    protected void systemExitNow()
    {
        System.exit(0);
    }

    public void updateTimeLightAndEntities()
    {
        super.updateTimeLightAndEntities();
        this.executePendingCommands();
    }

    public boolean getAllowNether()
    {
        return this.settings.getBooleanProperty("allow-nether", true);
    }

    public boolean allowSpawnMonsters()
    {
        return this.settings.getBooleanProperty("spawn-monsters", true);
    }

    public void addServerStatsToSnooper(PlayerUsageSnooper playerSnooper)
    {
        playerSnooper.addClientStat("whitelist_enabled", Boolean.valueOf(this.getConfigurationManager().isWhiteListEnabled()));
        playerSnooper.addClientStat("whitelist_count", Integer.valueOf(this.getConfigurationManager().getWhitelistedPlayerNames().length));
        super.addServerStatsToSnooper(playerSnooper);
    }

    /**
     * Returns whether snooping is enabled or not.
     */
    public boolean isSnooperEnabled()
    {
        return this.settings.getBooleanProperty("snooper-enabled", true);
    }

    public void addPendingCommand(String input, ICommandSender sender)
    {
        this.pendingCommandList.add(new ServerCommand(input, sender));
    }

    public void executePendingCommands()
    {
        while (!this.pendingCommandList.isEmpty())
        {
            ServerCommand servercommand = (ServerCommand)this.pendingCommandList.remove(0);
            this.getCommandManager().executeCommand(servercommand.sender, servercommand.command);
        }
    }

    public boolean isDedicatedServer()
    {
        return true;
    }

    public DedicatedPlayerList getConfigurationManager()
    {
        return (DedicatedPlayerList)super.getConfigurationManager();
    }

    /**
     * Gets an integer property. If it does not exist, set it to the specified value.
     */
    public int getIntProperty(String key, int defaultValue)
    {
        return this.settings.getIntProperty(key, defaultValue);
    }

    /**
     * Gets a string property. If it does not exist, set it to the specified value.
     */
    public String getStringProperty(String key, String defaultValue)
    {
        return this.settings.getStringProperty(key, defaultValue);
    }

    /**
     * Gets a boolean property. If it does not exist, set it to the specified value.
     */
    public boolean getBooleanProperty(String key, boolean defaultValue)
    {
        return this.settings.getBooleanProperty(key, defaultValue);
    }

    /**
     * Saves an Object with the given property name.
     */
    public void setProperty(String key, Object value)
    {
        this.settings.setProperty(key, value);
    }

    /**
     * Saves all of the server properties to the properties file.
     */
    public void saveProperties()
    {
        this.settings.saveProperties();
    }

    /**
     * Returns the filename where server properties are stored
     */
    public String getSettingsFilename()
    {
        File file1 = this.settings.getPropertiesFile();
        return file1 != null ? file1.getAbsolutePath() : "No settings file";
    }

    public void setGuiEnabled()
    {
        MinecraftServerGui.createServerGui(this);
        this.guiIsEnabled = true;
    }

    public boolean getGuiEnabled()
    {
        return this.guiIsEnabled;
    }

    /**
     * On dedicated does nothing. On integrated, sets commandsAllowedForAll, gameType and allows external connections.
     */
    public String shareToLAN(WorldSettings.GameType type, boolean allowCheats)
    {
        return "";
    }

    /**
     * Return whether command blocks are enabled.
     */
    public boolean isCommandBlockEnabled()
    {
        return this.settings.getBooleanProperty("enable-command-block", false);
    }

    /**
     * Return the spawn protection area's size.
     */
    public int getSpawnProtectionSize()
    {
        return this.settings.getIntProperty("spawn-protection", super.getSpawnProtectionSize());
    }

    /**
     * Returns true if a player does not have permission to edit the block at the given coordinates.
     */
    public boolean isBlockProtected(World inWorld, int x, int y, int z, EntityPlayer player)
    {
        if (inWorld.provider.dimensionId != 0)
        {
            return false;
        }
        else if (this.getConfigurationManager().getOppedPlayers().hasEntries())
        {
            return false;
        }
        else if (this.getConfigurationManager().canSendCommands(player.getGameProfile()))
        {
            return false;
        }
        else if (this.getSpawnProtectionSize() <= 0)
        {
            return false;
        }
        else
        {
            ChunkCoordinates chunkcoordinates = inWorld.getSpawnPoint();
            int l = MathHelper.abs_int(x - chunkcoordinates.posX);
            int i1 = MathHelper.abs_int(z - chunkcoordinates.posZ);
            int j1 = Math.max(l, i1);
            return j1 <= this.getSpawnProtectionSize();
        }
    }

    public int getOpPermissionLevel()
    {
        return this.settings.getIntProperty("op-permission-level", 4);
    }

    public void setPlayerIdleTimeout(int idleTimeout)
    {
        super.setPlayerIdleTimeout(idleTimeout);
        this.settings.setProperty("player-idle-timeout", Integer.valueOf(idleTimeout));
        this.saveProperties();
    }

    public boolean func_152363_m()
    {
        return this.settings.getBooleanProperty("broadcast-rcon-to-ops", true);
    }

    public boolean isAnnouncingPlayerAchievements()
    {
        return this.settings.getBooleanProperty("announce-player-achievements", true);
    }

    protected boolean convertFiles() throws IOException
    {
        boolean flag = false;
        int i;

        for (i = 0; !flag && i <= 2; ++i)
        {
            if (i > 0)
            {
                logger.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
                this.sleepFiveSeconds();
            }

            flag = PreYggdrasilConverter.convertUserBanlist(this);
        }

        boolean flag1 = false;

        for (i = 0; !flag1 && i <= 2; ++i)
        {
            if (i > 0)
            {
                logger.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
                this.sleepFiveSeconds();
            }

            flag1 = PreYggdrasilConverter.convertIpBanlist(this);
        }

        boolean flag2 = false;

        for (i = 0; !flag2 && i <= 2; ++i)
        {
            if (i > 0)
            {
                logger.warn("Encountered a problem while converting the op list, retrying in a few seconds");
                this.sleepFiveSeconds();
            }

            flag2 = PreYggdrasilConverter.convertOplist(this);
        }

        boolean flag3 = false;

        for (i = 0; !flag3 && i <= 2; ++i)
        {
            if (i > 0)
            {
                logger.warn("Encountered a problem while converting the whitelist, retrying in a few seconds");
                this.sleepFiveSeconds();
            }

            flag3 = PreYggdrasilConverter.convertWhitelist(this);
        }

        boolean flag4 = false;

        for (i = 0; !flag4 && i <= 2; ++i)
        {
            if (i > 0)
            {
                logger.warn("Encountered a problem while converting the player save files, retrying in a few seconds");
                this.sleepFiveSeconds();
            }

            flag4 = PreYggdrasilConverter.convertSaveFiles(this, this.settings);
        }

        return flag || flag1 || flag2 || flag3 || flag4;
    }

    private void sleepFiveSeconds()
    {
        try
        {
            Thread.sleep(5000L);
        }
        catch (InterruptedException interruptedexception)
        {
            ;
        }
    }
}