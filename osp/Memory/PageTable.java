/**
 * @Author Weifeng
 * @StudentID 110161112
 */

package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
    /** 
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
        // your code goes here
        super(ownerTask);
        int array_size = (int) Math.pow(2, MMU.getPageAddressBits());
        pages = new PageTableEntry[array_size];

        for (int i=0; i<array_size; i++){
            pages[i] = new PageTableEntry(this, i);
        }
    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
        // your code goes here
        TaskCB taskCB = getTask();
        for (int i=0; i<MMU.getFrameTableSize(); i++){

            FrameTableEntry frameTableEntry = MMU.getFrame(i);
            //PageTableEntry pageTableEntry = null;


            if (frameTableEntry != null && frameTableEntry.getPage() != null && frameTableEntry.getPage().getTask() == taskCB){
                frameTableEntry.setPage(null);
                frameTableEntry.setDirty(false);
                frameTableEntry.setReferenced(false);
                if (frameTableEntry.getReserved() == taskCB){
                    frameTableEntry.setUnreserved(taskCB);
                }
            }
        }
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/

/*
    I pledge my honor that all parts of this project were done by me individually, without collaboration with anyone, and without consulting external sources that help with similar projects.
 */