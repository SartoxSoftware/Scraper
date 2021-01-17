package open.nano.scraper;

import open.java.toolkit.Arrays;
import open.java.toolkit.Regex;
import open.java.toolkit.System;
import open.java.toolkit.console.Console;
import open.java.toolkit.console.LogType;
import open.java.toolkit.console.ansi.Foreground;
import open.java.toolkit.files.FileParser;
import open.java.toolkit.files.Files;
import open.java.toolkit.http.Request;
import open.java.toolkit.http.RequestUtils;
import open.java.toolkit.threading.Parallel;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.http.HttpClient;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static open.nano.scraper.JFrameHelper.*;

public class Main extends JFrame
{
    private Thread thread;
    private final JTextArea result;
    private final JLabel status, engine, website, keyword, size, proxy, link, retry;
    private String lastEngine = "None", lastWebsite = "None", lastKeyword = "None";
    private final ArrayList<String> scrapedLinks = new ArrayList<>();
    private boolean error = false;
    private final SecureRandom random = SecureRandom.getInstanceStrong();
    private final Util util = Util.getNewInstance();
    private FileParser settings = util.getSettings("settings/settings.txt");

    private int retries, timeout, threads;
    private boolean past24Hours, showErrors, logErrors, saveLastStatus;
    private String pattern, userAgent, proxyFile, linksFile, interfaceTheme;
    private boolean proxyLockToThread = true;

