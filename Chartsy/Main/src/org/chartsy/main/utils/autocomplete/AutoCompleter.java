package org.chartsy.main.utils.autocomplete;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.chartsy.main.SymbolChanger;

/**
 *
 * @author Viorel
 */
public abstract class AutoCompleter
{

	JList list = new JList();
	JPopupMenu popupMenu = new JPopupMenu();
	JTextComponent component;

	private static final String AUTOCOMPLETER = "AUTOCOMPLETER";

	public AutoCompleter(JTextComponent comp)
	{
		this.component = comp;
		this.component.putClientProperty(AUTOCOMPLETER, this);
		JScrollPane pane = new JScrollPane(list);
		pane.setBorder(null);

		list.setFocusable(true);
		pane.getVerticalScrollBar().setFocusable(false);
		pane.getHorizontalScrollBar().setFocusable(false);
		popupMenu.add(pane);

		if (this.component instanceof JTextField)
		{
			this.component.registerKeyboardAction(showAction, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), JComponent.WHEN_FOCUSED);
			this.component.getDocument().addDocumentListener(documentListener);
		}
		else
		{
			this.component.registerKeyboardAction(showAction, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_MASK), JComponent.WHEN_FOCUSED);
		}

		this.component.registerKeyboardAction(upAction, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), JComponent.WHEN_FOCUSED);
		this.component.registerKeyboardAction(hidePopupAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);

		popupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
                component.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
				if (component.getParent() instanceof SymbolChanger)
				{
					SymbolChanger changer = (SymbolChanger) component.getParent();
					component.registerKeyboardAction(changer.submit, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
				}
            }
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
		list.setRequestFocusEnabled(false);
	}

	static Action acceptAction = new AbstractAction()
	{
		public void actionPerformed(ActionEvent e)
		{
			JComponent comp = (JComponent) e.getSource();
			AutoCompleter completer = (AutoCompleter) comp.getClientProperty(AUTOCOMPLETER);
			completer.popupMenu.setVisible(false);
			completer.acceptedListItem(completer.list.getSelectedValue());
		}
	};

	DocumentListener documentListener = new DocumentListener()
	{
		public void insertUpdate(DocumentEvent e)
		{
			showPopup();
		}
		public void removeUpdate(DocumentEvent e)
		{
			showPopup();
		}
		public void changedUpdate(DocumentEvent e) {}
	};

	private void showPopup()
	{
		popupMenu.setVisible(false);
        if(component.isEnabled() && updateListData() && list.getModel().getSize()!=0)
		{
            if(!(component instanceof JTextField))
                component.getDocument().addDocumentListener(documentListener);
            component.registerKeyboardAction(acceptAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
            int size = list.getModel().getSize();
            list.setVisibleRowCount(size<10 ? size : 10);

            int x = 0;
            try
			{
                int pos = Math.min(component.getCaret().getDot(), component.getCaret().getMark());
                x = component.getUI().modelToView(component, pos).x;
            }
			catch(BadLocationException e)
			{}
            popupMenu.show(component, x, component.getHeight());
        }
		else
            popupMenu.setVisible(false);
        component.requestFocus();
	}

	static Action showAction = new AbstractAction(){
        public void actionPerformed(ActionEvent e){
            JComponent tf = (JComponent)e.getSource();
            AutoCompleter completer = (AutoCompleter)tf.getClientProperty(AUTOCOMPLETER);
            if(tf.isEnabled()){
                if(completer.popupMenu.isVisible())
                    completer.selectNextPossibleValue();
                else
                    completer.showPopup();
            }
        }
    };

    static Action upAction = new AbstractAction(){
        public void actionPerformed(ActionEvent e){
            JComponent tf = (JComponent)e.getSource();
            AutoCompleter completer = (AutoCompleter)tf.getClientProperty(AUTOCOMPLETER);
            if(tf.isEnabled()){
                if(completer.popupMenu.isVisible())
                    completer.selectPreviousPossibleValue();
            }
        }
    };

    static Action hidePopupAction = new AbstractAction(){
        public void actionPerformed(ActionEvent e){
            JComponent tf = (JComponent)e.getSource();
            AutoCompleter completer = (AutoCompleter)tf.getClientProperty(AUTOCOMPLETER);
            if(tf.isEnabled())
                completer.popupMenu.setVisible(false);
        }
    };

    /**
     * Selects the next item in the list.  It won't change the selection if the
     * currently selected item is already the last item.
     */
    protected void selectNextPossibleValue(){
        int si = list.getSelectedIndex();

        if(si < list.getModel().getSize() - 1){
            list.setSelectedIndex(si + 1);
            list.ensureIndexIsVisible(si + 1);
        }
    }

    /**
     * Selects the previous item in the list.  It won't change the selection if the
     * currently selected item is already the first item.
     */
    protected void selectPreviousPossibleValue(){
        int si = list.getSelectedIndex();

        if(si > 0){
            list.setSelectedIndex(si - 1);
            list.ensureIndexIsVisible(si - 1);
        }
    }

	// update list model depending on the data in textfield
    protected abstract boolean updateListData();

    // user has selected some item in the list. update textfield accordingly...
    protected abstract void acceptedListItem(Object selected);

}
