package com.drync.android;

import java.util.Comparator;

import com.drync.android.objects.Bottle;

public class BottleComparator implements Comparator<Bottle> {

	public static final int BY_VARIETAL = 0;
	public static final int BY_STYLE = 1;
	public static final int BY_VINTAGE = 2;
	
	public int primarySort = BY_VINTAGE;
	public int secondarySort = BY_VARIETAL;
	
	public int compare(Bottle arg0, Bottle arg1) {
		// only do secondary sort if primary sort is the same.
		// start with primary:
		int result = doSort(primarySort, arg0, arg1);
		
		if (result == 0)
			result = doSort(secondarySort, arg0, arg1);		
		
		return result;
	}
	
	private int doSort(int SORT_TYPE, Bottle arg0, Bottle arg1)
	{
		try
		{
			if (SORT_TYPE == BY_VINTAGE)
				return (((Integer)(arg0.getYear())).compareTo(((Integer)(arg1.getYear()))));
			else if (SORT_TYPE == BY_STYLE)
			{
				if ((arg0.getStyle() == null) || (arg1.getStyle() == null))
				{
					if ((arg0.getStyle() == null) && (arg1.getStyle() == null))
						return 0;
					else if (arg0.getStyle() == null)
						return -1;
					else
						return 1;
				}
				else
					return arg0.getStyle().compareTo(arg1.getStyle());
			}
			else if (SORT_TYPE == BY_VINTAGE)
			{
				if ((arg0.getGrape() == null) || (arg1.getGrape() == null))
				{
					if ((arg0.getGrape() == null) && (arg1.getGrape() == null))
						return 0;
					else if (arg0.getGrape() == null)
						return -1;
					else
						return 1;
				}
			}
			return (arg0.getGrape().compareTo(arg1.getGrape()));
		}
		catch (NullPointerException e)
		{
			return -1;
		}
	}

}
