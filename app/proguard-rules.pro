-keepclassmembers class com.respotfck.spotifck.bridge.SpotifyBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.respotfck.spotifck.bridge.SpotifyBridge { *; }
-keep class com.respotfck.spotifck.webview.SpotifyWebViewClient { *; }
-keep class com.respotfck.spotifck.webview.SpotifyWebChromeClient { *; }
-keep class com.respotfck.spotifck.service.MediaNotificationService { *; }
-keep class com.respotfck.spotifck.proxy.LocalProxyManager { *; }
-keep class com.respotfck.spotifck.ui.MainActivity { *; }
-keep class com.respotfck.spotifck.ui.SettingsActivity { *; }
-keep class com.respotfck.spotifck.ui.CertificateActivity { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.annotation.concurrent.GuardedBy
-keepclassmembers enum * { *; }
-keepclassmembers class * implements java.io.Serializable { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepattributes *Annotation*,JavascriptInterface,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
