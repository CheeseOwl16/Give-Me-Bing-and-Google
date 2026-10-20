package dev.cheeseowl.gmbg;

import android.content.Intent;
import android.net.Uri;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.net.URI;
import org.json.JSONObject;

/** Read-only classifications. Never emits raw query/URL/title/extras, resolves targets, or dispatches. */
final class SuggestionDiagnostics {
    private final Field owner,completionIntent,completionType;
    SuggestionDiagnostics(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> u=Class.forName("y2.U",false,loader),v=Class.forName("y2.V",false,loader);
        owner=field(u,"a",v);completionIntent=field(v,"W",Intent.class);completionType=field(v,"a0",String.class);
    }
    private static Field field(Class<?> type,String name,Class<?> expected) throws ReflectiveOperationException {
        Field field=type.getDeclaredField(name);if(field.getType()!=expected)throw new NoSuchFieldException();field.setAccessible(true);return field;
    }
    static boolean probe(DiagnosticLog.Hook id){
        for(TargetSpec.Spec spec:TargetSpec.OBSERVERS)if(spec.id()==id)return true;return false;
    }
    static boolean observes(DiagnosticLog.Hook id){return probe(id) || id==DiagnosticLog.Hook.QUERY || id==DiagnosticLog.Hook.BROWSER;}
    private static String string(Object value){return value instanceof String?(String)value:null;}
    private static boolean present(Object value){return value instanceof String && !((String)value).isEmpty();}
    private static SuggestionKinds.Source source(Object value){
        String text=string(value);if(text!=null && text.length()<=2048 && text.startsWith("{")){
            try{return SuggestionKinds.source(string(new JSONObject(text).opt("source")));}catch(Exception ignored){return SuggestionKinds.Source.OTHER;}
        }return SuggestionKinds.source(text);
    }
    private static Object[] url(Object value){
        String text=string(value);
        try{URI uri=text==null || text.length()>8192?null:new URI(text);
            return new Object[]{SuggestionKinds.input(text),SuggestionKinds.host(uri==null?null:uri.getHost()),SuggestionKinds.path(uri==null?null:uri.getPath())};
        }catch(Exception ignored){return new Object[]{SuggestionKinds.input(text),SuggestionKinds.Host.NONE,SuggestionKinds.Path.NONE};}
    }
    private static Object[] intent(Object value){
        if(!(value instanceof Intent))return new Object[]{false};
        Intent i=(Intent)value;Uri data=i.getData();
        return new Object[]{true,Privacy.action(i.getAction()),Privacy.scheme(data==null?null:data.getScheme()),
            SuggestionKinds.host(data==null?null:data.getHost()),SuggestionKinds.path(data==null?null:data.getPath()),
            i.getPackage()!=null,i.getComponent()!=null,i.getSelector()!=null,
            (i.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))!=0};
    }
    Object[] read(DiagnosticLog.Hook id,XposedInterface.Chain chain) throws ReflectiveOperationException {
        return switch(id){
            case COMPLETION -> {
                Object view=owner.get(chain.getThisObject());
                yield join(new Object[]{Privacy.tri(chain.getArg(2)),source(chain.getArg(0)),
                    SuggestionKinds.completion((String)completionType.get(view))},intent(completionIntent.get(view)));
            }
            case UI_SEARCH -> new Object[]{source(chain.getArg(1))};
            case UI_EXECUTE -> join(new Object[]{source(chain.getArg(0)),Privacy.tri(chain.getArg(2))},url(chain.getArg(1)));
            case SUGGEST_NATIVE -> join(new Object[]{source(chain.getArg(2)),Privacy.tri(chain.getArg(3))},url(chain.getArg(1)));
            case SUGGEST_JS -> javascript(chain.getArg(0));
            case QUERY -> new Object[]{source(chain.getArg(3)),SuggestionKinds.input(string(chain.getArg(1))),
                present(chain.getArg(2)),Privacy.tri(chain.getArg(4)),
                SuggestionKinds.flow(present(chain.getArg(1)),present(chain.getArg(2)),Boolean.TRUE.equals(chain.getArg(4)))};
            case WEB_DIRECT -> join(new Object[]{source(chain.getArg(2))},url(chain.getArg(1)));
            case WEB_PREPARE -> join(new Object[]{source(chain.getArg(3)),present(chain.getArg(2))},url(chain.getArg(1)));
            case BROWSER -> join(url(chain.getArg(2)),intent(chain.getArg(1)));
            case ACTIVITY_DISPATCH,FOREGROUND_SERVICE_DISPATCH,SERVICE_DISPATCH,SPECIAL_DISPATCH -> intent(chain.getArg(1));
            default -> new Object[]{Privacy.Tri.UNKNOWN};
        };
    }
    private static Object[] javascript(Object value){
        String raw=string(value);if(raw==null || raw.length()>32768)return new Object[]{false};
        try{
            JSONObject json=new JSONObject(raw);
            // Never fetch content/title/query/url values: only metadata needed to identify the branch.
            return new Object[]{true,json.has("content"),SuggestionKinds.source(string(json.opt("from"))),
                SuggestionKinds.aiMetadata(json.opt("aiSugWord"),json.has("aiSugWord")),
                SuggestionKinds.tab(string(json.opt("tab")),json.has("tab"))};
        }catch(Exception ignored){return new Object[]{false};}
    }
    static Object[] join(Object[] left,Object[] right){Object[] result=new Object[left.length+right.length];
        System.arraycopy(left,0,result,0,left.length);System.arraycopy(right,0,result,left.length,right.length);return result;}
}
