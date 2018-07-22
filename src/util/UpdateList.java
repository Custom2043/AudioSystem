package util;

import java.util.ArrayList;

public class UpdateList<Type>
{
	private ArrayList<Type> buf = new ArrayList<>(); // Utilisé pendant que l'opérateur utilise getList();
	private ArrayList<Type> vrai = new ArrayList<>();
	/**
	 * Used by thread 1
	 */
	public synchronized void add(Type t)
	{
		this.buf.add(t);
	}
	/**
	 * Used by thread 2
	 */
	public synchronized void update()
	{
		this.vrai.addAll(this.buf);
		this.buf.clear();
	}
	/**
	 * Used by thread 2
	 */
	public ArrayList<Type> getList()
	{
		this.update();
		return this.vrai;
	}
	/**
	 * Used by thread 1
	 */
	public synchronized ArrayList<Type> getImage()
	{
		ArrayList<Type> image = new ArrayList<>();
		image.addAll(this.vrai);
		image.addAll(this.buf);
		return image;
	}
}
