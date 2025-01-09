package top.fumiama.copymanga.json;

public class TypeBookListStructure extends ReturnBase {
    public Results results;
    public static class Results extends InfoBase {
        public TypeBookStructure[] list;
    }
}