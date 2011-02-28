package nl.blaatz0r.Trivia.CommandExecutors;

import nl.blaatz0r.Trivia.Trivia;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TriviaExecutor implements CommandExecutor {
    private final Trivia plugin;

    public TriviaExecutor(Trivia instance) {
        plugin = instance;
    }
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// TODO Auto-generated method stub
		
		if (sender instanceof Player) {
			Player player = (Player) sender;
			
			if (Trivia.Permissions.has(player, "Trivia.play")) {
				if (args.length == 0) {
					plugin.toggleTrivia(player);
				} else {
					Commands cmd2 = Commands.valueOf(args[0].toUpperCase());
					
					
					try {
						switch (cmd2) {
						case HELP:
		    				plugin.sendHelp(player);
							break;
							
						case ADMIN:
							
							if (Trivia.Permissions.has(player, "Trivia.admin.help")){
								plugin.sendAdminHelp(player);
							}
							break;
							
						case TOP:
							if (Trivia.Permissions.has(player, "Trivia.top")) {
								plugin.sendTop(player);
							}
		    				break;
		    				
						case RANK:
							if (Trivia.Permissions.has(player, "Trivia.rank")) {
								plugin.sendRanking(player);
							}
		    				break;
		    				
						case ADD:
							if (Trivia.Permissions.has(player, "Trivia.admin.add") || player.isOp()) {
								if (args.length == 2) {
									String f = args[1];
									plugin.addQuestions(f, player);
								} else {
									throw(new Exception("Illegal command"));
								}
							}
		    				break;
		    				
						case LOAD:
							if (Trivia.Permissions.has(player, "Trivia.admin.load") || player.isOp()) {
								plugin.loadQuestions(player);
							}
		    				break;
		    				
						case START:
							if ((Trivia.Permissions.has(player, "Trivia.admin.start") || player.isOp()) && !plugin.triviaRunning()) {
								plugin.startTrivia(true);
							}
							break;
							
						case STOP:
							if ((Trivia.Permissions.has(player, "Trivia.admin.stop") || player.isOp()) && plugin.triviaRunning()) {
								plugin.stopTrivia();
							}
							break;
							
						case RESTART:
							if ((Trivia.Permissions.has(player, "Trivia.admin.restart") || player.isOp()) && plugin.triviaRunning()) {
								plugin.stopTrivia();
								plugin.startTrivia(true);
							}
							
							break;
							
						default:
							throw(new Exception("Command not found"));
						}
					
					} catch (Exception ex) {
						// Unexpected error encountered.  Tell the user.  Can be thrown on purpose to notify the user of syntax errors and such.
						player.sendMessage(Trivia.PREFIX_TRIVIA + "Error: " + ex.getMessage());
					}
				}
				return true;	
			} else {
				return false;
			}
			
		} else {
			return false;			
		}
	}
	
	private enum Commands {
		TRIVIA,
		VOTENEXT,
		NEXT,
		HINT,
		ADD,
		LOAD,
		RANK,
		TOP,
		ADMIN,
		HELP,
		STOP,
		START,
		RESTART
	}
}
