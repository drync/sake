package com.drync.android;

import java.util.Comparator;

import org.restlet.Application;

import android.content.res.Resources;
import android.view.View;

import com.drync.android.objects.Bottle;

public class BottleComparator<T extends Bottle> implements Comparator<T>, OnSortListener {

	public static final String VARIETAL = "Varietal";
	public static final int BY_VARIETAL = 0;
	public static final String STYLE = "Style";
	public static final int BY_STYLE = 1;
	public static final String VINTAGE = "Vintage";
	public static final int BY_VINTAGE = 2;
	public static final String NAME="Name";
	public static final int BY_NAME = 3;
	public static final String ENTRY_DATE="Entry Date";
	public static final int BY_ENTRY_DATE = 4; 
	public static final String MY_RATING = "My Rating";
	public static final int BY_MY_RATING = 5;
	public static final String PRICE = "Price";
	public static final int BY_PRICE = 6;
	public static final String WINERY = "Winery";
	public static final int BY_WINERY = 7;
	//public static final int BY_DRINK_BY = 8; // not yet supported.
	
	protected static final CharSequence[] sortItems = 
			{ BottleComparator.VARIETAL,
			  BottleComparator.STYLE,
			  BottleComparator.VINTAGE,
			  BottleComparator.NAME,
			  BottleComparator.MY_RATING,
			  BottleComparator.PRICE,
			  BottleComparator.WINERY};
			
	protected static final CharSequence[] corkSortItems = 
	{ 
		BottleComparator.VARIETAL,
	  BottleComparator.STYLE,
	  BottleComparator.VINTAGE,
	  BottleComparator.NAME,
	  BottleComparator.MY_RATING,
	  BottleComparator.ENTRY_DATE,
	  BottleComparator.PRICE,
	  BottleComparator.WINERY};
	
	public int primarySort = BY_NAME;
	public int secondarySort = BY_NAME;
	
	public int compare(T arg0, T arg1) {
		// only do secondary sort if primary sort is the same.
		// start with primary:
		int result = doSort(primarySort, arg0, arg1);
		
		if (result == 0)
			result = doSort(secondarySort, arg0, arg1);		
		
		return result;
	}
	
	protected int doSort(int SORT_TYPE, T arg0, T arg1)
	{
		try
		{
			if (SORT_TYPE == BY_VINTAGE)
				return (((Integer)(arg0.getYear())).compareTo(((Integer)(arg1.getYear()))));
			else if (SORT_TYPE == BY_STYLE)
			{
				return compareStrings(arg0.getStyle(), arg1.getStyle());
			}
			else if (SORT_TYPE == BY_VARIETAL)
			{
				return compareStrings(arg0.getGrape(), arg1.getGrape());
			}
			else if (SORT_TYPE == BY_NAME)
			{
				return compareStrings(arg0.getName(), arg1.getName());
			}
			/*else if (SORT_TYPE == BY_ENTRY_DATE) // not yet supported.
			{
				return arg0.
			} */
			else if (SORT_TYPE == BY_MY_RATING)
			{
				return compareStringsAsFloats(arg0.getRating(), arg1.getRating());
			}
			else if (SORT_TYPE == BY_PRICE)
			{
				return compareStringsAsFloats(arg0.getPrice(), arg1.getPrice());
			}
			else if (SORT_TYPE == BY_WINERY)
			{
				return compareStrings(arg0.getWinery_name(), arg1.getWinery_name());
			}
			/*else if (SORT_TYPE == BY_DRINK_BY)
			{
				return arg0.get
			}*/
		}
		catch (NullPointerException e)
		{
			return -1;
		}
		return -1;
	}
	
	protected int compareStringsAsFloats(String sfloat1, String sfloat2)
	{
		float arg0float = 0; 
		try
		{
			if ((sfloat1 != null) && (!sfloat1.equals("")))
			{
				String sfloat1mod = sfloat1.replace("$", "");
				arg0float = Float.parseFloat(sfloat1mod);
			}
		}
		catch (NumberFormatException nfe)
		{
			arg0float = 0;
		}
		
		float arg1float = 0; 
		try
		{
			if ((sfloat2 != null) && (!sfloat2.equals("")))
			{
				String sfloat2mod = sfloat2.replace("$", "");
				arg1float = Float.parseFloat(sfloat2mod);
			}
		}
		catch (NumberFormatException nfe)
		{
			arg1float = 0;
		}
		
		return Float.compare(arg0float, arg1float);
	}
	
	protected int compareStrings(String arg0, String arg1)
	{
		if ((arg0 == null) || (arg1 == null))
		{
			if ((arg0 == null) && (arg1 == null))
				return 0;
			else if (arg0 == null)
				return -1;
			else
				return 1;
		}
		return (arg0.compareTo(arg1));
	}

	public BottleComparator(int primarySort) {
		super();
		this.primarySort = primarySort;
	}
	
	public BottleComparator() {
		super();
	}

	public static int SortNameToValue(String name)
	{
		if (name.equals(VARIETAL))
		{
			return BY_VARIETAL;
		}
		else if (name.equals(STYLE))
		{
			return BY_STYLE;
		}
		else if (name.equals(PRICE))
		{
			return BY_PRICE;
		}
		else if (name.equals(MY_RATING))
		{
			return BY_MY_RATING;
		}
		else if (name.equals(NAME))
		{
			return BY_NAME;
		}
		else if (name.equals(VINTAGE))
		{
			return BY_VINTAGE;
		}
		else if (name.equals(WINERY))
		{
			return BY_WINERY;
		}
		else if (name.equals(ENTRY_DATE))
		{
			return BY_ENTRY_DATE;
		}
		
		return BY_NAME;
	}
	
	public static String SortValueToName(int value)
	{
		
		switch (value)
		{
		case BY_VARIETAL:
			return VARIETAL;
		case BY_STYLE:
			return STYLE;
		case BY_VINTAGE:
			return VINTAGE;
		case BY_MY_RATING:
			return MY_RATING;
		case BY_NAME:
			return NAME;
		case BY_PRICE:
			return PRICE;
		case BY_WINERY:
			return WINERY;
		case BY_ENTRY_DATE:
			return ENTRY_DATE;
		default:
			return NAME;
		}
	}

	@Override
	public boolean onSortChanged(View arg0, int sortType) {
		primarySort = sortType;
		return true;
	}
	
	public static CharSequence[] getSortableItems()
	{
		return sortItems;
	}
	
	public static CharSequence[] getSortableCorkItems()
	{
		return corkSortItems;
	}
}
