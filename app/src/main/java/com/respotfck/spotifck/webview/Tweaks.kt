package com.respotfck.spotifck.webview

import android.content.SharedPreferences
import org.json.JSONObject

object TweakPrefs {
    const val HIDE_UPSELLS = "HideUpsells"
    const val HIDE_PODCASTS = "HidePodcasts"
    const val HIDE_SPONSORED = "HideSponsored"
    const val HIDE_CANVAS = "HideCanvas"
    const val LYRICS_FALLBACK = "LyricsFallback"
    const val REMEMBER_MODES = "RememberModes"
    const val NO_SMART_SHUFFLE = "NoSmartShuffle"
    const val DATA_SAVER = "DataSaver"
    const val START_PAGE = "StartPage"
    const val SAVED_SHUFFLE = "SavedShuffle"
    const val SAVED_REPEAT = "SavedRepeat"

    class StartPage(val id: String, val label: String, val url: String)

    val startPages = listOf(
        StartPage("liked", "Liked Songs", "https://open.spotify.com/collection/tracks"),
        StartPage("home", "Home", "https://open.spotify.com/"),
        StartPage("search", "Search", "https://open.spotify.com/search"),
        StartPage("library", "Your Library", "https://open.spotify.com/collection")
    )

    fun startPage(prefs: SharedPreferences): StartPage {
        val id = prefs.getString(START_PAGE, "liked")
        return startPages.firstOrNull { it.id == id } ?: startPages.first()
    }

    fun hideUpsells(p: SharedPreferences) = p.getBoolean(HIDE_UPSELLS, true)
    fun hidePodcasts(p: SharedPreferences) = p.getBoolean(HIDE_PODCASTS, false)
    fun hideSponsored(p: SharedPreferences) = p.getBoolean(HIDE_SPONSORED, true)
    fun hideCanvas(p: SharedPreferences) = p.getBoolean(HIDE_CANVAS, false)
    fun lyricsFallback(p: SharedPreferences) = p.getBoolean(LYRICS_FALLBACK, true)
    fun rememberModes(p: SharedPreferences) = p.getBoolean(REMEMBER_MODES, true)
    fun noSmartShuffle(p: SharedPreferences) = p.getBoolean(NO_SMART_SHUFFLE, false)
    fun dataSaver(p: SharedPreferences) = p.getBoolean(DATA_SAVER, false)
    fun blocksCanvas(p: SharedPreferences) = hideCanvas(p) || dataSaver(p)

    fun flagsJs(p: SharedPreferences): String {
        val o = JSONObject()
            .put("upsells", hideUpsells(p))
            .put("podcasts", hidePodcasts(p))
            .put("sponsored", hideSponsored(p))
            .put("canvas", blocksCanvas(p))
            .put("lyrics", lyricsFallback(p))
            .put("remember", rememberModes(p))
            .put("nosmart", noSmartShuffle(p))
            .put("saver", dataSaver(p))
            .put("shuffle", if (p.contains(SAVED_SHUFFLE)) p.getBoolean(SAVED_SHUFFLE, false) else JSONObject.NULL)
            .put("repeat", p.getString(SAVED_REPEAT, null) ?: JSONObject.NULL)
        return "window.rsTw=$o;if(window.rsTwApply)rsTwApply();"
    }

