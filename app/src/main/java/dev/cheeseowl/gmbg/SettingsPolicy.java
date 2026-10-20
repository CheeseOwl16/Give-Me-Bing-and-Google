package dev.cheeseowl.gmbg;

/** Upgrade semantics: the retired engine_enabled flag never enables authority mode. */
final class SettingsPolicy {
    static FeatureState read(Boolean takeover,Boolean browser,String choice,boolean choicePresent,long revision){
        return FeatureState.checked(Boolean.TRUE.equals(takeover),browser==null || browser,true,
            choicePresent?EngineChoice.from(choice):EngineChoice.BING,revision);
    }
    private SettingsPolicy(){}
}
