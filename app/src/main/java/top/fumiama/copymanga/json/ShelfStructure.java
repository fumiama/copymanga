package top.fumiama.copymanga.json;

public class ShelfStructure extends ReturnBase {
    public Results results;
    public static class Results extends InfoBase {
        public List[] list;
        public static class List {
            public int uuid;
            public boolean b_folder;
            public LastBrowseStructure last_browse;
            public HistoryComicStructure comic;
        }
    }
}
