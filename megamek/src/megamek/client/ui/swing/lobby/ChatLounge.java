/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.lobby;

import static megamek.client.ui.swing.util.UIUtil.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.*;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.boardview.BoardView1;
import megamek.client.ui.swing.dialog.MMConfirmDialog;
import megamek.client.ui.swing.dialog.MMConfirmDialog.Response;
import megamek.client.ui.swing.dialog.imageChooser.CamoChooserDialog;
import megamek.client.ui.swing.lobby.sorters.BVSorter;
import megamek.client.ui.swing.lobby.sorters.IDSorter;
import megamek.client.ui.swing.lobby.sorters.MekTableSorter;
import megamek.client.ui.swing.lobby.sorters.NameSorter;
import megamek.client.ui.swing.lobby.sorters.PlayerBVSorter;
import megamek.client.ui.swing.lobby.sorters.PlayerTonnageSorter;
import megamek.client.ui.swing.lobby.sorters.PlayerTransportIDSorter;
import megamek.client.ui.swing.lobby.sorters.TonnageSorter;
import megamek.client.ui.swing.lobby.sorters.TypeSorter;
import megamek.client.ui.swing.util.*;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.event.*;
import megamek.common.options.*;
import megamek.common.preference.*;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.util.BoardUtilities;
import megamek.common.util.fileUtils.MegaMekFile;
import static megamek.client.ui.swing.lobby.LobbyUtility.*;

