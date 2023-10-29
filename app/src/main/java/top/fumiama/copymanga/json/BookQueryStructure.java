package top.fumiama.copymanga.json;

public class BookQueryStructure extends ReturnBase {
    public Results results;
    public static class Results{
        public Browse browse;
        public int collect;
        public boolean is_lock;
        public boolean is_login;
        public boolean is_mobile_bind;
        public boolean is_vip;
        public static class Browse {
            public String comic_uuid;
            public String comic_id;
            public String path_word;
            public String chapter_uuid;
            public String chapter_id;
            public String chapter_name;
        }
    }
}
