package open.nano.scraper;

import open.java.toolkit.Arrays;
import open.java.toolkit.Regex;
import open.java.toolkit.System;
import open.java.toolkit.console.Console;
import open.java.toolkit.console.LogType;
import open.java.toolkit.console.ansi.Foreground;
import open.java.toolkit.files.FileCaching;
import open.java.toolkit.files.FileParser;
import open.java.toolkit.files.Files;
import open.java.toolkit.http.Request;
import open.java.toolkit.http.RequestUtils;
import open.java.toolkit.swing.themes.NimbusDark;
import open.java.toolkit.threading.Parallel;

import javax.swing.*;
import java.awt.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import static open.nano.scraper.JFrameHelper.*;
import static open.java.toolkit.swing.SwingHelper.*;

public class Main extends JFrame
{
    private Thread thread;
    private final JTextArea result;
    private final JLabel status, engine, website, keyword, size, proxy, link, retry;
    private String lastEngine = "None", lastWebsite = "None", lastKeyword = "None";
    private final ArrayList<String> scrapedLinks = new ArrayList<>();
    private boolean interrupted = false;
    private final Util util = Util.getNewInstance();
    private FileParser settings = util.getSettings("settings/settings.txt");
    private final Hashtable<String, String> proxyLocks = new Hashtable<>();
    private String[] proxies, links;

    private int retries, timeout, threads;
    private boolean past24Hours, showErrors, logErrors, saveLastStatus, proxyLockToThread;
    private String pattern, userAgent, proxyFile, linksFile, interfaceTheme;

