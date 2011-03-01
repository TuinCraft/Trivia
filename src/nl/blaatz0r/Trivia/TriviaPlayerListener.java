package nl.blaatz0r.Trivia;

import java.util.Date;
import java.util.List;
import java.util.Random;

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

