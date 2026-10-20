package dev.cheeseowl.gmbg;

import org.junit.Test;
import static org.junit.Assert.*;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.LinkedHashMap;

/** Fixed non-sensitive fixtures only. Not Android/QSB execution. */
public class TakeoverTest {
    private FeatureState flags(EngineChoice engine,long revision){return FeatureState.checked(true,true,true,engine,revision);}
    private DecisionLease ready(EngineChoice choice,String query){
        DecisionLease d=new DecisionLease(flags(choice,0),query,choice.template,new Object(),new Object());d.templated=true;
        assertTrue(d.encoded(choice.template,query,SearchUrlPolicy.expected(choice.template,query)));return d;
    }
    private boolean dispatch(DecisionLease d,FeatureState f){return d.dispatch(f,d.model,d.item,d.output,d.output);}
    @Test public void takeoverOffDoesNotInheritRetiredPersistenceDefault(){FeatureState f=SettingsPolicy.read(null,false,null,false,0);assertFalse(f.engine());assertFalse(f.browser());assertEquals(EngineChoice.BING,f.choice());}
    @Test public void existingGoogleChoiceAndBrowserSettingSurviveUpgrade(){FeatureState f=SettingsPolicy.read(true,false,"google",true,42);assertTrue(f.engine());assertFalse(f.browser());assertEquals(EngineChoice.GOOGLE,f.choice());assertEquals(42,f.revision());}
    @Test public void restartReadsSameEnumWithoutNativeChoice(){assertEquals(SettingsPolicy.read(true,true,"google",true,3),SettingsPolicy.read(true,true,"google",true,3));}
    @Test public void corruptChoiceIsNotSilentlyResetToBing(){assertFalse(SettingsPolicy.read(true,true,"custom",true,1).valid());assertFalse(SettingsPolicy.read(true,true,null,true,1).valid());}
    @Test public void unavailableOrInvalidRevisionDisablesAuthority(){assertFalse(FeatureState.unavailable().engine());assertFalse(flags(EngineChoice.BING,-1).valid());assertFalse(flags(EngineChoice.UNKNOWN,0).engine());}
    @Test public void onlyOneSelectedEnumCanExist(){assertEquals(EngineChoice.GOOGLE,EngineChoice.from("google"));assertEquals(EngineChoice.BING,EngineChoice.from("bing"));assertEquals(EngineChoice.UNKNOWN,EngineChoice.from("google,bing"));}
    @Test public void fourIndependentModes(){for(boolean engine:new boolean[]{false,true})for(boolean browser:new boolean[]{false,true}){FeatureState f=FeatureState.checked(engine,browser,true,EngineChoice.GOOGLE,2);assertEquals(engine,f.engine());assertEquals(browser,f.browser());assertEquals(EngineChoice.GOOGLE,f.choice());}}
    @Test public void fourModesComposeRealDecisionAndBrowserPolicies() throws Throwable {
        for(boolean engine:new boolean[]{false,true})for(boolean browser:new boolean[]{false,true}){
            FeatureState f=SettingsPolicy.read(engine,browser,"google",true,2);
            String built;DecisionLease lease=null;
            if(f.engine()){
                Object model=new Object(),item=new Object();lease=new DecisionLease(f,"中文 %&#+",EngineChoice.GOOGLE.template,model,item);
                lease.templated=true;built=EngineChoice.GOOGLE.template.replace("{searchTerms}",URLEncoder.encode(lease.query,"UTF-8"));
                assertTrue(lease.encoded(lease.template,lease.query,built));assertTrue(dispatch(lease,f));
            }else built="https://www.baidu.com/s?wd=fixed";
            final String originalUrl=built;final String effective=lease==null?"baidu":lease.flags.choice().id;
            AtomicInteger defaultStarts=new AtomicInteger(),nativeStarts=new AtomicInteger(),replay=new AtomicInteger();
            Around.Call route=()->new BrowserDispatcher().run(f.browser() &&
                WebPolicy.allows("android.intent.action.VIEW",originalUrl,originalUrl,effective,false,false,null,false),
                ()->defaultStarts.incrementAndGet(),
                ()->{nativeStarts.incrementAndGet();return null;},outcome->{});
            if(lease!=null)DispatchDecision.run(dispatch(lease,f),route,()->{replay.incrementAndGet();return null;});else route.invoke();
            assertEquals(browser?1:0,defaultStarts.get());assertEquals(browser?0:1,nativeStarts.get());assertEquals(0,replay.get());
            assertEquals(engine?"www.google.com":"www.baidu.com",new java.net.URI(originalUrl).getHost());
        }
    }
    @Test public void ordinaryScopeIncludesOnlyVerifiedButtonsSelectionAndHistory(){for(String s:new String[]{"right_search_btn","keyboard_search_btn","history","searchengine_click"})assertTrue(QueryScope.allows(s,"",false));}
    @Test public void nativeTrimAndHundredCodeUnitLimitAreRespected(){assertEquals("fixed",QueryScope.normalize("  fixed \n"));assertEquals(100,QueryScope.normalize("a".repeat(101)).length());assertFalse(SearchUrlPolicy.query(QueryScope.normalize("a".repeat(99)+"🦉")));assertFalse(SearchUrlPolicy.query(QueryScope.normalize("  \n")));}
    @Test public void localUnknownHotlistAdAndSpecialSourcesRemainNative(){for(String s:new String[]{"local_all","contact","file","settings","directlyOpen","ad","hotword_whole_net","whole_list","homefeed_hotword","from_sug_ai","unknown"})assertFalse(s,QueryScope.allows(s,"",false));assertFalse(QueryScope.allows(null,"",false));}
    @Test public void directUrlAndAiBranchesCannotAcquireLease(){assertFalse(QueryScope.allows("right_search_btn","https://example.org",false));assertFalse(QueryScope.allows("right_search_btn","",true));}
    @Test public void helperExistingItemsAreReusedAndNeverDuplicated() throws Exception {Object item=new Object();AtomicInteger creates=new AtomicInteger();CatalogPolicy.Selected s=CatalogPolicy.choose(true,item,()->{creates.incrementAndGet();return new Object();},v->true);assertSame(item,s.item());assertEquals(CatalogPolicy.Origin.EXISTING_UNATTRIBUTED,s.origin());assertEquals(0,creates.get());}
    @Test public void noHelperUsesDetachedItemWithoutMutatingCatalog() throws Exception {LinkedHashMap<String,Object> map=new LinkedHashMap<>();map.put("baidu",new Object());AtomicInteger creates=new AtomicInteger();CatalogPolicy.Selected s=CatalogPolicy.choose(map.containsKey("bing"),map.get("bing"),()->{creates.incrementAndGet();return new Object();},v->true);assertEquals(CatalogPolicy.Origin.DETACHED_QSB_FIX,s.origin());assertEquals(1,creates.get());assertEquals(1,map.size());assertFalse(map.containsKey("bing"));}
    @Test public void occupiedInvalidOrNullItemDoesNotCauseInjection() throws Exception {AtomicInteger calls=new AtomicInteger();assertNull(CatalogPolicy.choose(true,new Object(),()->{calls.incrementAndGet();return new Object();},v->false));assertNull(CatalogPolicy.choose(true,null,()->{calls.incrementAndGet();return new Object();},v->true));assertEquals(0,calls.get());}
    @Test public void failedConstructionCannotFabricateAvailability() {try{CatalogPolicy.choose(false,null,()->{throw new ReflectiveOperationException();},v->true);fail();}catch(ReflectiveOperationException expected){}}
    @Test public void malformedConstructedItemIsRejected() throws Exception {assertNull(CatalogPolicy.choose(false,null,()->new Object(),v->false));}
    @Test public void canonicalAndHelperStyleTemplatesAreSupported(){assertTrue(SearchUrlPolicy.template(EngineChoice.BING,"https://www.bing.com/search?q={searchTerms}"));assertTrue(SearchUrlPolicy.template(EngineChoice.GOOGLE,"https://www.google.com/search?ie=UTF-8&q={searchTerms}"));assertTrue(SearchUrlPolicy.template(EngineChoice.BING,"https://cn.bing.com/search?q={searchTerms}&form=QBLH"));}
    @Test public void hostValidationIsExactAndCaseNormalized(){assertTrue(SearchUrlPolicy.structure(EngineChoice.GOOGLE,"https://WWW.GOOGLE.COM/search?q=test"));for(String host:new String[]{"google.com.evil.test","evilgoogle.com","google.com.","www.google.com.evil","evil.test"})assertFalse(SearchUrlPolicy.template(EngineChoice.GOOGLE,"https://"+host+"/search?q={searchTerms}"));}
    @Test public void wrongEngineHttpsPathQueryAndUserinfoFail(){for(String t:new String[]{"http://www.google.com/search?q={searchTerms}","https://www.bing.com/search?q={searchTerms}","https://www.google.com/url?q={searchTerms}","https://u@www.google.com/search?q={searchTerms}","https://www.google.com:444/search?q={searchTerms}","https://www.google.com/search?q={searchTerms}#part","https://www.google.com/search?oq={searchTerms}","https://www.google.com/search?q={searchTerms}&q=x"})assertFalse(t,SearchUrlPolicy.template(EngineChoice.GOOGLE,t));}
    @Test public void duplicateOrMisplacedPlaceholdersFail(){for(String t:new String[]{"https://www.bing.com/search?q={searchTerms}&a={searchTerms}","https://www.bing.com/search?q=fixed&a={searchTerms}","https://www.bing.com/search?q={searchTerms}&x={unknown}"})assertFalse(SearchUrlPolicy.template(EngineChoice.BING,t));}
    @Test public void nativeFullSearchBaiduChannelStripCannotAlterAcceptedTemplate(){assertFalse(SearchUrlPolicy.template(EngineChoice.BING,"https://www.bing.com/search?q={searchTerms}&tn=channel"));assertFalse(SearchUrlPolicy.template(EngineChoice.GOOGLE,"https://www.google.com/search?q={searchTerms}&xtn=channel"));String t="https://www.google.com/search?q={searchTerms}&ie=UTF-8";assertTrue(SearchUrlPolicy.template(EngineChoice.GOOGLE,t));String u=SearchUrlPolicy.expected(t,"a&tn=not-a-channel");assertEquals(u,u.replaceAll("(&?tn=[^&]*)",""));}
    @Test public void fixedOrdinaryChineseSpacePercentAmpersandHashUnicodeQueries() throws Exception {
        String[] terms={"fixed test","中文测试","with space","100%","a&b","a#b","Unicode 🦉 café","%26&+# 中文"};
        for(EngineChoice choice:new EngineChoice[]{EngineChoice.BING,EngineChoice.GOOGLE})for(String q:terms){String encoded=URLEncoder.encode(q,"UTF-8");String built=choice.template.replace("{searchTerms}",encoded);assertTrue(SearchUrlPolicy.matches(choice,choice.template,q,built));assertEquals(encoded,built.substring(built.indexOf("q=")+2));}
    }
    @Test public void expectedUtf8FixturesHaveExactBytes(){assertEquals("https://www.bing.com/search?q=%E4%B8%AD%E6%96%87+%25%26%23%2B",SearchUrlPolicy.expected(EngineChoice.BING.template,"中文 %&#+"));}
    @Test public void multiParameterTemplatePreservesEveryStaticParameter(){String t="https://www.google.com/search?ie=UTF-8&q={searchTerms}&safe=active&client=fixed";String u="https://www.google.com/search?ie=UTF-8&q=a%26b&safe=active&client=fixed";assertTrue(SearchUrlPolicy.matches(EngineChoice.GOOGLE,t,"a&b",u));assertFalse(SearchUrlPolicy.matches(EngineChoice.GOOGLE,t,"a&b",u.replace("&safe=active","")));}
    @Test public void doubleEncodingTruncationAndDamagedUnicodeFail(){String t=EngineChoice.BING.template,u=SearchUrlPolicy.expected(t,"中文 %&");assertFalse(SearchUrlPolicy.matches(EngineChoice.BING,t,"中文 %&",u.replace("%","%25")));assertFalse(SearchUrlPolicy.matches(EngineChoice.BING,t,"中文 %&",u.substring(0,u.length()-1)));assertFalse(SearchUrlPolicy.query("bad\uD83D"));assertFalse(SearchUrlPolicy.query("bad\uDC00"));}
    @Test public void encodedBaiduUrlIsNeverAcceptedAsGoogle(){assertFalse(SearchUrlPolicy.matches(EngineChoice.GOOGLE,EngineChoice.GOOGLE.template,"fixed","https://www.baidu.com/s?wd=fixed"));}
    @Test public void aiOneSearchNonWebAndPendingLikeUrisFail(){for(String u:new String[]{"intent://x","market://x","content://x","https://www.bing.com/aiSearch/?q=x","https://www.google.com/onesearch?q=x","file:///x","tel:123"})assertFalse(SearchUrlPolicy.structure(EngineChoice.GOOGLE,u));}
    @Test public void validLeaseReachesDispatch(){DecisionLease d=ready(EngineChoice.GOOGLE,"fixed");assertTrue(dispatch(d,d.flags));}
    @Test public void noTemplateOrEncoderEvidenceCannotDispatch(){DecisionLease d=new DecisionLease(flags(EngineChoice.BING,0),"fixed",EngineChoice.BING.template,new Object(),new Object());assertFalse(d.encoded(d.template,d.query,SearchUrlPolicy.expected(d.template,d.query)));assertFalse(dispatch(d,d.flags));}
    @Test public void wrongTemplateOrQueryCannotEstablishEncodingProof(){DecisionLease d=ready(EngineChoice.GOOGLE,"fixed");assertFalse(d.encoded(EngineChoice.BING.template,"fixed",d.output));assertFalse(dispatch(d,d.flags));}
    @Test public void concurrentChoiceSwitchFailsBeforeDispatch(){DecisionLease d=ready(EngineChoice.BING,"fixed");assertFalse(dispatch(d,flags(EngineChoice.GOOGLE,1)));}
    @Test public void toggleOffOrUnreadableSettingsFailsBeforeDispatch(){DecisionLease d=ready(EngineChoice.GOOGLE,"fixed");assertFalse(dispatch(d,FeatureState.checked(false,true,true,EngineChoice.GOOGLE,1)));assertFalse(dispatch(d,FeatureState.unavailable()));}
    @Test public void changeThenChangeBackStillInvalidatesOldLease(){DecisionLease d=ready(EngineChoice.BING,"fixed");assertFalse(dispatch(d,flags(EngineChoice.BING,2)));}
    @Test public void providerUpdateOrItemReplacementFailsClosed(){DecisionLease d=ready(EngineChoice.BING,"fixed");assertFalse(d.dispatch(d.flags,new Object(),d.item,d.output,d.output));assertFalse(d.dispatch(d.flags,d.model,new Object(),d.output,d.output));}
    @Test public void mutatedFinalIntentAndBusinessUrlDoNotDispatch(){DecisionLease d=ready(EngineChoice.GOOGLE,"fixed");assertFalse(d.dispatch(d.flags,d.model,d.item,d.output+"&x=1",d.output));assertFalse(d.dispatch(d.flags,d.model,d.item,d.output,EngineChoice.BING.template));}
    @Test public void browserFlagChangeInvalidatesLeaseIndependently(){DecisionLease d=ready(EngineChoice.BING,"fixed");assertFalse(dispatch(d,FeatureState.checked(true,false,true,EngineChoice.BING,0)));}
    @Test public void successfulDecisionDoesNotReplay() throws Throwable {AtomicInteger launches=new AtomicInteger(),replay=new AtomicInteger();DispatchDecision.run(true,()->{launches.incrementAndGet();return null;},()->{replay.incrementAndGet();return null;});assertEquals(1,launches.get());assertEquals(0,replay.get());}
    @Test public void failedDecisionNeverCallsDispatchAndReplaysOnce() throws Throwable {AtomicInteger launches=new AtomicInteger(),replay=new AtomicInteger();DispatchDecision.run(false,()->{launches.incrementAndGet();return null;},()->{replay.incrementAndGet();return null;});assertEquals(0,launches.get());assertEquals(1,replay.get());}
    @Test public void nativeReplayFailurePropagatesWithoutRetry(){AtomicInteger count=new AtomicInteger();Error expected=new Error("fixture");try{DispatchDecision.run(false,()->null,()->{count.incrementAndGet();throw expected;});fail();}catch(Throwable got){assertSame(expected,got);}assertEquals(1,count.get());}
    public static class Fixture {public static boolean x(String ignored){return true;}private static boolean hidden(String ignored){return true;}}
    private TargetSpec.Spec spec(String name,String result,boolean isStatic,String...params){return new TargetSpec.Spec(DiagnosticLog.Hook.QUERY,Trace.Kind.QUERY,Fixture.class.getName(),name,result,isStatic,params);}
    @Test public void exactHookSignatureCanResolve() throws Exception {assertNotNull(TargetSpec.resolve(getClass().getClassLoader(),spec("x","boolean",true,"java.lang.String")));}
    @Test public void unrecognizedVersionCannotArm(){assertTrue(TargetSpec.knownVersion("12.10.0.07203",212100003));assertFalse(TargetSpec.knownVersion(null,212100003));assertFalse(TargetSpec.knownVersion("12.10.0.07204",212100003));assertFalse(TargetSpec.knownVersion("12.10.0.07203",212100004));}
    @Test public void everyAuthorityHookIsRequired(){java.util.EnumSet<DiagnosticLog.Hook> full=java.util.EnumSet.of(DiagnosticLog.Hook.QUERY,DiagnosticLog.Hook.TEMPLATE,DiagnosticLog.Hook.ITEM,DiagnosticLog.Hook.ENCODE,DiagnosticLog.Hook.BROWSER);assertTrue(TakeoverCapability.ready(full));for(DiagnosticLog.Hook hook:full){java.util.EnumSet<DiagnosticLog.Hook> partial=full.clone();partial.remove(hook);assertFalse(TakeoverCapability.ready(partial));}}
    @Test public void mismatchedHookReturnParametersStaticOrAccessAreRejected(){for(TargetSpec.Spec s:new TargetSpec.Spec[]{spec("x","void",true,"java.lang.String"),spec("x","boolean",false,"java.lang.String"),spec("x","boolean",true),spec("hidden","boolean",true,"java.lang.String")})try{TargetSpec.resolve(getClass().getClassLoader(),s);fail();}catch(ReflectiveOperationException expected){}}
    @Test public void loggingRejectsPrivateQueryTemplateAndRawUrl(){StringBuilder text=new StringBuilder();DiagnosticLog l=new DiagnosticLog(text::append);l.emit(DiagnosticLog.Event.URL_CHECK,DiagnosticLog.Hook.BROWSER,DiagnosticLog.Phase.STATUS,null,"private query","https://private.test/?q=x",new Object());assertFalse(text.toString().contains("private"));assertTrue(text.toString().contains("UNKNOWN"));}
}
