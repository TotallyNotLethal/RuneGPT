import org.dreambot.api.utilities.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

class ExpandableListCellRenderer extends DefaultListCellRenderer {
    @Serial
    private static final long serialVersionUID = 1L;
    private final Map<Integer, Boolean> expandedState = new HashMap<>();

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JTextArea textArea = new JTextArea(value.toString());
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        if (isSelected) {
            textArea.setBackground(list.getSelectionBackground());
            textArea.setForeground(list.getSelectionForeground());
        } else {
            textArea.setBackground(list.getBackground());
            textArea.setForeground(list.getForeground());
        }
        if (Boolean.TRUE.equals(expandedState.get(index))) {
            textArea.setPreferredSize(new Dimension(list.getWidth(), textArea.getPreferredSize().height));
        } else {
            textArea.setPreferredSize(new Dimension(list.getWidth(), list.getFixedCellHeight()));
        }
        return textArea;
    }

    public void toggleExpandedState(int index) {
        expandedState.put(index, !Boolean.TRUE.equals(expandedState.get(index)));
    }
}