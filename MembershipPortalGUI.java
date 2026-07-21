import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * Membership Management Portal
 * ------------------------------------------------------------------
 * A different front-end for the same GymMember / RegularMember /
 * PremiumMember model classes used by GymGUI.java, built around a
 * sidebar-navigation + dashboard + master-detail wireframe instead
 * of the original's button-driven, single-form-per-screen layout.
 *
 * Wireframe differences from the original:
 *   - One persistent window (CardLayout) instead of swapping JFrames
 *   - Left sidebar navigation instead of "Home" screen buttons
 *   - A Dashboard tab with live summary stat cards
 *   - Master-detail split: add-member form + live table on the left,
 *     selected member's details + actions on the right
 *   - Actions act on the row selected in the table instead of a
 *     manually typed-in ID field
 *   - Inline status messages instead of a pop-up dialog per action
 */
public class MembershipPortalGUI extends JFrame {

    // ---------- Shared data ----------
    private final ArrayList<GymMember> members = new ArrayList<>();

    // ---------- Theme ----------
    private static final Color SIDEBAR_BG    = new Color(30, 41, 59);
    private static final Color SIDEBAR_HOVER = new Color(51, 65, 85);
    private static final Color CONTENT_BG    = new Color(241, 245, 249);
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color BORDER_GRAY   = new Color(226, 232, 240);
    private static final Color TEXT_MUTED    = new Color(100, 116, 139);
    private static final Color ACCENT_TEAL   = new Color(13, 148, 136);
    private static final Color ACCENT_BLUE   = new Color(37, 99, 235);
    private static final Color ACCENT_PURPLE = new Color(147, 51, 234);
    private static final Color ACCENT_GREEN  = new Color(22, 163, 74);
    private static final Color ACCENT_AMBER  = new Color(202, 138, 4);
    private static final Color DANGER_RED    = new Color(220, 38, 38);

    private static final Font FONT_HEADER  = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_NAV     = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_FIELD   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_STAT    = new Font("Segoe UI", Font.BOLD, 30);
    private static final Font FONT_MONO    = new Font("Monospaced", Font.PLAIN, 13);

    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JLabel headerCountBadge;

    // Dashboard
    private JLabel totalValueLabel, regularValueLabel, premiumValueLabel, activeValueLabel;

    // Regular tab
    private JTextField regIdField, regNameField, regLocationField, regPhoneField,
            regEmailField, regDobField, regMsdField, regReferralField, regSearchField;
    private JComboBox<String> regGenderBox, regUpgradeBox;
    private DefaultTableModel regularTableModel;
    private JTable regularTable;
    private JTextArea regularDetailArea;
    private JLabel regStatusLabel;
    private JButton regActivateBtn, regDeactivateBtn, regAttendanceBtn, regUpgradeBtn, regRevertBtn, regRemoveBtn;

    // Premium tab
    private JTextField preIdField, preNameField, preLocationField, prePhoneField,
            preEmailField, preDobField, preMsdField, preTrainerField, prePayField, preSearchField;
    private JComboBox<String> preGenderBox;
    private DefaultTableModel premiumTableModel;
    private JTable premiumTable;
    private JTextArea premiumDetailArea;
    private JLabel preStatusLabel;
    private JButton preActivateBtn, preDeactivateBtn, preAttendanceBtn, prePayBtn, preDiscountBtn, preRevertBtn, preRemoveBtn;

    public MembershipPortalGUI() {
        buildUI();
    }

