/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.widget;

import com.drync.android.R;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;

public class ClearableSearch extends RelativeLayout implements OnClickListener,
        OnFocusChangeListener {

    private static final String TAG = "ClearableSearch";

    public interface OnChangedListener {
        void onChanged(ClearableSearch picker, int oldVal, int newVal);
    }
    

    private final EditText mText;
    private final ImageButton searchButton;
    private final ImageButton clearButton;
    private boolean commitOnClear = true;

    
    public boolean isCommitOnClear() {
		return commitOnClear;
	}

	public void setCommitOnClear(boolean commitOnClear) {
		this.commitOnClear = commitOnClear;
	}

	protected int mStart;
    protected int mEnd;
    protected int mCurrent;
    protected int mPrevious;
    private OnChangedListener mListener;
    private OnCommitListener mCommitListener;
   
    public ClearableSearch(Context context) {
        this(context, null);
    }

    public ClearableSearch(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    protected void inflateLayout(Context context)
    {
    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.clearablesearch, this, true);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public ClearableSearch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        
        inflateLayout(context);
        
        mText = (EditText) findViewById(R.id.searchentry);
        clearButton = (ImageButton)findViewById(R.id.clearFilterBtn);
        searchButton = (ImageButton)findViewById(R.id.searchBtn);
        
        clearButton.setOnClickListener(this);
        searchButton.setOnClickListener(this);
        
        mText.addTextChangedListener(new ClearableSearchTextWatcher());
        mText.setOnKeyListener(new OnKeyListener(){

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
						(keyCode == KeyEvent.KEYCODE_ENTER)) {
					notifyCommitListeners();
					return true;
				}
				return false;
			}});
        
        mText.setOnFocusChangeListener(this);

        if (!isEnabled()) {
            setEnabled(false);
        }
    }

    public void setText(String text)
    {
    	mText.setText(text);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        mText.setEnabled(enabled);
    }

    public void setOnChangeListener(OnChangedListener listener) {
        mListener = listener;
    }
    
    public Editable getEditableText()
    {
    	return mText.getEditableText();
    }

    public void onClick(View v) {
        if (!mText.hasFocus()) mText.requestFocus();

        if (R.id.clearFilterBtn == v.getId()) {
            mText.getEditableText().clear();
            if (commitOnClear)
            	notifyCommitListeners();
        }
        else if (R.id.searchBtn == v.getId()) {
                notifyCommitListeners();
        }
    }

    public void onFocusChange(View v, boolean hasFocus) {

        /* When focus is lost check that the text field
         * has valid values.
         */
        if (!hasFocus) {
  
        }
    }
    
    public void setOnCommitListener(OnCommitListener mCommitListener) {
		this.mCommitListener = mCommitListener;
	}

	public OnCommitListener getOnCommitListener() {
		return mCommitListener;
	}
	
	private void notifyCommitListeners()
	{
		if (mCommitListener != null)
		{
			this.mCommitListener.onCommit(this, this.getEditableText().toString());
		}
	}

	public interface OnCommitListener
    {
    	public boolean onCommit(View arg0, String text);
    }
	
	private class ClearableSearchTextWatcher implements TextWatcher
	{

		public void afterTextChanged(Editable s) {
			if (s.length() == 0)
			{
				ClearableSearch.this.clearButton.setEnabled(false);
			}
			else
			{
				ClearableSearch.this.clearButton.setEnabled(true);
			}
			
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub
			
		}
		
	}
}