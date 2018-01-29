package jmri.jmrit.beantable.signalmast;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import jmri.*;
import jmri.implementation.DccSignalMast;
import jmri.implementation.DefaultSignalAppearanceMap;
import jmri.implementation.MatrixSignalMast;
import jmri.implementation.SignalHeadSignalMast;
import jmri.implementation.TurnoutSignalMast;
import jmri.implementation.VirtualSignalMast;
import jmri.util.ConnectionNameFromSystemName;
import jmri.util.FileUtil;
import jmri.util.StringUtil;
import jmri.util.swing.BeanSelectCreatePanel;
import jmri.util.swing.JmriBeanComboBox;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPanel to create a new Signal Mast
 *
 * @author Bob Jacobsen Copyright (C) 2009, 2010, 2016
 * @author Egbert Broerse Copyright (C) 2016
 */
public class AddSignalMastPanel extends JPanel {

    // connection to preferences
    jmri.UserPreferencesManager prefs = jmri.InstanceManager.getDefault(jmri.UserPreferencesManager.class);
    String systemSelectionCombo = this.getClass().getName() + ".SignallingSystemSelected"; // NOI18N
    String mastSelectionCombo = this.getClass().getName() + ".SignallingMastSelected"; // NOI18N
    String driverSelectionCombo = this.getClass().getName() + ".SignallingDriverSelected"; // NOI18N
    
    // head matter
    JTextField userName = new JTextField(20); // N11N
    JComboBox<String> sigSysBox = new JComboBox<>();  // the basic signal system
    JComboBox<String> mastBox = new JComboBox<>(new String[]{Bundle.getMessage("MastEmpty")}); // the mast within the system NOI18N
    JComboBox<String> signalMastDriver;   // the specific SignalMast class type

    List<SignalMastAddPane> panes = new ArrayList<>();

    // center pane, which holds the specific display
    JPanel centerPanel = new JPanel();
    CardLayout cl = new CardLayout();
    SignalMastAddPane currentPane;
    
    // rest of structure
    JPanel signalHeadPanel = new JPanel();
    JButton cancel = new JButton(Bundle.getMessage("ButtonCancel")); // NOI18N
    JButton apply = new JButton(Bundle.getMessage("ButtonApply")); // NOI18N
    JButton create = new JButton(Bundle.getMessage("ButtonCreate")); // NOI18N

    // current
    SignalMast mast;
    
