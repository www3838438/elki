package de.lmu.ifi.dbs.data;

import java.util.BitSet;
import java.util.Comparator;

/**
 * BitSetComparator to compare BitSets.
 * @author Arthur Zimek
 */
public class BitSetComparator implements Comparator<BitSet>
{

    /**
     * Compares to BitSets b1 and b2.
     * b1 is less than b2 if it is smaller and vice versa.
     * If b1 and b2 are of equal size, the BitSet
     * will be treated as less than the other,
     * that has first a smaller index of a set bit.
     * 
     * @see java.util.Comparator#compare(Object, Object)
     */
    public int compare(BitSet b1, BitSet b2)
    {
        if(b1.size() < b2.size())
        {
            return -1;
        }
        else if(b1.size() > b2.size())
        {
            return 1;
        }
        else
        {            
            int i1 = 0;
            int i2 = 0;
            while(i1 >= 0 && i2 >= 0)
            {
                i1 = b1.nextSetBit(i1);
                i2 = b2.nextSetBit(i2);
                if(i1 < i2)
                {
                    return -1;
                }
                else if(i1 > i2)
                {
                    return 1;
                }
            }
        }
        return 0;
    }

}
