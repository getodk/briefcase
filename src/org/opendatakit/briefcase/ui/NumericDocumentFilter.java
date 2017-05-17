package org.opendatakit.briefcase.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class NumericDocumentFilter extends DocumentFilter {

    @Override
    //http://stackoverflow.com/questions/20541230/allow-only-numbers-in-jtextfield
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
    	Pattern regEx = Pattern.compile("\\d*");
        Matcher matcher = regEx.matcher(text);
        if(!matcher.matches()){
            return;
        }
        super.replace(fb, offset, length, text, attrs);
    }
}