    public Main()
    {
        Timer titleTimer = new Timer(1000, (e -> updateTitle()));
        titleTimer.setRepeats(true);
        titleTimer.start();

        if (settings.getString("interfaceTheme").equals("dark"))
            NimbusDark.configure(this);

        Dimension dimension = new Dimension(800, 600);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
        setPreferredSize(dimension);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Nano Scraper v1.3 - JavaToolkit v" + System.toolkitVersion);
        setDefaultLookAndFeelDecorated(true);

        JButton start = addButton(this, "Start", 20, 20, 110, 40);
        JButton stop = addButton(this, "Stop", 20, 70, 110, 40);
        JButton clean = addButton(this, "Clean", 20, 120, 110, 40);
        JButton save = addButton(this, "Save", 20, 170, 110, 40);
        JButton settings = addButton(this, "Settings", 20, 220, 110, 40);
        JButton engines1 = addButton(this, "Engines", 20, 270, 110, 40);
        JButton websites1 = addButton(this, "Websites", 20, 320, 110, 40);
        JButton keywords1 = addButton(this, "Keywords", 20, 370, 110, 40);

        result = alwaysUpdateCaret((JTextArea) addScrollPane(this, addTextArea(this, "", false, 150, 20, dimension.width - 160, dimension.height - 180)));

        status = addLabel(this, "Status : IDLE", 20, dimension.height - 70, dimension.width - 130, 20);
        engine = addLabel(this, "Engine : None", 20, dimension.height - 100, dimension.width - 130, 20);
        website = addLabel(this, "Website : None", 20, dimension.height - 130, dimension.width - 130, 20);
        keyword = addLabel(this, "Keyword : None", 20, dimension.height - 160, dimension.width - 130, 20);
        size = addLabel(this, "Size : 0", 460, dimension.height - 160, dimension.width - 130, 20);
        proxy = addLabel(this, "Proxy : None", 460, dimension.height - 130, dimension.width - 130, 20);
        link = addLabel(this, "Links : 0 / 0", 460, dimension.height - 100, dimension.width - 130, 20);
        retry = addLabel(this, "Retry : 0", 460, dimension.height - 70, dimension.width - 130, 20);

        start.addActionListener(e ->
        {
            thread = new Thread(() ->
            {
                for (int a = 0; a < 1; a++)
                {
                    refreshSettings();

                    Console.ansiWriteLine(Foreground.GREEN, "Started!", LogType.INFO);
                    Request.setTimeout(timeout);
                    Request.setUserAgent(userAgent.replace("+", " "));
                    Request.setFollowRedirects(HttpClient.Redirect.ALWAYS);
                    Request.setVersion(HttpClient.Version.HTTP_1_1);
                    Parallel.threads = threads;

                    resetStatus(true);

                    if (!Arrays.isEmpty(links))
                    {
                        scrape(links);
                        interrupted = true;
                        break;
                    }

                    String[] engines = util.getEngines();
                    String[] websites = util.getWebsites();
                    String[] keywords = util.getKeywords();

                    int i = 0, j = 0, k = 0;
                    if (saveLastStatus && !lastEngine.equals("None") && !lastWebsite.equals("None") && !lastKeyword.equals("None"))
                    {
                        i = Arrays.toArrayList(engines).indexOf(lastEngine);
                        j = Arrays.toArrayList(websites).indexOf(lastWebsite);
                        k = Arrays.toArrayList(keywords).indexOf(lastKeyword);
                    }
                    else
                    {
                        lastEngine = "None";
                        lastWebsite = "None";
                        lastKeyword = "None";
                    }

                    for (int ii = i; ii < engines.length; ii++)
                    {
                        if (interrupted) break;

                        String engine = engines[ii];
                        if (past24Hours && !saveLastStatus)
                        {
                            if (engine.contains("bing")) engine = "https://www.bing.com/search?filters=ex1%3a%22ez1%22&q=";
                            else if (engine.contains("yahoo")) engine = "https://search.yahoo.com/search?age=1d&btf=d&q=";
                            else if (engine.contains("yandex")) engine = "https://yandex.com/search/?within=77&text=";
                            else if (engine.contains("google")) engine = "https://www.google.com/search?tbs=qdr:d&q=";
                            else if (engine.contains("duckduckgo")) engine = "https://duckduckgo.com/?df=d&ia=web&q=";
                            else if (engine.contains("aol")) engine = "https://search.aol.com/aol/search?age=1d&btf=d&q=";
                        }

                        lastEngine = engine;
                        Files.writeString("settings/lastSettings.txt", lastEngine + "-_-_-_-_-" + lastWebsite + "-_-_-_-_-" + lastKeyword, false);

                        this.engine.setText("Engine : " + RequestUtils.urlDomainOnly(engine) + " (" + (ii + 1) + " / " + engines.length + ")");

                        if (interrupted) break;

                        for (int jj = j; jj < websites.length; jj++)
                        {
                            if (interrupted) break;

                            String website = websites[jj];

                            lastWebsite = website;
                            Files.writeString("settings/lastSettings.txt", lastEngine + "-_-_-_-_-" + lastWebsite + "-_-_-_-_-" + lastKeyword, false);

                            this.website.setText("Website : " + website + " (" + (jj + 1) + " / " + websites.length + ")");

                            if (interrupted) break;

                            for (int kk = k; kk < keywords.length; kk++)
                            {
                                String keyword = keywords[kk];

                                lastKeyword = keyword;
                                Files.writeString("settings/lastSettings.txt", lastEngine + "-_-_-_-_-" + lastWebsite + "-_-_-_-_-" + lastKeyword, false);

                                this.keyword.setText("Keyword : " + keyword + " (" + (kk + 1) + " / " + keywords.length + ")");
                                this.retry.setText("Retry : 0 / " + retries);

                                for (int r = 0; r <= retries; r++)
                                {
                                    if (interrupted) break;

                                    if (System.errorOccured)
                                    {
                                        this.retry.setText("Retry : " + r + " / " + retries);
                                        System.errorOccured = false;
                                    }

                                    if (interrupted) break;

                                    this.link.setText("Links : 0 / 0");

                                    if (!Arrays.isEmpty(proxies))
                                    {
                                        String proxy = proxies[Util.random.nextInt(proxies.length)];
                                        Request.setProxy(proxy);
                                        Request.forceBuild();
                                        this.proxy.setText("Proxy : " + proxy);
                                    }

                                    if (interrupted) break;

                                    String response = Request.sendGet(engine + "site:" + website + "+" + keyword.replace(" ", "+")).body();
                                    String[] matches = Arrays.removeDupes(Regex.getMatches(response, "https:\\/\\/" + website + "\\/\\w+")).stream().filter(link -> !scrapedLinks.contains(link)).toArray(String[]::new);

                                    if (interrupted) break;

                                    int size = matches.length;
                                    if (size > 0)
                                    {
                                        Console.ansiWriteLine(Foreground.GREEN, "Got " + size + " links.", LogType.INFO);
                                        status.setText("Status : Got " + size + " links.");

                                        scrape(matches);

                                        if (interrupted) break;
                                    }
                                    else
                                    {
                                        if (showErrors)
                                        {
                                            Console.ansiWriteLine(Foreground.RED, "Couldn't scrape any links.", LogType.ERROR);
                                            status.setText("Status : Couldn't scrape any links.");
                                        }

                                        if (interrupted) break;
                                        System.errorOccured = true;
                                    }

                                    if (!System.errorOccured)
                                        break;
                                }

                                if (interrupted) break;
                            }
                        }
                    }
                }

                resetStatus(false);
                this.retry.setText("Retry : 0 / " + retries);

                if (!interrupted)
                    Files.delete("settings/lastSettings.txt");
            });

            thread.start();
        });

        stop.addActionListener(e ->
        {
            interrupted = true;
            Console.ansiWriteLine(Foreground.GREEN, "Interrupted!", LogType.INFO);
            status.setText("Status : Interrupted!");
        });

        clean.addActionListener(e ->
        {
            ArrayList<String> result1 = Arrays.toArrayList(result.getText().split(System.newLine));
            result1 = Arrays.removeDupes(result1);
            result.setText(Arrays.toString(result1));
            size.setText("Size : " + result.getLineCount());
            Console.ansiWriteLine(Foreground.GREEN, "Removed duplicates!", LogType.INFO);
            status.setText("Status : Removed duplicates!");
        });

        save.addActionListener(e ->
        {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save your result");

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
            {
                String path = chooser.getSelectedFile().getAbsolutePath();
                path += (!path.endsWith(".txt") ? ".txt" : "");

                Files.writeLines(path, result.getText().split("\\r?\\n"), false);

                Console.ansiWriteLine(Foreground.GREEN, "Saved result to file '" + path + "'!", LogType.INFO);
                status.setText("Status : Saved result to file '" + path + "'!");
            }
        });

        JDialog dialog = new JDialog();
        Dimension dialogDimension = new Dimension(400, 400);

        dialog.setMinimumSize(dialogDimension);
        dialog.setMaximumSize(dialogDimension);
        dialog.setPreferredSize(dialogDimension);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setLayout(null);
        dialog.setTitle("Settings");

        refreshSettings();

        addLabel(dialog, "Pattern :", 20, 20, dialogDimension.width - 130, 20);
        JTextField pattern = addTextBox(dialog, this.pattern, true, 150, 20, dialogDimension.width - 170, 20);

        addLabel(dialog, "Retries :", 20, 40, dialogDimension.width - 130, 20);
        JTextField retries = addTextBox(dialog, String.valueOf(this.retries), true, 150, 40, dialogDimension.width - 170, 20);

        addLabel(dialog, "Timeout :", 20, 60, dialogDimension.width - 130, 20);
        JTextField timeout = addTextBox(dialog, String.valueOf(this.timeout), true, 150, 60, dialogDimension.width - 170, 20);

        addLabel(dialog, "Threads :", 20, 80, dialogDimension.width - 130, 20);
        JTextField threads = addTextBox(dialog, String.valueOf(this.threads), true, 150, 80, dialogDimension.width - 170, 20);

        addLabel(dialog, "Past 24 hours :", 20, 100, dialogDimension.width - 130, 20);
        JComboBox<Boolean> past24Hours = addSettingsComboBox(dialog, this.past24Hours, 150, 100, dialogDimension.width - 170, 20);

        addLabel(dialog, "Show errors :", 20, 120, dialogDimension.width - 130, 20);
        JComboBox<Boolean> showErrors = addSettingsComboBox(dialog, this.showErrors, 150, 120, dialogDimension.width - 170, 20);

        addLabel(dialog, "Log errors :", 20, 140, dialogDimension.width - 130, 20);
        JComboBox<Boolean> logErrors = addSettingsComboBox(dialog, this.logErrors, 150, 140, dialogDimension.width - 170, 20);

        addLabel(dialog, "Save last status :", 20, 160, dialogDimension.width - 130, 20);
        JComboBox<Boolean> saveLastStatus = addSettingsComboBox(dialog, this.saveLastStatus, 150, 160, dialogDimension.width - 170, 20);

        addLabel(dialog, "Lock proxies :", 20, 180, dialogDimension.width - 130, 20);
        JComboBox<Boolean> proxyLockToThread = addSettingsComboBox(dialog, this.proxyLockToThread, 150, 180, dialogDimension.width - 170, 20);

        addLabel(dialog, "Interface theme :", 20, 200, dialogDimension.width - 130, 20);
        JComboBox<String> interfaceTheme = addThemeComboBox(dialog, this.interfaceTheme, 150, 200, dialogDimension.width - 170, 20);

        addLabel(dialog, "Proxy file :", 20, 220, dialogDimension.width - 130, 20);
        JTextField proxyFile = addTextBox(dialog, this.proxyFile, true, 150, 220, dialogDimension.width - 170, 20);

        addLabel(dialog, "Links file :", 20, 240, dialogDimension.width - 130, 20);
        JTextField linksFile = addTextBox(dialog, this.linksFile, true, 150, 240, dialogDimension.width - 170, 20);

        addLabel(dialog, "User Agent :", 20, 260, dialogDimension.width - 130, 20);
        JTextField userAgent = addTextBox(dialog, this.userAgent.replace("+", " "), true, 150, 260, dialogDimension.width - 170, 20);

        JButton save2 = addButton(dialog, "Save", dialogDimension.width / 3 + 15, 300, 100, 40);
        save2.addActionListener(e2 ->
        {
            this.settings.setString("pattern", pattern.getText());
            this.settings.setInt("retries", Integer.parseInt(retries.getText()));
            this.settings.setInt("timeout", Integer.parseInt(timeout.getText()));
            this.settings.setString("past24Hours", String.valueOf(past24Hours.getSelectedItem()));
            this.settings.setString("showErrors", String.valueOf(showErrors.getSelectedItem()));
            this.settings.setString("logErrors", String.valueOf(logErrors.getSelectedItem()));
            this.settings.setString("proxyFile", proxyFile.getText());
            this.settings.setString("linksFile", linksFile.getText());
            this.settings.setString("saveLastStatus", String.valueOf(saveLastStatus.getSelectedItem()));
            this.settings.setString("proxyLockToThread", String.valueOf(proxyLockToThread.getSelectedItem()));
            this.settings.setString("interfaceTheme", String.valueOf(interfaceTheme.getSelectedItem()));
            this.settings.setInt("threads", Integer.parseInt(threads.getText()));
            this.settings.setString("userAgent", userAgent.getText().replace(" ", "+"));

            Console.ansiWriteLine(Foreground.GREEN, "Saved settings!", LogType.INFO);
        });

        settings.addActionListener(e -> dialog.setVisible(true));

        JDialog jEngines = JFrameHelper.createNewDialog(dialogDimension, "Engines");
        engines1.addActionListener(e -> jEngines.setVisible(true));

        JDialog jWebsites = JFrameHelper.createNewDialog(dialogDimension, "Websites");
        websites1.addActionListener(e -> jWebsites.setVisible(true));

        JDialog jKeywords = JFrameHelper.createNewDialog(dialogDimension, "Keywords");
        keywords1.addActionListener(e -> jKeywords.setVisible(true));

        setVisible(true);
    }

