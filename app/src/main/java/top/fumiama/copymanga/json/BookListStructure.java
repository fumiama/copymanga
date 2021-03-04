package top.fumiama.copymanga.json;

public class BookListStructure extends ReturnBase {
    public Results results;
    public static class Results {
        public int total;
        public ComicStructure[] list;
        public int limit;
        public int offset;
    }
}