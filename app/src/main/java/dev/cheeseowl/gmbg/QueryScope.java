package dev.cheeseowl.gmbg;

/** Closed source allowlist; special and unknown sources are native. */
final class QueryScope {
    static boolean allows(String source,String direct,boolean ai){
        return !ai && (direct==null || direct.isEmpty()) &&
            ("right_search_btn".equals(source) || "keyboard_search_btn".equals(source) || "history".equals(source) || "searchengine_click".equals(source));
    }
    static String normalize(String query){String value=query==null?"":query.trim();return value.length()>100?value.substring(0,100):value;}
    private QueryScope(){}
}
