package de.lmu.ifi.dbs.converter;

import de.lmu.ifi.dbs.data.ClassLabel;

/**
 * A nominal attribute.
 * The value is supposed to be a class label.
 * 
 * @author Arthur Zimek
 */
public class WekaNominalAttribute<L extends ClassLabel<L>> extends WekaAbstractAttribute<WekaNominalAttribute<L>>
{
    /**
     * Holds the value.
     */
    private L value;
    
    /**
     * Sets the value as a nominal attribute.
     * @param value the value of the attribute
     */
    public WekaNominalAttribute(L value)
    {
        super(NOMINAL);
        this.value = value;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.converter.WekaAttribute#getValue()
     */
    public String getValue()
    {
        return value.toString();
    }

    /**
     * Two nominal attributes are compared by their values.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(WekaNominalAttribute<L> o)
    {
        return this.value.compareTo(o.value);
    }

}