    public Main() throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, NoSuchAlgorithmException
    {
        Timer titleTimer = new Timer(1000, (e ->
        {
            try
            {
                updateTitle();
            } catch (IOException | InterruptedException ignored) {}
        }));
        titleTimer.setRepeats(true);
        titleTimer.start();

        if (settings.getString("interfaceTheme").equals("dark"))
        {
            configureDarkTheme(this);
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if (info.getName().equals("Nimbus"))
                {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
        }

        Dimension dimension = new Dimension(800, 600);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
        setPreferredSize(dimension);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Nano Scraper v1.2");
        setDefaultLookAndFeelDecorated(true);

        JButton start = addButton(this, "Start", 20, 20, 110, 40);
        JButton stop = addButton(this, "Stop", 20, 70, 110, 40);
        JButton clean = addButton(this, "Clean", 20, 120, 110, 40);
        JButton save = addButton(this, "Save", 20, 170, 110, 40);
        JButton settings = addButton(this, "Settings", 20, 220, 110, 40);
        JButton engines1 = addButton(this, "Engines", 20, 270, 110, 40);
        JButton websites1 = addButton(this, "Websites", 20, 320, 110, 40);
        JButton keywords1 = addButton(this, "Keywords", 20, 370, 110, 40);

        result = (JTextArea) addScrollPane(this, addTextBox(this, "", false, 150, 20, dimension.width - 160, dimension.height - 180));
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
                try
                {
                    refreshSettings();
                } catch (IOException ignored) {}

                String[] proxies = new String[0];
                String[] links = new String[0];

                try
                {
                    proxies = Files.readLines(proxyFile);
                    links = Files.readLines(linksFile);
                } catch (IOException ignored) {}

                Console.ansiWriteLine(Foreground.GREEN, "Started!", LogType.INFO);
                Request.setTimeout(timeout);
                Request.setContentType("text/html; charset=utf-8");
                Request.setUserAgent(userAgent.replace("+", " "));
                Request.setFollowRedirects(HttpClient.Redirect.ALWAYS);
                Request.setVersion(HttpClient.Version.HTTP_2);
                Parallel.threads = threads;

                resetStatus(true);

                if (!Arrays.isEmpty(links))
                {
                    try
                    {
                        scrape(links, proxies, links, random, pattern, showErrors);
                        thread.interrupt();
                    } catch (IOException | InterruptedException ignored) {}
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

                scraping:
                for (int ii = i; ii < engines.length; ii++)
                {
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
                    try
                    {
                        Files.writeString("settings/lastSettings.txt", lastEngine + "-_-_-_-_-" + lastWebsite + "-_-_-_-_-" + lastKeyword, false);
                    } catch (IOException ignored) {}

                    this.engine.setText("Engine : " + RequestUtils.urlDomainOnly(engine) + " (" + (ii + 1) + " / " + engines.length + ")");

                    for (int jj = j; jj < websites.length; jj++)
                    {
                        String website = websites[jj];

                        lastWebsite = website;
                        try
                        {
                            Files.writeString("settings/lastSettings.txt", lastEngine + "-_-_-_-_-" + lastWebsite + "-_-_-_-_-" + lastKeyword, false);
                        } catch (IOException ignored) {}

                        this.website.setText("Website : " + website + " (" + (jj + 1) + " / " + websites.length + ")");

                        for (int kk = k; kk < keywords.length; kk++)
                        {
                            String keyword = keywords[kk];

                            lastKeyword = keyword;
                            try
                            {
                                Files.writeString("settings/lastSettings.txt", lastEngine + "-_-_-_-_-" + lastWebsite + "-_-_-_-_-" + lastKeyword, false);
                            } catch (IOException ignored) {}

                            this.keyword.setText("Keyword : " + keyword + " (" + (kk + 1) + " / " + keywords.length + ")");
                            this.retry.setText("Retry : 0 / " + retries);

                            for (int r = 0; r <= retries; r++)
                            {
                                if (error)
                                {
                                    this.retry.setText("Retry : " + r + " / " + retries);
                                    error = false;
                                }

                                try
                                {
                                    this.link.setText("Links : 0 / 0");

                                    if (!Arrays.isEmpty(proxies))
                                    {
                                        String proxy = proxies[random.nextInt(proxies.length)];
                                        Request.setProxy(proxy);
                                        Request.forceBuild();
                                        this.proxy.setText("Proxy : " + proxy);
                                    }

                                    String response = Request.sendGet(engine + "site:" + website + "+" + keyword.replace(" ", "+")).body();
                                    String[] matches = Arrays.removeDupes(Regex.getMatches(response, "https:\\/\\/" + website + "\\/\\w+")).stream().filter(link -> !scrapedLinks.contains(link)).toArray(String[]::new);

                                    int size = matches.length;
                                    if (size > 0)
                                    {
                                        Console.ansiWriteLine(Foreground.GREEN, "Got " + size + " links.", LogType.INFO);
                                        status.setText("Status : Got " + size + " links.");

                                        scrape(matches, proxies, links, random, pattern, showErrors);
                                    }
                                    else
                                    {
                                        if (showErrors)
                                        {
                                            Console.ansiWriteLine(Foreground.RED, "Couldn't scrape any links.", LogType.ERROR);
                                            status.setText("Status : Couldn't scrape any links.");
                                        }

                                        error = true;
                                    }
                                }
                                catch (InterruptedException ex)
                                {
                                    break scraping;
                                }
                                catch (Throwable th)
                                {
                                    if (showErrors)
                                        th.printStackTrace();

                                    if (logErrors)
                                    {
                                        try
                                        {
                                            Files.writeString("errors.txt", th.getMessage() + System.newLine, true);
                                        } catch (IOException ignored) {}
                                    }

                                    error = true;
                                }

                                if (!error)
                                    break;
                            }
                        }
                    }
                }

                resetStatus(false);
                this.retry.setText("Retry : 0 / " + retries);
            });

            thread.start();
        });

        stop.addActionListener(e ->
        {
            thread.interrupt();
            Console.ansiWriteLine(Foreground.GREEN, "Interrupted!", LogType.INFO);
            status.setText("Status : Interrupted!");
        });

        clean.addActionListener(e ->
        {
            ArrayList<String> result1 = Arrays.toArrayList(result.getText().split(System.newLine));
            result1 = Arrays.removeDupes(result1);
            result.setText(java.util.Arrays.toString(result1.toArray(new String[0])).replace(", ", System.newLine).replace("[", "").replace("]", ""));
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

                try
                {
                    Files.writeLines(path + ".txt", result.getText().split("\\r?\\n"), false);
                } catch (IOException ignored) {}

                Console.ansiWriteLine(Foreground.GREEN, "Saved result to file '" + path + "'!", LogType.INFO);
                status.setText("Status : Saved result to file '" + path + "'!");
            }
        });

        JDialog dialog = new JDialog();
        Dimension dialogDimension = new Dimension(400, 380);

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
        JComboBox<Boolean> past24Hours = addComboBox(dialog, this.past24Hours, 150, 100, dialogDimension.width - 170, 20);
        addLabel(dialog, "Show errors :", 20, 120, dialogDimension.width - 130, 20);
        JComboBox<Boolean> showErrors = addComboBox(dialog, this.showErrors, 150, 120, dialogDimension.width - 170, 20);
        addLabel(dialog, "Log errors :", 20, 140, dialogDimension.width - 130, 20);
        JComboBox<Boolean> logErrors = addComboBox(dialog, this.logErrors, 150, 140, dialogDimension.width - 170, 20);
        addLabel(dialog, "Save last status :", 20, 160, dialogDimension.width - 130, 20);
        JComboBox<Boolean> saveLastStatus = addComboBox(dialog, this.saveLastStatus, 150, 160, dialogDimension.width - 170, 20);
        addLabel(dialog, "Interface theme :", 20, 180, dialogDimension.width - 130, 20);
        JComboBox<String> interfaceTheme = addThemeComboBox(dialog, this.interfaceTheme, 150, 180, dialogDimension.width - 170, 20);
        addLabel(dialog, "Proxy file :", 20, 200, dialogDimension.width - 130, 20);
        JTextField proxyFile = addTextBox(dialog, this.proxyFile, true, 150, 200, dialogDimension.width - 170, 20);
        addLabel(dialog, "Links file :", 20, 220, dialogDimension.width - 130, 20);
        JTextField linksFile = addTextBox(dialog, this.linksFile, true, 150, 220, dialogDimension.width - 170, 20);
        addLabel(dialog, "User Agent :", 20, 240, dialogDimension.width - 130, 20);
        JTextField userAgent = addTextBox(dialog, this.userAgent.replace("+", " "), true, 150, 240, dialogDimension.width - 170, 20);

        JButton save2 = addButton(dialog, "Save", dialogDimension.width / 3 + 15, 280, 100, 40);
        save2.addActionListener(e2 ->
        {
            try
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
                this.settings.setString("interfaceTheme", String.valueOf(interfaceTheme.getSelectedItem()));
                this.settings.setInt("threads", Integer.parseInt(threads.getText()));
                this.settings.setString("userAgent", userAgent.getText().replace(" ", "+"));

                Console.ansiWriteLine(Foreground.GREEN, "Saved settings!", LogType.INFO);
            } catch (IOException ignored) {}
        });

        settings.addActionListener(e -> dialog.setVisible(true));

        JDialog jEngines = createNewDialog(dialogDimension, "Engines");
        engines1.addActionListener(e -> jEngines.setVisible(true));

        JDialog jWebsites = createNewDialog(dialogDimension, "Websites");
        websites1.addActionListener(e -> jWebsites.setVisible(true));

        JDialog jKeywords = createNewDialog(dialogDimension, "Keywords");
        keywords1.addActionListener(e -> jKeywords.setVisible(true));

        setVisible(true);
    }

    private void scrape(String[] matches, String[] proxies, String[] links, SecureRandom random, String pattern, boolean showErrors) throws IOException, InterruptedException
    {
        AtomicInteger link1 = new AtomicInteger();
        int links1 = matches.length;
        this.link.setText("Links : 0 / " + links1);

        Parallel.forEach(matches, link ->
        {
            if (!Arrays.isEmpty(proxies) && !Arrays.isEmpty(links))
            {
                String proxy = proxies[random.nextInt(proxies.length)];
                Request.setProxy(proxy);
                Request.forceBuild();
                this.proxy.setText("Proxy : " + proxy);
            }

            String linkResponse = Request.sendGet(link).body().replace("|", ":");

            if (!linkResponse.contains(":"))
                linkResponse = linkResponse.replace(" ", ":");

            if (link.contains("anonfiles.com"))
            {
                ArrayList<String> linkMatches = Regex.getMatches(linkResponse, "https:\\/\\/.*.anonfiles.com\\/.*");
                if (linkMatches.size() > 0)
                {
                    String[] result = linkMatches.toString().replace(", ", System.newLine).replace("[", "").replace("]", "").split(System.newLine);
                    for (String str : result)
                    {
                        String body = Request.sendGet(str.replace(">                    <img", "").replace("\"", "").replace(" ", "+")).body();
                        int size = body.split(System.newLine).length;
                        Console.ansiWriteLine(Foreground.GREEN, "Scraped " + size + " result", LogType.INFO);
                        status.setText("Status : Scraped " + size + " result");

                        if (size > 33000)
                        {
                            int substringSize = size / 3;

                            int totalSubstrings = (int) Math.ceil((double)body.length() / substringSize);
                            String[] strSubstrings = new String[totalSubstrings];

                            int index = 0;
                            for(int i = 0; i < body.length(); i = i + substringSize)
                                strSubstrings[index++] = body.substring(i, Math.min(i + substringSize, body.length()));

                            for (String part : strSubstrings)
                                this.result.append(part + System.newLine);
                        } else this.result.append(body + System.newLine);

                        scrapedLinks.add(link);
                        this.size.setText("Size : " + this.result.getLineCount());
                    }
                }
                else
                {
                    if (showErrors)
                    {
                        Console.ansiWriteLine(Foreground.RED, "Couldn't scrape any result", LogType.ERROR);
                        status.setText("Status : Couldn't scrape any result");
                    }
                }
            }
            else
            {
                ArrayList<String> linkMatches = Regex.getMatches(linkResponse, pattern);
                int linkSize = linkMatches.size();

                if (linkSize > 0)
                {
                    Console.ansiWriteLine(Foreground.GREEN, "Scraped " + linkSize + " result", LogType.INFO);
                    status.setText("Status : Scraped " + linkSize + " result");

                    if (linkSize > 33000)
                    {
                        int substringSize = linkSize / 3;

                        int totalSubstrings = (int) Math.ceil((double)linkMatches.size() / substringSize);
                        String[] strSubstrings = new String[totalSubstrings];

                        int index = 0;
                        for(int i = 0; i < linkMatches.size(); i = i + substringSize)
                            strSubstrings[index++] = linkMatches.subList(i, Math.min(i + substringSize, linkMatches.size())).toString().replace(", ", System.newLine).replace("[", "").replace("]", "");

                        for (String part : strSubstrings)
                            result.append(part + System.newLine);
                    } else result.append(linkMatches.toString().replace(", ", System.newLine).replace("[", "").replace("]", "") + System.newLine);

                    scrapedLinks.add(link);
                    this.size.setText("Size : " + result.getLineCount());
                }
                else
                {
                    if (showErrors)
                    {
                        Console.ansiWriteLine(Foreground.RED, "Couldn't scrape any result", LogType.ERROR);
                        status.setText("Status : Couldn't scrape any result");
                    }
                }
            }

            link1.getAndIncrement();
            this.link.setText("Links : " + link1 + " / " + links1);
        });
    }

    private void refreshSettings() throws IOException
    {
        this.settings = util.getSettings("settings/settings.txt");

        retries = this.settings.getInt("retries");
        timeout = this.settings.getInt("timeout");
        threads = this.settings.getInt("threads");
        past24Hours = this.settings.getBoolean("past24Hours");
        showErrors = this.settings.getBoolean("showErrors");
        logErrors = this.settings.getBoolean("logErrors");
        saveLastStatus = this.settings.getBoolean("saveLastStatus");
        pattern = this.settings.getString("pattern");
        userAgent = this.settings.getString("userAgent");
        proxyFile = this.settings.getString("proxyFile");
        linksFile = this.settings.getString("linksFile");
        interfaceTheme = this.settings.getString("interfaceTheme");

        String[] lastSettings = Files.readFile("settings/lastSettings.txt").split(System.newLine)[0].split("-_-_-_-_-");
        lastEngine = lastSettings[0];
        lastWebsite = lastSettings[1];
        lastKeyword = lastSettings[2];
    }

    private void updateTitle() throws IOException, InterruptedException
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
                result.setText("");
                size.setText("Size : 0");
            }
        } else scrapedLinks.clear();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, NoSuchAlgorithmException
    {
        new Main();
    }
}
