package dev.cheeseowl.gmbg;

/** Ephemeral, synchronous request data. Never serialized or logged. */
final class DecisionLease {
    final FeatureState flags;final String query,template;final Object model,item;
    boolean templated,encoded;String output;
    DecisionLease(FeatureState flags,String query,String template,Object model,Object item){
        this.flags=flags;this.query=query;this.template=template;this.model=model;this.item=item;
    }
    boolean encoded(String actualTemplate,String actualQuery,String actualOutput){
        encoded=templated && template.equals(actualTemplate) && query.equals(actualQuery)
            && SearchUrlPolicy.matches(flags.choice(),template,query,actualOutput);
        output=encoded?actualOutput:null;return encoded;
    }
    boolean dispatch(FeatureState now,Object currentModel,Object currentItem,String intentUrl,String businessUrl){
        return flags.sameDecision(now) && model==currentModel && item==currentItem && encoded
            && output!=null && output.equals(intentUrl) && output.equals(businessUrl)
            && SearchUrlPolicy.matches(flags.choice(),template,query,output);
    }
}
