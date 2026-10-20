package dev.cheeseowl.gmbg;

final class TakeoverCapability {
    static boolean ready(java.util.Set<DiagnosticLog.Hook> hooks){return hooks.containsAll(java.util.EnumSet.of(
        DiagnosticLog.Hook.QUERY,DiagnosticLog.Hook.TEMPLATE,DiagnosticLog.Hook.ITEM,DiagnosticLog.Hook.ENCODE,DiagnosticLog.Hook.BROWSER));}
    private TakeoverCapability(){}
}
