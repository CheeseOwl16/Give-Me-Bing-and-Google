package dev.cheeseowl.gmbg;

/** One receiver delivery, one synchronous query; no sender-thread or persistent authority. */
final class HistoryScope {
    enum Reason { ACCEPTED, NO_CONTEXT, WRONG_BRANCH, NOT_RESUMED, WRONG_ACTION, WRONG_SOURCE,
        WRONG_TAB, WRONG_TYPE, SEARCH_DISABLED, NOT_TEXT, NESTED, STALE, WRONG_PARENT, WRONG_ARGS, USED, EXPIRED }
    enum Source { IME_HISTORY, LEGACY_HISTORY, JSON, UNKNOWN, NONE }
    record Delivery(Object receiver,Object intent,Object view,Object context,int branch,boolean resumed,
                    String action,String source,String tab,String type,boolean search,String query) {}
    static final class Token {
        final Delivery delivery;final Trace.Frame frame;final Token previous;boolean invalid,used;
        Token(Delivery d,Trace.Frame f,Token p){delivery=d;frame=f;previous=p;}
    }
    record Entry(Token token,Reason reason){}
    record Permit(Token token){}
    record Claim(Permit permit,Reason reason){}
    private final ThreadLocal<Token> current=new ThreadLocal<>();
    static Source source(String value){return value==null?Source.NONE:"history_ime_g".equals(value)?Source.IME_HISTORY:
        "history".equals(value)?Source.LEGACY_HISTORY:value.startsWith("{")?Source.JSON:Source.UNKNOWN;}
    static boolean text(String raw){
        if(raw==null || raw.length()>8192 || !SearchUrlPolicy.query(QueryScope.normalize(raw)))return false;
        String q=raw.trim();
        for(int i=0;i<q.length();i++)if(Character.isISOControl(q.charAt(i)))return false;
        // Conservative negatives: schemes, paths, host-like words (including bare history URLs).
        return !q.matches("(?s)^[A-Za-z][A-Za-z0-9+.-]*:.*") && !q.startsWith("/") &&
            !q.matches("(?is)^www\\..*") && !q.matches("(?s)^\\S*\\.[\\p{L}0-9-]+(?:[/:?#].*)?$") &&
            !q.matches("(?s)^\\[[0-9a-fA-F:]+\\].*$");
    }
    static Reason qualifies(Delivery d){
        if(d==null || d.receiver()==null || d.intent()==null || d.view()==null || d.context()==null)return Reason.NO_CONTEXT;
        if(d.branch()!=1)return Reason.WRONG_BRANCH;
        if(!d.resumed())return Reason.NOT_RESUMED;
        if(!"action_search_history".equals(d.action()))return Reason.WRONG_ACTION;
        if(!"history_ime_g".equals(d.source()))return Reason.WRONG_SOURCE;
        if(!"web_all".equals(d.tab()))return Reason.WRONG_TAB;
        if(!"2".equals(d.type()))return Reason.WRONG_TYPE;
        if(!d.search())return Reason.SEARCH_DISABLED;
        return text(d.query())?Reason.ACCEPTED:Reason.NOT_TEXT;
    }
    Entry enter(Delivery d,Trace.Frame frame){
        Token previous=current.get();Reason reason=qualifies(d);
        if(previous!=null){previous.invalid=true;reason=Reason.NESTED;}
        if(frame==null || frame.kind()!=Trace.Kind.HISTORY_RECEIVE)reason=Reason.WRONG_PARENT;
        Token token=new Token(d,frame,previous);token.invalid=reason!=Reason.ACCEPTED;current.set(token);return new Entry(token,reason);
    }
    private static boolean same(Delivery a,Delivery b){return a!=null && b!=null && a.receiver()==b.receiver() &&
        a.intent()==b.intent() && a.view()==b.view() && a.context()==b.context() && a.equals(b);}
    Claim claim(Object context,String query,String source,String direct,boolean ai,Trace.Frame frame,Delivery fresh){
        Token t=current.get();if(t==null || t.invalid)return new Claim(null,Reason.NO_CONTEXT);
        if(t.used)return new Claim(null,Reason.USED);t.used=true;
        if(frame==null || frame.kind()!=Trace.Kind.QUERY || frame.parent()!=t.frame.id())return new Claim(null,Reason.WRONG_PARENT);
        if(!same(t.delivery,fresh) || qualifies(fresh)!=Reason.ACCEPTED)return new Claim(null,Reason.STALE);
        if(context!=fresh.context() || !fresh.query().equals(query) || !fresh.source().equals(source) ||
            (direct!=null && !direct.isEmpty()) || ai)return new Claim(null,Reason.WRONG_ARGS);
        return new Claim(new Permit(t),Reason.ACCEPTED);
    }
    Delivery delivery(){Token t=current.get();return t==null?null:t.delivery;}
    boolean live(Permit permit,Delivery fresh){return permit!=null && current.get()==permit.token() &&
        !permit.token().invalid && permit.token().used && same(permit.token().delivery,fresh) && qualifies(fresh)==Reason.ACCEPTED;}
    void exit(Token token){
        if(current.get()!=token){current.remove();return;}
        token.invalid=true;if(token.previous==null)current.remove();else current.set(token.previous);
    }
}
