package dev.cheeseowl.gmbg;

import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;

/** Validation only: the URL dispatched by production is always produced by QSB. */
final class SearchUrlPolicy {
    private static final String SLOT="{searchTerms}";
    static boolean template(EngineChoice choice,String value){
        if(choice==null || choice==EngineChoice.UNKNOWN || value==null || value.length()>4096 ||
            value.indexOf(SLOT)<0 || value.indexOf(SLOT)!=value.lastIndexOf(SLOT) || value.contains("tn="))return false;
        String probe=value.replace(SLOT,"qsbFixedProbe");
        if(probe.indexOf('{')>=0 || probe.indexOf('}')>=0)return false;
        return structure(choice,probe) && SLOT.equals(parameters(value.substring(value.indexOf('?')+1)).get("q"));
    }
    static boolean structure(EngineChoice choice,String value){
        try{
            if(value==null || value.length()>8192)return false;
            for(int i=0;i<value.length();i++)if(Character.isWhitespace(value.charAt(i)) || Character.isISOControl(value.charAt(i)))return false;
            URI uri=new URI(value);String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme()) || host==null || uri.isOpaque() || uri.getRawUserInfo()!=null ||
                (uri.getPort()!=-1 && uri.getPort()!=443) || uri.getRawFragment()!=null || !"/search".equals(uri.getRawPath()))return false;
            host=host.toLowerCase(Locale.ROOT);
            boolean trusted=choice==EngineChoice.BING ? host.equals("www.bing.com") || host.equals("cn.bing.com") || host.equals("bing.com"):
                choice==EngineChoice.GOOGLE && (host.equals("www.google.com") || host.equals("google.com"));
            return trusted && parameters(uri.getRawQuery()).containsKey("q");
        }catch(Exception ignored){return false;}
    }
    private static HashMap<String,String> parameters(String query){
        HashMap<String,String> result=new HashMap<>();if(query==null || query.length()>4096)return result;
        for(String pair:query.split("&",-1)){
            int eq=pair.indexOf('=');if(eq<1)return new HashMap<>();
            String key=pair.substring(0,eq);if(!key.matches("[A-Za-z0-9_-]+") || result.containsKey(key))return new HashMap<>();
            result.put(key,pair.substring(eq+1));
        }return result;
    }
    static boolean query(String value){
        if(value==null || value.isEmpty() || value.length()>100)return false;
        for(int i=0;i<value.length();i++){
            char ch=value.charAt(i);
            if(Character.isHighSurrogate(ch)){if(++i>=value.length() || !Character.isLowSurrogate(value.charAt(i)))return false;}
            else if(Character.isLowSurrogate(ch))return false;
        }return true;
    }
    static String expected(String template,String query){
        try{return template.replace(SLOT,URLEncoder.encode(query,"UTF-8"));}
        catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);}
    }
    static boolean matches(EngineChoice choice,String template,String query,String output){
        return template(choice,template) && query(query) && structure(choice,output) && expected(template,query).equals(output);
    }
    private SearchUrlPolicy(){}
}
