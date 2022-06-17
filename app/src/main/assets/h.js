javascript:
if (typeof (loaded) == "undefined"){
    var loaded = true;
    function scanChapters(chapter) {
        var chapterList = chapter.getElementsByClassName("tab-pane fade show active")[0].getElementsByTagName("ul")[0].getElementsByTagName("a");
        var chapterArr = Array();
        for(var i = 0; i < chapterList.length; i++){
            chapterArr.push(JSON.constructor());
            chapterArr[i]["name"] = chapterList[i].title;
            chapterArr[i]["url"] = chapterList[i].href;
        }
        return chapterArr;
    }
    function modify() {
        var url = location.href;
        if(url.indexOf("/chapter/")>0){
            window.scroll({ top: document.body.scrollHeight, left: 0, behavior: 'smooth' });
            setTimeout(() => {
                window.scroll({ top: document.body.scrollHeight, left: 0, behavior: 'smooth' });
                setTimeout(() => {
                    window.scroll({ top: document.body.scrollHeight, left: 0, behavior: 'smooth' });
                    var imglist = document.getElementsByClassName("container-fluid comicContent")[0].getElementsByTagName("li");
                    var nextChapter = document.getElementsByClassName("comicContent-next")[0].getElementsByTagName("a")[0].href;
                    var prevChapter = document.getElementsByClassName("comicContent-prev")[1].getElementsByTagName("a")[0].href;
                    if(nextChapter == location.href) nextChapter = "null";
                    if(prevChapter == location.href) prevChapter = "null";
                    var liststr = document.title.split(" - ")[1] + " " + location.href.substring(location.href.lastIndexOf("/")+1) + "\n" + nextChapter + "\n" + prevChapter;
                    for(var i = 0; i < imglist.length; i++) liststr += "\n" + imglist[i].getElementsByTagName("img")[0].dataset.src;
                    GM.loadChapter(liststr);
                }, 500);
            }, 500);
        } else {
            var json = Array();
            var chapters = document.getElementsByClassName("upLoop")[0].children;
            var newObj = null;
            for(var i = 0; i < chapters.length; i++) {
                if(i % 2) {
                    newObj["chapters"] = scanChapters(chapters[i]);
                    json.push(newObj);
                    newObj = null;
                }
                else {
                    newObj = JSON.constructor();
                    newObj["name"] = chapters[i].innerText;
                }
            }
            GM.setTitle(document.getElementsByTagName("h6")[0].title);
            GM.setFab(JSON.stringify(json));
        }
    }
    modify();
}else modify();