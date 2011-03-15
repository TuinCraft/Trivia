package nl.blaatz0r.Trivia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import nl.blaatz0r.Trivia.CommandExecutors.*;

import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import com.nijikokun.bukkit.Permissions.Permissions;
//import com.nijikokun.bukkit.iConomy.iConomy;
import com.nijiko.Messaging;
import com.nijiko.permissions.PermissionHandler;
import org.bukkit.plugin.Plugin;

/**
 * Trivia for Bukkit
 * Trivia is quiz game where players can answer questions by typing in the answer to a question.
 * The player receives points for each correct answer and/or optionally a random item.
 * Every 15 seconds or so a hint is given to make it a bit easier, but the answer will be worth less points for each hint!
 * @author blaatz0r
 */
public class Trivia extends JavaPlugin {

    public String name;
    public String version;
	private final TriviaPlayerListener playerListener = new TriviaPlayerListener(this);
    //public static iConomy ic;
    public static Logger log;    
    public static PermissionHandler Permissions = null;
    public static final String PREFIX_TRIVIA = ChatColor.AQUA + "[Trivia] " + ChatColor.WHITE;
    
    
    public ArrayList<Player> triviaUsers;
    protected TimerThread timerThread;
    private Database db;
    public long startTime;
    private String question;
    private String answer;
    private String hint;
    public int hints;
    private String[] questions;
    public boolean canAnswer;
    public List<Player> voted;
    private boolean triviaRunning;
    
    // DEFAULT PLUGIN FUNCTIONS
    
