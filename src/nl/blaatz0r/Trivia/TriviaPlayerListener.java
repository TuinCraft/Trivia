package nl.blaatz0r.Trivia;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
//import com.nijikokun.bukkit.iConomy.Database;
/**
 * Handle events for all Player related events
 * @author blaatz0r
 */
public class TriviaPlayerListener extends PlayerListener {
    private final Trivia plugin;

    public TriviaPlayerListener(Trivia instance) {
        plugin = instance;
    }
    

    
    public void reward(Player p) { 
    	
    	if (TriviaSettings.rewardPoints){
    		rewardPoints(p, TriviaSettings.points);
    	}
    	
    	if (TriviaSettings.rewardItems) {
    		rewardItems(p, TriviaSettings.items);
    	}
    	
    	if (TriviaSettings.rewardCoins) {
    		//rewardCoins(p,10);
    	}
    }
    
    /*
    public void rewardCoins(Player p, int i) {
    	Database d = Trivia.ic.db;
    	String name = p.getDisplayName();
    	d.set_balance(name, d.get_balance(name) + 10);
    	System.out.println("Trivia awarded " + i + "coins to " + name);
    }
    */
    
    public void rewardPoints(Player p, int i) {
    	
    	int points = Math.max(0,TriviaSettings.points - (plugin.hints * TriviaSettings.decrease));
        int total = points;
        
		try {
			Connection conn = plugin.getDb().getConnection();
			Statement stat = conn.createStatement();
			stat.executeUpdate("CREATE TABLE IF NOT EXISTS scores (name, score);");
			
			ResultSet rs = stat.executeQuery("SELECT * FROM scores WHERE name = '" + p.getName()+ "';");
			if (rs.next()) {
				total += rs.getInt("score");
				stat.execute("UPDATE scores SET score = " + total + " WHERE name = '" + p.getName() + "';");
			} else {
				PreparedStatement prep = conn.prepareStatement(
			      "INSERT INTO scores VALUES (?, ?);");
				prep.setString(1, p.getName());
				prep.setInt(2, points);
				prep.execute();
			}
			

		} catch (SQLException e) {
			e.printStackTrace();
		}

    	
    	p.sendMessage(Trivia.PREFIX_TRIVIA + "You scored " + ChatColor.GOLD + points + ChatColor.WHITE + " points! (" + ChatColor.GOLD + total + ChatColor.WHITE + " points total)");
    	
    }
    
    public void rewardItems(Player p, List<Integer> items) {
    	Random rand = new Random();
    	ItemStack is = new ItemStack(items.get(rand.nextInt(items.size())),1);
    	p.getInventory().addItem(is);
    	p.sendMessage(ChatColor.LIGHT_PURPLE + "You have been awarded a random item!");
    	
    }
    
    /*
    public void onPlayerCommand(PlayerChatEvent event) {
    	
    	//String[] split = event.getMessage().split(" +");
    	//String command = split[0];
    	Player player = event.getPlayer();
    	
    	String[] sects = event.getMessage().split(" +", 2); // This will contain two strings: the command, and everything else.
		String[] args = (sects.length > 1 ? sects[1].split(" +") : new String[0]); // This will contain all the arguments after the command, space-delimited.
		
		Commands cmd;
		
		try {
			// Determine a matching command based on sects[0], but remove the leading "/".
			cmd = Commands.valueOf(sects[0].substring(1).toUpperCase()); 
		} catch (Exception ex) {
			return;
		}

		try	{
			// We can use a switch because we converted the command string into an enum.
			switch (cmd) {
			case TRIVIA:
				
				break;
				
			case VOTENEXT:
					
				break;
				
			case NEXT:
				
				break;
			
			case HINT:
				
	    		break;
	    		
			default:
				return; // We forgot to implement a command: treat it as non-existent.
			}
		} catch (NoSuchMethodError ex) {
			// We are running an old version of CraftBukkit that does not support generateTree or generateBigTree.
			player.sendMessage("The server is not recent enough to support " + sects[0].toLowerCase() + ".");
		} catch (Exception ex) {
			// Unexpected error encountered.  Tell the user.  Can be thrown on purpose to notify the user of syntax errors and such.
			player.sendMessage(Trivia.PREFIX_TRIVIA + "Error: " + ex.getMessage());
		}
		
    	Logger.getLogger("Minecraft").log(Level.INFO, String.format("%1$s issued command: %2$s", player.getName(), event.getMessage()));
    	event.setCancelled(true);
	    
    	
    }
    */
    
    public void onPlayerChat(PlayerChatEvent event) {
    	String msg = event.getMessage();
    	Player player = event.getPlayer();
    	if((!event.isCancelled()) && plugin.triviaEnabled(player) && plugin.canAnswer && msg.equalsIgnoreCase(plugin.getAnswer())) {
    		plugin.canAnswer = false;
    		long endTime = (new Date().getTime()) - plugin.startTime;
    		
    		double time = Math.round((endTime/10));
    		time = time / 100;
    		
    		for (Player p : plugin.triviaUsers) {
    			plugin.correctAnswer(p, player, time);
	        }
    		this.reward(player);
    		plugin.nextQuestion();
    		event.setCancelled(true);
    	}
    }
    
    public void onPlayerQuit(PlayerEvent event) {
    	plugin.triviaUsers.remove(event.getPlayer());
    }
    
    

}

