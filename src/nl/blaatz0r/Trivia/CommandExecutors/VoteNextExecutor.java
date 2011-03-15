package nl.blaatz0r.Trivia.CommandExecutors;

import nl.blaatz0r.Trivia.Trivia;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VoteNextExecutor implements CommandExecutor {
    private final Trivia plugin;

    public VoteNextExecutor(Trivia instance) {
        plugin = instance;
    }
    
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (plugin.triviaEnabled(player) && plugin.triviaRunning()) {
				if (!Trivia.Permissions.has(player, "Trivia.vote")) {
					player.sendMessage(Trivia.PREFIX_TRIVIA + "You don't have permissions to vote.");						
				} else if (plugin.voted.contains(player)) {
					player.sendMessage(Trivia.PREFIX_TRIVIA + ChatColor.BLUE + "You have already voted!");
				} else {
					if (Trivia.Permissions.has(player, "Trivia.startvote")) {
					    if (plugin.voted.contains(player)) {
					        player.sendMessage(Trivia.PREFIX_TRIVIA + "You have already voted.");
					    } else {
						plugin.voted.add(player);
						double limit = ((double)plugin.triviaUsers.size() / 2.0);
						if (plugin.voted.size() > limit) {
							plugin.nextQuestion();
				    		for (Player p : plugin.triviaUsers) {
					        	p.sendMessage(Trivia.PREFIX_TRIVIA + ChatColor.BLUE + "Vote succeeded!");
					        }
						} else {
							for (Player p : plugin.triviaUsers) {
					        	p.sendMessage(Trivia.PREFIX_TRIVIA + ChatColor.BLUE + " voted for the next question. [" + plugin.voted.size() + "/" + (int)(Math.ceil(limit)+1) + ChatColor.BLUE + "]");
					        	if(plugin.voted.size() == 1 && p != player) {
					        		p.sendMessage(ChatColor.BLUE + "For the next question, use " + ChatColor.BLUE + "/votenext");
					        	}
					        }
						}
					    }
					} else {
						player.sendMessage(Trivia.PREFIX_TRIVIA + "You don't have permissions to start a new vote.");
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
