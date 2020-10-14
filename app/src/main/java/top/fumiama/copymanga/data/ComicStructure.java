package top.fumiama.copymanga.data;

public class ComicStructure {
    public String name;
    public Chapters[] chapters;
    public static class Chapters{
        public String name;
        public String url;
    }
}