package com.nickimpact.GTS;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.nickimpact.GTS.commands.*;
import com.nickimpact.GTS.configuration.Config;
import com.nickimpact.GTS.configuration.MessageConfig;
import com.nickimpact.GTS.listeners.JoinListener;
import com.nickimpact.GTS.storage.H2Provider;
import com.nickimpact.GTS.storage.MySQLProvider;
import com.nickimpact.GTS.storage.SQLDatabase;
import com.nickimpact.GTS.utils.LotCache;
import com.nickimpact.GTS.utils.LotUtils;
import com.nickimpact.GTS.utils.UpdateLotsTask;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static com.nickimpact.GTS.GTSInfo.*;

@Plugin(id=ID, name=NAME, version=VERSION, description=DESCRIPTION)
public class GTS {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot=false)
    private Path configDir;

    private Config config;
    private MessageConfig messageConfig;

    private static GTS plugin;
    private SQLDatabase sql;

    private EconomyService economy;

    private List<UUID> ignoreList = Lists.newArrayList();

    private List<LotCache> lots = Lists.newArrayList();
    private boolean enabled = true;

    @Listener
    public void onInitialization(GameInitializationEvent e){
        plugin = this;

        GTSInfo.startup();
        getConsole().sendMessage(Text.of(PREFIX, TextColors.DARK_AQUA, "Checking for dependencies..."));
        enabled = GTSInfo.dependencyCheck();

        if(enabled) {
            getConsole().sendMessage(Text.of(PREFIX, TextColors.DARK_AQUA, "Loading configuration..."));
            this.config = new Config();
            getConsole().sendMessage(Text.of(PREFIX, TextColors.DARK_AQUA, "Loading message configuration..."));
            this.messageConfig = new MessageConfig();

            getConsole().sendMessage(Text.of(PREFIX, TextColors.DARK_AQUA, "Registering commands..."));
            Sponge.getCommandManager().register(this, CommandSpec.builder()
                    .permission("gts.command.gts")
                    .executor(new GTSCommand())
                    .description(Text.of("Opens the GUI displaying all GTS listings"))
                    .child(AdditionCommand.registerCommand(), "add")
                    .child(SearchCommand.registerCommand(), "search")
                    .child(AuctionCommand.registerCommand(), "auc")
                    .child(LogCmd.registerCommand(), "logs")
                    .child(IgnoreCmd.registerCommand(), "ignore")
                    .child(CommandSpec.builder()
                            .executor(new ReloadCommand())
                            .permission("gts.admin.command.reload")
                            .description(Text.of("Reload the plugins configuration"))
                            .build(), "reload")
                    .child(CommandSpec.builder()
                            .executor(new ClearCommand())
                            .permission("gts.admin.command.clear")
                            .description(Text.of("Clear all listings in the GTS"))
                            .build(), "clear")
                    .child(CommandSpec.builder()
                            .executor(new EditCommand())
                            .permission("gts.admin.command.edit")
                            .description(Text.of("Edit listings in the GTS"))
                            .build(), "edit")
                    .child(CommandSpec.builder()
                            .executor(new HelpCommand())
                            .permission("gts.command.help")
                            .description(Text.of("Receive help info on GTS"))
                            .build(), "help")
                    .build(), "gts");

            getConsole().sendMessage(Text.of(PREFIX, TextColors.DARK_AQUA, "Registering listeners..."));
            Sponge.getEventManager().registerListeners(this, new JoinListener());

            getConsole().sendMessage(Text.of(PREFIX, TextColors.DARK_AQUA, "Initializing storage provider..."));
            if(config.getDatabaseType().equalsIgnoreCase("H2")){
                this.sql = new H2Provider(GTS.getInstance().getConfig().getMainTable(), GTS.getInstance().getConfig().getLogTable());
            } else if(config.getDatabaseType().equalsIgnoreCase("MySQL")){
                this.sql = new MySQLProvider(GTS.getInstance().getConfig().getMainTable(), GTS.getInstance().getConfig().getLogTable());
            } else {
                getConsole().sendMessage(Text.of(ERROR_PREFIX, TextColors.RED, "Database type invalid, defaulting to H2"));

                this.sql = new H2Provider(GTS.getInstance().getConfig().getMainTable(), GTS.getInstance().getConfig().getLogTable());
            }
            this.sql.createTables();
            this.sql.updateTables();

            getConsole().sendMessage(Text.of(PREFIX, TextColors.DARK_AQUA, "Caching listings..."));
            this.lots = this.sql.getAllLots();

            int max = -1;
            for(LotCache lot : this.lots){
                if(lot.getLot().getLotID() > max){
                    max = lot.getLot().getLotID();
                }
            }
            LotUtils.setPlacement(max + 1);

            getConsole().sendMessage(Text.of(PREFIX, TextColors.DARK_AQUA, "Successfully loaded"));
        } else {
            getConsole().sendMessage(Text.of(ERROR_PREFIX, Text.of(TextColors.DARK_RED, "All plugin functions will be disabled...")));
        }
    }

    @Listener
    public void onServerStart(GameStartedServerEvent e){
        if(enabled) {
            UpdateLotsTask.setupUpdateTask();
            UpdateLotsTask.saveTask();
        }
    }

    @Listener
    public void onReload(GameReloadEvent e){
        if(enabled){
            this.config = new Config();
            this.messageConfig = new MessageConfig();
            getLogger().info(Text.of(TextColors.YELLOW, "Configurations have been reloaded").toPlain());
        }
    }

    @Listener
    public void registerEconomyService(ChangeServiceProviderEvent e){
        if(e.getService().equals(EconomyService.class)){
            economy = (EconomyService) e.getNewProviderRegistration().getProvider();
        }
    }

    @Listener
    public void onServerStop(GameStoppingEvent e){
        if(enabled) {
            getLogger().info("Thanks for using " + NAME + " (" + VERSION + ")");
            try {
                sql.updateLots(LotUtils.getSqlCmds());
                sql.shutdown();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else {
            getLogger().info(Text.of(TextColors.RED, "Remember to fix the issue that came about earlier on startup!").toPlain());
        }
    }

    public Logger getLogger(){
        return this.logger;
    }

    public ConsoleSource getConsole(){
        return Sponge.getServer().getConsole();
    }

    public SQLDatabase getSql(){
        return this.sql;
    }

    public static GTS getInstance(){
        return plugin;
    }

    public Config getConfig(){
        return config;
    }

    public Path getConfigDir(){
        return configDir;
    }

    public EconomyService getEconomy(){
        return this.economy;
    }

    public boolean isPluginEnabled(){
        return this.enabled;
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public List<LotCache> getLots() {
        return lots;
    }

    public List<UUID> getIgnoreList() {
        return ignoreList;
    }
}