    const val TWEAKS_JS = """
(function(){
if(window.rsTwInit)return;window.rsTwInit=true;
var st=document.createElement('style');st.id='rs-tweaks-css';
function T(){return window.rsTw||{};}
function css(){
  var t=T(),c='';
  if(t.canvas)c+='[data-testid*=canvas] video,[class*=anvas] video{display:none!important}';
  if(t.upsells)c+='[data-testid=upgrade-button],a[href*="/premium"],[data-testid=create-button]{display:none!important}';
  st.textContent=c;
  if(!st.parentNode)(document.head||document.documentElement).appendChild(st);
}
function hide(el){if(el&&!el.hasAttribute('data-rs-hidden')){el.style.setProperty('display','none','important');el.setAttribute('data-rs-hidden','1');}}
function unhideAll(){document.querySelectorAll('[data-rs-hidden]').forEach(function(e){e.style.removeProperty('display');e.removeAttribute('data-rs-hidden');});}
function inPlayer(el){return !!el.closest('#spl-bar,#rs-lrc,[data-testid=now-playing-widget-v2],[data-testid=now-playing-widget],aside[data-testid=now-playing-bar]');}
function card(el){return el.closest('[data-encore-id=card],[role=row],[data-testid=tracklist-row],li')||el;}
var UP=/^(explore premium|upgrade|get premium|try premium|premium|upgrade to premium|try \d+ months? (for|of) premium.*)$/i;
var CHIP=/^(podcasts?|audiobooks?|podcasts & shows|shows|episodes)$/i;
var SPON=/^(sponsored|promoted|advertisement)$/i;
function sweep(){
  var t=T();
  if(t.upsells){
    document.querySelectorAll('a,button').forEach(function(b){
      if(inPlayer(b))return;
      var tx=(b.textContent||'').trim(),al=(b.getAttribute('aria-label')||'').trim();
      if(UP.test(tx)||/^(upgrade|explore premium|upgrade to premium|create)$/i.test(al))hide(b);
    });
  }
  if(t.podcasts){
    document.querySelectorAll('a[href*="/show/"],a[href*="/episode/"],a[href*="/audiobook/"],a[href*="/genre/podcasts"],a[href*="/genre/audiobooks"]').forEach(function(a){if(!inPlayer(a))hide(card(a));});
    document.querySelectorAll('button,[role=checkbox],[role=tab]').forEach(function(b){if(!inPlayer(b)&&CHIP.test((b.textContent||'').trim()))hide(b);});
    document.querySelectorAll('section').forEach(function(s){var h=s.querySelector('h2');if(h&&/podcast|episode|audiobook|\bshows?\b/i.test(h.textContent))hide(s);});
  }
  if(t.sponsored){
    document.querySelectorAll('span,p').forEach(function(e){
      if(e.childElementCount===0&&SPON.test((e.textContent||'').trim())&&!inPlayer(e))hide(e.closest('section,[data-encore-id=card],[role=row],li')||e);
    });
    document.querySelectorAll('[data-testid*=sponsor],[data-testid*=promoted],[aria-label^=Sponsored]').forEach(function(e){if(!inPlayer(e))hide(e.closest('section,[data-encore-id=card],li')||e);});
  }
  if(t.canvas){
    document.querySelectorAll('[data-testid*=canvas] video,[class*=anvas] video').forEach(function(v){try{if(!v.paused)v.pause();}catch(e){}});
  }
  if(t.saver){
    document.querySelectorAll('img').forEach(function(i){
      if(i.id==='spl-cover-img')return;
      var s=i.getAttribute('src')||'';
      if(i.hasAttribute('srcset')&&/i\.scdn\.co|mosaic\.scdn\.co/.test(s)){i.removeAttribute('srcset');i.removeAttribute('sizes');}
      var n=s.replace('ab67616d0000b273','ab67616d00001e02').replace('ab6761610000e5eb','ab67616100005174');
      if(n!==s)i.setAttribute('src',n);
    });
  }
}
function repBtn(){return document.querySelector('button[data-testid=control-button-repeat]')||document.querySelector('aside[data-testid=now-playing-bar] button[aria-label*=epeat]');}
window.rsRepeatMode=function(){
  var b=repBtn();if(!b)return null;
  var l=(b.getAttribute('aria-label')||'').toLowerCase(),c=b.getAttribute('aria-checked');
  if(/enable repeat one/.test(l))return 'context';
  if(/disable repeat/.test(l))return 'track';
  if(/enable repeat/.test(l))return 'off';
  return c==='true'?'context':(c==='mixed'?'track':'off');
};
var applied=false,tries=0;
function modes(){
  var t=T(),sb=window.rsShuffleBtn&&rsShuffleBtn();
  if(!sb||!(window.rsTrackEl&&rsTrackEl()))return;
  if(t.nosmart&&/disable smart shuffle/i.test(sb.getAttribute('aria-label')||'')){sb.click();return;}
  if(!t.remember){applied=true;return;}
  if(!applied){
    if(tries++>8){applied=true;return;}
    if(t.shuffle!==null&&t.shuffle!==undefined&&rsShuffleOn()!==t.shuffle){sb.click();return;}
    var rm=rsRepeatMode();
    if(t.repeat&&rm&&rm!==t.repeat){repBtn().click();return;}
    applied=true;return;
  }
  var sh=rsShuffleOn(),rp=rsRepeatMode()||'off';
  if(sh!==t.shuffle||rp!==t.repeat){t.shuffle=sh;t.repeat=rp;try{AndBridge.savePlayerModes(sh,rp);}catch(e){}}
}
function secs(el){if(!el)return NaN;var p=(el.textContent||'').trim().split(':').map(Number);var s=0;p.forEach(function(x){s=s*60+x;});return s;}
window.rsPos=function(){return secs(document.querySelector('[data-testid=playback-position]'));};
window.rsDur=function(){return secs(document.querySelector('[data-testid=playback-duration]'));};
window.rsStopAfter=null;
window.rsStopAfterTrack=function(on){var e=window.rsTrackEl&&rsTrackEl();window.rsStopAfter=on?((e&&e.textContent)||''):null;};
function stopWatch(){
  if(window.rsStopAfter===null)return;
  var e=window.rsTrackEl&&rsTrackEl(),cur=(e&&e.textContent)||'';
  var p=rsPos(),d=rsDur();
  if(cur!==window.rsStopAfter||(d>0&&p>=0&&d-p<=1)){
    window.rsStopAfter=null;
    if(typeof actPlayPause==='function')actPlayPause(false);
    try{AndBridge.sleepTimerDone();}catch(x){}
  }
}
var lrc=null;
function lrcBox(){
  var b=document.getElementById('rs-lrc');
  if(!b){
    b=document.createElement('div');b.id='rs-lrc';
    b.style.cssText='position:fixed;inset:0;z-index:2147483646;background:rgba(10,6,8,.96);color:#fff;display:flex;flex-direction:column;font-family:inherit';
    b.innerHTML='<div style="display:flex;align-items:center;padding:14px 16px;gap:8px;border-bottom:1px solid rgba(255,255,255,.1)"><div style="flex:1"><div id="rs-lrc-t" style="font-weight:700;font-size:16px"></div><div style="font-size:12px;opacity:.6">Lyrics from LRCLIB</div></div><button id="rs-lrc-x" style="background:none;border:0;color:#fff;font-size:26px;padding:4px 10px">&times;</button></div><div id="rs-lrc-body" style="flex:1;overflow-y:auto;padding:18px 20px 120px;font-size:22px;line-height:1.5;font-weight:700"></div>';
    document.body.appendChild(b);
    document.getElementById('rs-lrc-x').onclick=function(){b.remove();lrc=null;};
  }
  return b;
}
function esc(s){return String(s).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
window.rsLrc=function(){
  var e=window.rsTrackEl&&rsTrackEl(),title=e&&e.textContent;
  if(!title)return;
  var a=window.rsArtistEls?rsArtistEls():[],artist=a.length?a[0].textContent:'';
  lrcBox();document.getElementById('rs-lrc-t').textContent=title+(artist?' · '+artist:'');
  document.getElementById('rs-lrc-body').innerHTML='<div style="opacity:.6;font-size:16px">Searching…</div>';
  lrc={title:title,lines:null};
  AndBridge.fetchLyrics(artist,title,rsDur()||0);
};
window.rsLrcResult=function(o){
  var body=document.getElementById('rs-lrc-body');if(!body||!lrc)return;
  if(!o||(!o.synced&&!o.plain)){body.innerHTML='<div style="opacity:.6;font-size:16px">'+(o&&o.instrumental?'Instrumental track':'No lyrics found for this song')+'</div>';return;}
  if(o.synced){
    lrc.lines=[];
    o.synced.split('\n').forEach(function(l){var m=l.match(/^\[(\d+):(\d+(?:\.\d+)?)\](.*)$/);if(m)lrc.lines.push({t:+m[1]*60+(+m[2]),x:m[3].trim()});});
    body.innerHTML=lrc.lines.map(function(l,i){return '<div class="rs-ll" id="rs-ll'+i+'" style="opacity:.35;margin:6px 0;transition:opacity .2s">'+(esc(l.x)||'♪')+'</div>';}).join('');
  } else {
    lrc.lines=null;
    body.innerHTML='<div style="white-space:pre-wrap;font-size:18px;font-weight:600;opacity:.9">'+esc(o.plain)+'</div>';
  }
};
var lastLine=-1;
function lrcTick(){
  if(!lrc)return;
  var e=window.rsTrackEl&&rsTrackEl();
  if(e&&e.textContent!==lrc.title){rsLrc();return;}
  if(!lrc.lines)return;
  var p=rsPos(),idx=-1;
  for(var i=0;i<lrc.lines.length;i++){if(lrc.lines[i].t<=p+0.3)idx=i;else break;}
  if(idx!==lastLine){
    var o=document.getElementById('rs-ll'+lastLine);if(o)o.style.opacity='.35';
    var n=document.getElementById('rs-ll'+idx);if(n){n.style.opacity='1';n.scrollIntoView({block:'center',behavior:'smooth'});}
    lastLine=idx;
  }
}
window.rsTwApply=function(){css();if(!T().upsells&&!T().podcasts&&!T().sponsored)unhideAll();sweep();};
css();
setInterval(function(){try{sweep();modes();}catch(e){}},1500);
setInterval(function(){try{stopWatch();lrcTick();}catch(e){}},400);
})();
"""
}

