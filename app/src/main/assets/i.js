javascript:
if (typeof (loaded) == "undefined") {
    var loaded = true;
    var invoke = {
        preUrl: "",
        hideRanobeTab: function () {
            var tabs = document.getElementsByClassName("van-tabbar-item");
            for (i = 0; i < tabs.length; i++) {
                if (tabs[i].innerText == "輕小說") tabs[i].style = "display: none;";
            }
        },
        hideRanobeRack: function () {
            var tabs = document.getElementsByClassName("van-tabs van-tabs--line");
            if (tabs.length) tabs[0].hidden = true;
        },
        pinTitle: function () {
            var game = document.getElementsByName("exchange");
            if (game.length) game[0].hidden = true;
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
        if (url.endsWith("/index")) {
            invoke.pinTitle();
            invoke.hideRanobeTab();
        }
        else if (url.endsWith("/bookrack")) {
            invoke.hideRanobeTab();
            invoke.hideRanobeRack();
        }
        else if (url.indexOf("/searchContent") > 0) {
            invoke.hideRanobeRack();
        }
        else if (url.indexOf("/comicContent/") > 0) setTimeout(function () { invoke.loadChapter() }, 1000);
        else if (url.indexOf("/details/comic/") > 0) GM.loadComic(url);
        else if (url.indexOf("/personal") > 0) {
            invoke.hideRanobeTab();
            GM.enterProfile();
        }
    }
    modify();
    invoke.urlChangeListener(modify);
} else {
    setTimeout(modify, 1280);
}