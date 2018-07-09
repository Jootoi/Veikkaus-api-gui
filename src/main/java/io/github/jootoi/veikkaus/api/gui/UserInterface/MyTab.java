
package io.github.jootoi.veikkaus.api.gui.UserInterface;

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JTabbedPane;

/**
 *
 * @author Joonas Toimela
 */
public class MyTab extends JTabbedPane {
    ArrayList<Integer> children;
    int parent;
    int id;
    
    public MyTab(int parent, int id) {
        super();
        children = new ArrayList<>();
        this.parent = parent;
        this.id = id;
    }
    public MyTab() {
        super();
    }
    
    public Component add(String label,Component c, int id) {
        super.add(label,c);
        children.add(id);
        return c;
    }

    public Component add(String label, Component c, int poolID, int position) {
        super.insertTab(label,null,c,null,position);
        children.add(poolID);
        return c;
    }
}
