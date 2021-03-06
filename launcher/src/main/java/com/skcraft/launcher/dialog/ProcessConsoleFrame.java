/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.dialog;

import com.skcraft.launcher.swing.LinedBoxPanel;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.util.SharedLocale;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import nz.co.lolnet.james137137.LauncherGobalSettings;

/**
 * A version of the console window that can manage a process.
 */
public class ProcessConsoleFrame extends ConsoleFrame {

    private JButton killButton;
    private JButton minimizeButton;
    private TrayIcon trayIcon;
    private long age;
    private static java.util.List<ProcessConsoleFrame> consoleList = new ArrayList<>();

    @Getter
    private Process process;
    @Getter
    @Setter
    private boolean killOnClose;

    private PrintWriter processOut;

    /**
     * Create a new instance of the frame.
     *
     * @param numLines the number of log lines
     * @param colorEnabled whether color is enabled in the log
     */
    public ProcessConsoleFrame(int numLines, boolean colorEnabled) {
        super(SharedLocale.tr("console.title"), numLines, colorEnabled);
        age = System.currentTimeMillis();
        consoleList.add(this);
        processOut = new PrintWriter(
                getMessageLog().getOutputStream(new Color(0, 0, 255)), true);
        initComponents();
        updateComponents();
        for (ProcessConsoleFrame processConsoleFrame : consoleList) {
            if (!processConsoleFrame.equals(this)) {
                processConsoleFrame.dispatchEvent(new WindowEvent(processConsoleFrame, WindowEvent.WINDOW_CLOSING));
            }
        }
    }

    /**
     * Track the given process.
     *
     * @param process the process
     */
    public synchronized void setProcess(Process process) {
        try {
            Process lastProcess = this.process;
            if (lastProcess != null) {
                processOut.println(SharedLocale.tr("console.processEndCode", lastProcess.exitValue()));
            }
        } catch (IllegalThreadStateException e) {
        }

        if (process != null) {
            processOut.println(SharedLocale.tr("console.attachedToProcess"));
        }

        this.process = process;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateComponents();
            }
        });
    }

    private synchronized boolean hasProcess() {
        return process != null;
    }

    @Override
    protected void performClose() {
        if (hasProcess()) {
            if (killOnClose) {
                performKill();
            }
        }

        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }

        super.performClose();
    }

    private void performKill() {
        if (!confirmKill()) {
            return;
        }

        synchronized (this) {
            if (hasProcess()) {
                process.destroy();
                setProcess(null);
            }
        }

        updateComponents();
    }

    protected void initComponents() {
        killButton = new JButton(SharedLocale.tr("console.forceClose"));
        minimizeButton = new JButton(); // Text set later

        LinedBoxPanel buttonsPanel = getButtonsPanel();
        buttonsPanel.addGlue();
        buttonsPanel.addElement(killButton);
        buttonsPanel.addElement(minimizeButton);

        killButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performKill();
            }
        });

        minimizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                contextualClose();
            }
        });

        if (!setupTrayIcon()) {
            minimizeButton.setEnabled(true);
        }
    }

    private boolean setupTrayIcon() {
        if (!SystemTray.isSupported()) {
            return false;
        }

        trayIcon = new TrayIcon(getTrayRunningIcon());
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip(SharedLocale.tr("console.trayTooltip"));

        trayIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reshow();
            }
        });

        PopupMenu popup = new PopupMenu();
        MenuItem item;

        popup.add(item = new MenuItem(SharedLocale.tr("console.trayTitle")));
        item.setEnabled(false);
        String showOfflineButton = LauncherGobalSettings.get("IDontOwnMicrosoft");
        if ((showOfflineButton != null && showOfflineButton.equals("true"))) {
            item.setEnabled(true);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LauncherFrame.instance.setVisible(true);
                    LauncherFrame.instance.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    LauncherFrame.instance.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent ev) {
                            JOptionPane.showMessageDialog(null, "Please Close your minecraft session first", "Warning!", JOptionPane.WARNING_MESSAGE);
                        }
                    });
                }
            });
        }

        popup.add(item = new MenuItem(SharedLocale.tr("console.tray.showWindow")));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reshow();
            }
        });

        popup.add(item = new MenuItem(SharedLocale.tr("console.tray.forceClose")));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performKill();
            }
        });

        trayIcon.setPopupMenu(popup);

        try {
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(trayIcon);
            return true;
        } catch (AWTException e) {
        }

        return false;
    }

    private synchronized void updateComponents() {
        Image icon = hasProcess() ? getTrayRunningIcon() : getTrayClosedIcon();

        killButton.setEnabled(hasProcess());

        if (!hasProcess() || trayIcon == null) {
            minimizeButton.setText(SharedLocale.tr("console.closeWindow"));
        } else {
            minimizeButton.setText(SharedLocale.tr("console.hideWindow"));
        }

        if (trayIcon != null) {
            trayIcon.setImage(icon);
        }

        setIconImage(icon);
    }

    private synchronized void contextualClose() {
        if (!hasProcess() || trayIcon == null) {
            performClose();
        } else {
            minimize();
        }

        updateComponents();
    }

    private boolean confirmKill() {
        return SwingHelper.confirmDialog(this, SharedLocale.tr("console.confirmKill"), SharedLocale.tr("console.confirmKillTitle"));
    }

    private void minimize() {
        setVisible(false);
    }

    private void reshow() {
        setVisible(true);
        requestFocus();
    }

}
