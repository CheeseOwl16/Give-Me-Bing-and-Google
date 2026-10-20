package dev.cheeseowl.gmbg;

enum EngineChoice {
    BING("bing","https://www.bing.com/search?q={searchTerms}"),
    GOOGLE("google","https://www.google.com/search?q={searchTerms}"), UNKNOWN("","");
    final String id,template;
    EngineChoice(String id,String template){this.id=id;this.template=template;}
    static EngineChoice from(String id){return "bing".equals(id)?BING:"google".equals(id)?GOOGLE:UNKNOWN;}
    static boolean validNativeId(String id){
        if(id==null || id.isEmpty() || id.length()>128 || id.contains("://"))return false;
        for(int i=0;i<id.length();i++)if(Character.isISOControl(id.charAt(i)))return false;
        return true;
    }
}
