package top.fumiama.copymanga.json;

public class Chapter2Return extends ReturnBase {
    public Results results;
    public static class Results extends ResultsBase{
        public ComicStructure comic;
        public ChapterWithContent chapter;
    }
}
