package dev.cheeseowl.gmbg;

/** Closed settings schema. Unknown/unavailable configuration disables both features. */
record FeatureState(boolean engine,boolean browser,boolean valid,EngineChoice choice,long revision) {
    static FeatureState unavailable(){return new FeatureState(false,false,false,EngineChoice.UNKNOWN,-1);}
    static FeatureState checked(boolean engine,boolean browser,boolean valid){return checked(engine,browser,valid,EngineChoice.BING,0);}
    static FeatureState checked(boolean engine,boolean browser,boolean valid,EngineChoice choice,long revision){
        return valid && choice!=null && choice!=EngineChoice.UNKNOWN && revision>=0
            ? new FeatureState(engine,browser,true,choice,revision):unavailable();
    }
    boolean sameDecision(FeatureState other){return other!=null && valid && other.valid && engine && other.engine
        && choice==other.choice && revision==other.revision && browser==other.browser;}
}