    private void scrape(String[] matches)
    {
        AtomicInteger link1 = new AtomicInteger();
        int links1 = matches.length;
        this.link.setText("Links : 0 / " + links1);

        Parallel.forEach(matches, link ->
        {
            for (int i = 0; i < 1; i++)
            {
                if (interrupted) break;

                if (!Arrays.isEmpty(proxies) && !Arrays.isEmpty(links))
                {
                    String proxy = proxies[Util.random.nextInt(proxies.length)];
                    boolean valid = false;

                    if (interrupted) break;

                    if (proxyLockToThread)
                    {
                        if (!proxyLocks.containsKey(Thread.currentThread().getName()))
                            proxyLocks.put(Thread.currentThread().getName(), proxy);

                        if (proxyLocks.get(Thread.currentThread().getName()).equals(proxy))
                            valid = true;
                    } else valid = true;

                    if (interrupted) break;

                    if (valid)
                    {
                        Request.setProxy(proxy);
                        Request.forceBuild();
                        this.proxy.setText("Proxy : " + proxy);
                    }
                }

                if (interrupted) break;

                String linkResponse = Request.sendGet(link)
                        .body()
                        .replace("\\n", "\n")
                        .replace("|", ":")
                        .replace("; ", ":")
                        .replace("   ", ":")
                        .replace(" ", ":");

                if (interrupted) break;

                if (link.contains("anonfiles.com") || link.contains("anonfile.com"))
                {
                    ArrayList<String> linkMatches = Regex.getMatches(linkResponse, "https:\\/\\/.*." + link + "\\/.*");
                    if (linkMatches.size() > 0)
                    {
                        if (interrupted) break;

                        String[] result = Arrays.toString(linkMatches).split(System.newLine);
                        for (String str : result)
                        {
                            if (interrupted) break;

                            String body = Request.sendGet(str.replace(">                    <img", "").replace("\"", "").replace(" ", "+")).body();
                            ArrayList<String> anonMatches = Regex.getMatches(body, pattern);

                            if (interrupted) break;

                            if (anonMatches.size() > 0)
                            {
                                Console.ansiWriteLine(Foreground.GREEN, "Scraped " + anonMatches.size() + " result.", LogType.INFO);
                                status.setText("Status : Scraped " + anonMatches.size() + " result.");

                                if (interrupted) break;

                                this.result.append(Arrays.toString(anonMatches) + System.newLine);

                                if (interrupted) break;

                                scrapedLinks.add(link);
                                this.size.setText("Size : " + this.result.getLineCount());
                            }
                            else
                            {
                                if (showErrors)
                                {
                                    Console.ansiWriteLine(Foreground.RED, "Couldn't scrape any result.", LogType.ERROR);
                                    status.setText("Status : Couldn't scrape any result.");
                                }
                            }

                            if (interrupted) break;
                        }

                        if (interrupted) break;
                    }
                    else
                    {
                        if (interrupted) break;

                        if (showErrors)
                        {
                            Console.ansiWriteLine(Foreground.RED, "Couldn't scrape any links.", LogType.ERROR);
                            status.setText("Status : Couldn't scrape any links.");
                        }
                    }
                }
                else
                {
                    ArrayList<String> linkMatches = Regex.getMatches(linkResponse, pattern);
                    int linkSize = linkMatches.size();

                    if (interrupted) break;

                    if (linkSize > 0)
                    {
                        Console.ansiWriteLine(Foreground.GREEN, "Scraped " + linkSize + " result.", LogType.INFO);
                        status.setText("Status : Scraped " + linkSize + " result.");

                        if (interrupted) break;

                        result.append(Arrays.toString(linkMatches) + System.newLine);

                        if (interrupted) break;

                        scrapedLinks.add(link);
                        this.size.setText("Size : " + result.getLineCount());
                    }
                    else
                    {
                        if (showErrors)
                        {
                            Console.ansiWriteLine(Foreground.RED, "Couldn't scrape any result.", LogType.ERROR);
                            status.setText("Status : Couldn't scrape any result.");
                        }
                    }

                    if (interrupted) break;
                }

                if (interrupted) break;

                link1.getAndIncrement();
                this.link.setText("Links : " + link1 + " / " + links1);
            }
        });
    }

