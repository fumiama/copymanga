package top.fumiama.copymanga.json;

public class NetworkStructure extends ReturnBase {
    public Results results;
    public static class Results {
        public String[] share;
        public String[][] api;
    }
}
