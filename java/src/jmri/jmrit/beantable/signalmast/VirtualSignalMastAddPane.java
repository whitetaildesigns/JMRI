package jmri.jmrit.beantable.signalmast;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.annotation.Nonnull;

import jmri.*;
import jmri.implementation.*;
import jmri.util.*;

import org.openide.util.lookup.ServiceProvider;

/**
 * A pane for configuring VirtualSignalMast objects
 * <P>
 * @see jmri.jmrit.beantable.signalmast.SignalMastAddPane
 * @author Bob Jacobsen Copyright (C) 2018
 * @since 4.11.2
 */
public class VirtualSignalMastAddPane extends SignalMastAddPane {

    /** {@inheritDoc} */
    @Override
    @Nonnull public String getPaneName() {
        return Bundle.getMessage("VirtualMast");
    }

    JCheckBox allowUnLit = new JCheckBox();

    LinkedHashMap<String, JCheckBox> disabledAspects = new LinkedHashMap<>(14);
    JPanel disabledAspectsPanel = new JPanel();
    
    VirtualSignalMast currentMast = null;

    /** {@inheritDoc} */
    @Override
    public void setAspectNames(@Nonnull Enumeration<String> aspects) {
        // update immediately
        disabledAspects = new LinkedHashMap<>(10);
        disabledAspectsPanel.removeAll();
        while (aspects.hasMoreElements()) {
            String aspect = aspects.nextElement();
            JCheckBox disabled = new JCheckBox(aspect);
            disabledAspects.put(aspect, disabled);
        }
        disabledAspectsPanel.setLayout(new jmri.util.javaworld.GridLayout2(disabledAspects.size() + 1, 1));
        for (String aspect : disabledAspects.keySet()) {
            disabledAspectsPanel.add(disabledAspects.get(aspect));
        }

        disabledAspectsPanel.revalidate();
    }

    public VirtualSignalMastAddPane() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        // lit/unlit controls
        JPanel p = p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(new JLabel(Bundle.getMessage("AllowUnLitLabel") + ": "));
        p.add(allowUnLit);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(p);
        
        // disabled aspects controls
        TitledBorder disableborder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black));
        disableborder.setTitle(Bundle.getMessage("DisableAspectsLabel"));
        JScrollPane disabledAspectsScroll = new JScrollPane(disabledAspectsPanel);
        disabledAspectsScroll.setBorder(disableborder);
        add(disabledAspectsScroll);

    }

    /** {@inheritDoc} */
    @Override
    public boolean canHandleMast(@Nonnull SignalMast mast) {
        return mast instanceof jmri.implementation.VirtualSignalMast;
    }

    /** {@inheritDoc} */
    @Override
    public void setMast(SignalMast mast) { 
        if (mast == null) { 
            currentMast = null; 
            return; 
        }
        
        if (! (mast instanceof jmri.implementation.VirtualSignalMast) ) {
            log.error("mast was wrong type: {} {}", mast.getSystemName(), mast.getClass().getName());
            return;
        }

        currentMast = (VirtualSignalMast) mast;
        List<String> disabled = currentMast.getDisabledAspects();
        if (disabled != null) {
            for (String aspect : disabled) {
                System.err.println("try "+aspect+" "+disabledAspects.containsKey(aspect));
                if (disabledAspects.containsKey(aspect)) {
                    disabledAspects.get(aspect).setSelected(true);
                }
            }
            //+ do we need to clear non-disabled aspects?
        }
    }

    DecimalFormat paddedNumber = new DecimalFormat("0000");

    /** {@inheritDoc} */
    @Override
    public void createMast(@Nonnull String sigsysname, @Nonnull String mastname, @Nonnull String username) {
        if (currentMast == null) {
            // create a mast
            String name = "IF$vsm:"
                    + sigsysname
                    + ":" + mastname.substring(11, mastname.length() - 4);
            name += "($" + (paddedNumber.format(VirtualSignalMast.getLastRef() + 1)) + ")";
            currentMast = new VirtualSignalMast(name);
            if (!username.equals("")) {
                currentMast.setUserName(username);
            }
            InstanceManager.getDefault(jmri.SignalMastManager.class).register(currentMast);
        }
        
        // load a new or existing mast
        for (String aspect : disabledAspects.keySet()) {
            if (disabledAspects.get(aspect).isSelected()) {
                currentMast.setAspectDisabled(aspect);
            } else {
                currentMast.setAspectEnabled(aspect);
            }
        }
        currentMast.setAllowUnLit(allowUnLit.isSelected());
    }

    @ServiceProvider(service = SignalMastAddPane.SignalMastAddPaneProvider.class)
    static public class SignalMastAddPaneProvider extends SignalMastAddPane.SignalMastAddPaneProvider {
        /** {@inheritDoc} */
        @Override
        @Nonnull public String getPaneName() {
            return Bundle.getMessage("VirtualMast");
        }
        /** {@inheritDoc} */
        @Override
        @Nonnull public SignalMastAddPane getNewPane() {
            return new VirtualSignalMastAddPane();
        }
    }
    
    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddSignalMastPanel.class);
}
