package com.drync.android.widget;

import android.content.Context;
import android.text.Selection;
import android.util.AttributeSet;
import android.view.inputmethod.CompletionInfo;
import android.widget.AutoCompleteTextView;

public class CustomAutoCompleteTextView extends AutoCompleteTextView {

	public CustomAutoCompleteTextView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public CustomAutoCompleteTextView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public CustomAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}

	@Override
	public boolean enoughToFilter() {
		String currentText = this.getText().toString();
		int i = currentText.lastIndexOf(" ");
		
		if (i > 0)
		{
			if (currentText.length() > i+1)
				return true;
			else
				return false;
		}
		else
		{
			if (currentText.length() >= 1)
				return true;
		}
		
		return super.enoughToFilter();
	}

	@Override
	protected void replaceText(CharSequence text) {
		
		String currentText = this.getText().toString();
		int i = currentText.lastIndexOf(" ");
		CharSequence curText = currentText;
		if (i >= 0)
			curText = currentText.subSequence(0, i+1);
		else
			curText = "";
		
		StringBuilder bldr = new StringBuilder(curText);
		bldr.append(text);
		this.setText(bldr.toString());
		this.setSelection(this.getText().length());
		/*CharSequence appendText = text.subSequence(currentText.length()-i-1, text.length());
		super.append(appendText);*/
	}
}
