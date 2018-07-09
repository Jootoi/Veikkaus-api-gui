
package io.github.jootoi.veikkaus.api.gui.UserInterface;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author Joonas Toimela
 */
public class OddsTableComponent extends JPanel {
    private InfoPanel sidepanel;
    private JTable datatable;
    private JScrollPane datatableView;
    private int id;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public InfoPanel getSidepanel() {
        return sidepanel;
    }

    public void setSidepanel(InfoPanel sidepanel) {
        super.remove(this.sidepanel);
        super.add(sidepanel,BorderLayout.LINE_END);
        this.sidepanel = sidepanel;
        repaint();
        revalidate();
    }

    public JTable getDatatable() {
        return datatable;
    }

    public void setDatatable(JTable datatable) {
        super.remove(this.datatableView);
        javax.swing.JScrollPane dtWrapper = new javax.swing.JScrollPane(datatable);
        this.datatable = datatable;
        this.datatableView = dtWrapper;
        super.add(dtWrapper, BorderLayout.CENTER);
        repaint();
        revalidate();
    }
    
    public void setDatatable(Object[][] data, Object[] labels) {
        super.remove(this.datatableView);
        JTable dataTable = new JTable(new DefaultTableModel(data, labels) {
            @Override
            public boolean isCellEditable(int row, int column){ return false;}
        });
        javax.swing.JScrollPane dtWrapper = new javax.swing.JScrollPane(dataTable);
        super.add(dtWrapper, BorderLayout.CENTER);
        this.datatable = dataTable;
        this.datatableView = dtWrapper;
        repaint();
        revalidate();
    }
    
    public OddsTableComponent(InfoPanel ip, JTable dt) {
        super.setLayout(new BorderLayout());
        super.add(ip,BorderLayout.LINE_END);
        javax.swing.JScrollPane dtWrapper = new javax.swing.JScrollPane(dt);
        super.add(dtWrapper, BorderLayout.CENTER);
        this.sidepanel = ip;
        this.datatable = dt;
        this.datatableView = dtWrapper;
    }
    
    public OddsTableComponent(InfoPanel ip, Object[][] data, Object[] labels) {
        super.setLayout(new BorderLayout());
        super.add(ip,BorderLayout.LINE_END);
        JTable dataTable = new JTable(new DefaultTableModel(data, labels) {
            @Override
            public boolean isCellEditable(int row, int column){ return false;}
        });
        javax.swing.JScrollPane dtWrapper = new javax.swing.JScrollPane(dataTable);
        super.add(dtWrapper,BorderLayout.CENTER);
        this.sidepanel = ip;
        this.datatable = dataTable;
        this.datatableView = dtWrapper;
    }
    
    public OddsTableComponent(InfoPanel ip, TableModel tm) {
        super.setLayout(new BorderLayout());
        super.add(ip,BorderLayout.LINE_END);

        JTable dataTable = new JTable(tm);
        javax.swing.JScrollPane dtWrapper = new javax.swing.JScrollPane(dataTable);
        super.add(dtWrapper, BorderLayout.CENTER);
        this.sidepanel = ip;
        this.datatable = dataTable;
        this.datatableView = dtWrapper;
    }
}
