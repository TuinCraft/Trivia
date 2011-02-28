package nl.blaatz0r.Trivia;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class Database {
	
	private Connection connection = null;
	private static boolean connected = false;
    private final Trivia plugin;
	
    public Database(Trivia plugin){
    	this.plugin = plugin;
    	
    }
    
	public static boolean isConnected() {
		return connected;
	}
	
	public Connection getConnection() {
		return connection;
	}	
	
	public void init() {
		try {
			Connection conn = getConnection();
			Statement stat = conn.createStatement();
			stat.executeUpdate("CREATE TABLE IF NOT EXISTS scores (name, score);");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String getDatabasePath() {
		File db = new File(plugin.getDataFolder(),TriviaSettings.dbName);
		return db.getPath();
	}
	
	public boolean connect(){
		if (connection != null) {
			return true;
		}

		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + getDatabasePath());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		connected = true;

		return true;
	}
}