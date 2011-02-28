package nl.blaatz0r.Trivia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.util.config.Configuration;

public class TriviaSettings {
	private static final String settingsFile = "Trivia.yml";
		
	public static LinkedHashMap<String,String> defaults;
    public static int timer;
    public static int backupTimer;
    public static int questionTimeout;
    public static int maxHints;
    public static boolean rewardPoints;
    public static boolean rewardItems;
    public static boolean rewardCoins;
    public static int points;
    public static List<Integer> items;
    public static int decrease;
    //public static boolean permNext;
    //public static boolean permHint;
    public static String dbName;
    public static String questionsDir;
    public static int lettersPerHint;
    
    public static void initialize(File dataFolder) {
    	
    	if(!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        
        
    	defaults = new LinkedHashMap<String,String>();
        defaults.put("timer", "15");
        defaults.put("time-out", "5");
        defaults.put("max-hints", "3");
        defaults.put("reward-points", "true");
        defaults.put("reward-items", "false");
        defaults.put("reward-coins", "true");
        defaults.put("points", "10");
        defaults.put("# decrease", "Number of points deducted per hint from the maximum points reward");
        defaults.put("decrease", "2");
        defaults.put("# items", "A list of item IDs of which one is randomly rewarded");
        defaults.put("items", "[]");
        //defaults.put("permissions-next", "false");
        //defaults.put("permissions-hint", "true");
        defaults.put("db-name", "trivia.db");
        defaults.put("questions-dir", "questions");
        defaults.put("# letters-per-hint", "One letter will be revealed per hint for this many letters in the answer");
        defaults.put("letters-per-hint", "8");
           
        File configFile = new File(dataFolder, settingsFile);
        if(!configFile.exists()) {
            createSettingsFile(configFile);
        }
        Configuration config = new Configuration(configFile);
        config.load();
        timer 			= Math.max(1, config.getInt("timer", 15));
        backupTimer 	= timer;
        questionTimeout = Math.max(1, config.getInt("time-out", 5));
        maxHints 		= config.getInt("max-hints", 3);
        rewardPoints 	= config.getBoolean("reward-points", true);
        rewardItems		= config.getBoolean("reward-items", false);
        rewardCoins		= config.getBoolean("reward-coins", true);
        points			= config.getInt("points", 10);
        items			= config.getIntList("items", null);
        decrease		= config.getInt("points-decrease", 2);
        //permNext		= config.getBoolean("permissions-next", false);
        //permHint		= config.getBoolean("permissions-hint", true);
        dbName			= config.getString("db-name", "trivia.db");
        questionsDir	= config.getString("questions-dir", "questions");
        lettersPerHint	= config.getInt("letters-per-hint", 8);
    }
    
    public static void resetTimer() {
        timer = Math.max(1, backupTimer);
    }

    public static void setTimer(int t) {
    	timer = t;
    }
    
    private static void createSettingsFile(File configFile) {
        BufferedWriter bwriter = null;
        FileWriter fwriter = null;
        try {
            configFile.createNewFile();
            fwriter = new FileWriter(configFile, true);
            bwriter = new BufferedWriter(fwriter);
            
            for (Map.Entry<String, String> e : defaults.entrySet()) {
            	
            	bwriter.write(e.getKey() + ": " + e.getValue());
            	bwriter.newLine();
            }
            bwriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bwriter != null) {
                    bwriter.close();
                }
                if (fwriter != null)
                    fwriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
