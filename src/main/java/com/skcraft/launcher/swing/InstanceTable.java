/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.swing;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

public class InstanceTable extends JTable {

    public InstanceTable() {
        setShowGrid(true);
        setGridColor(Color.lightGray);
        setRowHeight(Math.max(getRowHeight() + 100, 50));
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setTableHeader(null);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    @Override
    public void setModel(TableModel dataModel) {
        super.setModel(dataModel);
        try {
            getColumnModel().getColumn(0).setMaxWidth(128);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }
}
