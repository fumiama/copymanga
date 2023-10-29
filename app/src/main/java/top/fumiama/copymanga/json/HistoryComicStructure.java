package top.fumiama.copymanga.json;

public class HistoryComicStructure {
    public String uuid;
    public boolean b_display;
    public String name;
    public String path_word;
    public ThemeStructure[] author;
    public ThemeStructure[] theme;
    public String cover;
    public int status;
    public int popular;
    public String datetime_updated;
    public String last_chapter_id;
    public String last_chapter_name;
    public Browse browse;

    public static class Browse {
        public String comic_uuid;
        public String path_word;
        public String chapter_uuid;
        public String chapter_name;

    }
}
