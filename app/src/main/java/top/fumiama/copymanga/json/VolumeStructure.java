package top.fumiama.copymanga.json;

public class VolumeStructure extends ReturnBase {
    public Results results;
    public static class Results extends InfoBase{
        public ChapterStructure[] list;
    }
}
