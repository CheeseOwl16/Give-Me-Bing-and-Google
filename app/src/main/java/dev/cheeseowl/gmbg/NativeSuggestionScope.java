package dev.cheeseowl.gmbg;

/** Single-use synchronous row -> bridge -> query qualification. No persistent user data. */
final class NativeSuggestionScope {
    enum Reason { ACCEPTED, NO_ROW, NESTED_ROW, WRONG_BRANCH, WRONG_TYPE, CATEGORY, SPECIAL_ITEM,
        NOT_TEXT_QUERY, STALE_ROW, WRONG_PARENT, WRONG_BRIDGE_ARGS, ALREADY_USED, NO_BRIDGE, WRONG_QUERY_ARGS, EXPIRED }
    record Row(Object listener,Object item,Object context,int branch,String type,String query,String category,boolean special){}
    static final class RowToken {
        final Row row;final Trace.Frame frame;final RowToken previous;boolean invalid,used;
        RowToken(Row row,Trace.Frame frame,RowToken previous){this.row=row;this.frame=frame;this.previous=previous;}
    }
    static final class Hop {
        final RowToken row;final Trace.Frame frame;final Hop previous;boolean consumed,valid;
        Hop(RowToken row,Trace.Frame frame,Hop previous,boolean valid){this.row=row;this.frame=frame;this.previous=previous;this.valid=valid;}
    }
    record Entry<T>(T token,Reason reason){}
    record Permit(Hop hop,Trace.Frame query){}
    record Claim(Permit permit,Reason reason){}
    private final ThreadLocal<RowToken> rows=new ThreadLocal<>();private final ThreadLocal<Hop> hops=new ThreadLocal<>();
    static Reason qualifies(Row r){
        if(r==null || r.listener()==null || r.item()==null || r.context()==null)return Reason.NO_ROW;
        if(r.branch()!=1)return Reason.WRONG_BRANCH;
        if(!"net_sug_word".equals(r.type()))return Reason.WRONG_TYPE;
        if(r.category()!=null && !r.category().isEmpty())return Reason.CATEGORY;
        if(r.special())return Reason.SPECIAL_ITEM;
        String q=r.query();
        if(q==null || q.length()>8192 || !SearchUrlPolicy.query(QueryScope.normalize(q)))return Reason.NOT_TEXT_QUERY;
        String text=q.trim();
        if(text.matches("(?s)^[A-Za-z][A-Za-z0-9+.-]*:.*") || text.startsWith("//") || text.startsWith("/") ||
            text.regionMatches(true,0,"www.",0,4))return Reason.NOT_TEXT_QUERY;
        return Reason.ACCEPTED;
    }
    Entry<RowToken> enterRow(Row row,Trace.Frame frame){
        RowToken previous=rows.get();Reason reason=qualifies(row);
        if(previous!=null){previous.invalid=true;reason=Reason.NESTED_ROW;}
        if(frame==null)reason=Reason.WRONG_PARENT;
        RowToken token=new RowToken(row,frame,previous);token.invalid=reason!=Reason.ACCEPTED;rows.set(token);
        return new Entry<>(token,reason);
    }
    void exitRow(RowToken row){
        if(rows.get()!=row){rows.remove();hops.remove();return;}
        row.invalid=true;if(row.previous==null)rows.remove();else rows.set(row.previous);
    }
    private static boolean same(Row a,Row b){return a!=null && b!=null && a.listener()==b.listener() && a.item()==b.item()
        && a.context()==b.context() && a.branch()==b.branch() && java.util.Objects.equals(a.type(),b.type())
        && java.util.Objects.equals(a.query(),b.query()) && java.util.Objects.equals(a.category(),b.category()) && a.special()==b.special();}
    Entry<Hop> enterBridge(Object context,String query,String source,boolean ai,Trace.Frame frame,Row fresh){
        RowToken row=rows.get();Hop previous=hops.get();Reason reason=Reason.ACCEPTED;
        if(previous!=null){previous.valid=false;reason=Reason.ALREADY_USED;}
        else if(row==null || row.invalid)reason=Reason.NO_ROW;
        else if(row.used)reason=Reason.ALREADY_USED;
        else if(frame==null || row.frame==null || frame.parent()!=row.frame.id())reason=Reason.WRONG_PARENT;
        else if(!same(row.row,fresh) || qualifies(fresh)!=Reason.ACCEPTED)reason=Reason.STALE_ROW;
        else if(context!=row.row.context() || !row.row.query().equals(query) || !"sug_page".equals(source) || ai)reason=Reason.WRONG_BRIDGE_ARGS;
        if(row!=null)row.used=true;
        Hop hop=new Hop(row,frame,previous,reason==Reason.ACCEPTED);hops.set(hop);return new Entry<>(hop,reason);
    }
    void exitBridge(Hop hop){
        if(hops.get()!=hop){hops.remove();return;}
        hop.valid=false;if(hop.previous==null)hops.remove();else hops.set(hop.previous);
    }
    Claim claim(Object context,String query,String source,String direct,boolean ai,Trace.Frame frame,Row fresh){
        Hop hop=hops.get();
        if(hop==null || !hop.valid || hop.row==null || rows.get()!=hop.row || hop.row.invalid)return new Claim(null,Reason.NO_BRIDGE);
        if(hop.consumed)return new Claim(null,Reason.ALREADY_USED);hop.consumed=true;
        if(frame==null || hop.frame==null || frame.parent()!=hop.frame.id())return new Claim(null,Reason.WRONG_PARENT);
        if(!same(hop.row.row,fresh) || qualifies(fresh)!=Reason.ACCEPTED)return new Claim(null,Reason.STALE_ROW);
        if(context!=fresh.context() || !fresh.query().equals(query) || !"sug_page".equals(source) ||
            (direct!=null && !direct.isEmpty()) || ai)return new Claim(null,Reason.WRONG_QUERY_ARGS);
        return new Claim(new Permit(hop,frame),Reason.ACCEPTED);
    }
    Row current(){RowToken row=rows.get();return row==null?null:row.row;}
    boolean live(Permit permit){return permit!=null && hops.get()==permit.hop() && permit.hop().valid &&
        permit.hop().row!=null && rows.get()==permit.hop().row && !permit.hop().row.invalid;}
}
