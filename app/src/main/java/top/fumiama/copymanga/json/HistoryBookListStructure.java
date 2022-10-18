package top.fumiama.copymanga.json;

public class HistoryBookListStructure extends ReturnBase {
    public Results results;
    public static class Results {
        public ListItem[] list;
        public int total;
        public int limit;
        public int offset;
    }
    public static class ListItem {
        public String last_chapter_id;
        public String last_chapter_name;
        public HistoryComicStructure comic;
    }
}