    // ================================================================
    //  MAIN LAYOUT
    // ================================================================
    private void buildUI() {
        setTitle("Membership Management Portal");
        setSize(1180, 760);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(buildDashboardPanel(), "DASHBOARD");
        contentPanel.add(buildRegularPanel(), "REGULAR");
        contentPanel.add(buildPremiumPanel(), "PREMIUM");
        add(contentPanel, BorderLayout.CENTER);

        refreshDashboard();
        refreshRegularTable();
        refreshPremiumTable();

        setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SIDEBAR_BG);
        header.setPreferredSize(new Dimension(0, 64));
        header.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));

        JLabel title = new JLabel("Membership Management Portal");
        title.setFont(FONT_HEADER);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        headerCountBadge = new JLabel("0 members total");
        headerCountBadge.setFont(FONT_LABEL);
        headerCountBadge.setForeground(new Color(148, 163, 184));
        header.add(headerCountBadge, BorderLayout.EAST);

        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));

        sidebar.add(makeNavButton("Dashboard", "DASHBOARD"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        sidebar.add(makeNavButton("Regular Members", "REGULAR"));
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        sidebar.add(makeNavButton("Premium Members", "PREMIUM"));
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton makeNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_NAV);
        btn.setForeground(Color.WHITE);
        btn.setBackground(SIDEBAR_BG);
        btn.setBorder(BorderFactory.createEmptyBorder(14, 25, 14, 25));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(SIDEBAR_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(SIDEBAR_BG); }
        });

        btn.addActionListener(e -> {
            switch (cardName) {
                case "DASHBOARD" -> showDashboardTab();
                case "REGULAR" -> showRegularTab();
                case "PREMIUM" -> showPremiumTab();
            }
        });

        return btn;
    }

    public void showDashboardTab() { cardLayout.show(contentPanel, "DASHBOARD"); refreshDashboard(); }
    public void showRegularTab()   { cardLayout.show(contentPanel, "REGULAR"); refreshRegularTable(); }
    public void showPremiumTab()   { cardLayout.show(contentPanel, "PREMIUM"); refreshPremiumTable(); }

    // ================================================================
    //  DASHBOARD
    // ================================================================
    private JPanel buildDashboardPanel() {
        JPanel dash = new JPanel(new BorderLayout());
        dash.setBackground(CONTENT_BG);
        dash.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(CONTENT_BG);

        JLabel heading = new JLabel("Dashboard Overview");
        heading.setFont(FONT_SECTION);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(heading);
        top.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel sub = new JLabel("Live snapshot of every member currently on record.");
        sub.setFont(FONT_LABEL);
        sub.setForeground(TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(sub);
        top.add(Box.createRigidArea(new Dimension(0, 20)));

        totalValueLabel = new JLabel("0");
        regularValueLabel = new JLabel("0");
        premiumValueLabel = new JLabel("0");
        activeValueLabel = new JLabel("0");

        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 20, 0));
        statsGrid.setBackground(CONTENT_BG);
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.add(makeStatCard("Total Members", totalValueLabel, ACCENT_TEAL));
        statsGrid.add(makeStatCard("Regular Members", regularValueLabel, ACCENT_BLUE));
        statsGrid.add(makeStatCard("Premium Members", premiumValueLabel, ACCENT_PURPLE));
        statsGrid.add(makeStatCard("Active Members", activeValueLabel, ACCENT_GREEN));
        top.add(statsGrid);

        dash.add(top, BorderLayout.NORTH);

        JPanel filler = new JPanel();
        filler.setBackground(CONTENT_BG);
        dash.add(filler, BorderLayout.CENTER);

        return dash;
    }

    private JPanel makeStatCard(String caption, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setPreferredSize(new Dimension(0, 120));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, accent),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        valueLabel.setFont(FONT_STAT);
        valueLabel.setForeground(new Color(30, 41, 59));
        card.add(valueLabel, BorderLayout.CENTER);

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(FONT_LABEL);
        captionLabel.setForeground(TEXT_MUTED);
        card.add(captionLabel, BorderLayout.SOUTH);

        return card;
    }

    private void refreshDashboard() {
        int total = members.size();
        long regularCount = members.stream().filter(m -> m instanceof RegularMember).count();
        long premiumCount = members.stream().filter(m -> m instanceof PremiumMember).count();
        long activeCount = members.stream().filter(GymMember::getActiveStatus).count();

        totalValueLabel.setText(String.valueOf(total));
        regularValueLabel.setText(String.valueOf(regularCount));
        premiumValueLabel.setText(String.valueOf(premiumCount));
        activeValueLabel.setText(String.valueOf(activeCount));
        headerCountBadge.setText(total + " member" + (total == 1 ? "" : "s") + " total");
    }

    // ================================================================
    //  REGULAR MEMBERS TAB
    // ================================================================
    private JPanel buildRegularPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(CONTENT_BG);
        wrap.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("Regular Membership");
        heading.setFont(FONT_SECTION);
        wrap.add(heading, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildRegularLeft(), buildRegularRight());
        split.setBorder(null);
        split.setDividerLocation(660);
        split.setResizeWeight(0.62);
        split.setBackground(CONTENT_BG);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(CONTENT_BG);
        body.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        body.add(split, BorderLayout.CENTER);

        wrap.add(body, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildRegularLeft() {
        JPanel left = new JPanel(new BorderLayout(0, 15));
        left.setBackground(CONTENT_BG);

        JPanel addCard = new JPanel(new GridBagLayout());
        addCard.setBackground(CARD_BG);
        addCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel formTitle = new JLabel("Add New Regular Member");
        formTitle.setFont(FONT_SECTION);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        addCard.add(formTitle, gbc);
        gbc.gridwidth = 1;

        regIdField = new JTextField();
        regNameField = new JTextField();
        regLocationField = new JTextField();
        regPhoneField = new JTextField();
        regEmailField = new JTextField();
        regGenderBox = new JComboBox<>(new String[]{"Male", "Female"});
        regDobField = new JTextField();
        regDobField.setToolTipText("e.g. 15-Aug-2001");
        regMsdField = new JTextField();
        regMsdField.setToolTipText("e.g. 01-Jul-2026");
        regReferralField = new JTextField();

        int row = 1;
        row = addFieldRow(addCard, gbc, row, "ID", regIdField, "Name", regNameField);
        row = addFieldRow(addCard, gbc, row, "Location", regLocationField, "Phone", regPhoneField);
        row = addFieldRow(addCard, gbc, row, "Email", regEmailField, "Gender", regGenderBox);
        row = addFieldRow(addCard, gbc, row, "Date of Birth", regDobField, "Start Date", regMsdField);
        row = addFieldRow(addCard, gbc, row, "Referral Source", regReferralField, null, null);

        JButton addBtn = styledButton("Add Regular Member", ACCENT_BLUE);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4; gbc.insets = new Insets(12, 8, 5, 8);
        addCard.add(addBtn, gbc);
        addBtn.addActionListener(e -> handleAddRegular());

        left.add(addCard, BorderLayout.NORTH);

        JPanel tableSection = new JPanel(new BorderLayout(0, 8));
        tableSection.setBackground(CONTENT_BG);

        regSearchField = new JTextField();
        regSearchField.setFont(FONT_FIELD);
        JPanel searchRow = new JPanel(new BorderLayout());
        searchRow.setBackground(CONTENT_BG);
        JLabel searchLbl = new JLabel("Search: ");
        searchLbl.setFont(FONT_LABEL);
        searchRow.add(searchLbl, BorderLayout.WEST);
        searchRow.add(regSearchField, BorderLayout.CENTER);
        tableSection.add(searchRow, BorderLayout.NORTH);

        String[] cols = {"ID", "Name", "Phone", "Attendance", "Plan", "Price (Rs.)", "Active"};
        regularTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        regularTable = new JTable(regularTableModel);
        regularTable.setFont(FONT_FIELD);
        regularTable.setRowHeight(26);
        regularTable.getTableHeader().setFont(FONT_LABEL);
        regularTable.setSelectionBackground(new Color(191, 219, 254));

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(regularTableModel);
        regularTable.setRowSorter(sorter);
        attachSearchFilter(regSearchField, sorter);

        regularTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRegularSelectionChanged();
        });

        tableSection.add(new JScrollPane(regularTable), BorderLayout.CENTER);
        left.add(tableSection, BorderLayout.CENTER);

        return left;
    }

    private JPanel buildRegularRight() {
        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setBackground(CONTENT_BG);
        right.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        JLabel heading = new JLabel("Member Details");
        heading.setFont(FONT_SECTION);
        right.add(heading, BorderLayout.NORTH);

        regularDetailArea = new JTextArea("Select a member from the table to view full details.");
        regularDetailArea.setFont(FONT_MONO);
        regularDetailArea.setEditable(false);
        regularDetailArea.setBackground(CARD_BG);
        regularDetailArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane detailScroll = new JScrollPane(regularDetailArea);
        detailScroll.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        right.add(detailScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(CONTENT_BG);
        actions.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel row1 = new JPanel(new GridLayout(1, 3, 8, 0));
        row1.setBackground(CONTENT_BG);
        regActivateBtn = styledButton("Activate", ACCENT_GREEN);
        regDeactivateBtn = styledButton("Deactivate", TEXT_MUTED);
        regAttendanceBtn = styledButton("Mark Attendance", ACCENT_TEAL);
        row1.add(regActivateBtn); row1.add(regDeactivateBtn); row1.add(regAttendanceBtn);
        actions.add(row1);
        actions.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel row2 = new JPanel(new BorderLayout(8, 0));
        row2.setBackground(CONTENT_BG);
        regUpgradeBox = new JComboBox<>(new String[]{"Basic", "Standard", "Deluxe"});
        regUpgradeBtn = styledButton("Upgrade Plan", ACCENT_BLUE);
        row2.add(regUpgradeBox, BorderLayout.CENTER);
        row2.add(regUpgradeBtn, BorderLayout.EAST);
        actions.add(row2);
        actions.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel row3 = new JPanel(new GridLayout(1, 2, 8, 0));
        row3.setBackground(CONTENT_BG);
        regRevertBtn = styledButton("Revert Member", ACCENT_AMBER);
        regRemoveBtn = styledButton("Remove Member", DANGER_RED);
        row3.add(regRevertBtn); row3.add(regRemoveBtn);
        actions.add(row3);
        actions.add(Box.createRigidArea(new Dimension(0, 10)));

        regStatusLabel = new JLabel(" ");
        regStatusLabel.setFont(FONT_LABEL);
        actions.add(regStatusLabel);

        right.add(actions, BorderLayout.SOUTH);

        regActivateBtn.addActionListener(e -> {
            RegularMember r = getSelectedRegular();
            if (r == null) return;
            String msg = captureOutput(r::activeMembership);
            showStatus(regStatusLabel, msg, false);
            refreshRegularDetail(r); refreshRegularTable(); refreshDashboard();
        });
        regDeactivateBtn.addActionListener(e -> {
            RegularMember r = getSelectedRegular();
            if (r == null) return;
            String msg = captureOutput(r::deactivateMembership);
            showStatus(regStatusLabel, msg, false);
            refreshRegularDetail(r); refreshRegularTable(); refreshDashboard();
        });
        regAttendanceBtn.addActionListener(e -> {
            RegularMember r = getSelectedRegular();
            if (r == null) return;
            String msg = captureOutput(r::markAttendance);
            boolean failed = !msg.isEmpty();
            showStatus(regStatusLabel, failed ? msg : "Attendance marked.", failed);
            refreshRegularDetail(r); refreshRegularTable(); refreshDashboard();
        });
        regUpgradeBtn.addActionListener(e -> {
            RegularMember r = getSelectedRegular();
            if (r == null) return;
            String plan = (String) regUpgradeBox.getSelectedItem();
            String result = r.upgradePlan(plan);
            showStatus(regStatusLabel, result, result.startsWith("You're not"));
            refreshRegularDetail(r); refreshRegularTable(); refreshDashboard();
        });
        regRevertBtn.addActionListener(e -> {
            RegularMember r = getSelectedRegular();
            if (r == null) return;
            String reason = JOptionPane.showInputDialog(this, "Enter a reason for reverting this member:",
                    "Revert Member", JOptionPane.QUESTION_MESSAGE);
            if (reason == null) return;
            if (reason.trim().isEmpty()) { showStatus(regStatusLabel, "A reason is required.", true); return; }
            r.revertRegularMember(reason.trim());
            showStatus(regStatusLabel, "Member " + r.getId() + " has been reverted.", false);
            refreshRegularDetail(r); refreshRegularTable(); refreshDashboard();
        });
        regRemoveBtn.addActionListener(e -> {
            RegularMember r = getSelectedRegular();
            if (r == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove regular member " + r.getId() + " (" + r.getName() + ")? This cannot be undone.",
                    "Confirm Removal", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                members.remove(r);
                regularDetailArea.setText("Select a member from the table to view full details.");
                refreshRegularTable(); refreshDashboard();
            }
        });

        setRegularActionsEnabled(false);
        return right;
    }

    private void onRegularSelectionChanged() {
        RegularMember r = getSelectedRegular();
        if (r != null) {
            refreshRegularDetail(r);
            setRegularActionsEnabled(true);
        } else {
            regularDetailArea.setText("Select a member from the table to view full details.");
            setRegularActionsEnabled(false);
        }
    }

    private void setRegularActionsEnabled(boolean enabled) {
        regActivateBtn.setEnabled(enabled);
        regDeactivateBtn.setEnabled(enabled);
        regAttendanceBtn.setEnabled(enabled);
        regUpgradeBtn.setEnabled(enabled);
        regRevertBtn.setEnabled(enabled);
        regRemoveBtn.setEnabled(enabled);
    }

    private void handleAddRegular() {
        if (isBlank(regIdField) || isBlank(regNameField) || isBlank(regLocationField) ||
                isBlank(regPhoneField) || isBlank(regEmailField) || isBlank(regDobField) ||
                isBlank(regMsdField) || isBlank(regReferralField)) {
            showStatus(regStatusLabel, "All fields are required.", true);
            return;
        }
        int id;
        try {
            id = Integer.parseInt(regIdField.getText().trim());
        } catch (NumberFormatException ex) {
            showStatus(regStatusLabel, "ID must be a whole number.", true);
            return;
        }
        if (!isIdUnique(id)) {
            showStatus(regStatusLabel, "Member ID " + id + " already exists.", true);
            return;
        }
        RegularMember r = new RegularMember(id, regNameField.getText().trim(), regLocationField.getText().trim(),
                regPhoneField.getText().trim(), regEmailField.getText().trim(),
                (String) regGenderBox.getSelectedItem(), regDobField.getText().trim(),
                regMsdField.getText().trim(), regReferralField.getText().trim());
        members.add(r);
        clearRegularForm();
        refreshRegularTable();
        refreshDashboard();
        showStatus(regStatusLabel, "Regular member " + id + " added successfully.", false);
    }

    private void clearRegularForm() {
        regIdField.setText(""); regNameField.setText(""); regLocationField.setText("");
        regPhoneField.setText(""); regEmailField.setText(""); regDobField.setText("");
        regMsdField.setText(""); regReferralField.setText(""); regGenderBox.setSelectedIndex(0);
    }

    private RegularMember getSelectedRegular() {
        int viewRow = regularTable.getSelectedRow();
        if (viewRow == -1) return null;
        int modelRow = regularTable.convertRowIndexToModel(viewRow);
        int id = (Integer) regularTableModel.getValueAt(modelRow, 0);
        GymMember m = findMemberById(id);
        return (m instanceof RegularMember) ? (RegularMember) m : null;
    }

    private void refreshRegularDetail(RegularMember r) {
        regularDetailArea.setText(captureOutput(r::display));
        regularDetailArea.setCaretPosition(0);
    }

    private void refreshRegularTable() {
        regularTableModel.setRowCount(0);
        for (GymMember m : members) {
            if (m instanceof RegularMember r) {
                regularTableModel.addRow(new Object[]{
                        r.getId(), r.getName(), r.getPhone(), r.getAttendance(),
                        r.getPlan(), r.getPrice(), r.getActiveStatus() ? "Yes" : "No"
                });
            }
        }
    }

    // ================================================================
    //  PREMIUM MEMBERS TAB
    // ================================================================
    private JPanel buildPremiumPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(CONTENT_BG);
        wrap.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("Premium Membership");
        heading.setFont(FONT_SECTION);
        wrap.add(heading, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildPremiumLeft(), buildPremiumRight());
        split.setBorder(null);
        split.setDividerLocation(660);
        split.setResizeWeight(0.62);
        split.setBackground(CONTENT_BG);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(CONTENT_BG);
        body.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        body.add(split, BorderLayout.CENTER);

        wrap.add(body, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildPremiumLeft() {
        JPanel left = new JPanel(new BorderLayout(0, 15));
        left.setBackground(CONTENT_BG);

        JPanel addCard = new JPanel(new GridBagLayout());
        addCard.setBackground(CARD_BG);
        addCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel formTitle = new JLabel("Add New Premium Member");
        formTitle.setFont(FONT_SECTION);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        addCard.add(formTitle, gbc);
        gbc.gridwidth = 1;

        preIdField = new JTextField();
        preNameField = new JTextField();
        preLocationField = new JTextField();
        prePhoneField = new JTextField();
        preEmailField = new JTextField();
        preGenderBox = new JComboBox<>(new String[]{"Male", "Female"});
        preDobField = new JTextField();
        preDobField.setToolTipText("e.g. 15-Aug-2001");
        preMsdField = new JTextField();
        preMsdField.setToolTipText("e.g. 01-Jul-2026");
        preTrainerField = new JTextField();

        int row = 1;
        row = addFieldRow(addCard, gbc, row, "ID", preIdField, "Name", preNameField);
        row = addFieldRow(addCard, gbc, row, "Location", preLocationField, "Phone", prePhoneField);
        row = addFieldRow(addCard, gbc, row, "Email", preEmailField, "Gender", preGenderBox);
        row = addFieldRow(addCard, gbc, row, "Date of Birth", preDobField, "Start Date", preMsdField);
        row = addFieldRow(addCard, gbc, row, "Personal Trainer", preTrainerField, null, null);

        JButton addBtn = styledButton("Add Premium Member", ACCENT_PURPLE);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4; gbc.insets = new Insets(12, 8, 5, 8);
        addCard.add(addBtn, gbc);
        addBtn.addActionListener(e -> handleAddPremium());

        left.add(addCard, BorderLayout.NORTH);

        JPanel tableSection = new JPanel(new BorderLayout(0, 8));
        tableSection.setBackground(CONTENT_BG);

        preSearchField = new JTextField();
        preSearchField.setFont(FONT_FIELD);
        JPanel searchRow = new JPanel(new BorderLayout());
        searchRow.setBackground(CONTENT_BG);
        JLabel searchLbl = new JLabel("Search: ");
        searchLbl.setFont(FONT_LABEL);
        searchRow.add(searchLbl, BorderLayout.WEST);
        searchRow.add(preSearchField, BorderLayout.CENTER);
        tableSection.add(searchRow, BorderLayout.NORTH);

        String[] cols = {"ID", "Name", "Trainer", "Paid (Rs.)", "Full Payment", "Discount (Rs.)", "Active"};
        premiumTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        premiumTable = new JTable(premiumTableModel);
        premiumTable.setFont(FONT_FIELD);
        premiumTable.setRowHeight(26);
        premiumTable.getTableHeader().setFont(FONT_LABEL);
        premiumTable.setSelectionBackground(new Color(233, 213, 255));

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(premiumTableModel);
        premiumTable.setRowSorter(sorter);
        attachSearchFilter(preSearchField, sorter);

        premiumTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onPremiumSelectionChanged();
        });

        tableSection.add(new JScrollPane(premiumTable), BorderLayout.CENTER);
        left.add(tableSection, BorderLayout.CENTER);

        return left;
    }

    private JPanel buildPremiumRight() {
        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setBackground(CONTENT_BG);
        right.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        JLabel heading = new JLabel("Member Details");
        heading.setFont(FONT_SECTION);
        right.add(heading, BorderLayout.NORTH);

        premiumDetailArea = new JTextArea("Select a member from the table to view full details.");
        premiumDetailArea.setFont(FONT_MONO);
        premiumDetailArea.setEditable(false);
        premiumDetailArea.setBackground(CARD_BG);
        premiumDetailArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane detailScroll = new JScrollPane(premiumDetailArea);
        detailScroll.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        right.add(detailScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(CONTENT_BG);
        actions.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel row1 = new JPanel(new GridLayout(1, 3, 8, 0));
        row1.setBackground(CONTENT_BG);
        preActivateBtn = styledButton("Activate", ACCENT_GREEN);
        preDeactivateBtn = styledButton("Deactivate", TEXT_MUTED);
        preAttendanceBtn = styledButton("Mark Attendance", ACCENT_TEAL);
        row1.add(preActivateBtn); row1.add(preDeactivateBtn); row1.add(preAttendanceBtn);
        actions.add(row1);
        actions.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel row2 = new JPanel(new BorderLayout(8, 0));
        row2.setBackground(CONTENT_BG);
        prePayField = new JTextField();
        prePayBtn = styledButton("Pay Due Amount", ACCENT_PURPLE);
        row2.add(prePayField, BorderLayout.CENTER);
        row2.add(prePayBtn, BorderLayout.EAST);
        actions.add(row2);
        actions.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel row3 = new JPanel(new GridLayout(1, 3, 8, 0));
        row3.setBackground(CONTENT_BG);
        preDiscountBtn = styledButton("Calc. Discount", ACCENT_BLUE);
        preRevertBtn = styledButton("Revert", ACCENT_AMBER);
        preRemoveBtn = styledButton("Remove", DANGER_RED);
        row3.add(preDiscountBtn); row3.add(preRevertBtn); row3.add(preRemoveBtn);
        actions.add(row3);
        actions.add(Box.createRigidArea(new Dimension(0, 10)));

        preStatusLabel = new JLabel(" ");
        preStatusLabel.setFont(FONT_LABEL);
        actions.add(preStatusLabel);

        right.add(actions, BorderLayout.SOUTH);

        preActivateBtn.addActionListener(e -> {
            PremiumMember p = getSelectedPremium();
            if (p == null) return;
            String msg = captureOutput(p::activeMembership);
            showStatus(preStatusLabel, msg, false);
            refreshPremiumDetail(p); refreshPremiumTable(); refreshDashboard();
        });
        preDeactivateBtn.addActionListener(e -> {
            PremiumMember p = getSelectedPremium();
            if (p == null) return;
            String msg = captureOutput(p::deactivateMembership);
            showStatus(preStatusLabel, msg, false);
            refreshPremiumDetail(p); refreshPremiumTable(); refreshDashboard();
        });
        preAttendanceBtn.addActionListener(e -> {
            PremiumMember p = getSelectedPremium();
            if (p == null) return;
            String msg = captureOutput(p::markAttendance);
            boolean failed = !msg.isEmpty();
            showStatus(preStatusLabel, failed ? msg : "Attendance marked.", failed);
            refreshPremiumDetail(p); refreshPremiumTable(); refreshDashboard();
        });
        prePayBtn.addActionListener(e -> {
            PremiumMember p = getSelectedPremium();
            if (p == null) return;
            double amount;
            try {
                amount = Double.parseDouble(prePayField.getText().trim());
            } catch (NumberFormatException ex) {
                showStatus(preStatusLabel, "Enter a valid payment amount.", true);
                return;
            }
            String result = p.payDueAmount(amount);
            showStatus(preStatusLabel, result, result.toLowerCase().contains("must be positive"));
            prePayField.setText("");
            refreshPremiumDetail(p); refreshPremiumTable(); refreshDashboard();
        });
        preDiscountBtn.addActionListener(e -> {
            PremiumMember p = getSelectedPremium();
            if (p == null) return;
            String msg = captureOutput(p::calculateDiscount);
            showStatus(preStatusLabel, msg, msg.toLowerCase().contains("not eligible"));
            refreshPremiumDetail(p); refreshPremiumTable(); refreshDashboard();
        });
        preRevertBtn.addActionListener(e -> {
            PremiumMember p = getSelectedPremium();
            if (p == null) return;
            p.revertPremiumMember();
            showStatus(preStatusLabel, "Member " + p.getId() + " has been reverted.", false);
            refreshPremiumDetail(p); refreshPremiumTable(); refreshDashboard();
        });
        preRemoveBtn.addActionListener(e -> {
            PremiumMember p = getSelectedPremium();
            if (p == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove premium member " + p.getId() + " (" + p.getName() + ")? This cannot be undone.",
                    "Confirm Removal", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                members.remove(p);
                premiumDetailArea.setText("Select a member from the table to view full details.");
                refreshPremiumTable(); refreshDashboard();
            }
        });

        setPremiumActionsEnabled(false);
        return right;
    }

    private void onPremiumSelectionChanged() {
        PremiumMember p = getSelectedPremium();
        if (p != null) {
            refreshPremiumDetail(p);
            setPremiumActionsEnabled(true);
        } else {
            premiumDetailArea.setText("Select a member from the table to view full details.");
            setPremiumActionsEnabled(false);
        }
    }

    private void setPremiumActionsEnabled(boolean enabled) {
        preActivateBtn.setEnabled(enabled);
        preDeactivateBtn.setEnabled(enabled);
        preAttendanceBtn.setEnabled(enabled);
        prePayBtn.setEnabled(enabled);
        preDiscountBtn.setEnabled(enabled);
        preRevertBtn.setEnabled(enabled);
        preRemoveBtn.setEnabled(enabled);
    }

    private void handleAddPremium() {
        if (isBlank(preIdField) || isBlank(preNameField) || isBlank(preLocationField) ||
                isBlank(prePhoneField) || isBlank(preEmailField) || isBlank(preDobField) ||
                isBlank(preMsdField) || isBlank(preTrainerField)) {
            showStatus(preStatusLabel, "All fields are required.", true);
            return;
        }
        int id;
        try {
            id = Integer.parseInt(preIdField.getText().trim());
        } catch (NumberFormatException ex) {
            showStatus(preStatusLabel, "ID must be a whole number.", true);
            return;
        }
        if (!isIdUnique(id)) {
            showStatus(preStatusLabel, "Member ID " + id + " already exists.", true);
            return;
        }
        PremiumMember p = new PremiumMember(id, preNameField.getText().trim(), preLocationField.getText().trim(),
                prePhoneField.getText().trim(), preEmailField.getText().trim(),
                (String) preGenderBox.getSelectedItem(), preDobField.getText().trim(),
                preMsdField.getText().trim(), preTrainerField.getText().trim());
        members.add(p);
        clearPremiumForm();
        refreshPremiumTable();
        refreshDashboard();
        showStatus(preStatusLabel, "Premium member " + id + " added successfully.", false);
    }

    private void clearPremiumForm() {
        preIdField.setText(""); preNameField.setText(""); preLocationField.setText("");
        prePhoneField.setText(""); preEmailField.setText(""); preDobField.setText("");
        preMsdField.setText(""); preTrainerField.setText(""); preGenderBox.setSelectedIndex(0);
    }

    private PremiumMember getSelectedPremium() {
        int viewRow = premiumTable.getSelectedRow();
        if (viewRow == -1) return null;
        int modelRow = premiumTable.convertRowIndexToModel(viewRow);
        int id = (Integer) premiumTableModel.getValueAt(modelRow, 0);
        GymMember m = findMemberById(id);
        return (m instanceof PremiumMember) ? (PremiumMember) m : null;
    }

    private void refreshPremiumDetail(PremiumMember p) {
        premiumDetailArea.setText(captureOutput(p::display));
        premiumDetailArea.setCaretPosition(0);
    }

    private void refreshPremiumTable() {
        premiumTableModel.setRowCount(0);
        for (GymMember m : members) {
            if (m instanceof PremiumMember p) {
                premiumTableModel.addRow(new Object[]{
                        p.getId(), p.getName(), p.getPersonalTrainer(), p.getPaidAmount(),
                        p.getIsFullPayment() ? "Yes" : "No", p.getDiscountAmount(),
                        p.getActiveStatus() ? "Yes" : "No"
                });
            }
        }
    }

    // ================================================================
    //  SHARED HELPERS
    // ================================================================
    private int addFieldRow(JPanel panel, GridBagConstraints gbc, int row,
                             String label1, JComponent field1, String label2, JComponent field2) {
        gbc.gridy = row;

        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(styledLabel(label1), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        field1.setFont(FONT_FIELD);
        panel.add(field1, gbc);

        if (label2 != null) {
            gbc.gridx = 2; gbc.weightx = 0;
            panel.add(styledLabel(label2), gbc);
            gbc.gridx = 3; gbc.weightx = 1;
            field2.setFont(FONT_FIELD);
            panel.add(field2, gbc);
        }
        return row + 1;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        return l;
    }

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_LABEL);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 38));
        return btn;
    }

    private boolean isBlank(JTextField field) {
        return field.getText().trim().isEmpty();
    }

    private void showStatus(JLabel label, String message, boolean isError) {
        label.setText(message);
        label.setForeground(isError ? DANGER_RED : new Color(21, 128, 61));
    }

    private GymMember findMemberById(int id) {
        for (GymMember m : members) {
            if (m.getId() == id) return m;
        }
        return null;
    }

    private boolean isIdUnique(int id) {
        return findMemberById(id) == null;
    }

    private String captureOutput(Runnable action) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        System.setOut(ps);
        try {
            action.run();
        } finally {
            System.out.flush();
            System.setOut(old);
        }
        return baos.toString().trim();
    }

    private void attachSearchFilter(JTextField searchField, TableRowSorter<DefaultTableModel> sorter) {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
                }
            }
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
    }

    // ================================================================
    //  ENTRY POINT
    // ================================================================
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(MembershipPortalGUI::new);
    }
}
