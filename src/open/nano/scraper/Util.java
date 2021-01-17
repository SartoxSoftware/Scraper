package open.nano.scraper;

import open.java.toolkit.files.FileParser;
import open.java.toolkit.files.Files;

import java.io.IOException;

public class Util
{
    private String[] engines, websites, keywords;

    public static Util getNewInstance() throws IOException
    {
        return new Util();
    }

    public Util() throws IOException
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

    public FileParser getSettings(String path) throws IOException
    {
        return new FileParser(path, "#", "=", true, 1);
    }
}
