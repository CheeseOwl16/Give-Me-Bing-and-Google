package dev.cheeseowl.gmbg;

import java.net.URI;
import java.util.Locale;

/** Classification, never an eligibility/authority rule. No query extraction or decoding. */
final class SuggestionKinds {
    enum Source { RIGHT_BUTTON, KEYBOARD, HISTORY, ENGINE_CLICK, SUG_PAGE, WEBVIEW, LOCAL_SUG_WORD, QUANSOU_SUGWORD,
        QSB, HOTWORD, WHOLE_LIST, HOMEFEED_HOTWORD, AD, DIRECT_OPEN, NONE, OTHER }
    enum Input { EMPTY, TEXT, HTTP, HTTPS, OTHER_SCHEME, MALFORMED }
    enum Host { BAIDU, BING, GOOGLE, OTHER, NONE }
    enum Path { SEARCH_PATH, AI_PATH, ONESEARCH_PATH, OTHER, NONE }
    enum Flow { EMPTY, AI_BRANCH, DIRECT_URL_BRANCH, TEMPLATE_BRANCH_CANDIDATE }
    enum Completion { CLEAR, LOCAL_APP, LOCAL_SETTINGS, OTHER, NONE }
    enum Tab { WEB_ALL, LOCAL_ALL, OTHER, DEFAULT_WEB_ALL }
    static Source source(String value){
        if(value==null || value.isEmpty())return Source.NONE;
        return switch(value){
            case "right_search_btn" -> Source.RIGHT_BUTTON;case "keyboard_search_btn" -> Source.KEYBOARD;
            case "history" -> Source.HISTORY;case "searchengine_click" -> Source.ENGINE_CLICK;
            case "sug_page" -> Source.SUG_PAGE;case "webview" -> Source.WEBVIEW;case "local_sug_word" -> Source.LOCAL_SUG_WORD;
            case "quansou_sugword" -> Source.QUANSOU_SUGWORD;case "qsb" -> Source.QSB;case "hotword_whole_net" -> Source.HOTWORD;
            case "whole_list" -> Source.WHOLE_LIST;case "homefeed_hotword" -> Source.HOMEFEED_HOTWORD;
            case "ad" -> Source.AD;case "directlyOpen" -> Source.DIRECT_OPEN;default -> Source.OTHER;
        };
    }
    static Input input(String value){
        if(value==null || value.isEmpty())return Input.EMPTY;
        if(value.indexOf(':')<0)return Input.TEXT;
        try{String scheme=new URI(value).getScheme();return scheme==null?Input.TEXT:
            "https".equalsIgnoreCase(scheme)?Input.HTTPS:"http".equalsIgnoreCase(scheme)?Input.HTTP:Input.OTHER_SCHEME;}
        catch(Exception ignored){return Input.MALFORMED;}
    }
    static Host host(String value){
        if(value==null || value.isEmpty())return Host.NONE;
        return switch(value.toLowerCase(Locale.ROOT)){
            case "baidu.com","www.baidu.com","m.baidu.com" -> Host.BAIDU;
            case "bing.com","www.bing.com","cn.bing.com" -> Host.BING;
            case "google.com","www.google.com" -> Host.GOOGLE;default -> Host.OTHER;
        };
    }
    static Path path(String value){
        if(value==null || value.isEmpty())return Path.NONE;String lower=value.toLowerCase(Locale.ROOT);
        if(lower.equals("/aisearch") || lower.startsWith("/aisearch/"))return Path.AI_PATH;
        if(lower.equals("/onesearch") || lower.startsWith("/onesearch/"))return Path.ONESEARCH_PATH;
        return lower.equals("/search") || lower.equals("/s")?Path.SEARCH_PATH:Path.OTHER;
    }
    static Flow flow(boolean queryPresent,boolean directPresent,boolean ai){
        return directPresent?Flow.DIRECT_URL_BRANCH:!queryPresent?Flow.EMPTY:ai?Flow.AI_BRANCH:Flow.TEMPLATE_BRANCH_CANDIDATE;
    }
    static Completion completion(String value){return value==null?Completion.NONE:switch(value){
        case "clear" -> Completion.CLEAR;case "local_app" -> Completion.LOCAL_APP;case "local_settings" -> Completion.LOCAL_SETTINGS;default -> Completion.OTHER;};}
    static Tab tab(String value,boolean present){return !present?Tab.DEFAULT_WEB_ALL:
        "web_all".equals(value)?Tab.WEB_ALL:"local_all".equals(value)?Tab.LOCAL_ALL:Tab.OTHER;}
    static Privacy.Tri aiMetadata(Object value,boolean present){
        if(!present)return Privacy.Tri.FALSE;
        if(!(value instanceof String))return Privacy.Tri.UNKNOWN;
        return "1".equals(value)?Privacy.Tri.TRUE:Privacy.Tri.FALSE;
    }
    private SuggestionKinds(){}
}