    public void onEnable() {
    	this.name = this.getDescription().getName();
    	this.version = this.getDescription().getVersion();

        log = Logger.getLogger("Minecraft");
        log.info(name + " " + version + " enabled");
        
        this.getDataFolder().mkdir();
        
        TriviaSettings.initialize(getDataFolder());
		
        this.setupPermissions();
        
        //this.setupIconomy();
        
        File questionsDir = new File(getDataFolder(), TriviaSettings.questionsDir);
		questionsDir.mkdir();
        
        this.db = new Database(this);
        db.connect();
        db.init();
        
        triviaUsers = new ArrayList<Player>();
    	
    	// Read trivia question files (only once)
        List<String> files = this.loadQuestions();

        log.info("[Trivia] loaded questions from " + files.toString());
        
        // Register our events
        PluginManager pm = getServer().getPluginManager();        
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Low, this);
        pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Low, this);
        //pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Normal, this); 
        
        // Commands
        
        getCommand("trivia").setExecutor(new TriviaExecutor(this));
        getCommand("hint").setExecutor(new HintExecutor(this));
        getCommand("next").setExecutor(new NextExecutor(this));
        getCommand("votenext").setExecutor(new VoteNextExecutor(this));
        
        this.startTrivia(false);
        
    }
    
    /*
    public void setupIconomy() {
    	Plugin ic = this.getServer().getPluginManager().getPlugin("iConomy");
    	
    	if(Trivia.ic == null) {
    	    if(ic != null) {
    	    	Trivia.ic = ((iConomy)ic);
    	    } else {
	    		log.info(Messaging.bracketize(name) + " iConomy system not enabled.");
	    		//this.getServer().getPluginManager().disablePlugin(this);
    	    }
    	}
    }
    */
    
    public void setupPermissions() {
    	Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

    	if(Trivia.Permissions == null) {
    	    if(test != null) {
    	    	Trivia.Permissions = ((Permissions)test).getHandler();
    	    } else {
	    		log.info(Messaging.bracketize(name) + " Permission system not enabled. Disabling plugin.");
	    		this.getServer().getPluginManager().disablePlugin(this);
    	    }
    	}
    }
    
	public void startTrivia(boolean verbose) {
		if (questions.length != 0) { 
			if (verbose) {
	            log.info("[Trivia] Trivia has started!");
				
	            for (Player p : this.triviaUsers) {
	            	p.sendMessage(Trivia.PREFIX_TRIVIA + "Trivia has started! \\o/");
	            }
			}
	        voted = new ArrayList<Player>();
	        startTime = new Date().getTime();
	    	hints = 0;
	    	canAnswer = true;
	    	triviaRunning = true;
	        nextQuestion();
	    	
	    	// Start a new timer
	        if (TriviaSettings.timer > 0) {
	        	this.timerThread = new TimerThread(this);
	        	this.timerThread.start();
	        	
	        }
		} else {
        	log.warning("[Trivia] No questions were loaded!");
        	log.warning("[Trivia] Add some files to the /plugins/Trivia/" + TriviaSettings.questionsDir +"/ directory and use /load <filename>, then use /trivia start");
        	for (Player p : this.triviaUsers) {
            	p.sendMessage(Trivia.PREFIX_TRIVIA + "Trivia cannot start because no questions were loaded.");
            }
        }
	}

	public void onDisable() {
		triviaUsers = new ArrayList<Player>();
        this.stopTrivia();
        
        log = Logger.getLogger("Minecraft");
        log.info(name + " " + version + " disabled");

        
    }
    
    public void stopTrivia() {

    	for (Player p : this.triviaUsers) {
    		p.sendMessage(Trivia.PREFIX_TRIVIA + "Trivia has stopped. :(");
    	}
    	
        voted = new ArrayList<Player>();
        hints = 0;
        canAnswer = false;
    	triviaRunning = false;
        
    	if (timerThread != null) {
            timerThread.stop();
            timerThread = null;
        }
		
	}

	// TRIVIA FUNCTIONS
    
	/**
	 * Method to select and start the next question. 
	 * Resets the number of hints, reads a question, creates a hint and enables answering.
	 */
	public void nextQuestion() {
		this.voted = new ArrayList<Player>();
		this.hints = 0;
		this.startTime = new Date().getTime();
		readQuestion();
		makeHint();
	    this.canAnswer = true;
	}
	
	
    /**
     * Helper method to determine the number of characters to show in the next hint.
     * One character is shown for every ten characters that the answer is long.
     */
    public void updateHint() {
		int length = this.hint.length();
    	int times = (int)(Math.ceil(((double)length)/TriviaSettings.lettersPerHint));
    	if (this.hints != 0) {
	    	for (int i = 0; i < times; i++) {
	    		updateHintR();
	    	}
    	}
    	for (Player player : this.triviaUsers) {
    		this.sendQuestion(player, false);
        }
	    //System.out.println("Q: " + this.getQuestion());
	    //System.out.println("H: " + "[" + this.hints + "/" + TriviaSettings.maxHints + "] " + this.getHint() + " " + this.getAnswer());
	    
	    this.hints++;
    }

    /**
     * Shows another random character from the answer in the hint.
     */
	public void updateHintR() {
		
		int length = this.hint.length();
		ArrayList<Integer> pos = new ArrayList<Integer>();
		for (int i = 0; i < length; i++){
			if (hint.charAt(i) == '*') {
				pos.add(i);
			}
		}
		
		if (pos.size() > 1) {
			int random = new Random().nextInt(pos.size()-1);
			int replacer = pos.get(random);
			if (replacer == 0) {
				this.hint = this.answer.charAt(0) + hint.substring(1);
			} else if (replacer == length-1) {
				this.hint = hint.substring(0,replacer) + answer.charAt(replacer);
			} else {
				this.hint = hint.substring(0, replacer) + this.answer.charAt(replacer) + hint.substring(replacer+1);
			}
		}
		
	}

	/**
	 * Makes a hint from the current answer.
	 * Replaces alphanumeric characters for asterisks and leaves all other characters as is.
	 */
	public void makeHint() {
		String res = "";
		int length = this.getAnswer().length();
		for(int i = 0; i < length; i++) {
			if (!String.valueOf(this.getAnswer().charAt(i)).matches("[a-zA-Z0-9]")){
				res += this.getAnswer().charAt(i);
			} else {
				res += '*';
			}
		}
		this.setHint(res);
	}

    public void loadQuestions (Player p) {
    	List<String> fNames = loadQuestions();
    	if (fNames.isEmpty()) {
    		p.sendMessage(Trivia.PREFIX_TRIVIA + " no files were loaded!");    		
    	} else {
    		p.sendMessage(Trivia.PREFIX_TRIVIA + " loaded questions from " + fNames.toString());
    	}
    	
    }
    
    public List<String> loadQuestions() {
    	List<String> fNames = new ArrayList<String>();
        log = Logger.getLogger("Minecraft");
    	File dir = new File(this.getDataFolder(),TriviaSettings.questionsDir);
        File[] list = dir.listFiles();
        if (list == null) {
            log.severe("Could not list questions directory: " + dir.getPath());
        } else {
        	List<String> q = new ArrayList<String>();
        	fNames = new ArrayList<String>();
        	questions = null;
            try {
    	        for (File f : list){
    	        	q.addAll(this.readLines(f));
    	        	fNames.add(f.getName());
    	        	
    	        }
            } catch(IOException e) {
    			e.printStackTrace();
            }
            questions = q.toArray(new String[q.size()]);
            
        }
        return fNames;
        
	}
    
    public void addQuestions(String s, Player p) {
    	log = Logger.getLogger("Minecraft");
    	File file = new File(this.getDataFolder(), "questions/" + s);
    	if (file.exists() && file.isFile()) {
    		List<String> q = new ArrayList<String>();
	        try {
		        q.addAll(this.readLines(file));
	        } catch(IOException e) {
				e.printStackTrace();
	        }
	        String[] qA = q.toArray(new String[q.size()]);
	        String[] tmp = new String[this.questions.length+qA.length];
	 	    System.arraycopy(questions, 0, tmp, 0, questions.length);
	 	    System.arraycopy(qA, 0, tmp, questions.length, qA.length);
	 	    questions = new String[tmp.length];
	 	    questions = tmp.clone();
	 	    p.sendMessage(Trivia.PREFIX_TRIVIA + " loaded questions from " + s);
            log.info("[Trivia] loaded questions from " + s);
    	} else {
    		log.warning("[Trivia] failed to load " + s);
    	}
    	

    }
    
	/**
	 * Reads a question from a file and updates question and answer.
	 */
	public void readQuestion() {
    	log = Logger.getLogger("Minecraft");
	     Random rand = new Random();
	     String result = this.questions[rand.nextInt(this.questions.length)];
	     
	     if (result.contains("*")){
	    	 String q = result.split("\\*")[0];
	    	 String a = result.split("\\*")[1];
	    	 
	    	 if (q.equalsIgnoreCase("Scramble")) {
	    		 this.setQuestion("Unscramble this word: " + scramble(a));
	    	 } else {
	    		 this.setQuestion(q);
	    	 }
		     this.setAnswer(a);
		     
	     } else {
	    	 log.warning("[Trivia] Bad format in question: " + result);
	    	 readQuestion();
	     }
	}
	
	public boolean triviaEnabled(Player p) {
		return this.triviaUsers.contains(p);
	}
	

	
	public String scramble(String s) {
		Random rand = new Random();
		String res = "";
		String tmp = s;
		int length = s.length();
		
		for (int i = 0; i < length; i++){
			char c = tmp.charAt(rand.nextInt(tmp.length()));
			tmp = tmp.replaceFirst(String.valueOf(c), "");
			res += String.valueOf(c);
		}
		
		return res;
	}
	
    public List<String> readLines(File f) throws IOException {
        FileReader fileReader = new FileReader(f);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        bufferedReader.close();
        return lines;
    }
    
	public void toggleTrivia(Player p) {
		if (triviaEnabled(p)) {
			this.triviaUsers.remove(p);
			p.sendMessage(Trivia.PREFIX_TRIVIA + "disabled.");
		} else{
			this.triviaUsers.add(p);
			p.sendMessage(Trivia.PREFIX_TRIVIA + "enabled!");
			if (this.triviaRunning()) {
				if (this.canAnswer) {
					this.sendQuestion(p, true);
				}
			} else {
				p.sendMessage("Trivia is not running at the moment");
			}
		}
	}
	
    public boolean canNext(Player p) {
    	return this.triviaEnabled(p) && Trivia.Permissions.has(p, "Trivia.next") || p.isOp();
    }

    public boolean canHint(Player p) {
    	return this.triviaEnabled(p) && Trivia.Permissions.has(p, "Trivia.hint") && this.canAnswer && this.hints <= TriviaSettings.maxHints;
    }
    
	// MESSAGES
	
	/**
	 * Formats and sends a question and hint to a player.
	 * @param player The player to send the question to.
	 */
	public void sendQuestion(Player player, Boolean joined) {
        
		String q = Trivia.PREFIX_TRIVIA + this.question;
		String h = ChatColor.AQUA + "Hint [" + ChatColor.WHITE + (joined ? (this.hints-1) : this.hints) + ChatColor.AQUA + "/" + ChatColor.WHITE + TriviaSettings.maxHints + ChatColor.AQUA + "]: " + ChatColor.WHITE + this.hint;
		
		player.sendMessage(q);
		player.sendMessage(h);		
	}
	
	/**
	 * Method to tell a player that someone answered correctly.
	 * @param target The target player.
	 * @param winner The player who answered correctly.
	 */
	public void correctAnswer(Player target, Player winner, double time) {
		Random rand = new Random();
		String[] grats = {"Nice! ", "Congratulations! ", "Woop! ", "Bingo! ", "Zing! ", "Huzzah! ", "Grats! ", "Who's the man?! ", "YEAHH! ", "Well done! "};
		String q = ChatColor.DARK_GREEN + grats[rand.nextInt(grats.length)] + ChatColor.GREEN + winner.getDisplayName() + ChatColor.DARK_GREEN + " got the answer in " + ChatColor.GREEN + String.valueOf(time) + ChatColor.DARK_GREEN + " seconds!";
		String t = ChatColor.DARK_GREEN + "The answer was "+ ChatColor.GREEN + this.answer;
		
		target.sendMessage(q);
		target.sendMessage(t);
		
	}
	
	/**
	 * Notify a player that the current question timed out.
	 * @param p
	 */
	public void noAnswer(Player p) {
		String q = Trivia.PREFIX_TRIVIA + ChatColor.DARK_AQUA + "Nobody got it right. The answer was " + ChatColor.LIGHT_PURPLE + getAnswer();
		String t = ChatColor.DARK_AQUA + "Next question in " + ChatColor.LIGHT_PURPLE + TriviaSettings.questionTimeout + ChatColor.DARK_AQUA + " seconds.";
		p.sendMessage(q);
		p.sendMessage(t);
	}
	
	public void sendTop(Player p) {
		Connection con = db.getConnection();
		try {
			Statement stat = con.createStatement();
			ResultSet rs = stat.executeQuery("SELECT * FROM scores ORDER BY score DESC LIMIT 5;");
			
			int i = 1;
			while(rs.next()) {
				String q = ChatColor.AQUA + String.valueOf(i) + ". " + ChatColor.BLUE + rs.getString("name") + ChatColor.AQUA + " - " + ChatColor.BLUE + rs.getInt("score") + " points";
				p.sendMessage(q);
				i++;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendRanking(Player p) {
		Connection con = db.getConnection();
		try {
			Statement stat = con.createStatement();
			ResultSet rs = stat.executeQuery("SELECT (COUNT(*)+1) AS rank FROM scores WHERE score > (SELECT score FROM scores WHERE name = '" + p.getName() + "' LIMIT 1);");
			
			String q = "";
			int rank = rs.getInt("rank");
			ResultSet check = stat.executeQuery("SELECT * FROM scores WHERE name = '" + p.getName() + "';");
			if (check.next()) {
				String rankName = "";
				switch(rank) {
					case(1):
						rankName = "st";
						break;
					case(2):
						rankName = "nd";
						break;
					case(3):
						rankName = "rd";
						break;
					default:
						rankName = "th";					
				}
				q = ChatColor.AQUA + "You are currently ranked " + ChatColor.BLUE + rank + rankName + ChatColor.AQUA + ".";
				p.sendMessage(q);
			} else {
				p.sendMessage(ChatColor.AQUA + "You are currently not ranked.");
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void sendHelp(Player p) {
		p.sendMessage("/trivia" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Disables Trivia messages for you");
		p.sendMessage("/trivia help" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Shows this help menu");
		p.sendMessage("/trivia rank" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Shows your rank");
		p.sendMessage("/trivia top" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Shows the top 5 ranked players");
		p.sendMessage("/votenext" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Vote to skip this question (majority vote)");
		p.sendMessage("/hint" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Shows another hint");
		if (p.isOp()) {
			p.sendMessage(ChatColor.RED + "/trivia admin" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Shows commands for ops.");
			
		}
	}
	
	// BASIC GETTERS AND SETTERS
	public boolean triviaRunning() {
		return this.triviaRunning;
	}
	
	public void setQuestion(String question) {
		this.question = question;
	}

	public String getQuestion() {
		return question;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getAnswer() {
		return answer;
	}
	
	public String getHint() {
		return hint;
	}
	
	public void setHint(String hint) {
		this.hint = hint;
	}

	public Database getDb() {
		return db;
	}

	public void sendAdminHelp(Player p) {
		p.sendMessage(ChatColor.RED + "/trivia start" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Starts Trivia");
		p.sendMessage(ChatColor.RED + "/trivia stop" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Stops Trivia");
		p.sendMessage(ChatColor.RED + "/trivia restarts" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Stops and then starts Trivia");
		p.sendMessage(ChatColor.RED + "/trivia load" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Reloads all files in Trivia/questions directory");
		p.sendMessage(ChatColor.RED + "/trivia add [file]" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Adds the file to the questions");
		p.sendMessage(ChatColor.RED + "/next" + ChatColor.AQUA + " - " + ChatColor.WHITE + "Skips to the next question");
		
	}
}

