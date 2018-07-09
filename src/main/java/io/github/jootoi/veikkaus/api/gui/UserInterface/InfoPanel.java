
package io.github.jootoi.veikkaus.api.gui.UserInterface;

import java.awt.Component;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Joonas Toimela
 */
public class InfoPanel extends JPanel{

    public InfoPanel(Component[] components) {
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        for(Component c : components) {
            super.add(c);
        }
    }
    
    
    public InfoPanel(String[] labels) {
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        for(String string:labels) {
            JLabel label = new JLabel(string);
            super.add(label);
        }
    }

}
