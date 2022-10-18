package top.fumiama.copymanga.json;

public class LoginInfoStructure extends ReturnBase{
    public Results results;
    public static class Results {
        public String token;
        public String user_id;
        public String username;
        public String nickname;
        public String avatar;
    }
}
