package top.fumiama.copymanga.json;

public class TopicStructure extends ReturnBase {
    public Results results;
    public static class Results {
        public String title;
        public String period;
        public String intro;
        public int type;
        public String datetime_created;
    }
}
