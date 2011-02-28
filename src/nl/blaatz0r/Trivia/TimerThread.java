package nl.blaatz0r.Trivia;
import org.bukkit.entity.Player;

public class TimerThread implements Runnable {

	private Trivia plugin;
	private Thread thread;
	private boolean running = false;
	
	public TimerThread(Trivia plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void run() {        
		while (this.running) {
	        	        
	        if (plugin.hints <= TriviaSettings.maxHints) { 
	        	
	        	plugin.updateHint();
	        	
	        	
	        } else {
	        	TriviaSettings.setTimer(TriviaSettings.questionTimeout);
				plugin.canAnswer = false;
		        for (Player player : plugin.triviaUsers) {
		        	plugin.noAnswer(player);
		        }
	        	plugin.hints = 0;
	        }
	        
	        try {
	            Thread.sleep(TriviaSettings.timer*1000);
	        } catch (InterruptedException localInterruptedException) {
	        }
	        
	        if (!plugin.canAnswer) {
        		TriviaSettings.resetTimer();
				plugin.nextQuestion();	
				plugin.canAnswer = true;
	        }
        }
    }
    public void start() {
        this.running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        this.running = false;
        thread.interrupt();
    }

}
