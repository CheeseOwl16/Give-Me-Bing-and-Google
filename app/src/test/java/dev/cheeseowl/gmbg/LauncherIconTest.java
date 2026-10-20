package dev.cheeseowl.gmbg;

// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Cheese owl
import org.junit.Test;
import static org.junit.Assert.*;

public final class LauncherIconTest {
    private static class Backend implements LauncherIconController.Backend {
        int state, writes; boolean failRead, failWrite, noChange, partial, failAfter;
        public int read() { if(failRead || (failAfter && writes>0))throw new SecurityException();return state; }
        public void setVisible(boolean visible) {
            writes++;
            if(partial) { state=visible?1:2;throw new IllegalArgumentException(); }
            if(failWrite)throw new SecurityException();
            if(!noChange)state=visible?1:2;
        }
    }
    private LauncherIconController controller(Backend b) { return new LauncherIconController(b); }
    @Test public void manifestDefaultVisible(){Backend b=new Backend();assertEquals(LauncherIconController.State.VISIBLE,controller(b).read());assertEquals(0,b.writes);}
    @Test public void enabledVisible(){Backend b=new Backend();b.state=1;assertEquals(LauncherIconController.State.VISIBLE,controller(b).read());}
    @Test public void disabledStatesReadHidden(){for(int i:new int[]{2,3,4}){Backend b=new Backend();b.state=i;assertEquals(LauncherIconController.State.HIDDEN,controller(b).read());}}
    @Test public void hideAndRestoreUseActualState(){Backend b=new Backend();LauncherIconController c=controller(b);assertTrue(c.setHidden(true).success);assertEquals(2,b.state);assertTrue(c.setHidden(false).success);assertEquals(1,b.state);assertEquals(2,b.writes);}
    @Test public void repeatedToggleAndRecreation(){Backend b=new Backend();for(int i=0;i<3;i++){assertTrue(controller(b).setHidden(true).success);assertEquals(LauncherIconController.State.HIDDEN,controller(b).read());assertTrue(controller(b).setHidden(false).success);}}
    @Test public void readFailureDoesNotWrite(){Backend b=new Backend();b.failRead=true;assertEquals(LauncherIconController.State.UNKNOWN,controller(b).read());assertFalse(controller(b).setHidden(true).success);assertEquals(0,b.writes);}
    @Test public void unknownStateDoesNotWrite(){Backend b=new Backend();b.state=99;assertEquals(LauncherIconController.State.UNKNOWN,controller(b).setHidden(true).state);assertEquals(0,b.writes);}
    @Test public void securityFailureRetainsVisible(){Backend b=new Backend();b.failWrite=true;LauncherIconController.Result r=controller(b).setHidden(true);assertFalse(r.success);assertEquals(LauncherIconController.State.VISIBLE,r.state);}
    @Test public void partialFailureRereadsActualState(){Backend b=new Backend();b.partial=true;LauncherIconController.Result r=controller(b).setHidden(true);assertFalse(r.success);assertEquals(LauncherIconController.State.HIDDEN,r.state);}
    @Test public void silentNoOpIsNotSuccess(){Backend b=new Backend();b.noChange=true;LauncherIconController.Result r=controller(b).setHidden(true);assertFalse(r.success);assertEquals(LauncherIconController.State.VISIBLE,r.state);}
    @Test public void readbackFailureIsUnknown(){Backend b=new Backend();b.failAfter=true;LauncherIconController.Result r=controller(b).setHidden(true);assertFalse(r.success);assertEquals(LauncherIconController.State.UNKNOWN,r.state);}
    @Test public void externalChangeAndRestoreReadFreshState(){Backend b=new Backend();LauncherIconController c=controller(b);assertTrue(c.setHidden(true).success);b.state=1;assertEquals(LauncherIconController.State.VISIBLE,c.read());assertTrue(c.setHidden(false).success);}
}
