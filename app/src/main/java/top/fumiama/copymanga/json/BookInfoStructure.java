package top.fumiama.copymanga.json;

import java.util.HashMap;

public class BookInfoStructure extends ReturnBase {
    public Results results;
    public static class Results extends ResultsBase{
        public ComicStructure comic;
        public HashMap<String, ThemeStructure> groups;
    }
}
