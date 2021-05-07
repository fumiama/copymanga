package top.fumiama.copymanga.json;

public class TypeBookListStructure extends ReturnBase {
    public Results results;
    public static class Results {
        public int total;
        public TypeBook[] list;
        public int limit;
        public int offset;
    }
    public static class TypeBook {
        public int type;
        public String name;
        public String datetime_created;
        public ComicStructure comic;
    }
}