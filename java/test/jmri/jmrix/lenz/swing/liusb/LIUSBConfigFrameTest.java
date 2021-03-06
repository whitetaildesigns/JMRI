package jmri.jmrix.lenz.swing.liusb;

import java.awt.GraphicsEnvironment;
import jmri.util.JUnitUtil;
import org.junit.*;

/**
 * LIUSBConfigFrameTest.java
 *
 * Description:	tests for the jmri.jmrix.lenz.swing.liusb.LIUSBConfigFrame class
 *
 * @author	Paul Bender
 */
public class LIUSBConfigFrameTest extends jmri.util.JmriJFrameTestBase {

    private jmri.jmrix.lenz.XNetInterfaceScaffold t = null;
    private jmri.jmrix.lenz.XNetSystemConnectionMemo memo = null;

    // The minimal setup for log4J
    @Before
    @Override
    public void setUp() {
        JUnitUtil.setUp();
        jmri.util.JUnitUtil.resetProfileManager();
        t = new jmri.jmrix.lenz.XNetInterfaceScaffold(new jmri.jmrix.lenz.LenzCommandStation());
        memo = new jmri.jmrix.lenz.XNetSystemConnectionMemo(t);
        if(!GraphicsEnvironment.isHeadless()){
           frame = new LIUSBConfigFrame(memo);
        }
    }

    @After
    @Override
    public void tearDown() {
        memo = null;
        t = null;
        super.tearDown();
    }


}
