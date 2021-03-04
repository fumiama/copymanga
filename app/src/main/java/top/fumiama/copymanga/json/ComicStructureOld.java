package top.fumiama.copymanga.json;

public class ComicStructureOld {
    public String name;
    public Chapters[] chapters;
    public static class Chapters{
        public String name;
        public String url;
    }
}