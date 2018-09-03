package util;

import java.util.LinkedList;
import java.util.List;

public class UpdateList<Type>
{
	private List<Type> updatedList = new LinkedList<>();
	private List<Type> addings = new LinkedList<>();
	private List<Type> removals = new LinkedList<>();
	
	public UpdateList()
	{
		this(new LinkedList<Type>(), new LinkedList<Type>(), new LinkedList<Type>());
	}
	
	public UpdateList(List<Type> updatedList, List<Type> addingsTemp, List<Type> removalsTemp)
	{
		this.updatedList = updatedList;
		this.addings = addingsTemp;
		this.removals = removalsTemp;
	}


	/**
	 * Can be called by multiple thread
	 * Add the object to the adding temp list
	 */
	public synchronized void add(Type object)
	{
		this.addings.add(object);
	}

	/**
	 * Can be called by multiple thread
	 * Add the object to the removals temp list
	 */
	public synchronized void remove(Type object)
	{
		this.removals.remove(object);
	}

	/**
	 * Can be called by multiple thread
	 * Clear temp list
	 */
	public synchronized void clearTemp()
	{
		this.addings.clear();
		this.removals.clear();
	}
	
	/**
	 * Can be called by multiple thread
	 * Clear temp list
	 */
	public synchronized List<Type> getAddingsTempList()
	{
		return this.addings;
	}
	
	/**
	 * Can be called by multiple thread
	 * Clear temp list
	 */
	public synchronized List<Type> getRemovalsTempList()
	{
		return this.removals;
	}
	
	/**
	 * Should be called by a single thread or one at a time
	 */
	public synchronized void clearUpdated()
	{
		this.updatedList.clear();
	}
	
	/**
	 * Should be called by a single thread or one at a time
	 */
	public synchronized void update()
	{
		this.updatedList.addAll(this.addings);
		this.updatedList.removeAll(this.removals);
		this.clearTemp();
	}

	/**
	 * Should be called by a single thread or one at a time
	 */
	public synchronized List<Type> getList()
	{
		this.update();
		return this.updatedList;
	}
}
