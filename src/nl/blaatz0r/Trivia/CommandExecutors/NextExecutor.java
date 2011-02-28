package nl.blaatz0r.Trivia.CommandExecutors;

import nl.blaatz0r.Trivia.Trivia;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NextExecutor implements CommandExecutor {
    private final Trivia plugin;

    public NextExecutor(Trivia instance) {
        plugin = instance;
    }
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (sender instanceof Player) {
			Player player = (Player) sender;
			
			if (plugin.triviaRunning()) {
				if (plugin.triviaEnabled(player) && plugin.canNext(player)) {
					plugin.nextQuestion();
		    		for (Player p : plugin.triviaUsers) {
			        	p.sendMessage(Trivia.PREFIX_TRIVIA + ChatColor.BLUE + "Next question coming up!");
			        }
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
