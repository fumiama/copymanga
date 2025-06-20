javascript:
if (typeof (loaded) == "undefined") {
    var loaded = true;
    function scanChapters(chapter) {
        var chapterList = chapter.getElementsByClassName("tab-pane fade show active")[0].getElementsByTagName("ul")[0].getElementsByTagName("a");
        var chapterArr = Array();
        for (var i = 0; i < chapterList.length; i++) {
            chapterArr.push(JSON.constructor());
            chapterArr[i]["name"] = chapterList[i].title;
            chapterArr[i]["url"] = chapterList[i].href;
        }
        return chapterArr;
    }
    function smoothLoadChapter(speed, interval) {
        let prevHeight = document.body.scrollHeight;
        let lastTime = 0;
        let ticking = false;
        function requestTick() {
            if (!ticking) {
                ticking = true;
                requestAnimationFrame(step);
            }
        }
        function step(timestamp) {
            if (!lastTime) lastTime = timestamp;
            const elapsed = timestamp - lastTime;
            if (elapsed >= interval) {
                const index = document.getElementsByClassName("comicIndex")[0].innerText;
                const count = document.getElementsByClassName("comicCount")[0].innerText;
                GM.setLoadingDialogProgress(index, count);
                window.scrollBy(0, speed);
                lastTime = timestamp;
                const currentHeight = document.body.scrollHeight;
                if (Math.round(window.innerHeight+window.scrollY+0.5) >= currentHeight) { /*避免小数不符无法触发*/
                    if (currentHeight === prevHeight) {
                        var images = document.getElementsByClassName("container-fluid comicContent")[0].getElementsByTagName("li");
                        var nextChapter = document.getElementsByClassName("comicContent-next")[0].getElementsByTagName("a")[0].href;
                        var prevChapter = document.getElementsByClassName("comicContent-prev")[1].getElementsByTagName("a")[0].href;
                        if(nextChapter == location.href) nextChapter = "null";
                        if(prevChapter == location.href) prevChapter = "null";
                        var result = document.title.split(" - ")[1] + " " + location.href.substring(location.href.lastIndexOf("/")+1) + "\n" + nextChapter + "\n" + prevChapter;
                        for(var i = 0; i < images.length; i++) result += "\n" + images[i].getElementsByTagName("img")[0].dataset.src;
                        GM.setLoadingDialog(false);
                        GM.loadChapter(result);
                        return;
                    }
                    prevHeight = currentHeight;
                }
            }
            ticking = false;
            requestTick();
        }
        requestTick();
    }
    function modify() {
        var url = location.href;
        if(url.indexOf("/chapter/") > 0){
            GM.setLoadingDialog(true);
            smoothLoadChapter(320, 16);
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
} else modify();