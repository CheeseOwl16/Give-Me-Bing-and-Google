package dev.cheeseowl.gmbg;

/** Existing invalid IDs are never silently replaced; absent IDs may use one detached item. */
final class CatalogPolicy {
    enum Origin { EXISTING_UNATTRIBUTED, DETACHED_QSB_FIX }
    interface Create {Object get() throws ReflectiveOperationException;}
    interface Validate {boolean test(Object value) throws ReflectiveOperationException;}
    record Selected(Object item,Origin origin){}
    static Selected choose(boolean present,Object existing,Create create,Validate validate) throws ReflectiveOperationException {
        Object item=present?existing:create.get();
        return item!=null && validate.test(item)?new Selected(item,present?Origin.EXISTING_UNATTRIBUTED:Origin.DETACHED_QSB_FIX):null;
    }
    private CatalogPolicy(){}
}
