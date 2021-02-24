package open.nano.scraper;

import open.java.toolkit.files.FileParser;
import open.java.toolkit.files.Files;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Util
{
    private String[] engines, websites, keywords;
    public static SecureRandom random;

    static
    {
        try
        {
            random = SecureRandom.getInstanceStrong();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public static Util getNewInstance()
    {
        return new Util();
    }

    public Util()
    {
        refreshFiles();
    }

    public void refreshFiles()
    {
        engines = Files.readLines("settings/engines.txt");
        websites = Files.readLines("settings/websites.txt");
        keywords = Files.readLines("settings/keywords.txt");
    }

    public String[] getEngines()
    {
        return engines;
    }

    public String[] getWebsites()
    {
        return websites;
    }

    public String[] getKeywords()
    {
        return keywords;
    }

    public FileParser getSettings(String path)
    {
        return new FileParser(path, "#", "%", true, 1);
    }
}
