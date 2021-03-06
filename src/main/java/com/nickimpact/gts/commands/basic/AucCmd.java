package com.nickimpact.gts.commands.basic;

import com.google.common.collect.Lists;
import com.nickimpact.gts.GTS;
import com.nickimpact.gts.api.GtsService;
import com.nickimpact.gts.api.json.Registry;
import com.nickimpact.gts.api.listings.entries.Entry;
import com.nickimpact.gts.api.utils.MessageUtils;
import com.nickimpact.impactor.api.commands.SpongeCommand;
import com.nickimpact.impactor.api.commands.SpongeSubCommand;
import com.nickimpact.impactor.api.commands.annotations.Aliases;
import com.nickimpact.impactor.api.plugins.SpongePlugin;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import java.util.List;

@Aliases({"auc", "auction"})
public class AucCmd extends SpongeSubCommand {

	private static List<SpongeSubCommand> children = Lists.newArrayList();

	public AucCmd(SpongePlugin plugin) {
		super(plugin);
	}

	@Override
	public CommandElement[] getArgs() {
		return new CommandElement[0];
	}

	@Override
	public Text getDescription() {
		return Text.of("Auction listings");
	}

	@Override
	public Text getUsage() {
		return Text.of("/gts auc <type>");
	}

	@Override
	public SpongeCommand[] getSubCommands() {
		getEntryCommandSpecs(children, true);
		return children.toArray(new SpongeSubCommand[children.size()]);
	}

	@SuppressWarnings("unchecked")
	static void getEntryCommandSpecs(List<SpongeSubCommand> children, boolean isAuction) {
		((Registry<Entry>) GTS.getInstance().getService().getRegistry(GtsService.RegistryType.ENTRY)).getTypings().forEach((key, value) -> {
			try {
				Entry entry = value.newInstance();
				if (entry.commandSpec(isAuction) == null) {
					MessageUtils.genAndSendErrorMessage(
							"Invalid Entry Command Spec",
							"Command Spec is null",
							"Offender: " + value.getSimpleName()
					);
				}
				children.add(entry.commandSpec(isAuction));
			} catch (InstantiationException | IllegalAccessException e1) {
				e1.printStackTrace();
			}
		});
	}

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		return CommandResult.empty();
	}
}