    private void refreshSettings()
    {
        util.refreshFiles();
        interrupted = false;

        settings = util.getSettings("settings/settings.txt");

        retries = settings.getInt("retries");
        timeout = settings.getInt("timeout");
        threads = settings.getInt("threads");
        past24Hours = settings.getBoolean("past24Hours");
        showErrors = settings.getBoolean("showErrors");
        logErrors = settings.getBoolean("logErrors");
        saveLastStatus = settings.getBoolean("saveLastStatus");
        pattern = settings.getString("pattern");
        userAgent = settings.getString("userAgent");
        proxyFile = settings.getString("proxyFile");
        linksFile = settings.getString("linksFile");
        interfaceTheme = settings.getString("interfaceTheme");
        proxyLockToThread = settings.getBoolean("proxyLockToThread");

        System.showErrors = showErrors;
        System.logErrors = logErrors;

        if (Files.fileExists(proxyFile))
        {
            FileCaching.cache(proxyFile);
            proxies = new String(FileCaching.get(proxyFile), StandardCharsets.UTF_8).split(System.newLine);
        }

        if (Files.fileExists(linksFile))
        {
            FileCaching.cache(linksFile);
            links = new String(FileCaching.get(linksFile), StandardCharsets.UTF_8).split(System.newLine);
        }

        String[] lastSettings = Files.fileExists("settings/lastSettings.txt") ? Files.readFile("settings/lastSettings.txt").split("-_-_-_-_-") : new String[] { "None", "None", "None" };
        lastEngine = lastSettings[0].equals("None") ? util.getEngines()[0] : lastSettings[0];
        lastWebsite = lastSettings[1].equals("None") ? util.getWebsites()[0] : lastSettings[1];
        lastKeyword = lastSettings[2].equals("None") ? util.getKeywords()[0] : lastSettings[2];
    }

    private void updateTitle()
    {
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = total - free;

        Console.setTitle("Memory : " + used / 1024 / 1024 + " / " + total / 1024 / 1024 + " MB");
    }

    private void resetStatus(boolean start)
    {
        proxy.setText("Proxy : None");
        status.setText("Status : IDLE");
        engine.setText("Engine : None (0 / 0)");
        website.setText("Website : None (0 / 0)");
        keyword.setText("Keyword : None (0 / 0)");
        link.setText("Links : 0 / 0");
        retry.setText("Retry : 0 / " + retries);

        if (start)
        {
            if (!saveLastStatus)
            {
                scrapedLinks.clear();
                proxyLocks.clear();
                result.setText("");
                size.setText("Size : 0");
            }
        }
        else
        {
            scrapedLinks.clear();
            proxyLocks.clear();
        }
    }

    public static void main(String[] args)
    {
        new Main();
    }
}
