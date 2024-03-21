import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

class AutoIndentDocumentFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offs, String str, AttributeSet a) throws BadLocationException {
        if ("\n".equals(str)) {
            String content = fb.getDocument().getText(0, offs);
            int lastNewline = content.lastIndexOf('\n');
            StringBuilder sb = new StringBuilder(str);
            if (lastNewline >= 0) {
                int lineStart = lastNewline + 1;
                while (lineStart < offs && Character.isWhitespace(content.charAt(lineStart))) {
                    sb.append(content.charAt(lineStart++));
                }
                if (content.charAt(lineStart - 1) == '{') {
                    sb.append('\t'); // Assumes tab character for indentation
                }
            }
            str = sb.toString();
        }
        super.insertString(fb, offs, str, a);
    }

    @Override
    public void replace(FilterBypass fb, int offs, int length, String str, AttributeSet a) throws BadLocationException {
        if (str != null && str.contains("\n")) {
            insertString(fb, offs, str, a);
        } else {
            super.replace(fb, offs, length, str, a);
        }
    }
}