object LrcLib {
    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

    /** Looks the track up on lrclib.net and returns a JSON object for the page, or null on network failure. */
    fun fetch(artist: String, title: String, duration: Int): String? = runCatching {
        val q = "track_name=" + enc(title) + (if (artist.isNotBlank()) "&artist_name=" + enc(artist) else "")
        val conn = java.net.URL("https://lrclib.net/api/search?$q").openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "ReSpotifuck (https://github.com/ReSpotF-ck/-ReSpotif-ck-App)")
        if (conn.responseCode != 200) return@runCatching "{}"
        val arr = org.json.JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
        var best: JSONObject? = null
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val d = o.optDouble("duration", 0.0)
            if (duration > 0 && d > 0 && kotlin.math.abs(d - duration) > 4) continue
            if (best == null) best = o
            if (!o.isNull("syncedLyrics") && o.optString("syncedLyrics").isNotBlank()) { best = o; break }
        }
        if (best == null && arr.length() > 0) best = arr.getJSONObject(0)
        val b = best ?: return@runCatching "{}"
        fun field(k: String): Any = if (b.isNull(k)) JSONObject.NULL else b.optString(k).ifBlank { null } ?: JSONObject.NULL
        JSONObject()
            .put("synced", field("syncedLyrics"))
            .put("plain", field("plainLyrics"))
            .put("instrumental", b.optBoolean("instrumental"))
            .toString()
    }.getOrNull()
}
