package movieUpdates;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Movies {

	
	//To handle errors
	private static void createLogFiles(Exception e) {
		//error
		try {
			System.setErr(new PrintStream(new FileOutputStream(System.getProperty("user.dir") + File.separator + "error.log")));
			e.printStackTrace();
		} 
		catch (FileNotFoundException e1) {
			System.out.println("Couldn't create error.log");
		}
		
		//system info
		try {
			System.setOut(new PrintStream(new FileOutputStream(System.getProperty("user.dir") + File.separator + "systemInfo.log")));
			System.out.println("OS name: " + System.getProperty("os.name"));
			System.out.println("OS version: " + System.getProperty("os.version"));
			System.out.println("OS architecture: " + System.getProperty("os.arch"));
			System.out.println("Java version: " + System.getProperty("java.version"));
		} 
		catch (FileNotFoundException e1) {
			System.out.println("Couldn't create systemInfo.log");
		}
		
		
	}
	
	
	//For general notifications
	private static void showNotification(String title, MessageType type) {
		SystemTray tray = SystemTray.getSystemTray();
		Image icon = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "icon.png");
		TrayIcon trayIcon = new TrayIcon(icon, "Tray Demo");
		trayIcon.setImageAutoSize(true);
		trayIcon.setToolTip("Movie Updates");
		try {
			tray.add(trayIcon);
		} 
		catch (AWTException e) {
		    createLogFiles(e);
		}
		trayIcon.displayMessage(title, "Contact developer for support.", type);
	}
	
	
	
	//Create required files
	private static void create(String fileName) {
		File file = new File(System.getProperty("user.dir") + File.separator + fileName+".txt");
		if (!file.exists()) {
			try {
				file.createNewFile();
				if (fileName.equalsIgnoreCase("watchlist")) System.out.println("New watchlist was created. Add movies to begin.");
				if (fileName.equalsIgnoreCase("site")) System.out.println("No sites found. Add sites to get updates.");
			} catch (IOException e) {
				System.out.println("File creation failed.");
				createLogFiles(e);
				showNotification("File creation failed.", MessageType.ERROR);
			}
		}
	}
	
	
	
	//Initialize
	private static void initialize() {
        sendNotification(null, null, false); //startup notification
		//to clear console
		try {
			if (System.getProperty("os.name").equals("Windows 7") || System.getProperty("os.name").equals("Windows XP") || System.getProperty("os.name").equals("Windows 10"))
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		} 
		catch (InterruptedException | IOException e) {
			createLogFiles(e);
            showNotification("Error.", MessageType.ERROR);
		}
		
		//check for requirements
		create("watchlist");
		create("site");
		create("update interval (minutes)");
		
		try {
			getData();
		} catch (IOException e) {
			System.out.println("Error getting data.");
			createLogFiles(e);
            showNotification("Error getting data.", MessageType.ERROR);
		}
	}
	
	
	
	

	//Get Data
	@SuppressWarnings("resource")
	private static void getData() throws IOException {
		Runnable runnable = new Runnable() {
			public void run() {
				//site
				File sitetxt = new File(System.getProperty("user.dir") + File.separator + "site.txt");
				Scanner scan1=null;
				try {
					scan1 = new Scanner(sitetxt);
				} catch (Exception e) {
					System.out.println("'site.txt' not found");
					createLogFiles(e);
                    showNotification("'site.txt' not found", MessageType.ERROR);
				}
				String site=null;
				String url=null;
				assert scan1 != null;
				if (scan1.hasNext())
					site = scan1.next();
				if (site.equalsIgnoreCase("psarips"))
					url = "https://psarips.eu/feed/";
				if (site.equalsIgnoreCase("yts"))
					url = "https://yts.lt/rss";
				
				//watchlist
				System.out.println();
				System.out.println("Connecting...");
				File watchlist = new File(System.getProperty("user.dir") + File.separator + "watchlist.txt");
				Scanner scan2;
				try {
					scan2 = new Scanner(watchlist);
					if (!scan2.hasNext()) System.out.println("Watchlist is empty. Add some movies to get updates.");
					while (scan2.hasNextLine()) {
						String movieName = scan2.nextLine();
						check(movieName, url, site);
					}	
				} 
				catch (FileNotFoundException e) {
					System.out.println("'watchlist.txt' not found");
					createLogFiles(e);
                    showNotification("'watchlist.txt' not found", MessageType.ERROR);
				}
			}
		};
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		int interval = new Scanner(new File(System.getProperty("user.dir") + File.separator + "update interval (minutes).txt")).nextInt();
		scheduler.scheduleAtFixedRate(runnable, 0, interval, TimeUnit.MINUTES);
	}
	
	
	
	
	
	
	//Check
	private static void check(String movieName, String url, String site) {
		//connect
		URLConnection connection = null;
		try {
	        connection = new URL(url).openConnection(); 
	        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
	        connection.connect();
		}
		catch (Exception e) { 
			System.out.println("Connection error. Please try again."); 
			createLogFiles(e);			
			showNotification("Connection error. Please try again.", MessageType.ERROR);
		}
        
        //read
		boolean found = false;
        boolean movieurl = false;
		try {
			assert connection != null;
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
	        
	        //find
	        String line;
	        start:
	        while((line = reader.readLine()) != null) {
	        	String[] arr = line.split("<");
				for (String s : arr) {
					//psarips
					if (s.equalsIgnoreCase("link>https://psarips.eu/movie/" + movieName.toLowerCase().replace(" ", "-").replace("(", "").replace(")", "") + "/")) {
						movieurl = true;
						found = true;
						break start;
					}
					if (s.equalsIgnoreCase("link>https://psarips.eu/tv-show/" + movieName.toLowerCase().replace(" ", "-").replace("(", "").replace(")", "") + "/")) {
						found = true;
						break start;
					}
					//yts
					if (s.equalsIgnoreCase("link>https://yts.lt/movie/" + movieName.toLowerCase().replace(" ", "-").replace("(", "").replace(")", ""))) {
						found = true;
						break start;
					}
				}
	        }
	        reader.close();
		}
		catch (Exception e) { 
			System.out.println("Error reading from site.");
		    showNotification("Error reading from site.", MessageType.ERROR);
			createLogFiles(e);	
		}

	    actions(movieName, found, site, movieurl);
    }
			
		
	
	
	
	
	//Actions
	private static void actions(String movieName, boolean found, String site, boolean movieurl) {
        System.out.println();
        if (found) {
        	System.out.println(movieName + " is available on " + site);
        	sendNotification(movieName, site, movieurl);
        }
        if (!found) {
        	System.out.println(movieName + " is not available on " + site);
        }
	}
	
	
	
	
	
	//For movie notifications
	private static void sendNotification(String movieName, String site, boolean movieurl) {

		//notification
		Image icon = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "icon.png");
		TrayIcon trayIcon = new TrayIcon(icon, "Movie Updates");
		SystemTray tray = SystemTray.getSystemTray();
		trayIcon.setImageAutoSize(true);
		if (movieName != null) //to avoid triggering for the main method
			trayIcon.setToolTip("Click here to open " + "\"" + movieName + "\"" + " in browser");
		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
		    System.out.println("Failed to add the tray icon");
			createLogFiles(e);
            showNotification("Failed to add the tray icon", MessageType.ERROR);
		}
		if (movieName != null)
			trayIcon.displayMessage(movieName + " is available on " + site, "Click the system tray icon anytime, to open it in browser", MessageType.INFO);
		//for the main method only
		else trayIcon.displayMessage("'Movie Updates' is running in the background", "You can right-click the system tray icon to access everything", MessageType.INFO);

		//right-click menu
		PopupMenu popup = new PopupMenu();

		//manual check
		Desktop desktop = Desktop.getDesktop();
		File checkManually = new File(System.getProperty("user.dir") + File.separator + "CHECK MANUALLY.bat");
        MenuItem manualCheck = new MenuItem("Check manually");
        manualCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    desktop.open(checkManually);
                } catch (IOException e1) {
                    System.out.println("CHECK MANUALLY.bat not found.");
                    createLogFiles(e1);
                    showNotification("CHECK MANUALLY.bat not found.", MessageType.ERROR);
                }
            }
        });
        popup.add(manualCheck);

        //watchlist
		File watchlisttxt = new File(System.getProperty("user.dir") + File.separator + "watchlist.txt");
		MenuItem editWatchlist = new MenuItem("Edit watchlist");
		editWatchlist.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					desktop.open(watchlisttxt);
				} catch (IOException e1) {
					System.out.println("watchlist.txt not found.");
					createLogFiles(e1);
					showNotification("watchlist.txt not found.", MessageType.ERROR);
				}
			}
		});
		popup.add(editWatchlist);

		//change site
		File sitetxt = new File(System.getProperty("user.dir") + File.separator + "site.txt");
		MenuItem changeSite = new MenuItem("Change site");
		changeSite.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					desktop.open(sitetxt);
				} catch (IOException e1) {
					System.out.println("site.txt not found.");
					createLogFiles(e1);
					showNotification("site.txt not found.", MessageType.ERROR);
				}
			}
		});
		popup.add(changeSite);

		//update interval
		File intervaltxt = new File(System.getProperty("user.dir") + File.separator + "update interval (minutes).txt");
		MenuItem updateInterval = new MenuItem("Set update interval");
		updateInterval.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					desktop.open(intervaltxt);
				} catch (IOException e1) {
					System.out.println("update interval (minutes).txt not found.");
					createLogFiles(e1);
					showNotification("update interval (minutes).txt not found.", MessageType.ERROR);
				}
			}
		});
		popup.add(updateInterval);

		//contact me
		File contactMe = new File(System.getProperty("user.dir") + File.separator + "contact me.txt");
		MenuItem contact = new MenuItem("Contact me");
		contact.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					desktop.open(contactMe);
				} catch (IOException e1) {
					System.out.println("contact.txt not found.");
					createLogFiles(e1);
					showNotification("contact.txt not found.", MessageType.ERROR);
				}
			}
		});
		popup.add(contact);

		//restart
		MenuItem restart = new MenuItem("Restart");
		restart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				main(null);
				tray.remove(trayIcon);
			}
		});
		popup.add(restart);

        //exit
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(1);
            }
        });
        popup.add(exit);

		//double-click
		trayIcon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (site.equalsIgnoreCase("psarips") & movieurl) {
						assert movieName != null;
						desktop.browse(new URI("https://psarips.eu/movie/"+movieName.toLowerCase().replace(" ", "-").replace("(", "").replace(")", "")));
					}
					if (site.equalsIgnoreCase("psarips") & !movieurl) {
						assert movieName != null;
						desktop.browse(new URI("https://psarips.eu/tv-show/"+movieName.toLowerCase().replace(" ", "-").replace("(", "").replace(")", "")));
					}
					if (site.equalsIgnoreCase("yts")) {
						assert movieName != null;
						desktop.browse(new URI("https://yts.lt/movie/"+movieName.toLowerCase().replace(" ", "-").replace("(", "").replace(")", "")));
					}
				} catch (Exception e1) {
					System.out.println("Error");
					e1.printStackTrace();
					createLogFiles(e1);
				}
			}
		});

		trayIcon.setPopupMenu(popup);

	}


	//Add or remove from startup
	private static void startup() {
	    if (System.getProperty("os.name").equals("Windows 7") || System.getProperty("os.name").equals("Windows 10")) {
            String path = System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
	        File startup = new File(path);
            try {
                System.setOut(new PrintStream(new FileOutputStream(path)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            	createLogFiles(e);
            }
            System.out.println("start javaw -Xmx200m -jar " + System.getProperty("user.dir") + "\\MovieUpdates.jar");
        }

    }
	

	public static void main(String []args) {
		initialize();
    }
}




//Coded by: Tashila Pathum (linkedin.com/in/tashilapathum)
//Date: 23.09.2019