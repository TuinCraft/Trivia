package nl.blaatz0r.Trivia.CommandExecutors;

import nl.blaatz0r.Trivia.Trivia;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HintExecutor implements CommandExecutor {
    private final Trivia plugin;

    public HintExecutor(Trivia instance) {
        plugin = instance;
    }
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (sender instanceof Player) {
			Player player = (Player) sender;
			
			if (plugin.triviaRunning()) {
				if (plugin.triviaEnabled(player) && plugin.canHint(player)) {
					plugin.updateHint();
				}
			} else {
				player.sendMessage(Trivia.PREFIX_TRIVIA + "Trivia is not running.");
			}	
			return true;
		} else {
			return false;
		}
	}
	
}
