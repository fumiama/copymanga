javascript:
if (typeof (loaded) == "undefined") {
    var loaded = true;
    var invoke = {
        preUrl: "",
        pinTitle: function () {
            /*document.getElementsByClassName("van-button__content")[2].click();*/
            document.getElementsByClassName("indexTitle")[0].style.position = "fixed";
            document.getElementsByClassName("indexTitle")[0].style.zIndex = 999;
            document.getElementsByClassName("indexTitle")[0].style.width = document.body.clientWidth - 18 + "px";
            document.getElementsByClassName("copySwiper")[0].style.marginTop = "56px";
            document.getElementsByClassName("indexTitle")[0].style.marginTop = "-56px";
        },
        notCallGM: function (url) {
            if (this.preUrl == url) return false;
            else {
                this.preUrl = url;
                return true;
            }
        },
        clickClass: function (name, index) { document.getElementsByClassName(name)[index].click(); },
        clickClassCenter: function (name, index) {
            var ev = document.createEvent('HTMLEvents');
            ev.clientX = innerWidth / 2;
            ev.clientY = innerHeight / 2;
            ev.initEvent('click', false, true);
            document.getElementsByClassName(name)[index].dispatchEvent(ev);
        },
        resetPreUrl: function () { this.preUrl = ""; },
        loadChapter: function () { this.clickClassCenter("comicContentPopupImageItem", 0); GM.loadComic(location.href); },
        urlChangeListener: function (todo) {
            setInterval(function () { if (invoke.notCallGM(location.href)) { todo(); } }, 1000);
        }
    };
    function modify() {
        var url = location.href;
        GM.hideFab();
        if (url.endsWith("/index")) invoke.pinTitle();
        else if (url.indexOf("/comicContent/") > 0) setTimeout(function () { invoke.loadChapter() }, 1000);
        else if (url.indexOf("/details/comic/") > 0) GM.loadComic(url);
    }
    modify();
    invoke.urlChangeListener(modify);
} else {
    setTimeout(modify, 1280);
}