public class ChatLounge extends AbstractPhaseDisplay implements  
        ListSelectionListener, MouseListener, IMapSettingsObserver, IPreferenceChangeListener {
    private static final long serialVersionUID = 1454736776730903786L;

    // UI display control values
    private static final int MEKTABLE_ROWHEIGHT_COMPACT = 20;
    private static final int MEKTABLE_ROWHEIGHT_FULL = 65;
    private static final int PLAYERTABLE_ROWHEIGHT = 60;
    
    private JTabbedPane panTabs;
    private JPanel panMain = new JPanel();
    private JPanel panMap = new JPanel();

    // Main panel top bar
    private JButton butOptions = new JButton(Messages.getString("ChatLounge.butOptions"));
    private JLabel lblMapSummary = new JLabel("");
    private JLabel lblGameYear = new JLabel("");
    private JLabel lblTechLevel = new JLabel("");

    /* Unit Configuration Panel */
    private JPanel panUnitInfo = new JPanel();
    private JButton butAdd = new JButton(Messages.getString("ChatLounge.butLoad"));
    private JButton butArmy = new JButton(Messages.getString("ChatLounge.butArmy"));
    private JButton butSkills = new JButton(Messages.getString("ChatLounge.butSkills"));
    private JButton butNames = new JButton(Messages.getString("ChatLounge.butNames"));
    private JButton butLoadList = new JButton(Messages.getString("ChatLounge.butLoadList"));
    private JButton butSaveList = new JButton(Messages.getString("ChatLounge.butSaveList"));
    private JToggleButton butShowUnitID = new JToggleButton(Messages.getString("ChatLounge.butShowUnitID"));

    /* Unit Table */
    private JTable mekTable;
    private JScrollPane scrMekTable;
    private JToggleButton butCompact = new JToggleButton(Messages.getString("ChatLounge.butCompact"));
    private MekTableModel mekModel;

    /* Player Configuration Panel */
    private JPanel panPlayerInfo;
    private JComboBox<String> comboTeam = new JComboBox<String>();
    private JButton butCamo;
    private JButton butAddBot;
    private JButton butRemoveBot;
    private JButton butBotSettings = new JButton("Bot Settings...");
    private JButton butConfigPlayer = new JButton("Configure Player...");
    private JTable tablePlayers;
    private JScrollPane scrPlayers;
    private PlayerTableModel playerModel;

    /* Map Settings Panel */
    MapSettings mapSettings;
    private JButton butConditions;
    private JPanel panGroundMap;
    private JPanel panSpaceMap;
    private JComboBox<String> comboMapType;
    @SuppressWarnings("rawtypes")
    private JComboBox<Comparable> comboMapSizes;
    private JButton butMapSize;
    private JButton butRandomMap;
    private JButton buttonBoardPreview;
    private JScrollPane scrMapButtons;
    private JPanel panMapButtons;
    private JLabel lblBoardsSelected;
    private JList<String> lisBoardsSelected;
    private JScrollPane scrBoardsSelected;
    private JButton butChange;
    private JLabel lblBoardsAvailable;
    private JList<String> lisBoardsAvailable;
    private JScrollPane scrBoardsAvailable;
    private JCheckBox chkRotateBoard;
    private JCheckBox chkIncludeGround;
    private JCheckBox chkIncludeSpace;
    private JButton butSpaceSize;
    private Set<BoardDimensions> mapSizes = new TreeSet<>();
    boolean resetAvailBoardSelection = false;
    boolean resetSelectedBoards = true;
    private JPanel mapPreviewPanel;
    private MiniMap miniMap = null;
    private ClientDialog boardPreviewW;
    private Game boardPreviewGame = new Game();
    private String cmdSelectedTab = null;
    
    private MekTableSorter activeSorter;
    private ArrayList<MekTableSorter> unitSorters = new ArrayList<>();
    private ArrayList<MekTableSorter> bvSorters = new ArrayList<>();

    private MechSummaryCache.Listener mechSummaryCacheListener = new MechSummaryCache.Listener() {
        @Override
        public void doneLoading() {
            butAdd.setEnabled(true);
            butArmy.setEnabled(true);
            butLoadList.setEnabled(true);
        }
    };
    
    private CamoChooserDialog camoDialog;

    //region Action Commands
    static final String NAME_COMMAND = "NAME";
    static final String CALLSIGN_COMMAND = "CALLSIGN";
    //endregion Action Commands

    /**
     * Creates a new chat lounge for the clientgui.getClient().
     */
    public ChatLounge(ClientGUI clientgui) {
        super(clientgui, SkinSpecification.UIComponents.ChatLounge.getComp(),
                SkinSpecification.UIComponents.ChatLoungeDoneButton.getComp());

        setLayout(new BorderLayout());
        panTabs = new JTabbedPane();
        panTabs.add("Select Units", panMain); 
        panTabs.add("Select Map", panMap); 
        add(panTabs, BorderLayout.CENTER);

        clientgui.getClient().getGame().addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        lblGameYear.setToolTipText(Messages.getString("ChatLounge.GameYearLabelToolTip")); 
        lblTechLevel.setToolTipText(Messages.getString("ChatLounge.TechLevelLabelToolTip")); 

        butDone.setText(Messages.getString("ChatLounge.butDone")); 
        Font font = new Font("sansserif", Font.BOLD, 12); 
        butDone.setFont(font);

        setupSorters();
        setupPlayerInfo();
        refreshGameSettings();
        setupEntities();
        setupUnitConfiguration();
        setupMainPanel();

        refreshMapSummaryLabel();
        refreshGameYearLabel();
        refreshTechLevelLabel();
        
        adaptToGUIScale();

        GUIPreferences.getInstance().addPreferenceChangeListener(this);
        PreferenceManager.getClientPreferences().addPreferenceChangeListener(this);
        tablePlayers.getSelectionModel().addListSelectionListener(this);
        lisBoardsAvailable.addListSelectionListener(this);
        lisBoardsAvailable.setCellRenderer(new BoardNameRenderer());
        
        butOptions.addActionListener(lobbyListener);
        butCompact.addActionListener(lobbyListener);
    }
    
    /** Initializes the Mek Table sorters. */
    private void setupSorters() {
        unitSorters.add(new PlayerTransportIDSorter(clientgui));
        unitSorters.add(new IDSorter(MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new IDSorter(MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new NameSorter(MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new NameSorter(MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new TypeSorter());
        unitSorters.add(new PlayerTonnageSorter(clientgui, MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new PlayerTonnageSorter(clientgui, MekTableSorter.Sorting.DESCENDING));
        unitSorters.add(new TonnageSorter(MekTableSorter.Sorting.ASCENDING));
        unitSorters.add(new TonnageSorter(MekTableSorter.Sorting.DESCENDING));
        bvSorters.add(new PlayerBVSorter(clientgui, MekTableSorter.Sorting.ASCENDING));
        bvSorters.add(new PlayerBVSorter(clientgui, MekTableSorter.Sorting.DESCENDING));
        bvSorters.add(new BVSorter(MekTableSorter.Sorting.ASCENDING));
        bvSorters.add(new BVSorter(MekTableSorter.Sorting.DESCENDING));
        activeSorter = unitSorters.get(0);
    }

    /**
     * Sets up the entities table
     */
    private void setupEntities() {
        mekModel = new MekTableModel(clientgui, this);
        mekTable = new JTable(mekModel) {
            private static final long serialVersionUID = -4054214297803021212L;

            @Override
            public Point getToolTipLocation(MouseEvent event) {
                int row = mekTable.rowAtPoint(event.getPoint());
                int col = mekTable.columnAtPoint(event.getPoint());
                Rectangle cellRect = mekTable.getCellRect(row, col, true);
                int x = cellRect.x + cellRect.width + 10;
                int y = cellRect.y + 10;
                return new Point(x, y);
            }
        };
        setTableRowHeights();
        mekTable.setIntercellSpacing(new Dimension(0, 0));
        mekTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        for (int i = 0; i < MekTableModel.N_COL; i++) {
            TableColumn column = mekTable.getColumnModel().getColumn(i);
            column.setCellRenderer(mekModel.getRenderer());
            setColumnWidth(column);
        }
        mekTable.addMouseListener(new MekTableMouseAdapter());
        mekTable.addKeyListener(new MekTableKeyAdapter());
        scrMekTable = new JScrollPane(mekTable);
        scrMekTable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mekTable.getTableHeader().addMouseListener(mekTableHeaderMouseListener);
    }

    /**
     * Sets up the unit configuration panel
     */
    private void setupUnitConfiguration() {
        // Initialize the RandomNameGenerator and RandomCallsignGenerator
        RandomNameGenerator.getInstance();
        RandomCallsignGenerator.getInstance();

        MechSummaryCache mechSummaryCache = MechSummaryCache.getInstance();
        mechSummaryCache.addListener(mechSummaryCacheListener);
        boolean mscLoaded = mechSummaryCache.isInitialized();

        butLoadList.setActionCommand("load_list"); 
        butLoadList.addActionListener(lobbyListener);
        butLoadList.setEnabled(mscLoaded);
        
        butSaveList.setActionCommand("save_list"); 
        butSaveList.addActionListener(lobbyListener);
        butSaveList.setEnabled(false);
        
        butAdd.setEnabled(mscLoaded);
        butAdd.setActionCommand("load_mech"); 
        butAdd.addActionListener(lobbyListener);
        
        butArmy.setEnabled(mscLoaded);
        butArmy.addActionListener(lobbyListener);

        butSkills.addActionListener(lobbyListener);
        
        butNames.addActionListener(lobbyListener);

        panUnitInfo.setBorder(BorderFactory.createTitledBorder("Unit Setup"));

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panUnitInfo.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = 2;
        panUnitInfo.add(butAdd, c);
        
        c.gridy = 1;
        c.gridwidth = 1;
        panUnitInfo.add(butArmy, c);

        c.gridx = 1;
        panUnitInfo.add(butLoadList, c);

        c.gridx = 0;
        c.gridy = 2;
        panUnitInfo.add(butSkills, c);

        c.gridx = 1;
        panUnitInfo.add(butSaveList, c);

        c.gridx = 0;
        c.gridy = 3;
        panUnitInfo.add(butNames, c);
        
        c.gridx = 1;
        panUnitInfo.add(butShowUnitID, c);
    }

    /**
     * Sets up the player info (team, camo) panel
     */
    private void setupPlayerInfo() {

        playerModel = new PlayerTableModel();
        tablePlayers = new JTable(playerModel) {
            private static final long serialVersionUID = 6252953920509362407L;

            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                IPlayer player = playerModel.getPlayerAt(rowAtPoint(p));
                if (player == null) {
                    return null;
                }
                
                StringBuilder result = new StringBuilder("<HTML>");
                result.append(guiScaledFontHTML(PlayerColors.getColor(player.getColorIndex())));
                result.append(player.getName() + "</FONT>");
                
                result.append(guiScaledFontHTML());
                if ((clientgui.getClient() instanceof BotClient) && player.equals(getLocalPlayer())) {
                    result.append(" (This Bot)");
                } else if (clientgui.getBots().containsKey(player.getName())) {
                    result.append(" (Your Bot)");
                } else if (getLocalPlayer().equals(player)) {
                    result.append(" (You)");
                }
                result.append("<BR>");
                if (player.getConstantInitBonus() != 0) {
                    String sign = (player.getConstantInitBonus() > 0) ? "+" : "";
                    result.append("Initiative Modifier: ").append(sign);
                    result.append(player.getConstantInitBonus());
                } else {
                    result.append("No Initiative Modifier");
                }
                if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
                    int mines = player.getNbrMFConventional() + player.getNbrMFActive() 
                                + player.getNbrMFInferno() + player.getNbrMFVibra();
                    result.append("<BR>Minefields: ").append(mines);
                }
                return result.toString();
            }
        };
        tablePlayers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePlayers.addMouseListener(new PlayerTableMouseAdapter());
        for (int i = 0; i < PlayerTableModel.N_COL; i++) {
            TableColumn column = tablePlayers.getColumnModel().getColumn(i);
            column.setCellRenderer(new PlayerRenderer());
        }

        scrPlayers = new JScrollPane(tablePlayers);
        scrPlayers.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panPlayerInfo = new JPanel();
        panPlayerInfo.setBorder(BorderFactory.createTitledBorder("Player Setup"));

        butAddBot = new JButton(Messages.getString("ChatLounge.butAddBot")); 
        butAddBot.setActionCommand("add_bot"); 
        butAddBot.addActionListener(lobbyListener);

        butRemoveBot = new JButton(Messages.getString("ChatLounge.butRemoveBot")); 
        butRemoveBot.setEnabled(false);
        butRemoveBot.setActionCommand("remove_bot"); 
        butRemoveBot.addActionListener(lobbyListener);
        
        butBotSettings.setEnabled(false);
        butBotSettings.setActionCommand("BOTCONFIG"); 
        butBotSettings.addActionListener(lobbyListener);
        
        butConfigPlayer.setEnabled(false);
        butConfigPlayer.setActionCommand("CONFIGURE"); 
        butConfigPlayer.addActionListener(lobbyListener);
        
        butShowUnitID.setSelected(PreferenceManager.getClientPreferences().getShowUnitId());
        butShowUnitID.addActionListener(lobbyListener);

        setupTeams();

        butCamo = new JButton();
        butCamo.setPreferredSize(new Dimension(84, 72));
        butCamo.setActionCommand("camo");
        butCamo.addActionListener(e -> {
            // Show the CamoChooser for the selected player
            IPlayer player = getSelectedPlayer().getLocalPlayer();
            int result = camoDialog.showDialog(player);

            // If the dialog was canceled or nothing selected, do nothing
            if ((result == JOptionPane.CANCEL_OPTION) || (camoDialog.getSelectedItem() == null)) {
                return;
            }

            // Update the player from the camo selection
            AbstractIcon selectedItem = camoDialog.getSelectedItem();
            if (Camouflage.NO_CAMOUFLAGE.equals(selectedItem.getCategory())) {
                player.setColorIndex(camoDialog.getSelectedIndex());
            }
            player.setCamoCategory(selectedItem.getCategory());
            player.setCamoFileName(selectedItem.getFilename());
            butCamo.setIcon(player.getCamouflage().getImageIcon());
            getSelectedPlayer().sendPlayerInfo();
        });
        camoDialog = new CamoChooserDialog(clientgui.getFrame());
        refreshCamos();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panPlayerInfo.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        panPlayerInfo.add(comboTeam, c);

        c.gridy = 1;
        panPlayerInfo.add(butConfigPlayer, c);

        c.gridy = 2;
        panPlayerInfo.add(butAddBot, c);

        c.gridy = 3;
        panPlayerInfo.add(butRemoveBot, c);
        
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 4;
        c.weighty = 1.0;
        panPlayerInfo.add(butCamo, c);

        refreshPlayerInfo();
    }

    private void setupMainPanel() {
        setupMap();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMain.setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(5, 1, 5, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        gridbag.setConstraints(butOptions, c);
        panMain.add(butOptions);

        JPanel panel1 = new JPanel(new GridBagLayout());
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.WEST;
        panel1.add(lblMapSummary, c);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 20);
        panel1.add(lblGameYear, c);
        c.gridx = 2;
        panel1.add(lblTechLevel, c);
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 3;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHEAST;
        panel1.add(butCompact, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        c.gridx = 1;
        c.gridy = 0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        panMain.add(panel1, c);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridheight = 3;
        panMain.add(scrMekTable, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.1;
        c.weighty = 0.0;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        panMain.add(panUnitInfo, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 2;
        c.weighty = 0.0;
        panMain.add(panPlayerInfo, c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 3;
        c.weighty = 1.0;
        panMain.add(scrPlayers, c);
    }

    private void setupMap() {

        mapSettings = MapSettings.getInstance(clientgui.getClient().getMapSettings());

        butConditions = new JButton(Messages.getString("ChatLounge.butConditions")); 
        butConditions.addActionListener(lobbyListener);

        butRandomMap = new JButton(Messages.getString("BoardSelectionDialog.GeneratedMapSettings")); 
        butRandomMap.addActionListener(lobbyListener);

        chkIncludeGround = new JCheckBox(Messages.getString("ChatLounge.IncludeGround")); 
        chkIncludeGround.addActionListener(lobbyListener);

        chkIncludeSpace = new JCheckBox(Messages.getString("ChatLounge.IncludeSpace")); 
        chkIncludeSpace.addActionListener(lobbyListener);

        setupGroundMap();
        setupSpaceMap();
        refreshSpaceGround();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMap.setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(5, 1, 5, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        panMap.add(butConditions, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        panMap.add(butRandomMap, c);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;
        c.weighty = 0.75;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.NORTHWEST;
        panMap.add(panGroundMap, c);

        c.weighty = 0.25;
        c.gridy = 2;
        panMap.add(panSpaceMap, c);

    }

    /**
     * Sets up the ground map selection panel
     */
    @SuppressWarnings("rawtypes")
    private void setupGroundMap() {

        panGroundMap = new JPanel();
        panGroundMap.setBorder(BorderFactory.createTitledBorder("Planetary Map"));

        panMapButtons = new JPanel();

        comboMapType = new JComboBox<String>();
        setupMapChoice();

        butMapSize = new JButton(Messages.getString("ChatLounge.MapSize")); 
        butMapSize.addActionListener(lobbyListener);

        comboMapSizes = new JComboBox<Comparable>();
        setupMapSizes();

        buttonBoardPreview = new JButton(Messages.getString("BoardSelectionDialog.ViewGameBoard")); 
        buttonBoardPreview.addActionListener(lobbyListener);
        buttonBoardPreview.setToolTipText(Messages.getString("BoardSelectionDialog.ViewGameBoardTooltip"));

        butChange = new JButton("<<");
        butChange.addActionListener(lobbyListener);

        lblBoardsSelected = new JLabel(Messages.getString("BoardSelectionDialog.MapsSelected"), SwingConstants.CENTER); 
        lblBoardsAvailable = new JLabel(Messages.getString("BoardSelectionDialog.mapsAvailable"), 
                SwingConstants.CENTER);

        lisBoardsSelected = new JList<String>(new DefaultListModel<String>());
        lisBoardsSelected.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lisBoardsAvailable = new JList<String>(new DefaultListModel<String>());
        refreshBoardsSelected();
        refreshBoardsAvailable();
        lisBoardsAvailable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lisBoardsAvailable.addMouseListener(this);
        

        chkRotateBoard = new JCheckBox(Messages.getString("BoardSelectionDialog.RotateBoard")); 
        chkRotateBoard.addActionListener(lobbyListener);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panGroundMap.setLayout(gridbag);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        panGroundMap.add(chkIncludeGround, c);

        c.gridy = 1;
        panGroundMap.add(comboMapType, c);

        c.gridy = 2;
        panGroundMap.add(comboMapSizes, c);

        c.gridy = 3;
        panGroundMap.add(butMapSize, c);

        c.gridy = 4;
        panGroundMap.add(buttonBoardPreview, c);

        scrMapButtons = new JScrollPane(panMapButtons);
        refreshMapButtons();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        panGroundMap.add(scrMapButtons, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridy = 1;
        panGroundMap.add(lblBoardsSelected, c);

        scrBoardsSelected = new JScrollPane(lisBoardsSelected);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridy = 2;
        c.gridheight = 4;
        panGroundMap.add(scrBoardsSelected, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 2;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.CENTER;
        panGroundMap.add(butChange, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 3;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        panGroundMap.add(lblBoardsAvailable, c);

        scrBoardsAvailable = new JScrollPane(lisBoardsAvailable);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 4;
        c.anchor = GridBagConstraints.NORTHWEST;
        panGroundMap.add(scrBoardsAvailable, c);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 4;
        c.gridy = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.CENTER;
        panGroundMap.add(chkRotateBoard, c);

        mapPreviewPanel = new JPanel();

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 4;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 4;
        c.anchor = GridBagConstraints.NORTHWEST;
        panGroundMap.add(mapPreviewPanel, c);

        try {
            miniMap = new MiniMap(mapPreviewPanel, null);
            // Set a default size for the minimap object to ensure it will have
            // space on the screen to be drawn.
            miniMap.setSize(160, 200);
            miniMap.setZoom(2);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, Messages.getString("BoardEditor.CouldNotInitialiseMinimap") + e,
                    Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE); 
        }
        mapPreviewPanel.add(miniMap);

        // setup the board preview window.
        boardPreviewW = new ClientDialog(clientgui.frame, 
                Messages.getString("BoardSelectionDialog.ViewGameBoard"), 
                false);
        boardPreviewW.setLocationRelativeTo(clientgui.frame);
        boardPreviewW.setVisible(false);

        try {
            BoardView1 bv = new BoardView1(boardPreviewGame, null, null);
            bv.setDisplayInvalidHexInfo(false);
            bv.setUseLOSTool(false);
            boardPreviewW.add(bv.getComponent(true));
            boardPreviewW.setSize(clientgui.frame.getWidth()/2, clientgui.frame.getHeight()/2);
            bv.zoomOut();
            bv.zoomOut();
            bv.zoomOut();
            bv.zoomOut();
            boardPreviewW.center();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                            Messages.getString("BoardEditor.CouldntInitialize") + e,
                            Messages.getString("BoardEditor.FatalError"), JOptionPane.ERROR_MESSAGE); 
        }

    }

    private void setupSpaceMap() {
        panSpaceMap = new JPanel();
        panSpaceMap.setBorder(BorderFactory.createTitledBorder("Space Map"));

        butSpaceSize = new JButton(Messages.getString("ChatLounge.MapSize"));
        butSpaceSize.addActionListener(lobbyListener);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panSpaceMap.setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        panSpaceMap.add(chkIncludeSpace, c);

        c.weighty = 1.0;
        c.gridy = 1;
        panSpaceMap.add(butSpaceSize, c);
    }

    /**
     * Set up the map chooser panel
     */
    private void setupMapChoice() {
        comboMapType.addItem(MapSettings.getMediumName(MapSettings.MEDIUM_GROUND));
        comboMapType.addItem(MapSettings.getMediumName(MapSettings.MEDIUM_ATMOSPHERE));
        comboMapType.addActionListener(lobbyListener);
        refreshMapChoice();
    }

    private void setupMapSizes() {
        int oldSelection = comboMapSizes.getSelectedIndex();
        mapSizes = clientgui.getClient().getAvailableMapSizes();
        comboMapSizes.removeActionListener(lobbyListener);
        comboMapSizes.removeAllItems();
        for (BoardDimensions size : mapSizes) {
            comboMapSizes.addItem(size);
        }
        comboMapSizes.addItem(Messages.getString("ChatLounge.CustomMapSize"));
        comboMapSizes.setSelectedIndex(oldSelection != -1 ? oldSelection : 0);
        comboMapSizes.addActionListener(lobbyListener);
    }

    private void refreshMapChoice() {
        comboMapType.removeActionListener(lobbyListener);
        if (mapSettings.getMedium() < MapSettings.MEDIUM_SPACE) {
            comboMapType.setSelectedIndex(mapSettings.getMedium());
        }
        comboMapType.addActionListener(lobbyListener);
    }

    private void refreshSpaceGround() {
        chkIncludeGround.removeActionListener(lobbyListener);
        chkIncludeSpace.removeActionListener(lobbyListener);
        boolean inSpace = mapSettings.getMedium() == MapSettings.MEDIUM_SPACE;
        chkIncludeSpace.setSelected(inSpace);
        chkIncludeGround.setSelected(!inSpace);
        comboMapType.setEnabled(!inSpace);
        butMapSize.setEnabled(!inSpace);
        comboMapSizes.setEnabled(!inSpace);
        buttonBoardPreview.setEnabled(!inSpace);
        lisBoardsSelected.setEnabled(!inSpace);
        butChange.setEnabled(!inSpace);
        lisBoardsAvailable.setEnabled(!inSpace);
        chkRotateBoard.setEnabled(!inSpace);
        butSpaceSize.setEnabled(inSpace);
        butConditions.setEnabled(!inSpace);
        chkIncludeGround.addActionListener(lobbyListener);
        chkIncludeSpace.addActionListener(lobbyListener);
    }

    private void refreshBoardsAvailable() {
        int selectedRow = lisBoardsAvailable.getSelectedIndex();
        ((DefaultListModel<String>) lisBoardsAvailable.getModel()).removeAllElements();
        for (String s : mapSettings.getBoardsAvailableVector()) {
            ((DefaultListModel<String>) lisBoardsAvailable.getModel()).addElement(s);
        }
        if (resetAvailBoardSelection) {
            lisBoardsAvailable.setSelectedIndex(0);
            resetAvailBoardSelection = false;
        } else {
            lisBoardsAvailable.setSelectedIndex(selectedRow);
        }
    }

    private void refreshBoardsSelected() {
        int selectedRow = lisBoardsSelected.getSelectedIndex();
        ((DefaultListModel<String>) lisBoardsSelected.getModel()).removeAllElements();
        int index = 0;
        for (Iterator<String> i = mapSettings.getBoardsSelected(); i.hasNext();) {
            ((DefaultListModel<String>) lisBoardsSelected.getModel()).addElement(index++ + ": " + i.next()); 
        }
        lisBoardsSelected.setSelectedIndex(selectedRow);
        if (resetSelectedBoards) {
            lisBoardsSelected.setSelectedIndex(0);
            resetSelectedBoards = false;
        } else {
            lisBoardsSelected.setSelectedIndex(selectedRow);
        }
    }

    /**
     * Fills the Map Buttons scroll pane with the appropriate amount of buttons
     * in the appropriate layout
     */
    private void refreshMapButtons() {
        panMapButtons.removeAll();

        panMapButtons.setLayout(new GridLayout(mapSettings.getMapHeight(), mapSettings.getMapWidth()));

        for (int i = 0; i < mapSettings.getMapHeight(); i++) {
            for (int j = 0; j < mapSettings.getMapWidth(); j++) {
                JButton button = new JButton(Integer.toString((i * mapSettings.getMapWidth()) + j));
                Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
                button.setFont(scaledFont);
                button.addActionListener(lobbyListener);
                panMapButtons.add(button);
            }
        }

        scrMapButtons.validate();

        lblBoardsAvailable.setText(mapSettings.getBoardWidth() + "x" + mapSettings.getBoardHeight() + " "
                + Messages.getString("BoardSelectionDialog.mapsAvailable"));
        comboMapSizes.removeActionListener(lobbyListener);
        int items = comboMapSizes.getItemCount();

        boolean mapSizeSelected = false;
        for (int i = 0; i < (items - 1); i++) {
            BoardDimensions size = (BoardDimensions) comboMapSizes.getItemAt(i);

            if ((size.width() == mapSettings.getBoardWidth()) && (size.height() == mapSettings.getBoardHeight())) {
                comboMapSizes.setSelectedIndex(i);
                mapSizeSelected = true;
            }
        }
        // If we didn't select a size, select the last item: 'Custom Size'
        if (!mapSizeSelected) {
            comboMapSizes.setSelectedIndex(items - 1);
        }
        comboMapSizes.addActionListener(lobbyListener);

    }

    public void previewMapsheet() {
        String boardName = lisBoardsAvailable.getSelectedValue();
        if (lisBoardsAvailable.getSelectedIndex() > 2) {
            IBoard board = new Board(16, 17);
            board.load(new MegaMekFile(Configuration.boardsDir(), boardName + ".board").getFile());
            if (chkRotateBoard.isSelected()) {
                BoardUtilities.flip(board, true, true);
            }
            if (board.isValid())
                miniMap.setBoard(board);
        }
    }

    public void previewGameBoard() {
        MapSettings temp = mapSettings;
        temp.replaceBoardWithRandom(MapSettings.BOARD_RANDOM);
        temp.replaceBoardWithRandom(MapSettings.BOARD_SURPRISE);
        IBoard[] sheetBoards = new IBoard[temp.getMapWidth() * temp.getMapHeight()];
        List<Boolean> rotateBoard = new ArrayList<>();
        for (int i = 0; i < (temp.getMapWidth() * temp.getMapHeight()); i++) {
            sheetBoards[i] = new Board();
            String name = temp.getBoardsSelectedVector().get(i);
            boolean isRotated = false;
            if (name.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                // only rotate boards with an even width
                if ((temp.getBoardWidth() % 2) == 0) {
                    isRotated = true;
                }
                name = name.substring(Board.BOARD_REQUEST_ROTATION.length());
            }
            if (name.startsWith(MapSettings.BOARD_GENERATED) || (temp.getMedium() == MapSettings.MEDIUM_SPACE)) {
                sheetBoards[i] = BoardUtilities.generateRandom(temp);
            } else {
                sheetBoards[i].load(new MegaMekFile(Configuration.boardsDir(), name
                        + ".board").getFile());
                BoardUtilities.flip(sheetBoards[i], isRotated, isRotated);
            }
            rotateBoard.add(isRotated);
        }

        IBoard newBoard = BoardUtilities.combine(temp.getBoardWidth(), temp.getBoardHeight(), temp.getMapWidth(),
                temp.getMapHeight(), sheetBoards, rotateBoard, temp.getMedium());
        
        boardPreviewGame.setBoard(newBoard);
        boardPreviewW.setVisible(true);
    }

    /**
     * Refreshes the game settings with new info from the client
     */
    private void refreshGameSettings() {
        refreshTeams();
        refreshDoneButton();
    }

    /**
     * Refreshes the entities from the client
     */
    public void refreshEntities() {
        mekModel.clearData();
        ArrayList<Entity> allEntities = new ArrayList<Entity>(clientgui.getClient().getEntitiesVector());
        Collections.sort(allEntities, activeSorter);

        boolean localUnits = false;
        GameOptions opts = clientgui.getClient().getGame().getOptions();
        
        for (Entity entity : allEntities) {
            // Remember if the local player has units.
            if (!localUnits && entity.getOwner().equals(getLocalPlayer())) {
                localUnits = true;
            }

            if (!opts.booleanOption(OptionsConstants.RPG_PILOT_ADVANTAGES)) { 
                entity.getCrew().clearOptions(PilotOptions.LVL3_ADVANTAGES);
            }

            if (!opts.booleanOption(OptionsConstants.EDGE)) { 
                entity.getCrew().clearOptions(PilotOptions.EDGE_ADVANTAGES);
            }

            if (!opts.booleanOption(OptionsConstants.RPG_MANEI_DOMINI)) { 
                entity.getCrew().clearOptions(PilotOptions.MD_ADVANTAGES);
            }

            if (!opts.booleanOption(OptionsConstants.ADVANCED_STRATOPS_PARTIALREPAIRS)) { 
                entity.clearPartialRepairs();
            }
            
            // Remove some deployment options when a unit is carried
            if (entity.getTransportId() != Entity.NONE) { 
                entity.setHidden(false);
                entity.setProne(false);
                entity.setHullDown(false);
            }
            
            if (!opts.booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) { 
                entity.setHidden(false);
            }
            
            // Handle the "Blind Drop" option. In blind drop, units must be added
            // but they will be obscured in the table. In real blind drop, units
            // don't even get added to the table. Teams see their units in any case.
            boolean localUnit = entity.getOwner().equals(getLocalPlayer());
            boolean teamUnit = !entity.getOwner().isEnemyOf(getLocalPlayer());
            boolean realBlindDrop = opts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
            if (localUnit || teamUnit || !realBlindDrop) {
                mekModel.addUnit(entity);
            }
        }

        // Enable the "Save Unit List..." button if the local player has units.
        butSaveList.setEnabled(localUnits);
        clientgui.getMenuBar().setUnitList(localUnits);
    }
    
    private void toggleCompact() {
        setTableRowHeights();
        mekModel.refreshCells();
    }

    public static final String DOT_SPACER = " \u2B1D ";

    /** Refreshes the player info table. */
    private void refreshPlayerInfo() {
        // Remember the selected player
        Client c = getSelectedPlayer();
        String selPlayer = "";
        if (c != null) {
            selPlayer = c.getLocalPlayer().getName();
        }
        
        // Empty and refill the player table
        playerModel.clearData();
        for (Enumeration<IPlayer> i = clientgui.getClient().getPlayers(); i.hasMoreElements();) {
            final IPlayer player = i.nextElement();
            if (player == null) {
                continue;
            }
            playerModel.addPlayer(player);
        }
        
        // re-select the previously selected player, if possible
        if (c != null && playerModel.getRowCount() > 0) {
            for (int row = 0; row < playerModel.getRowCount(); row++) {
                IPlayer p = playerModel.getPlayerAt(row);
                if (p.getName().equals(selPlayer)) {
                    tablePlayers.setRowSelectionInterval(row, row);
                    break;
                }
            }
        }
    }

    private void refreshCamos() {
        if ((tablePlayers == null) || (playerModel == null) || (tablePlayers.getSelectedRow() == -1)) {
            return;
        }
        IPlayer player = playerModel.getPlayerAt(tablePlayers.getSelectedRow());
        if (player != null) {
            butCamo.setIcon(player.getCamouflage().getImageIcon());
        }
    }

    /** Sets up the team choice box. */
    private void setupTeams() {
        for (int i = 0; i < IPlayer.MAX_TEAMS; i++) {
            comboTeam.addItem(IPlayer.teamNames[i]);
        }
        comboTeam.addActionListener(lobbyListener);
    }

    /**
     * Highlight the team the player is playing on.
     */
    private void refreshTeams() {
        comboTeam.removeActionListener(lobbyListener);
        comboTeam.setSelectedIndex(getLocalPlayer().getTeam());
        comboTeam.addActionListener(lobbyListener);
    }

    /**
     * Refreshes the done button. The label will say the opposite of the
     * player's "done" status, indicating that clicking it will reverse the
     * condition.
     */
    private void refreshDoneButton(boolean done) {
        butDone.setText(done ? Messages.getString("ChatLounge.notDone") : Messages.getString("ChatLounge.imDone"));
    }

    private void refreshDoneButton() {
        refreshDoneButton(getLocalPlayer().isDone());
    }

    /** Change the team of a controlled player (the local player or one of his bots). */
    private void changeTeam(int team) {
        Client c = getSelectedPlayer();
        
        // If the team was not actually changed or the selected player 
        // is not editable (not the local player or local bot), do nothing
        if ((c == null) || (c.getLocalPlayer().getTeam() == team)) {
            return;
        }
        
        // Since different teams are always enemies, changing the team forces 
        // the units of this player to offload and disembark from 
        // all units of other players
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: c.getGame().getPlayerEntities(c.getLocalPlayer(), false)) {
            offloadFromDifferentOwner(entity, updateCandidates);
            disembarkDifferentOwner(entity, updateCandidates);
        }
        sendUpdate(updateCandidates);

        c.getLocalPlayer().setTeam(team);
        c.sendPlayerInfo();
    }

    /**
     * Pop up the customize mech dialog
     */

    private void customizeMech() {
        if (mekTable.getSelectedRow() == -1) {
            return;
        }
        customizeMech(mekModel.getEntityAt(mekTable.getSelectedRow()));
    }

    /**
     * Embarks the given carried Entity onto the carrier given as carrierId.
     */
    private void loadOnto(Entity carried, int carrierId, int bayNumber) {
        Entity carrier = clientgui.getClient().getGame().getEntity(carrierId);
        if (carrier == null || !isLoadable(carried, carrier)) {
            return;
        }

        // We need to make sure our current bomb choices fit onto the new
        // fighter
        if (carrier instanceof FighterSquadron) {
            FighterSquadron fSquad = (FighterSquadron) carrier;
            // We can't use Aero.getBombPoints() because the bombs haven't been
            // loaded yet, only selected, so we have to count the choices
            int[] bombChoice = fSquad.getBombChoices();
            int numLoadedBombs = 0;
            for (int i = 0; i < bombChoice.length; i++) {
                numLoadedBombs += bombChoice[i];
            }
            // We can't load all of the squadrons bombs
            if (numLoadedBombs > ((IBomber)carried).getMaxBombPoints()) {
                JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("FighterSquadron.bomberror"),
                        Messages.getString("FighterSquadron.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        getLocalClient(carried).sendLoadEntity(carried.getId(), carrierId, bayNumber);
        // TODO: it would probably be a good idea 
        // to disable it in customMechDialog
    }

    /** 
     * Have the given entity disembark if it is carried by another unit.
     * Entities that are modified and need an update to be sent to the server
     * are added to the given updateCandidates. 
     */
    private void disembark(Entity entity, Collection<Entity> updateCandidates) {
        if (entity.getTransportId() == Entity.NONE) {
            return;
        }
        Entity carrier = clientgui.getClient().getGame().getEntity(entity.getTransportId());
        if (carrier != null) {
            carrier.unload(entity);
            entity.setTransportId(Entity.NONE);
            updateCandidates.add(entity);
            updateCandidates.add(carrier);
        }
    }
    
    /** 
     * Have the given entity disembark if it is carried by a unit of another player.
     * Entities that were modified and need an update to be sent to the server
     * are added to the given updateCandidate set. 
     */
    private void disembarkDifferentOwner(Entity entity, Collection<Entity> updateCandidates) {
        if (entity.getTransportId() == Entity.NONE) {
            return;
        }
        Entity carrier = clientgui.getClient().getGame().getEntity(entity.getTransportId());
        if (carrier != null && (ownerOf(entity) != ownerOf(carrier))) {
            disembark(entity, updateCandidates);
        }
    }
    
    /** 
     * Have the given entities offload all the units they are carrying.
     * Returns a set of entities that need to be sent to the server. 
     */
    private void offloadAll(Collection<Entity> entities, Collection<Entity> updateCandidates) {
        for (Entity carrier: editableEntities(entities)) {
            offloadFrom(carrier, updateCandidates);
        }
    }
    
    /** 
     * Have the given entity offload all the units it is carrying.
     * Returns a set of entities that need to be sent to the server. 
     */
    private void offloadFrom(Entity entity, Collection<Entity> updateCandidates) {
        if (isEditable(entity)) {
            for (Entity carriedUnit: entity.getLoadedUnits()) {
                disembark(carriedUnit, updateCandidates);
            } 
        }
    }
    
    /** 
     * Have the given entity offload all units of different players it is carrying.
     * Returns a set of entities that need to be sent to the server. 
     */
    private void offloadFromDifferentOwner(Entity entity, Collection<Entity> updateCandidates) {
        for (Entity carriedUnit: entity.getLoadedUnits()) {
            if (ownerOf(carriedUnit) != ownerOf(entity)) {
                disembark(carriedUnit, updateCandidates);
            }
        } 
    }
    
    /** Change the given entities' controller to the player with ID newOwnerId. */
    private void changeOwner(Collection<Entity> entities, int newOwnerId) {
        IPlayer new_owner = clientgui.getClient().getGame().getPlayer(newOwnerId);
        if (new_owner == null) {
            return;
        }
        
        // Store entities that need to be sent to the Server to avoid sending them twice
        Set<Entity> updateCandidates = new HashSet<>();
        
        // For any units that are switching teams, offload units from them
        // and have them disembark if carried
        for (Entity entity: editableEntities(entities)) {
            if (entity.getOwner().isEnemyOf(new_owner)) {
                offloadFrom(entity, updateCandidates);
                disembark(entity, updateCandidates);
            }
        }

        // Update any changed entities except for the entities changing owner (treated below)
        updateCandidates.removeAll(entities);
        sendUpdate(updateCandidates);
        
        // The entities themselves must be updated from the correct client
        // to make the update work when changing owner to a remote player
        for (Entity entity: editableEntities(entities)) {
            Client formerClient = getLocalClient(entity);
            entity.setOwner(new_owner);
            formerClient.sendUpdateEntity(entity);
        }
    }
    
    /**
     * Swaps pilots between the given entity (that was selected in the
     * Mektable) and another entity of the given swapperId
     */
    private void swapPilots(Entity swapee, int swapperId) {
        Entity swapper = clientgui.getClient().getGame().getEntity(swapperId);
        if (swapper == null || !isEditable(swapee) || !isEditable(swapper)) {
            return;
        }
        Crew temp = swapper.getCrew();
        swapper.setCrew(swapee.getCrew());
        swapee.setCrew(temp);
        getLocalClient(swapee).sendUpdateEntity(swapee);
        getLocalClient(swapper).sendUpdateEntity(swapper);
    }

    /** Sends the entities in the given Collection to the Server. */
    private void sendUpdate(Collection<Entity> updateCandidates) {
        updateCandidates.stream()
                .filter(e -> isEditable(e))
                .forEach(e -> getLocalClient(e).sendUpdateEntity(e));
    }

    /** Deletes the given entities, offloading/disembarking them first. */
    private void deleteEntities(Collection<Entity> entities) {
        // Only consider entities that can be deleted by the local player
        Collection<Entity> deletionCandidates = editableEntities(entities);
        
        // Store entities that need to be sent to the Server to avoid sending them twice
        Set<Entity> updateCandidates = new HashSet<>();

        // Cycle the entities, disembark/offload all
        for (Entity entity: deletionCandidates) {
            offloadFrom(entity, updateCandidates);
            disembark(entity, updateCandidates);
        }
        
        // Update the units, but not those that will be deleted anyway
        updateCandidates.removeAll(deletionCandidates);
        sendUpdate(updateCandidates);

        // Finally, delete them
        for (Entity entity: deletionCandidates) {
            getLocalClient(entity).sendDeleteEntity(entity.getId());
        }
    }
    
    /** OK
     * Sets random skills for the given entities, as far as they can
     * be configured by the local player. 
     */
    private void setRandomSkills(Collection<Entity> entities) {
        entities.stream().filter(e -> isEditable(e)).forEach(e -> {
            Client c = getLocalClient(e);
            for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
                int[] skills = c.getRandomSkillsGenerator().getRandomSkills(e, true);
                e.getCrew().setGunnery(skills[0], i);
                e.getCrew().setPiloting(skills[1], i);
                if (e.getCrew() instanceof LAMPilot) {
                    skills = c.getRandomSkillsGenerator().getRandomSkills(e, true);
                    ((LAMPilot) e.getCrew()).setGunneryAero(skills[0]);
                    ((LAMPilot) e.getCrew()).setPilotingAero(skills[1]);
                }
            }
            e.getCrew().sortRandomSkills();
            getLocalClient(e).sendUpdateEntity(e);
        });
    }
    
    /** OK
     * Sets random names for the given entities' pilots, as far as they can
     * be configured by the local player. 
     */
    private void setRandomNames(Collection<Entity> entities) {
        entities.stream().filter(e -> isEditable(e)).forEach(e -> {
            for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
                Gender gender = RandomGenderGenerator.generate();
                e.getCrew().setGender(gender, i);
                e.getCrew().setName(RandomNameGenerator.getInstance().generate(gender, e.getOwner().getName()), i);
            }
            getLocalClient(e).sendUpdateEntity(e);
        });
    }
    
    /** OK
     * Sets random callsigns for the given entities' pilots, as far as they can
     * be configured by the local player. 
     */
    private void setRandomCallsigns(Collection<Entity> entities) {
        entities.stream().filter(e -> isEditable(e)).forEach(e -> {
            for (int i = 0; i < e.getCrew().getSlotCount(); i++) {
                e.getCrew().setNickname(RandomCallsignGenerator.getInstance().generate(), i);
            }
            getLocalClient(e).sendUpdateEntity(e);
        });
    }
    
    /** 
     * Disembarks all given entities from any transports they are in. 
     */
    private void disembarkAll(Collection<Entity> entities) {
        Set<Entity> updateCandidates = new HashSet<>();
        entities.stream().filter(e -> isEditable(e)).forEach(e -> disembark(e, updateCandidates));
        sendUpdate(updateCandidates);
    }
    
    /** Returns true if the given entities all belong to the same player. */
    private boolean haveSingleOwner(Collection<Entity> entities) {
        return entities.stream().mapToInt(e -> e.getOwner().getId()).distinct().count() == 1;
    }

    /**
     *
     * @param entities
     */
    public void customizeMechs(List<Entity> entities) {
        // Only call this for when selecting a valid list of entities
        if (entities.size() < 1 || !haveSingleOwner(entities)) {
            return;
        }
        String ownerName = entities.get(0).getOwner().getName();
        int ownerId = entities.get(0).getOwner().getId();

        boolean editable = clientgui.getBots().get(ownerName) != null;
        Client client;
        if (editable) {
            client = clientgui.getBots().get(ownerName);
        } else {
            editable |= ownerId == getLocalPlayer().getId();
            client = clientgui.getClient();
        }

        CustomMechDialog cmd = new CustomMechDialog(clientgui, client, entities, editable);
        cmd.setSize(new Dimension(GUIPreferences.getInstance().getCustomUnitWidth(),
                GUIPreferences.getInstance().getCustomUnitHeight()));
        cmd.setTitle(Messages.getString("ChatLounge.CustomizeUnits")); 
        cmd.setVisible(true);
        GUIPreferences.getInstance().setCustomUnitHeight(cmd.getSize().height);
        GUIPreferences.getInstance().setCustomUnitWidth(cmd.getSize().width);
        if (editable && cmd.isOkay()) {
            // send changes
            for (Entity entity : entities) {
                // If a LAM with mechanized BA was changed to non-mech mode, unload the BA.
                if ((entity instanceof LandAirMech)
                        && entity.getConversionMode() != LandAirMech.CONV_MODE_MECH) {
                    for (Entity loadee : entity.getLoadedUnits()) {
                        entity.unload(loadee);
                        loadee.setTransportId(Entity.NONE);
                        client.sendUpdateEntity(loadee);
                    }
                }

                client.sendUpdateEntity(entity);

                // Changing state to a transporting unit can update state of
                // transported units, so update those as well
                for (Transporter transport : entity.getTransports()) {
                    for (Entity loaded : transport.getLoadedUnits()) {
                        client.sendUpdateEntity(loaded);
                    }
                }

                // Customizations to a Squadron can effect the fighters
                if (entity instanceof FighterSquadron) {
                    entity.getSubEntities().ifPresent(ents -> ents.forEach(client::sendUpdateEntity));
                }
            }
        }
        if (cmd.isOkay() && (cmd.getStatus() != CustomMechDialog.DONE)) {
            Entity nextEnt = cmd.getNextEntity(cmd.getStatus() == CustomMechDialog.NEXT);
            customizeMech(nextEnt);
        }
    }

    /** 
     * Confirms that the player really wants to delete the units, and deletes them.
     * Assumes that entities contains at least one entity that the player can
     * actually delete. 
     */
    private void deleteAction(Collection<Entity> entities) {
        // Only count entities that the player can actually delete
        HashSet<Entity> configurableEntities = new HashSet<>(entities);
        configurableEntities.removeIf(e -> !isEditable(e));
        int count = configurableEntities.size();
        
        if (count == 0) {
            JOptionPane.showMessageDialog(clientgui.frame, "You cannot delete any of the selected units!");
            return;
        }
        
        String question = "Really delete ";
        question += (count == 1) ? "one unit?" : configurableEntities.size() + " units?";
        if (Response.YES == MMConfirmDialog.confirm(clientgui.getFrame(), "Delete Units...", question)) {
            deleteEntities(entities);
        }
        
    }

    /**
     *
     * @param entity
     */
    public void customizeMech(Entity entity) {
        boolean editable = clientgui.getBots().get(entity.getOwner().getName()) != null;
        Client c;
        if (editable) {
            c = clientgui.getBots().get(entity.getOwner().getName());
        } else {
            editable |= entity.getOwnerId() == getLocalPlayer().getId();
            c = clientgui.getClient();
        }
        // When we customize a single entity's C3 network setting,
        // **ALL** members of the network may get changed.
        Entity c3master = entity.getC3Master();
        ArrayList<Entity> c3members = new ArrayList<Entity>();
        Iterator<Entity> playerUnits = c.getGame().getPlayerEntities(c.getLocalPlayer(), false).iterator();
        while (playerUnits.hasNext()) {
            Entity unit = playerUnits.next();
            if (!entity.equals(unit) && entity.onSameC3NetworkAs(unit)) {
                c3members.add(unit);
            }
        }

        boolean doneCustomizing = false;
        while (!doneCustomizing) {
            // display dialog
            List<Entity> entities = new ArrayList<>();
            entities.add(entity);
            CustomMechDialog cmd = new CustomMechDialog(clientgui, c, entities, editable);
            cmd.setSize(new Dimension(GUIPreferences.getInstance().getCustomUnitWidth(),
                    GUIPreferences.getInstance().getCustomUnitHeight()));
            cmd.refreshOptions();
            cmd.refreshQuirks();
            cmd.refreshPartReps();
            cmd.setTitle(entity.getShortName());
            if (cmdSelectedTab != null) {
                cmd.setSelectedTab(cmdSelectedTab);
            }
            cmd.setVisible(true);
            GUIPreferences.getInstance().setCustomUnitHeight(cmd.getSize().height);
            GUIPreferences.getInstance().setCustomUnitWidth(cmd.getSize().width);
            cmdSelectedTab = cmd.getSelectedTab();
            if (editable && cmd.isOkay()) {
                // If a LAM with mechanized BA was changed to non-mech mode, unload the BA.
                if ((entity instanceof LandAirMech)
                        && entity.getConversionMode() != LandAirMech.CONV_MODE_MECH) {
                    for (Entity loadee : entity.getLoadedUnits()) {
                        entity.unload(loadee);
                        loadee.setTransportId(Entity.NONE);
                        c.sendUpdateEntity(loadee);
                    }
                }

                // send changes
                c.sendUpdateEntity(entity);

                // Changing state to a transporting unit can update state of
                // transported units, so update those as well
                for (Transporter transport : entity.getTransports()) {
                    for (Entity loaded : transport.getLoadedUnits()) {
                        c.sendUpdateEntity(loaded);
                    }
                }

                // Customizations to a Squadron can effect the fighters
                if (entity instanceof FighterSquadron) {
                    entity.getSubEntities().ifPresent(ents -> ents.forEach(c::sendUpdateEntity));
                }

                // Do we need to update the members of our C3 network?
                if (((c3master != null) && !c3master.equals(entity.getC3Master()))
                        || ((c3master == null) && (entity.getC3Master() != null))) {
                    for (Entity unit : c3members) {
                        c.sendUpdateEntity(unit);
                    }
                }
            }
            if (cmd.isOkay() && (cmd.getStatus() != CustomMechDialog.DONE)) {
                entity = cmd.getNextEntity(cmd.getStatus() == CustomMechDialog.NEXT);
            } else {
                doneCustomizing = true;
            }
        }
    }

    /** 
     * Displays a CamoChooser to choose an individual camo for 
     * the given entities. The camo will only be applied
     * to units configurable by the local player, i.e. his own units
     * or those of his bots.
     */
    public void mechCamo(Collection<Entity> entities) {
        Collection<Entity> editableEntities = editableEntities(entities);
        if (editableEntities.isEmpty()) {
            return;
        }

        // We need one of the selected units to base some tests on
        Entity randomSelected = editableEntities.stream().findAny().get();
        
        // Display the CamoChooser and await the result
        // The dialog is preset to the first selected unit's settings
        CamoChooserDialog mcd = new CamoChooserDialog(clientgui.getFrame());
        int result = mcd.showDialog(randomSelected);

        // If the dialog was canceled or nothing was selected, do nothing
        if ((result == JOptionPane.CANCEL_OPTION) || (mcd.getSelectedItem() == null)) {
            return;
        }

        // Choosing the player camo resets the units to have no 
        // individual camo.
        AbstractIcon selectedItem = mcd.getSelectedItem();
        IPlayer owner = randomSelected.getOwner();
        AbstractIcon ownerCamo = owner.getCamouflage();
        boolean noIndividualCamo = selectedItem.equals(ownerCamo);
        
        // Update all allowed entities with the camo
        for (Entity ent : editableEntities) {
            if (noIndividualCamo) {
                ent.setCamoCategory(null);
                ent.setCamoFileName(null);
            } else {
                ent.setCamoCategory(selectedItem.getCategory());
                ent.setCamoFileName(selectedItem.getFilename());
            }
            getLocalClient(ent).sendUpdateEntity(ent);
        }
    }
    
    /** 
     * Returns true when the given entity may be configured by the local player,
     * i.e. if it is his own unit or one of his bot's units.
     * <P>Note that this is more restrictive than the Server is. The Server
     * accepts entity changes also for teammates so that entity updates that 
     * signal transporting a teammate's unit don't get rejected. I feel that
     * configuration other than transporting units should be limited to one's
     * own units (and bots) though.
     */
    boolean isEditable(Entity entity) {
        return clientgui.getBots().containsKey(entity.getOwner().getName())
                || (entity.getOwnerId() == getLocalPlayer().getId());
    }
    
    /** 
     * Returns the Client associated with a given entity that may be configured
     * by the local player (his own unit or one of his bot's units).
     * For a unit that cannot be configured (owned by a remote player) the client
     * of the local player is returned.
     */
    private Client getLocalClient(Entity entity) {
        if (clientgui.getBots().containsKey(entity.getOwner().getName())) {
            return clientgui.getBots().get(entity.getOwner().getName());
        } else {
            return clientgui.getClient();
        }
    }

    /** Shows the dialog which allows adding pre-existing damage to units. */
    public void configureDamage(Entity entity) {
        if (!isEditable(entity)) {
            return;
        }

        UnitEditorDialog med = new UnitEditorDialog(clientgui.getFrame(), entity);
        med.setVisible(true);
        getLocalClient(entity).sendUpdateEntity(entity);
    }

    public void configPlayer() {
        Client c = getSelectedPlayer();
        if (null == c) {
            return;
        }
        
        PlayerSettingsDialog psd = new PlayerSettingsDialog(clientgui, c);
        boolean okay = psd.showDialog();
        
        if (okay) {
            IPlayer player = c.getLocalPlayer();
            player.setConstantInitBonus(psd.getInit());
            player.setNbrMFConventional(psd.getCnvMines());
            player.setNbrMFVibra(psd.getVibMines());
            player.setNbrMFActive(psd.getActMines());
            player.setNbrMFInferno(psd.getInfMines());
            
            // The deployment position
            int startPos = psd.getStartPos();
            final GameOptions gOpts = clientgui.getClient().getGame().getOptions();
            if (gOpts.booleanOption(OptionsConstants.BASE_DEEP_DEPLOYMENT)
                    && (startPos >= 1) && (startPos <= 9)) {
                startPos += 10;
            }
            c.getLocalPlayer().setStartingPos(startPos);
            c.sendPlayerInfo();
            
            // If the gameoption set_arty_player_homeedge is set, adjust the player's offboard 
            // arty units to be behind the newly selected home edge.
            OffBoardDirection direction = OffBoardDirection.translateStartPosition(startPos);
            if (direction != OffBoardDirection.NONE && 
                    gOpts.booleanOption(OptionsConstants.BASE_SET_ARTY_PLAYER_HOMEEDGE)) {
                for (Entity entity: c.getGame().getPlayerEntities(c.getLocalPlayer(), false)) {
                    if (entity.getOffBoardDirection() != OffBoardDirection.NONE) {
                        entity.setOffBoard(entity.getOffBoardDistance(), direction);
                    }
                }
            }
        }

    }

    /**
     * Pop up the view mech dialog
     */
    private void mechReadout(Entity entity) {
        final JDialog dialog = new JDialog(clientgui.frame, Messages.getString("ChatLounge.quickView"), false); 
        dialog.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_SPACE) {
                    e.consume();
                    dialog.setVisible(false);
                } else if (code == KeyEvent.VK_ENTER) {
                    e.consume();
                    dialog.setVisible(false);
                }
            }
        });
        // FIXME: this isn't working right, but is necessary for the key
        // listener to work right
        // dialog.setFocusable(true);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.setVisible(false);
            }
        });
        MechViewPanel mvp = new MechViewPanel();
        mvp.setMech(entity);
        JButton btn = new JButton(Messages.getString("Okay")); 
        btn.addActionListener(e -> dialog.setVisible(false));

        dialog.getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints c;

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        dialog.getContentPane().add(mvp, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        dialog.getContentPane().add(btn, c);
        dialog.setSize(mvp.getBestWidth(), mvp.getBestHeight() + 75);
        dialog.validate();
        dialog.setVisible(true);
    }

    /**
     * @param entity the entity to display the BV Calculation for
     */
    private void mechBVDisplay(Entity entity) {
        final JDialog dialog = new JDialog(clientgui.frame, "BV Calculation Display", false);
        dialog.getContentPane().setLayout(new GridBagLayout());

        final int width = 500;
        final int height = 400;
        Dimension size = new Dimension(width, height);

        JEditorPane tEditorPane = new JEditorPane();
        tEditorPane.setContentType("text/html");
        tEditorPane.setEditable(false);
        tEditorPane.setBorder(null);
        entity.calculateBattleValue();
        tEditorPane.setText(entity.getBVText());
        tEditorPane.setCaretPosition(0);

        JScrollPane tScroll = new JScrollPane(tEditorPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tScroll.setBorder(null);
        tScroll.setPreferredSize(size);
        tScroll.setMinimumSize(size);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        dialog.getContentPane().add(tScroll, gridBagConstraints);

        JButton button = new JButton(Messages.getString("Okay"));
        button.addActionListener(e -> dialog.setVisible(false));
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.weighty = 0.0;
        dialog.getContentPane().add(button, gridBagConstraints);

        dialog.setSize(new Dimension(width + 25, height + 75));
        dialog.validate();
        dialog.setVisible(true);
    }

    /**
     * Pop up the dialog to load a mech
     */
    private void addUnit() {
        clientgui.getMechSelectorDialog().updateOptionValues();
        clientgui.getMechSelectorDialog().setVisible(true);
    }
    
    /** OK
     * Creates a fighter squadron from the given list of entities.
     * Checks if all entities are fighters and if the number of entities
     * does not exceed squadron capacity. Asks for a squadron name.
     */
    public void createSquadron(Collection<Entity> entities) {
        if (!validateFightersForSquadron(entities)) {
            return;
        }
        
        // Obtain the IDs
        Vector<Integer> fighterIds = 
                new Vector<>(entities.stream().map(e -> e.getId()).collect(Collectors.toList()));
        
        // Make sure the number of fighters does not exceed squadron capacity
        GameOptions opts = clientgui.getClient().getGame().getOptions();
        if ((!opts.booleanOption(OptionsConstants.ADVAERORULES_ALLOW_LARGE_SQUADRONS)
                && (fighterIds.size() > FighterSquadron.MAX_SIZE))
                || (opts.booleanOption(OptionsConstants.ADVAERORULES_ALLOW_LARGE_SQUADRONS)
                && (fighterIds.size() > FighterSquadron.ALTERNATE_MAX_SIZE))) {
            JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("FighterSquadron.toomany"));
            return;
        } 
        
        // Ask for a squadron name
        String name = JOptionPane.showInputDialog(clientgui.frame, "Choose a squadron designation");
        if ((name == null) || (name.trim().length() == 0)) {
            name = "Alpha";
        }
        
        // Now, actually create the squadron
        FighterSquadron fs = new FighterSquadron(name);
        fs.setOwner(createSquadronOwner(entities));
        clientgui.getClient().sendAddSquadron(fs, fighterIds);
    }
    
    private IPlayer createSquadronOwner(Collection<Entity> entities) {
        if (entities.stream().anyMatch(e -> e.getOwner().equals(getLocalPlayer()))) {
            return getLocalPlayer();
        } else {
            for (Entry<String, Client> en: clientgui.getClient().bots.entrySet()) {
                IPlayer bot = en.getValue().getLocalPlayer();
                if (entities.stream().anyMatch(e -> e.getOwner().equals(bot))) {
                    return bot;
                }
            }
        }
        // Should not arrive here because that means that none of the entities are 
        // editable by the local player.
        MegaMek.getLogger().error("Could not find a suitable owner for creating a fighter squadron.");
        return getLocalPlayer();
    }

    /** 
     * Validates the selected units for fighter squadron creation. Returns true
     * if they can form a squadron 
     */
    private boolean validateFightersForSquadron(Collection<Entity> entities) {
        if (entities.size() == 0) {
            return false;
        }
        if (!areAllied(entities) || !canEditAny(entities)) {
            JOptionPane.showMessageDialog(clientgui.frame, "Enemy units cannot be transported!");
            return false;
        }
        for (Entity e: entities) {
            if (!e.isFighter()) {
                JOptionPane.showMessageDialog(clientgui.frame, "Only aerospace and conventional fighters can join squadrons!");
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns true if no two of the given entities are enemies. This is
     * true when all entities belong to a single player. If they belong to 
     * different players, it is true when all belong to the same team and 
     * that team is one of Teams 1 through 5 (not "No Team").
     * <P>Returns true when entities is empty or has only one entity. The case of
     * entities being empty should be considered by the caller.  
     */
    public boolean areAllied(Collection<Entity> entities) {
        if (entities.size() == 0) {
            MegaMek.getLogger().warning("Empty collection of entities received, cannot determine if no entities are all allied.");
            return true;
        }
        Entity randomEntry = entities.stream().findAny().get();
        return !entities.stream().anyMatch(e -> e.isEnemyOf(randomEntry));
    }

    private void createArmy() {
        clientgui.getRandomArmyDialog().setVisible(true);
    }

    public void loadRandomSkills() {
        clientgui.getRandomSkillDialog().showDialog(clientgui.getClient().getGame().getEntitiesVector());
    }

    public void loadRandomNames() {
        clientgui.getRandomNameDialog().showDialog(clientgui.getClient().getGame().getEntitiesVector());
    }

    /**
     * Changes all selected boards to be the specified board
     */
    private void changeMap(String board) {
        int[] selected = lisBoardsSelected.getSelectedIndices();
        for (final int newVar : selected) {
            String name = board;
            if (!MapSettings.BOARD_RANDOM.equals(name) && !MapSettings.BOARD_SURPRISE.equals(name)
                    && chkRotateBoard.isSelected()) {
                name = Board.BOARD_REQUEST_ROTATION + name;
            }

            // Validate the map
            IBoard b = new Board(16, 17);
            if (!MapSettings.BOARD_GENERATED.equals(board) && !MapSettings.BOARD_RANDOM.equals(board)
                    && !MapSettings.BOARD_SURPRISE.equals(board)) {
                b.load(new MegaMekFile(Configuration.boardsDir(), board + ".board").getFile());
                if (!b.isValid()) {
                    JOptionPane.showMessageDialog(this, "The Selected board is invalid, please select another.");
                    return;
                }
            }
            ((DefaultListModel<String>) lisBoardsSelected.getModel()).setElementAt(newVar + ": " + name, newVar); 
            mapSettings.getBoardsSelectedVector().set(newVar, name);
        }
        lisBoardsSelected.setSelectedIndices(selected);
        clientgui.getClient().sendMapSettings(mapSettings);
        if (boardPreviewW.isVisible()) {
            previewGameBoard();
        }
    }

    //
    // GameListener
    //
    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }

        refreshDoneButton();
        clientgui.getClient().getGame().setupTeams();
        refreshPlayerInfo();
        // Update camo info, unless the player is currently making changes
        if ((camoDialog != null) && !camoDialog.isVisible()) {
            refreshCamos();
        }
        refreshEntities();
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }
        
        if (clientgui.getClient().getGame().getPhase() == IGame.Phase.PHASE_LOUNGE) {
            refreshDoneButton();
            refreshGameSettings();
            refreshPlayerInfo();
            refreshTeams();
            refreshCamos();
            refreshEntities();
        }
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshPlayerInfo();
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshPlayerInfo();
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshGameSettings();
        // The table sorting may no longer be allowed (e.g. when blind drop was activated)
        if (!activeSorter.isAllowed(clientgui.getClient().getGame().getOptions())) {
            nextSorter(unitSorters);
            updateTableHeaders();
        }
        refreshEntities();
        refreshPlayerInfo();
        setupMapSizes();
    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {
        // Do nothing
    }

    
    private ActionListener lobbyListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent ev) {

            // Are we ignoring events?
            if (isIgnoringEvents()) {
                return;
            }
            
            if (ev.getSource().equals(butAdd)) {
                addUnit();
                
            } else if (ev.getSource().equals(butArmy)) {
                createArmy();
                
            } else if (ev.getSource().equals(butSkills)) {
                loadRandomSkills();
                
            } else if (ev.getSource().equals(butNames)) {
                loadRandomNames();
                
            } else if (ev.getSource().equals(mekTable)) {
                customizeMech();
                
            } else if (ev.getSource().equals(tablePlayers)) {
                configPlayer();
                
            } else if (ev.getSource().equals(comboTeam)) {
                changeTeam(comboTeam.getSelectedIndex());
                
            } else if (ev.getSource().equals(butConfigPlayer)) {
                configPlayer();
                
            } else if (ev.getSource().equals(butBotSettings)) {
                IPlayer player = playerModel.getPlayerAt(tablePlayers.getSelectedRow());
                BotClient bot = (BotClient) clientgui.getBots().get(player.getName());
                BotConfigDialog bcd = new BotConfigDialog(clientgui.frame, bot);
                bcd.setVisible(true);

                if (bcd.dialogAborted) {
                    return; // user didn't click 'ok', add no bot
                } else if (bot instanceof Princess) {
                    ((Princess) bot).setBehaviorSettings(bcd.getBehaviorSettings());
                    
                    // bookkeeping:
                    clientgui.getBots().remove(player.getName());
                    bot.setName(bcd.getBotName());
                    clientgui.getBots().put(bot.getName(), bot);
                    player.setName(bcd.getBotName());
                    clientgui.chatlounge.refreshPlayerInfo();
                }
                
            } else if (ev.getSource().equals(butOptions)) {
                // Make sure the game options dialog is editable.
                if (!clientgui.getGameOptionsDialog().isEditable()) {
                    clientgui.getGameOptionsDialog().setEditable(true);
                }
                // Display the game options dialog.
                clientgui.getGameOptionsDialog().update(clientgui.getClient().getGame().getOptions());
                clientgui.getGameOptionsDialog().setVisible(true);
                
            } else if (ev.getSource().equals(butCompact)) {
                toggleCompact();
                
            } else if (ev.getSource().equals(butLoadList)) {
                // Allow the player to replace their current
                // list of entities with a list from a file.
                Client c = getSelectedPlayer();
                if (c == null) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                            Messages.getString("ChatLounge.SelectBotOrPlayer"));  //$NON-NLS-2$
                    return;
                }
                clientgui.loadListFile(c.getLocalPlayer());
                
            } else if (ev.getSource().equals(butSaveList)) {
                // Allow the player to save their current
                // list of entities to a file.
                Client c = getSelectedPlayer();
                if (c == null) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                            Messages.getString("ChatLounge.SelectBotOrPlayer"));
                    return;
                }
                clientgui.saveListFile(c.getGame().getPlayerEntities(c.getLocalPlayer(), false),
                        c.getLocalPlayer().getName());
                
            } else if (ev.getSource().equals(butAddBot)) {
                BotConfigDialog bcd = new BotConfigDialog(clientgui.frame);
                bcd.setVisible(true);
                if (bcd.dialogAborted) {
                    return; // user didn't click 'ok', add no bot
                }
                if (clientgui.getBots().containsKey(bcd.getBotName())) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.AlertExistsBot.title"),
                            Messages.getString("ChatLounge.AlertExistsBot.message"));  //$NON-NLS-2$
                } else {
                    BotClient c = bcd.getSelectedBot(clientgui.getClient().getHost(), clientgui.getClient().getPort());
                    c.setClientGUI(clientgui);
                    c.getGame().addGameListener(new BotGUI(c));
                    try {
                        c.connect();
                    } catch (Exception e) {
                        clientgui.doAlertDialog(Messages.getString("ChatLounge.AlertBot.title"),
                                Messages.getString("ChatLounge.AlertBot.message"));  //$NON-NLS-2$
                    }
                    clientgui.getBots().put(bcd.getBotName(), c);
                }
                
            } else if (ev.getSource().equals(butRemoveBot)) {
                Client c = getSelectedPlayer();
                if ((c == null) || c.equals(clientgui.getClient())) {
                    clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"),
                            Messages.getString("ChatLounge.SelectBo"));
                    return;
                }
                // Delete units first, which safely disembarks and offloads them
                deleteEntities(clientgui.getClient().getGame().getPlayerEntities(c.getLocalPlayer(), false));
                c.die();
                clientgui.getBots().remove(c.getName());
                
            } else if (ev.getSource().equals(butShowUnitID)) {
                PreferenceManager.getClientPreferences().setShowUnitId(butShowUnitID.isSelected());
                mekModel.refreshCells();
                repaint();
                
            } else if (ev.getSource() == butConditions) {
                PlanetaryConditionsDialog pcd = new PlanetaryConditionsDialog(clientgui);
                boolean userOkay = pcd.showDialog();
                if (userOkay) {
                    clientgui.getClient().sendPlanetaryConditions(pcd.getConditions());
                }
                
            } else if (ev.getSource() == butRandomMap) {
                RandomMapDialog rmd = new RandomMapDialog(clientgui.frame, ChatLounge.this, clientgui.getClient(), mapSettings);
                rmd.activateDialog(clientgui.getBoardView().getTilesetManager().getThemes());
                
            } else if (ev.getSource().equals(butChange)) {
                if (lisBoardsAvailable.getSelectedIndex() != -1) {
                    changeMap(lisBoardsAvailable.getSelectedValue());
                    lisBoardsSelected.setSelectedIndex(lisBoardsSelected.getSelectedIndex() + 1);
                }
                
            } else if (ev.getSource().equals(buttonBoardPreview)) {
                previewGameBoard();
                
            } else if (ev.getSource().equals(butMapSize) || ev.getSource().equals(butSpaceSize)) {
                MapDimensionsDialog mdd = new MapDimensionsDialog(clientgui, mapSettings);
                mdd.setVisible(true);
                
            } else if (ev.getSource().equals(comboMapSizes)) {
                if ((comboMapSizes.getSelectedItem() != null)
                        && !comboMapSizes.getSelectedItem().equals(Messages.getString("ChatLounge.CustomMapSize"))) {
                    BoardDimensions size = (BoardDimensions) comboMapSizes.getSelectedItem();
                    mapSettings.setBoardSize(size.width(), size.height());
                    resetAvailBoardSelection = true;
                    resetSelectedBoards = true;
                    clientgui.getClient().sendMapSettings(mapSettings);
                }
                
            } else if (ev.getSource().equals(chkRotateBoard) && (lisBoardsAvailable.getSelectedIndex() != -1)) {
                previewMapsheet();
                
            } else if (ev.getSource().equals(comboMapType)) {
                mapSettings.setMedium(comboMapType.getSelectedIndex());
                clientgui.getClient().sendMapSettings(mapSettings);
                
            } else if (ev.getSource().equals(chkIncludeGround)) {
                if (chkIncludeGround.isSelected()) {
                    mapSettings.setMedium(comboMapType.getSelectedIndex());
                } else {
                    mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                    // set default size for space maps
                    mapSettings.setBoardSize(50, 50);
                    mapSettings.setMapSize(1, 1);
                }
                clientgui.getClient().sendMapDimensions(mapSettings);
            } else if (ev.getSource().equals(chkIncludeSpace)) {
                if (chkIncludeSpace.isSelected()) {
                    mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
                    // set default size for space maps
                    mapSettings.setBoardSize(50, 50);
                    mapSettings.setMapSize(1, 1);
                } else {
                    mapSettings.setMedium(comboMapType.getSelectedIndex());
                }
                clientgui.getClient().sendMapDimensions(mapSettings);
            }
        }
    };

    @Override
    public void mouseClicked(MouseEvent arg0) {
        if ((arg0.getClickCount() == 1) && arg0.getSource().equals(lisBoardsAvailable)) {
            previewMapsheet();
        }
        if ((arg0.getClickCount() == 2) && arg0.getSource().equals(lisBoardsAvailable)) {
            if (lisBoardsAvailable.getSelectedIndex() != -1) {
                changeMap(lisBoardsAvailable.getSelectedValue());
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
        // ignore
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        // ignore
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
        // ignore
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
        // ignore
    }

    /**
     * Updates to show the map settings that have, presumably, just been sent by
     * the server.
     */
    @Override
    public void updateMapSettings(MapSettings newSettings) {
        mapSettings = MapSettings.getInstance(newSettings);
        refreshMapButtons();
        refreshMapChoice();
        refreshSpaceGround();
        refreshBoardsSelected();
        refreshBoardsAvailable();
        refreshMapSummaryLabel();
        refreshGameYearLabel();
        refreshTechLevelLabel();
    }

    public void refreshMapSummaryLabel() {
        String txt = Messages.getString("ChatLounge.MapSummary"); 
        txt += " " + (mapSettings.getBoardWidth() * mapSettings.getMapWidth()) + " x " 
                + (mapSettings.getBoardHeight() * mapSettings.getMapHeight());
        if (chkIncludeGround.isSelected()) {
            txt += " " + (String) comboMapType.getSelectedItem();
        } else {
            txt += " " + "Space Map"; 
        }
        lblMapSummary.setText(txt);

        StringBuilder selectedMaps = new StringBuilder();
        selectedMaps.append("<html>"); 
        selectedMaps.append(Messages.getString("ChatLounge.MapSummarySelectedMaps"));
        selectedMaps.append("<br>"); 
        ListModel<String> model = lisBoardsSelected.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            String map = model.getElementAt(i);
            selectedMaps.append(map);
            if ((i + 1) < model.getSize()) {
                selectedMaps.append("<br>"); 
            }
        }
        lblMapSummary.setToolTipText(scaleStringForGUI(selectedMaps.toString()));
    }

    public void refreshGameYearLabel() {
        String txt = Messages.getString("ChatLounge.GameYear"); 
        txt += " " + clientgui.getClient().getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        lblGameYear.setText(txt);
    }

    public void refreshTechLevelLabel() {
        String tlString;
        IOption tlOpt = clientgui.getClient().getGame().getOptions().getOption("techlevel");
        if (tlOpt != null) {
            tlString = tlOpt.stringValue();
        } else {
            tlString = TechConstants.getLevelDisplayableName(TechConstants.T_TECH_UNKNOWN);
        }
        String txt = Messages.getString("ChatLounge.TechLevel"); 
        txt = txt + " " + tlString; 
        lblTechLevel.setText(txt);
    }

    @Override
    public void ready() {
        final Client client = clientgui.getClient();
        final IGame game = client.getGame();
        final GameOptions gOpts = game.getOptions();
        
        // enforce exclusive deployment zones in double blind
        for (IPlayer player: client.getGame().getPlayersVector()) {
            if (!isValidStartPos(game, player)) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.OverlapDeploy.title"), 
                        Messages.getString("ChatLounge.OverlapDeploy.msg"));
                return;
            }
        }

        // Make sure player has a commander if Commander killed victory is on
        if (gOpts.booleanOption(OptionsConstants.VICTORY_COMMANDER_KILLED)) {
            List<String> players = new ArrayList<>();
            if ((game.getLiveCommandersOwnedBy(getLocalPlayer()) < 1)
                    && (game.getEntitiesOwnedBy(getLocalPlayer()) > 0)) {
                players.add(client.getLocalPlayer().getName());
            }
            for (Client bc : clientgui.getBots().values()) {
                if ((game.getLiveCommandersOwnedBy(bc.getLocalPlayer()) < 1)
                        && (game.getEntitiesOwnedBy(bc.getLocalPlayer()) > 0)) {
                    players.add(bc.getLocalPlayer().getName());
                }
            }
            if (players.size() > 0) {
                String title = Messages.getString("ChatLounge.noCmdr.title"); 
                String msg = Messages.getString("ChatLounge.noCmdr.msg"); 
                for (String player : players) {
                    msg += player + "\n";
                }
                clientgui.doAlertDialog(title, msg);
                return;
            }

        }

        boolean done = !getLocalPlayer().isDone();
        client.sendDone(done);
        refreshDoneButton(done);
        for (Client botClient : clientgui.getBots().values()) {
            botClient.sendDone(done);
        }
    }

    Client getSelectedPlayer() {
        if ((tablePlayers == null) || (tablePlayers.getSelectedRow() == -1)) {
            return clientgui.getClient();
        }
        String name = playerModel.getPlayerAt(tablePlayers.getSelectedRow()).getName();
        BotClient c = (BotClient) clientgui.getBots().get(name);
        if ((c == null) && clientgui.getClient().getName().equals(name)) {
            return clientgui.getClient();
        }
        return c;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        GUIPreferences.getInstance().removePreferenceChangeListener(this);
        PreferenceManager.getClientPreferences().removePreferenceChangeListener(this);
        MechSummaryCache.getInstance().removeListener(mechSummaryCacheListener);
    }

    /**
     * Returns true if the given list of entities can be configured as a group.
     * This requires that they all have the same owner, and that none of the
     * units are being transported. Also, the owner must be 
     */
    boolean canConfigureMultipleDeployment(Collection<Entity> entities) {
        return haveSingleOwner(entities) && !containsTransportedUnit(entities);
    }
    
    private boolean containsTransportedUnit(Collection<Entity> entities) {
        return entities.stream().anyMatch(e -> e.getTransportId() != Entity.NONE);
    }
    
    /**
     * Returns true if the given collection contains at least one entity
     * that the local player can edit, i.e. is his own or belongs to
     * one of his bots. Does not check if the units are otherwise configured,
     * e.g. transported.
     * <P>See also {@link #isEditable(Entity)}
     */
    boolean canEditAny(Collection<Entity> entities) {
        return entities.stream().anyMatch(e -> isEditable(e));
    }
    
    /**
     * Returns true if the local player can see all of the given entities.
     * This is true except when a blind drop option is active and one or more
     * of the entities are not his own.
     */
    boolean canSeeAll(Collection<Entity> entities) {
        if (!clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP)
                && !clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP)) {
            return true;
        }
        for (Entity entity: entities) {
            if (!entityInLocalTeam(entity)) {
                return false;
            }
        }
        return true;
    }
    
    boolean entityInLocalTeam(Entity entity) {
        return !getLocalPlayer().isEnemyOf(entity.getOwner());
    }
    

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        
        if (event.getSource().equals(tablePlayers.getSelectionModel()) 
                || event.getSource().equals(butRemoveBot)) { // ??

            Client selClient = getSelectedPlayer();
            comboTeam.setEnabled(selClient != null);
            butLoadList.setEnabled(selClient != null);
            butCamo.setEnabled(selClient != null);
            butConfigPlayer.setEnabled(selClient != null);
            refreshCamos();
            // Disable the Remove Bot button for the "player" of a "Connect As Bot" client
            butRemoveBot.setEnabled(selClient instanceof BotClient
                    && !selClient.getLocalPlayer().equals(getLocalPlayer()));
            butBotSettings.setEnabled(selClient instanceof BotClient);
            if (selClient != null) {
                IPlayer selPlayer = selClient.getLocalPlayer();
                boolean hasUnits = !selClient.getGame().getPlayerEntities(selPlayer, false).isEmpty();
                butSaveList.setEnabled(hasUnits && unitsVisible(selPlayer));
                setTeamSelectedItem(selPlayer.getTeam());
            }
        } else if (event.getSource().equals(lisBoardsAvailable)) {
            previewMapsheet();
        }
    }
    
    private void setTeamSelectedItem(int team) {
        comboTeam.removeActionListener(lobbyListener);
        comboTeam.setSelectedIndex(team);
        comboTeam.addActionListener(lobbyListener);
    }
    
    /** 
     * Returns false when any blind-drop option is active and player is not the local player; 
     * true otherwise. When true, individual units of the given player should not be shown/saved/etc. 
     */ 
    private boolean unitsVisible(IPlayer player) {
        GameOptions opts = clientgui.getClient().getGame().getOptions();
        boolean isBlindDrop = opts.booleanOption(OptionsConstants.BASE_BLIND_DROP)
                || opts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
        return player.equals(getLocalPlayer()) || !isBlindDrop;
    }

    /**
     * A table model for displaying players
     */
    public class PlayerTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -1372393680232901923L;

        private static final int COL_PLAYER = 0;
        private static final int COL_FORCE = 1;
        private static final int N_COL = 2;

        private ArrayList<IPlayer> players;
        private ArrayList<Integer> bvs;
        private ArrayList<Long> costs;
        private ArrayList<Double> tons;

        public PlayerTableModel() {
            players = new ArrayList<>();
            bvs = new ArrayList<>();
            costs = new ArrayList<>();
            tons = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            return players.size();
        }

        public void clearData() {
            players = new ArrayList<>();
            bvs = new ArrayList<>();
            costs = new ArrayList<>();
            tons = new ArrayList<>();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public void addPlayer(IPlayer player) {
            players.add(player);
            long cost = 0;
            double ton = 0;
            for (Entity entity : clientgui.getClient().getEntitiesVector()) {
                if (entity.getOwner().equals(player) && !entity.isPartOfFighterSquadron()) {
                    cost += (long)entity.getCost(false);
                    ton += entity.getWeight();
                }
            }
            bvs.add(player.getBV());
            costs.add(cost);
            tons.add(ton);
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            String result = "<HTML>" + UIUtil.guiScaledFontHTML();
            switch (column) {
                case (COL_PLAYER):
                    return result + Messages.getString("ChatLounge.colPlayer");
                default:
                    return result + "Force";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int col) {
            StringBuilder result = new StringBuilder("<HTML><NOBR>" + UIUtil.guiScaledFontHTML());
            IPlayer player = getPlayerAt(row);
            boolean realBlindDrop = !player.equals(getLocalPlayer()) 
                    && clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);

            if (col == COL_FORCE) {
                if (realBlindDrop) {
                    result.append("&nbsp;<BR><I>Unknown</I><BR>&nbsp;");
                } else {
                    double ton = tons.get(row);
                    if (ton < 10) {
                        result.append(String.format("%.2f", ton) + " Tons");
                    } else {
                        result.append(String.format("%,d", Math.round(ton)) + " Tons");
                    }
                    if (costs.get(row) < 10000000) {
                        result.append("<BR>" + String.format("%,d", costs.get(row)) + " C-Bills");
                    } else {
                        result.append("<BR>" + String.format("%,d", costs.get(row) / 1000000) + "\u00B7M C-Bills");
                    }
                    result.append("<BR>" + String.format("%,d", bvs.get(row)) + " BV");
                }
            } else {
                result.append(guiScaledFontHTML(0.1f));
                result.append(player.getName() + "</FONT>");
                boolean isEnemy = getLocalPlayer().isEnemyOf(player);
                result.append(guiScaledFontHTML(isEnemy ? Color.RED : uiGreen()));
                result.append("<BR>" + IPlayer.teamNames[player.getTeam()] + "</FONT>");
                result.append(guiScaledFontHTML());
                result.append("<BR>Start: " + IStartingPositions.START_LOCATION_NAMES[player.getStartingPos()]);
                if (!isValidStartPos(clientgui.getClient().getGame(), player)) {
                    result.append(guiScaledFontHTML(uiYellow())); 
                    result.append(WARNING_SIGN + "</FONT>");
                }
            }
            return result.toString();
        }

        public IPlayer getPlayerAt(int row) {
            return players.get(row);
        }
    }
    
    public class PlayerTableMouseAdapter extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = tablePlayers.rowAtPoint(e.getPoint());
                IPlayer player = playerModel.getPlayerAt(row);
                if (player != null) {
                    boolean isLocalPlayer = player.equals(getLocalPlayer());
                    boolean isLocalBot = clientgui.getBots().get(player.getName()) != null;
                    if ((isLocalPlayer || isLocalBot)) {
                        configPlayer();
                    }
                }
            }
        }
    }

    public class MekTableKeyAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if (mekTable.getSelectedRowCount() == 0) {
                return;
            }
            List<Entity> entities = getSelectedEntities();
            int code = e.getKeyCode();
            if ((code == KeyEvent.VK_DELETE) || (code == KeyEvent.VK_BACK_SPACE)) {
                e.consume();
                deleteAction(entities);
            } else if (code == KeyEvent.VK_SPACE) {
                e.consume();
                mechReadoutAction(entities);
            } else if (code == KeyEvent.VK_ENTER) {
                e.consume();
                if (entities.size() == 1) {
                    customizeMech(entities.get(0));
                } else if (canConfigureMultipleDeployment(entities)) {
                    customizeMechs(entities);
                }
            }
        }
    }

    public class MekTableMouseAdapter extends MouseInputAdapter implements ActionListener {
        
        @Override
        public void actionPerformed(ActionEvent action) {
            Entity entity = mekModel.getEntityAt(mekTable.getSelectedRow());
            List<Entity> entities = getSelectedEntities();
            boolean oneSelected = entities.size() == 1;
            if ((null == entity) || (entities.size() == 0)) {
                return;
            }
            
            StringTokenizer st = new StringTokenizer(action.getActionCommand(), "|");
            String command = st.nextToken();
            Set<Entity> updateCandidates = new HashSet<>();
            int id;

            switch (command) {
            case "VIEW":
                mechReadoutAction(entities);
                break;

            case "BV":
                if (oneSelected) {
                    mechBVDisplay(entity);
                }
                break;

            case "DAMAGE":
                if (oneSelected) {
                    configureDamage(entity);
                }
                break;

            case "INDI_CAMO":
                mechCamo(entities);
                break;

            case "CONFIGURE":
                if (oneSelected) {
                    customizeMech(entity);
                }
                break;

            case "CONFIGURE_ALL":
                customizeMechs(entities);
                break;
            
            case "DELETE":
                deleteAction(entities);
                break;

            case "SKILLS":
                setRandomSkills(entities);
                break;
                
            case NAME_COMMAND:
                setRandomNames(entities);
                break;
                
            case CALLSIGN_COMMAND:
                setRandomCallsigns(entities);
                break;
                
            case "LOAD":
                load(entities, st);
                break;
                
            case "UNLOAD":
                disembarkAll(entities);
                for (Entity e: entities) {
                    disembark(e, updateCandidates);
                }
                sendUpdate(updateCandidates);
                break;
                
            case "UNLOADALL":
                offloadAll(entities, updateCandidates);
                sendUpdate(updateCandidates);
                break;
                
            case "UNLOADALLFROMBAY":
                id = Integer.parseInt(st.nextToken());
                Bay bay = entity.getBayById(id);
                for (Entity loadee : bay.getLoadedUnits()) {
                    disembark(loadee, updateCandidates);
                }
                sendUpdate(updateCandidates);
                break;
                
            case "SQUADRON":
                createSquadron(entities);
                break;
                
            case "SWAP":
                if (oneSelected) {
                    id = Integer.parseInt(st.nextToken());
                    swapPilots(entity, id);
                }
                break;
                
            case "CHANGE_OWNER":
                id = Integer.parseInt(st.nextToken());
                changeOwner(entities, id);
                break;
                
            case "SAVE_QUIRKS_ALL":
                for (Entity e : entities) {
                    QuirksHandler.addCustomQuirk(e, false);
                }
                break;
                
            case "SAVE_QUIRKS_MODEL":
                for (Entity e : entities) {
                    QuirksHandler.addCustomQuirk(e, true);
                }
                break;
                
            case "RAPIDFIREMG_ON":
            case "RAPIDFIREMG_OFF":
                toggleBurstMg(entities, command.equals("RAPIDFIREMG_ON"));
                break;

            case "HOTLOAD_ON":
            case "HOTLOAD_OFF":
                toggleHotLoad(entities, command.equals("HOTLOAD_ON"));
                break;
            } 
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = mekTable.rowAtPoint(e.getPoint());
                Entity entity = mekModel.getEntityAt(row);
                if (entity != null && isEditable(entity)) {
                    customizeMech(entity);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                // If the right mouse button is pressed over an unselected entity,
                // clear the selection and select that entity instead
                int row = mekTable.rowAtPoint(e.getPoint());
                if (!mekTable.isRowSelected(row)) {
                    mekTable.changeSelection(row, row, false, false);
                }
                showPopup(e);
            }
        }

        /** Shows the right-click menu on the mek table */
        private void showPopup(MouseEvent e) {
            if (mekTable.getSelectedRowCount() == 0) {
                return;
            }
            List<Entity> entities = getSelectedEntities();
            int row = mekTable.getSelectedRow();
            Entity entity = mekModel.getEntityAt(row);
            ScalingPopup popup = MekTablePopup.mekTablePopup(clientgui, entities, entity, this, ChatLounge.this);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /** OK
     * Returns a Collection that contains only those of the given entities
     * that the local player can affect, i.e. his units or those of his bots. 
     * The returned Collection is a new Collection and can be safely altered.
     * (The entities are not copies of course.)
     * <P>See also {@link #isEditable(Entity)} 
     */
    private Set<Entity> editableEntities(Collection<Entity> entities) {
        return entities.stream().filter(e -> isEditable(e)).collect(Collectors.toSet());
    }
    
    /** 
     * Returns true if the given carrier and the entities can be edited to transport 
     * all the given entities. That is the case when carrier and entities are 
     * all teammates and either the carrier or all the entities can be edited 
     * by the local player. 
     * Note: this method does NOT check if the loadings are rules-valid.
     * <P>See also {@link #isEditable(Entity)}
     * 
     */
    private boolean isLoadable(Collection<Entity> entities, Entity carrier) {
        return entities.stream().allMatch(e -> isLoadable(e, carrier));
    }
    
    /** 
     * Returns true if the given carrier and carried can be edited to have the 
     * carrier transport the given carried entity. That is the case when they 
     * are teammates and one of the entities can be edited by the local player. 
     * Note: this method does NOT check if the loading is rules-valid.
     * <P>See also {@link #isEditable(Entity)}
     */
    private boolean isLoadable(Entity carried, Entity carrier) {
        return !carrier.getOwner().isEnemyOf(carried.getOwner()) 
                && (isEditable(carrier) || isEditable(carried));
    }
    
    /** 
     * Returns a Collection that contains only those of the given entities that can 
     * be edited to be transported by the carrier. For each of the given entities,
     * that is the case when it and the carrier are teammates and one of them 
     * can be edited by the local player. 
     * Note: this method does NOT check if the loading is rules-valid.
     * The returned Collection is a new Collection and can be safely altered.
     * (The entities are not copies of course.)
     * <P>See also {@link #isLoadable(Entity)} 
     */
    private Set<Entity> loadableEntities(Collection<Entity> entities, Entity carrier) {
        return entities.stream().filter(e -> isLoadable(e, carrier)).collect(Collectors.toSet());
    }
    

    /** OK
     * Toggles hot loading LRMs for the given entities to the state given as hotLoadOn
     */
    private void toggleHotLoad(Collection<Entity> entities, boolean hotLoadOn) {
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: editableEntities(entities)) {
            for (Mounted m: entity.getAmmo()) { 
                // setHotLoad checks the Ammo to see if it can be hotloaded
                m.setHotLoad(hotLoadOn);
                // TODO: The following should be part of setHotLoad in Mounted
                if (hotLoadOn) {
                    m.setMode("HotLoad");
                } else if (((EquipmentType)m.getType()).hasModeType("HotLoad")) {
                    m.setMode("");
                }
                updateCandidates.add(entity);
            };
        }
        sendUpdate(updateCandidates);
    }
    
    /** 
     * Toggles burst MG fire for the given entities to the state given as burstOn
     */
    private void toggleBurstMg(Collection<Entity> entities, boolean burstOn) {
        Set<Entity> updateCandidates = new HashSet<>();
        for (Entity entity: editableEntities(entities)) {
            for (Mounted m: entity.getWeaponList()) {
                if (((WeaponType) m.getType()).hasFlag(WeaponType.F_MG)) {
                    m.setRapidfire(burstOn);
                    updateCandidates.add(entity);
                }
            }
        }
        sendUpdate(updateCandidates);
    }
    
    private void mechReadoutAction(List<Entity> entities) {
        if ((entities.size() == 1) && canSeeAll(entities)) {
            mechReadout(entities.get(0));
        }
    }
    
    public void load(List<Entity> entities, StringTokenizer st) {
        StringTokenizer stLoad = new StringTokenizer(st.nextToken(), ":");
        int id = Integer.parseInt(stLoad.nextToken());
        int bayNumber = Integer.parseInt(stLoad.nextToken());
        Entity loadingEntity = clientgui.getClient().getEntity(id);
        boolean loadRear = false;
        if (stLoad.hasMoreTokens()) {
            loadRear = Boolean.parseBoolean(stLoad.nextToken());
        }

        double capacity;
        boolean hasEnoughCargoCapacity;
        String errorMessage = "";
        if (bayNumber != -1) {
            Bay bay = loadingEntity.getBayById(bayNumber);
            if (null != bay) {
                double loadSize = entities.stream().mapToDouble(bay::spaceForUnit).sum();
                capacity = bay.getUnused();
                hasEnoughCargoCapacity = loadSize <= capacity;
                errorMessage = Messages.getString("LoadingBay.baytoomany",
                        (int) bay.getUnusedSlots(), bay.getDefaultSlotDescription());
            } else if (loadingEntity.hasETypeFlag(Entity.ETYPE_MECH)
                    && entities.get(0).hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
                // We're also using bay number to distinguish between front and rear locations
                // for protomech mag clamp systems
                hasEnoughCargoCapacity = entities.size() == 1;
                errorMessage = Messages.getString("LoadingBay.protostoomany");
            } else {
                hasEnoughCargoCapacity = false;
                errorMessage = Messages.getString("LoadingBay.bayNumberNotFound", bayNumber);
            }
        } else {
            HashMap<Long, Double> capacities = new HashMap<>();
            HashMap<Long, Double> counts = new HashMap<>();
            HashMap<Transporter, Double> potentialLoad = new HashMap<>();
            // Get the counts and capacities for all present types
            for (Entity e : entities) {
                long entityType = e.getEntityType();
                long loaderType = loadingEntity.getEntityType();
                double unitSize;
                if ((entityType & Entity.ETYPE_MECH) != 0) {
                    entityType = Entity.ETYPE_MECH;
                    unitSize = 1;
                } else if ((entityType & Entity.ETYPE_INFANTRY) != 0) {
                    entityType = Entity.ETYPE_INFANTRY;
                    boolean useCount = true;
                    if ((loaderType & Entity.ETYPE_TANK) != 0) {
                        // This is a super hack... When getting
                        // capacities, troopspace gives unused space in
                        // terms of tons, and BattleArmorHandles gives
                        // it in terms of unit count. If I call
                        // getUnused, it sums these together, and is
                        // meaningless, so we'll go through all
                        // transporters....
                        boolean hasTroopSpace = false;
                        for (Transporter t : loadingEntity.getTransports()) {
                            if (t instanceof TankTrailerHitch) {
                                continue;
                            }
                            double loadWeight = e.getWeight();
                            if (potentialLoad.containsKey(t)) {
                                loadWeight += potentialLoad.get(t);
                            }
                            if (!(t instanceof BattleArmorHandlesTank) && t.canLoad(e)
                                    && (loadWeight <= t.getUnused())) {
                                hasTroopSpace = true;
                                potentialLoad.put(t, loadWeight);
                                break;
                            }
                        }
                        if (hasTroopSpace) {
                            useCount = false;
                        }
                    }
                    // TroopSpace uses tonnage
                    // bays and BA handlebars use a count
                    if (useCount) {
                        unitSize = 1;
                    } else {
                        unitSize = e.getWeight();
                    }
                } else if ((entityType & Entity.ETYPE_PROTOMECH) != 0) {
                    entityType = Entity.ETYPE_PROTOMECH;
                    unitSize = 1;
                    // Loading using mag clamps; user can specify front or rear.
                    // Make use of bayNumber field
                    if ((loaderType & Entity.ETYPE_MECH) != 0) {
                        bayNumber = loadRear? 1 : 0;
                    }
                } else if ((entityType & Entity.ETYPE_DROPSHIP) != 0) {
                    entityType = Entity.ETYPE_DROPSHIP;
                    unitSize = 1;
                } else if ((entityType & Entity.ETYPE_JUMPSHIP) != 0) {
                    entityType = Entity.ETYPE_JUMPSHIP;
                    unitSize = 1;
                } else if ((entityType & Entity.ETYPE_AERO) != 0) {
                    entityType = Entity.ETYPE_AERO;
                    unitSize = 1;
                } else if ((entityType & Entity.ETYPE_TANK) != 0) {
                    entityType = Entity.ETYPE_TANK;
                    unitSize = 1;
                } else {
                    unitSize = 1;
                }

                Double count = counts.get(entityType);
                if (count == null) {
                    count = 0.0;
                }
                count = count + unitSize;
                counts.put(entityType, count);

                Double cap = capacities.get(entityType);
                if (cap == null) {
                    cap = loadingEntity.getUnused(e);
                    capacities.put(entityType, cap);
                }
            }
            hasEnoughCargoCapacity = true;
            capacity = 0;
            for (Long typeId : counts.keySet()) {
                double currCount = counts.get(typeId);
                double currCapacity = capacities.get(typeId);
                if (currCount > currCapacity) {
                    hasEnoughCargoCapacity = false;
                    capacity = currCapacity;
                    String messageName;
                    if (typeId == Entity.ETYPE_INFANTRY) {
                        messageName = "LoadingBay.nonbaytoomanyInf";
                    } else {
                        messageName = "LoadingBay.nonbaytoomany";
                    }
                    errorMessage = Messages.getString(messageName, currCount,
                            Entity.getEntityTypeName(typeId), currCapacity);
                }
            }
        }
        if (hasEnoughCargoCapacity) {
            for (Entity e : entities) {
                loadOnto(e, id, bayNumber);
            }
        } else {
            JOptionPane.showMessageDialog(clientgui.frame, errorMessage, 
                    Messages.getString("LoadingBay.error"), JOptionPane.ERROR_MESSAGE);
        }
        
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            // Change to the GUI scale: adapt the UI accordingly
            adaptToGUIScale();
        } else if (e.getName().equals(IClientPreferences.SHOW_UNIT_ID)) {
            // Show/Hide unit IDs from the client settings: adapt the button and mek table
            setButUnitIDState(PreferenceManager.getClientPreferences().getShowUnitId());
            mekModel.refreshCells();
        }
    }
    
    private void setButUnitIDState(boolean state) {
        butShowUnitID.removeActionListener(lobbyListener);
        butShowUnitID.setSelected(PreferenceManager.getClientPreferences().getShowUnitId());
        butShowUnitID.addActionListener(lobbyListener);
    }
    
    /** Sets the row height of the MekTable according to compact mode and GUI scale */
    private void setTableRowHeights() {
        int rowbaseHeight = butCompact.isSelected() ? MEKTABLE_ROWHEIGHT_COMPACT : MEKTABLE_ROWHEIGHT_FULL;
        mekTable.setRowHeight(UIUtil.scaleForGUI(rowbaseHeight));
        tablePlayers.setRowHeight(UIUtil.scaleForGUI(PLAYERTABLE_ROWHEIGHT));
    }

    /** Refreshes the table headers of the MekTable and PlayerTable. */
    private void updateTableHeaders() {
        // The mek table
        JTableHeader header = mekTable.getTableHeader();
        TableColumnModel colMod = header.getColumnModel();
        for (int i = 0; i < colMod.getColumnCount(); i++) {
            TableColumn tabCol = colMod.getColumn(i);
            String headerText = mekModel.getColumnName(i);
            // Add info about the current sorting
            if (activeSorter.getColumnIndex() == i) {
                headerText += "&nbsp;&nbsp;&nbsp;" + guiScaledFontHTML(uiGray());
                if (activeSorter.getSortingDirection() == MekTableSorter.Sorting.ASCENDING) {
                    headerText += "\u25B4 ";    
                } else {
                    headerText += "\u25BE ";
                }
                headerText += activeSorter.getDisplayName();
            }
            tabCol.setHeaderValue(headerText);
        }
        header.repaint();
        
        // The player table
        header = tablePlayers.getTableHeader();
        colMod = header.getColumnModel();
        for (int i = 0; i < colMod.getColumnCount(); i++) {
            TableColumn tabCol = colMod.getColumn(i);
            tabCol.setHeaderValue(playerModel.getColumnName(i));
        }
        header.repaint();
    }

    /** Returns the owner of the given entity. Should be used over entity.getowner(). */
    private IPlayer ownerOf(Entity entity) {
        return clientgui.getClient().getGame().getPlayer(entity.getOwnerId());
    }
    
    /** Sets the column width of the given table column of the MekTable with the value stored in the GUIP. */
    private void setColumnWidth(TableColumn column) {
        String key;
        switch (column.getModelIndex()) {
        case (MekTableModel.COL_PILOT):
            key = GUIPreferences.LOBBY_MEKTABLE_PILOT_WIDTH;
            break;
        case (MekTableModel.COL_UNIT):
            key = GUIPreferences.LOBBY_MEKTABLE_UNIT_WIDTH;
            break;
        case (MekTableModel.COL_PLAYER):
            key = GUIPreferences.LOBBY_MEKTABLE_PLAYER_WIDTH;
            break;
        default:
            key = GUIPreferences.LOBBY_MEKTABLE_BV_WIDTH;
        }
        column.setPreferredWidth(GUIPreferences.getInstance().getInt(key));
    }
    
    /** Adapts the whole Lobby UI (both panels) to the current guiScale. */
    private void adaptToGUIScale() {
        updateTableHeaders();
        setTableRowHeights();
        refreshMapSummaryLabel();
        refreshGameYearLabel();
        refreshTechLevelLabel();
        refreshCamos();
        refreshMapButtons();
        mekModel.refreshCells();

        Font scaledFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
        Font scaledBigFont = new Font("Dialog", Font.PLAIN, UIUtil.scaleForGUI(UIUtil.FONT_SCALE1 + 3));

        butChange.setFont(scaledFont);
        butCompact.setFont(scaledFont);
        butOptions.setFont(scaledFont);
        butLoadList.setFont(scaledFont);
        butSaveList.setFont(scaledFont);
        butArmy.setFont(scaledFont);
        butSkills.setFont(scaledFont);
        butNames.setFont(scaledFont);
        butAddBot.setFont(scaledFont);
        butRemoveBot.setFont(scaledFont);
        butConfigPlayer.setFont(scaledFont);
        butBotSettings.setFont(scaledFont);
        butShowUnitID.setFont(scaledFont);
        butMapSize.setFont(scaledFont);
        butConditions.setFont(scaledFont);
        butRandomMap.setFont(scaledFont);
        butSpaceSize.setFont(scaledFont);
        buttonBoardPreview.setFont(scaledFont);
        comboMapSizes.setFont(scaledFont);
        comboMapType.setFont(scaledFont);
        comboTeam.setFont(scaledFont);
        chkIncludeGround.setFont(scaledFont);
        chkIncludeGround.setIconTextGap(UIUtil.scaleForGUI(5));
        chkIncludeSpace.setFont(scaledFont);
        chkIncludeSpace.setIconTextGap(UIUtil.scaleForGUI(5));
        chkRotateBoard.setFont(scaledFont);
        chkRotateBoard.setIconTextGap(UIUtil.scaleForGUI(5));
        lblBoardsAvailable.setFont(scaledFont);
        lblBoardsSelected.setFont(scaledFont);
        lisBoardsAvailable.setFont(scaledFont);
        lisBoardsSelected.setFont(scaledFont);
        lblGameYear.setFont(scaledFont);
        lblMapSummary.setFont(scaledFont);
        lblTechLevel.setFont(scaledFont);

        butAdd.setFont(scaledBigFont);
        panTabs.setFont(scaledBigFont);

        ((TitledBorder)panUnitInfo.getBorder()).setTitleFont(scaledFont);
        ((TitledBorder)panPlayerInfo.getBorder()).setTitleFont(scaledFont);
        ((TitledBorder)panGroundMap.getBorder()).setTitleFont(scaledFont);
        ((TitledBorder)panSpaceMap.getBorder()).setTitleFont(scaledFont);

        lblGameYear.setToolTipText(scaleStringForGUI(Messages.getString("ChatLounge.GameYearLabelToolTip"))); 
        lblTechLevel.setToolTipText(scaleStringForGUI(Messages.getString("ChatLounge.TechLevelLabelToolTip"))); 
        buttonBoardPreview.setToolTipText(scaleStringForGUI(Messages.getString("BoardSelectionDialog.ViewGameBoardTooltip")));

        // Makes a new tooltip appear immediately (rescaled and possibly for a different unit)
        ToolTipManager manager = ToolTipManager.sharedInstance();
        long time = System.currentTimeMillis() - manager.getInitialDelay() + 1;
        Point locationOnScreen = MouseInfo.getPointerInfo().getLocation();
        Point locationOnComponent = new Point(locationOnScreen);
        SwingUtilities.convertPointFromScreen(locationOnComponent, mekTable);
        MouseEvent event = new MouseEvent(mekTable, -1, time, 0, 
                locationOnComponent.x, locationOnComponent.y, 0, 0, 1, false, 0);
        manager.mouseMoved(event);
    }
    
    /** 
     * Mouse Listener for the table header of the Mek Table.
     * Saves column widths of the Mek Table when the mouse button is released. 
     * Also switches between table sorting types
     */
    MouseListener mekTableHeaderMouseListener = new MouseAdapter()
    {
        @Override
        public void mouseReleased(MouseEvent e)
        {
            // Save table widths
            for (int i = 0; i < MekTableModel.N_COL; i++) {
                TableColumn column = mekTable.getColumnModel().getColumn(i);
                String key;
                switch (column.getModelIndex()) {
                case (MekTableModel.COL_PILOT):
                    key = GUIPreferences.LOBBY_MEKTABLE_PILOT_WIDTH;
                    break;
                case (MekTableModel.COL_UNIT):
                    key = GUIPreferences.LOBBY_MEKTABLE_UNIT_WIDTH;
                    break;
                case (MekTableModel.COL_PLAYER):
                    key = GUIPreferences.LOBBY_MEKTABLE_PLAYER_WIDTH;
                    break;
                default:
                    key = GUIPreferences.LOBBY_MEKTABLE_BV_WIDTH;
                }
                GUIPreferences.getInstance().setValue(key, column.getWidth());
            }
            
            changeMekTableSorter(e);
        }

    };
    
    /** OK
     * Sets the sorting used in the Mek Table depending on the column header 
     * that was clicked.  
     */ 
    private void changeMekTableSorter(MouseEvent e) {
        int col = mekTable.columnAtPoint(e.getPoint());
        MekTableSorter previousSorter = activeSorter;
        List<MekTableSorter> sorters;
        
        // find the right list of sorters (or do nothing, if the column is not sortable)
        if (col == MekTableModel.COL_UNIT) {
            sorters = unitSorters;
        } else if (col == MekTableModel.COL_BV) {
            sorters = bvSorters;
        } else {
            return;
        }
        
        // Select the next allowed sorter and refresh the display if the sorter was changed
        nextSorter(sorters);
        if (activeSorter != previousSorter) {
            refreshEntities();
            updateTableHeaders();
        }
    }
    
    /** OK Selects the next allowed sorter in the given list of sorters. */
    private void nextSorter(List<MekTableSorter> sorters) {
        // Set the next sorter as active, if this column was already sorted, or
        // the first sorter otherwise
        int index = sorters.indexOf(activeSorter);
        if (index == -1) {
            activeSorter = sorters.get(0);
        } else {
            index = (index + 1) % sorters.size();
            activeSorter = sorters.get(index);
        }
        
        // Find an allowed sorter (e.g. blind drop may prohibit some)
        int counter = 0; // Endless loop safeguard
        while (!activeSorter.isAllowed(clientgui.getClient().getGame().getOptions())
                && ++counter < 100) {
            index = (index + 1) % sorters.size();
            activeSorter = sorters.get(index);
        }
    }
    
    public class PlayerRenderer extends JPanel implements TableCellRenderer {
        
        private JLabel lblImage = new JLabel();

        public PlayerRenderer() {
            setLayout(new GridLayout(1,1,5,0));
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            add(lblImage);
        }
        
        private void setImage(Image img) {
            lblImage.setIcon(new ImageIcon(img));
        }

        private static final long serialVersionUID = 4947299735765324311L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            lblImage.setText(value.toString());

            if (column == PlayerTableModel.COL_PLAYER) {
                lblImage.setIconTextGap(scaleForGUI(5));
                IPlayer player = playerModel.getPlayerAt(row);
                if (player == null) {
                    return null;
                }
                Image camo = player.getCamouflage().getImage();
                int size = scaleForGUI(PLAYERTABLE_ROWHEIGHT) / 2;
                setImage(camo.getScaledInstance(-1, size, Image.SCALE_SMOOTH));
            } else {
                lblImage.setIconTextGap(scaleForGUI(5));
                lblImage.setIcon(null);
            }
            
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                Color background = table.getBackground();
                if (row % 2 != 0) {
                    background = alternateTableBGColor();
                }
                setBackground(background);
            }

            if (hasFocus) {
                if (!isSelected) {
                    Color col = UIManager.getColor("Table.focusCellForeground");
                    if (col != null) {
                        setForeground(col);
                    }
                    col = UIManager.getColor("Table.focusCellBackground");
                    if (col != null) {
                        setBackground(col);
                    }
                }
            }

            return this;
        }
    }

    /** OK Returns true when the compact view is active. */ 
    public boolean isCompact() {
        return butCompact.isSelected();
    }
    
    /** OK 
     * Returns a list of the selected entities in the Mek table. 
     * The list may be empty but not null. 
     */
    private ArrayList<Entity> getSelectedEntities() {
        ArrayList<Entity> result = new ArrayList<>();
        int[] rows = mekTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            result.add(mekModel.getEntityAt(rows[i]));
        }
        return result;
    }
    
    /** OK Helper method to shorten calls. */
    private IPlayer getLocalPlayer() {
        return clientgui.getClient().getLocalPlayer();
    }
    
    /** A renderer for the list of available boards that hides the directories. */
    public static class BoardNameRenderer extends DefaultListCellRenderer  {
        private static final long serialVersionUID = -3218595828938299222L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {

            File boardFile = new File((String)value);
            return super.getListCellRendererComponent(list, boardFile.getName(), index, isSelected, cellHasFocus);
        }
    }
    
}
