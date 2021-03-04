package top.fumiama.copymanga.json;

public class IndexStructure extends ReturnBase {
    public Results results;
    public static class Results{
        public Banners[] banners;
        public Topics topics;
        public RecComics recComics;
        public RankComics rankDayComics;
        public RankComics rankWeekComics;
        public RankComics rankMonthComics;
        public ComicWrap[] hotComics;
        public ComicWrap[] newComics;
        public FinishComics finishComics;

        public static class Banners{
            public String cover;
            public String brief;
            public String out_uuid;
            public ComicStructure comic;
        }
        public static class Topics extends InfoBase{
            public List[] list;

            public static class List{
                public String title;
                public SeriesStructure series;
                public String journal;
                public String cover;
                public String period;
                public int type;
                public String brief;
                public String path_word;
                public String datetime_created;
            }
        }
        public static class RecComics extends InfoBase{
            public List[] list;

            public static class List{
                public int type;
                public ComicStructure comic;
            }
        }
        public static class RankComics extends InfoBase{
            public InfoStructure[] list;
        }
        public static class ComicWrap{
            public ComicStructure comic;
        }
        public static class FinishComics extends InfoBase{
            public ComicStructure[] list;
            public String path_word;
            public String name;
            public String type;
        }
    }
}
