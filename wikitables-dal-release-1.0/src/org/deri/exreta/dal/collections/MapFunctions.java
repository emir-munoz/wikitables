package org.deri.exreta.dal.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MapFunctions
{
	public MapFunctions()
	{
	}

	public static <T, E> List<CustomEntry> convertMapToList(Map<T, E> map)
	{
		List<CustomEntry> list = new ArrayList<CustomEntry>();
		Set<Entry<T, E>> entrySet = map.entrySet();
		Iterator<Entry<T, E>> iterator = entrySet.iterator();
		while (iterator.hasNext())
		{
			Map.Entry<T, E> entry = (Map.Entry<T, E>) iterator.next();
			CustomEntry customEntry = new CustomEntry(entry);
			list.add(customEntry);
		}
		return list;
	}
}
