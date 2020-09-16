package org.osmorc.run.ui;

import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.EventObject;

/**
 * @author <a href="janthomae@janthomae.de">Jan Thom&auml;</a>
 * @version $Id:$
 */
public class JSpinnerCellEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
  final JSpinner spinner = new JSpinner();

  private static final Logger LOG = Logger.getInstance(JSpinnerCellEditor.class);


  public JSpinnerCellEditor() {
    spinner.setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)spinner.getEditor();
    editor.getTextField().setFormatterFactory(new DefaultFormatterFactory(new MyNumberFormatter("Default")));
  }

  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    spinner.setValue(value);
    table.setRowHeight(row, spinner.getPreferredSize().height);
    spinner.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        fireEditingStopped();
      }
    });
    spinner.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        fireEditingStopped();
      }
    });
    return spinner;
  }

  public boolean isCellEditable(EventObject evt) {
    if (evt instanceof MouseEvent) {
      return ((MouseEvent)evt).getClickCount() >= 1;
    }
    return true;
  }

  public Object getCellEditorValue() {
    return spinner.getValue();
  }

  public boolean stopCellEditing() {
    try {
      spinner.commitEdit();
    }
    catch (ParseException exc) {
      return false;
    }
    return super.stopCellEditing();
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    try {
      spinner.setValue(value);
    }
    catch (IllegalArgumentException e) {
      LOG.warn("INvalid value: " + value, e);
    }
    table.setRowHeight(row, spinner.getPreferredSize().height);
    return spinner;
  }


  public static class MyNumberFormatter extends NumberFormatter {

    private String myZeroValue;

    public MyNumberFormatter(@Nonnull String zeroValue) {
      myZeroValue = zeroValue;
      setValueClass(Integer.class);
    }

    @Override
    public String valueToString(Object value) throws ParseException {


      if ((value instanceof Long && value.equals(0L)) || (value instanceof Integer && value.equals(0))) {
        return myZeroValue;
      }
      else {
        return super.valueToString(value);
      }
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
      if (text.equals(myZeroValue)) {
        return 0;
      }
      else {
        return super.stringToValue(text);
      }
    }
  }
}
