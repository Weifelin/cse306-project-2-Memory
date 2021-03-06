/**
 * @Author Weifeng
 * @StudentID 110161112
 */

package osp.Memory;

import osp.Hardware.*;
import osp.Devices.*;
import osp.IFLModules.*;
import osp.Threads.ThreadCB;

/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.
   
   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);
	   
       as its first statement.

       @OSPProject Memory
    */

    private long timestamp;
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
        // your code goes here
        super(ownerPageTable, pageNumber);
        timestamp = HClock.get();

    }

    /**
       This method increases the lock count on the page by one. 

	The method must FIRST increment lockCount, THEN  
	check if the page is valid, and if it is not and no 
	page validation event is present for the page, start page fault 
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the 
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
    {
        // your code goes here
        ThreadCB threadCB = iorb.getThread();
        this.setTimestamp(HClock.get());
        //getFrame().incrementLockCount();

        int ret = SUCCESS;

        if(!isValid()){

            if(getValidatingThread() == null){
            //pagefault occurs when getValidatingThread returns null.
                ret = PageFaultHandler.handlePageFault(threadCB, MemoryLock, this);

            }else if(getValidatingThread().getID() != threadCB.getID()){
//                getFrame().incrementLockCount();
//                return SUCCESS;
                threadCB.suspend(this);
            }
        }

        if(threadCB.getStatus() == ThreadKill || ret == FAILURE){
            return FAILURE;
        }

        getFrame().incrementLockCount();
        return SUCCESS;

    }

    /** This method decreases the lock count on the page by one. 

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {
        // your code goes here
        if(getFrame().getLockCount() > 0){
            getFrame().decrementLockCount();
        }

    }



    public long getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
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