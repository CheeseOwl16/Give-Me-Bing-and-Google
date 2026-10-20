package dev.cheeseowl.gmbg;

/** Closed output vocabulary. Untrusted inputs never appear verbatim in logs. */
final class Privacy {
    private Privacy() {}
    enum Engine { BAIDU, BING, GOOGLE, DOUYIN, SO360, SOGOU, SM, AISEARCH, ONESEARCH, CUSTOM, OTHER, NONE, UNKNOWN }
    enum Scene { BOX, HOT, OTHER, NONE, UNKNOWN }
    enum Action { VIEW, SEARCH, WEB_SEARCH, MAIN, OTHER, NONE, UNKNOWN }
    enum Scheme { HTTP, HTTPS, MARKET, MI, INTENT, CONTENT, FILE, TEL, OTHER, NONE, UNKNOWN }
    enum Target { MI_BROWSER, MI_FULLSEARCH, MI_BROWSER_ACTIVITY, QSB, MARKET, OTHER, NONE, UNKNOWN }
    enum Tri { TRUE, FALSE, UNKNOWN }
    static Engine engine(Object value) {
        if (value == null) return Engine.NONE;
        if (!(value instanceof String)) return Engine.UNKNOWN;
        return switch ((String) value) {
            case "baidu" -> Engine.BAIDU; case "bing" -> Engine.BING; case "google" -> Engine.GOOGLE;
            case "douyin" -> Engine.DOUYIN; case "360" -> Engine.SO360; case "sogou" -> Engine.SOGOU;
            case "sm" -> Engine.SM; case "aisearch" -> Engine.AISEARCH; case "onesearch" -> Engine.ONESEARCH;
            case "custom" -> Engine.CUSTOM; default -> Engine.OTHER;
        };
    }
    static Scene scene(Object value) {
        if (value == null) return Scene.NONE;
        if (!(value instanceof String)) return Scene.UNKNOWN;
        return switch ((String) value) {
            case "globalSearchSearchBox" -> Scene.BOX; case "globalSearchHotList" -> Scene.HOT;
            default -> Scene.OTHER;
        };
    }
    static Action action(String value) {
        if (value == null) return Action.NONE;
        return switch (value) {
            case "android.intent.action.VIEW" -> Action.VIEW; case "android.intent.action.SEARCH" -> Action.SEARCH;
            case "android.intent.action.WEB_SEARCH" -> Action.WEB_SEARCH; case "android.intent.action.MAIN" -> Action.MAIN;
            default -> Action.OTHER;
        };
    }
    static Scheme scheme(String value) {
        if (value == null) return Scheme.NONE;
        return switch (value) {
            case "http" -> Scheme.HTTP; case "https" -> Scheme.HTTPS; case "market" -> Scheme.MARKET;
            case "mi" -> Scheme.MI; case "intent" -> Scheme.INTENT; case "content" -> Scheme.CONTENT;
            case "file" -> Scheme.FILE; case "tel" -> Scheme.TEL; default -> Scheme.OTHER;
        };
    }
    static Target target(String value) {
        if (value == null) return Target.NONE;
        return switch (value) {
            case "com.android.browser" -> Target.MI_BROWSER;
            case "com.android.browser.fullsearch.FullSearchActivity" -> Target.MI_FULLSEARCH;
            case "com.android.browser.BrowserActivity" -> Target.MI_BROWSER_ACTIVITY;
            case "com.android.quicksearchbox" -> Target.QSB; case "com.xiaomi.market" -> Target.MARKET;
            default -> Target.OTHER;
        };
    }
    static Tri tri(Object value) { return value instanceof Boolean ? ((Boolean)value ? Tri.TRUE : Tri.FALSE) : Tri.UNKNOWN; }
}
