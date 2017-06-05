package org.opendatakit.briefcase.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

//Sources:
//https://stackoverflow.com/questions/16632104/jspinner-with-display-format-numbers-only-and-manual-edit
//http://stackoverflow.com/questions/20541230/allow-only-numbers-in-jtextfield
public class JIntegerSpinner extends JSpinner {

  public JIntegerSpinner(int value, int min, int max, int step) {
    super (new SpinnerNumberModel(8080, 0, 65535, 1));
    JTextComponent txt = ((JSpinner.DefaultEditor) this.getEditor()).getTextField();
    txt.setDocument(new IntegerDocument());
  }
  
  private class IntegerDocument extends PlainDocument {
    private IntegerDocumentFilter filter;
    
    protected IntegerDocument() {
      filter = new IntegerDocumentFilter();
    }
    @Override
    public DocumentFilter getDocumentFilter() {
        return filter;
    }
  }
  
  private class IntegerDocumentFilter extends DocumentFilter {
    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
      Pattern regEx = Pattern.compile("\\d*");
      Matcher matcher = regEx.matcher(text);
      if(!matcher.matches()){
          return;
      }
      super.replace(fb, offset, length, text, attrs);
    }
  }
}