    /**
     * Constructor providing a blank panel to configure a new signal mast after
     * pressing 'Add...' on the Signal Mast Table.
     * <p>
     * Responds to choice of signal system, mast type and driver
     * {@link #updateSelectedDriver()}
     */
    public AddSignalMastPanel() {
        // get the list of possible signal types (as shown by panes)
        SignalMastAddPane.SignalMastAddPaneProvider.getInstancesCollection().forEach(
            (provider)-> {
                if (provider.isAvailable()) {
                    panes.add(provider.getNewPane());
                }
            }
        );
        
        { // scoping for temporary variables

            String[] mastNames = new String[panes.size()];
            int i = 0;
            for (SignalMastAddPane pane : panes) {
                mastNames[i++] = pane.getPaneName();
            }
            signalMastDriver = new JComboBox<>(mastNames);
        }

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel p;
        p = new JPanel();
        p.setLayout(new jmri.util.javaworld.GridLayout2(5, 2));

        JLabel l = new JLabel(Bundle.getMessage("LabelUserName"));
        p.add(l);
        p.add(userName);

        l = new JLabel(Bundle.getMessage("SigSys") + ": "); // NOI18N
        p.add(l);
        p.add(sigSysBox);

        l = new JLabel(Bundle.getMessage("MastType") + ": "); // NOI18N
        p.add(l);
        p.add(mastBox);

        l = new JLabel(Bundle.getMessage("DriverType") + ": "); // NOI18N
        p.add(l);
        p.add(signalMastDriver);

        add(p);

        // central region
        centerPanel.setLayout(cl);
        for (SignalMastAddPane pane : panes) {
            centerPanel.add(pane, pane.getPaneName()); // assumes names are systemwide-unique
        }
        add(centerPanel);
        signalMastDriver.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent evt) {
                    selection((String)evt.getItem());
                }
        });
        
        // button region
        JPanel buttonHolder = new JPanel();
        buttonHolder.setLayout(new FlowLayout(FlowLayout.TRAILING));
        cancel.setVisible(true);
        buttonHolder.add(cancel);
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //+ cancelPressed(e);
            } // Cancel button on add new mast pane
        });
        cancel.setVisible(true);
        buttonHolder.add(create);
        create.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed();
            } // Create button on add new mast pane
        });
        create.setVisible(true);
        buttonHolder.add(apply);
        apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed();
            } // Apply button on Edit existing mast pane
        });
        apply.setVisible(false);
        add(buttonHolder); // add bottom row of buttons (to me)

        // set a remembered signalmast type, if present
        if (prefs.getComboBoxLastSelection(driverSelectionCombo) != null) {
            signalMastDriver.setSelectedItem(prefs.getComboBoxLastSelection(driverSelectionCombo));
        }
        
        // configure responsive actions
        signalMastDriver.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //+ updateSelectedDriver();
            }
        });
        //+ includeUsed.addActionListener(new ActionListener() {
        //+     @Override
        //+    public void actionPerformed(ActionEvent e) {
        //+        //+ refreshHeadComboBox();
        //+    }
        //+});

        // default to 1st pane
        currentPane = panes.get(0);
        
        // load the list of signal systems
        SignalSystemManager man = InstanceManager.getDefault(SignalSystemManager.class);
        String[] names = man.getSystemNameArray();
        for (int i = 0; i < names.length; i++) {
            sigSysBox.addItem(man.getSystem(names[i]).getUserName());
        }
        if (prefs.getComboBoxLastSelection(systemSelectionCombo) != null) {
            sigSysBox.setSelectedItem(prefs.getComboBoxLastSelection(systemSelectionCombo));
        }

        loadMastDefinitions();

        // select the 1st one  //+ should be load from preference
        selection(panes.get(0).getPaneName());  // there has to be at least one, so we can do the update

        //+ updateHeads();
        //+ refreshHeadComboBox();
        sigSysBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                loadMastDefinitions();
                updateSelectedDriver();
            }
        });


    }    

    /**
     * Select a particular signal implementation to display
     */
    void selection(String view) {
        // find the new pane
        for (SignalMastAddPane pane : panes) {
            if (pane.getPaneName().equals(view)) {
                currentPane = pane;
            }
        }
        
        // update that selected pane before display
        updateSelectedDriver();
        
        // and show
        cl.show(centerPanel, view);
    }

    /**
     * Build a panel filled in for existing mast after pressing 'Edit' in the
     * Signal Mast table.
     *
     * @param mast {@code NamedBeanHandle<SignalMast> } for the signal mast to
     *             be retrieved
     * @see #AddSignalMastPanel()
     */
    public AddSignalMastPanel(SignalMast mast) {
        this(); // calls the above method to build the base for an edit panel

        //+ inEditMode = true;
        this.mast = mast;
        
        // can't change some things from original settings
        sigSysBox.setEnabled(false);
        mastBox.setEnabled(false);
        signalMastDriver.setEnabled(false);
        userName.setText(mast.getUserName());
        userName.setEnabled(false);
        sigSysBox.setSelectedItem(mast.getSignalSystem().getUserName());
        
        // select and show
        for (SignalMastAddPane pane : panes) {
            if (pane.canHandleMast(mast)) {
                currentPane = pane;
                selection(pane.getPaneName());
                pane.setMast(mast);
                break;
            }
        }
        
    }

    // signal system definition variables
    String sigsysname;
    ArrayList<File> mastNames = new ArrayList<>(); // signal system definition files
    LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
 
    // load the mast definitions from the selected signal system
    void loadMastDefinitions() {
        // need to remove itemListener before addItem() or item event will occur
        if (mastBox.getItemListeners().length > 0) { // should this be a while loop?
            mastBox.removeItemListener(mastBox.getItemListeners()[0]);
        }
        mastBox.removeAllItems();
        try {
            mastNames = new ArrayList<>();
            SignalSystemManager man = InstanceManager.getDefault(jmri.SignalSystemManager.class);

            // get the signals system name from the user name in combo box
            String u = (String) sigSysBox.getSelectedItem();
            sigsysname = man.getByUserName(u).getSystemName();
            map = new LinkedHashMap<>();

            // do file IO to get all the appearances
            // gather all the appearance files
            //Look for the default system defined ones first
            URL path = FileUtil.findURL("xml/signals/" + sigsysname, FileUtil.Location.INSTALLED);
            if (path != null) {
                File[] apps = new File(path.toURI()).listFiles();
                for (File app : apps) {
                    if (app.getName().startsWith("appearance") && app.getName().endsWith(".xml")) {
                        log.debug("   found file: {}", app.getName());
                        // load it and get name
                        mastNames.add(app);
                        jmri.jmrit.XmlFile xf = new jmri.jmrit.XmlFile() {
                        };
                        Element root = xf.rootFromFile(app);
                        String name = root.getChild("name").getText();
                        mastBox.addItem(name);
                        map.put(name, root.getChild("appearances")
                                .getChild("appearance")
                                .getChildren("show")
                                .size());
                    }
                }
            }
        } catch (org.jdom2.JDOMException e) {
            mastBox.addItem(Bundle.getMessage("ErrorSignalMastBox1"));
            log.warn("in loadMastDefinitions", e);
        } catch (java.io.IOException | URISyntaxException e) {
            mastBox.addItem(Bundle.getMessage("ErrorSignalMastBox2"));
            log.warn("in loadMastDefinitions", e);
        }

        try {
            URL path = FileUtil.findURL("signals/" + sigsysname, FileUtil.Location.USER, "xml", "resources");
            if (path != null) {
                File[] apps = new File(path.toURI()).listFiles();
                for (File app : apps) {
                    if (app.getName().startsWith("appearance") && app.getName().endsWith(".xml")) {
                        log.debug("   found file: {}", app.getName());
                        // load it and get name
                        // If the mast file name already exists no point in re-adding it
                        if (!mastNames.contains(app)) {
                            mastNames.add(app);
                            jmri.jmrit.XmlFile xf = new jmri.jmrit.XmlFile() {
                            };
                            Element root = xf.rootFromFile(app);
                            String name = root.getChild("name").getText();
                            //if the mast name already exist no point in readding it.
                            if (!map.containsKey(name)) {
                                mastBox.addItem(name);
                                map.put(name, root.getChild("appearances")
                                        .getChild("appearance")
                                        .getChildren("show")
                                        .size());
                            }
                        }
                    }
                }
            }
        } catch (org.jdom2.JDOMException | java.io.IOException | URISyntaxException e) {
            log.warn("in loadMastDefinitions", e);
        }
        mastBox.addItemListener((ItemEvent e) -> {
            updateSelectedDriver();
        });
        updateSelectedDriver();

        if (prefs.getComboBoxLastSelection(mastSelectionCombo + ":" + ((String) sigSysBox.getSelectedItem())) != null) {
            mastBox.setSelectedItem(prefs.getComboBoxLastSelection(mastSelectionCombo + ":" + ((String) sigSysBox.getSelectedItem())));
        }
    }

    /**
     * Update contents of Add/Edit mast panel appropriate for chosen Driver
     * type.
     * <p>
     * Hides the other JPanels. Invoked when selecting a Signal Mast Driver in
     * {@link #loadMastDefinitions}
     */
    protected void updateSelectedDriver() {
        //+ have to do whatever updates are needed to show the display
        //+ this is redundant computation to find the mast info??
        String mastType = mastNames.get(mastBox.getSelectedIndex()).getName();
        mastType = mastType.substring(11, mastType.indexOf(".xml"));
        DefaultSignalAppearanceMap sigMap = DefaultSignalAppearanceMap.getMap(sigsysname, mastType);
        currentPane.setAspectNames(sigMap.getAspects());
        
        validate();
        if (getTopLevelAncestor() != null) {
            ((jmri.util.JmriJFrame) getTopLevelAncestor()).setSize(((jmri.util.JmriJFrame) getTopLevelAncestor()).getPreferredSize());
            ((jmri.util.JmriJFrame) getTopLevelAncestor()).pack();
        }
        repaint();
    }

    /**
     * Check of user name done when creating new SignalMast.
     * In case of error, it looks a message and (if not headless) shows a dialog.
     * @ return true if OK to proceed
     */
    boolean checkUserName(String nam) {
        if (!((nam == null) || (nam.equals("")))) {
            // user name provided, check if that name already exists
            NamedBean nB = InstanceManager.getDefault(jmri.SignalMastManager.class).getByUserName(nam);
            if (nB != null) {
                log.error("User Name \"{}\" is already in use", nam);
                if (!GraphicsEnvironment.isHeadless()) {
                    String msg = Bundle.getMessage("WarningUserName", new Object[]{("" + nam)});
                    JOptionPane.showMessageDialog(null, msg,
                            Bundle.getMessage("WarningTitle"),
                            JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }
            // Check to ensure that the username doesn't exist as a systemname.
            nB = InstanceManager.getDefault(jmri.SignalMastManager.class).getBySystemName(nam);
            if (nB != null) {
                log.error("User Name \"{}\" already exists as a System name", nam);
                if (!GraphicsEnvironment.isHeadless()) {
                    String msg = Bundle.getMessage("WarningUserNameAsSystem", new Object[]{("" + nam)});
                    JOptionPane.showMessageDialog(null, msg,
                            Bundle.getMessage("WarningTitle"),
                            JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Store user input for a signal mast definition in new or existing mast
     * object.
     * <p>
     * Invoked from Apply/Create button.
     */
    void okPressed() {
        // get and validate entered global information 
        String mastname = mastNames.get(mastBox.getSelectedIndex()).getName();
        String user = NamedBean.normalizeUserName(userName.getText());
        if (!GraphicsEnvironment.isHeadless()) {
            if (user.equals("")) {
                int i = JOptionPane.showConfirmDialog(null, "No Username has been defined, this may cause issues when editing the mast later.\nAre you sure that you want to continue?",
                        "No UserName Given",
                        JOptionPane.YES_NO_OPTION);
                if (i != 0) {
                    return;
                }
            }
        }
        
        // ask top-most pane to make a signal
        currentPane.createMast(sigsysname,mastname,user);
        
        clearPanel();
    }

    /**
     * Called when an already-initialized AddSignalMastPanel is being
     * displayed again, right before it's set visible.
     */
    public void refresh() {
        //+
    }

    /**
     * Close and dispose() panel.
     * <p>
     * Called at end of okPressed() and from Cancel Add or Edit mode
     */
    void clearPanel() {
        ((jmri.util.JmriJFrame) getTopLevelAncestor()).dispose();
        userName.setText(""); // clear user name
    }


    private final static Logger log = LoggerFactory.getLogger(AddSignalMastPanel.class);
}
