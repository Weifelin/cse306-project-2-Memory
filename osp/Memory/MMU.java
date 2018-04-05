/**
 * @Author Weifeng
 * @StudentID 110161112
 */

package osp.Memory;

import java.util.*;

import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /** 
        This method is called once before the simulation starts. 
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {
        // your code goes here
        for (int i = 0; i<MMU.getFrameTableSize(); i++){
            MMU.setFrame(i, new FrameTableEntry(i));
        }

        Daemon.create("swapping out", new CleanDaemon(), 20000);
    }

    /**
       This method handlies memory references. The method must 
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault 
       by making an interrupt if the page is invalid, finally, 
       if the page is still valid, i.e., not swapped out by another 
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue, 
       and it is possible that some other thread will take away the frame.)
       
       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform 
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,
					  int referenceType, ThreadCB thread)
    {
        // your code goes here
        int offset_length = MMU.getVirtualAddressBits() - MMU.getPageAddressBits();
        int page_size = (int) Math.pow(2, offset_length);
        int page_index = memoryAddress/page_size;

        PageTableEntry page = thread.getTask().getPageTable().pages[page_index];

        if(page.isValid()){
            page.getFrame().setReferenced(true);
            if(referenceType == MemoryWrite){
                page.getFrame().setDirty(true);
            }
            return page;
        }else {

            if(page.getValidatingThread() != null){
                //case 1: some other thread of the same task has already caused a pagefault
                // and the page is already on its way to main memory.

                thread.suspend(page);

            }else {
                //case 2: No other thread caused a pagefault on this invalid page.
                InterruptVector.setPage(page);
                InterruptVector.setReferenceType(referenceType);
                InterruptVector.setThread(thread);
                CPU.interrupt(PageFault);

            }


        }

        if(thread.getStatus() == ThreadKill){
            page.setTimestamp(HClock.get());
            return page;
        }

        page.getFrame().setReferenced(true);
        if(referenceType == MemoryWrite){
            page.getFrame().setDirty(true);
        }
        page.setTimestamp(HClock.get());
        return page;

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
     
	@OSPProject Memory
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
      @OSPProject Memory
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}




class CleanDaemon implements DaemonInterface{

    @Override
    public void unleash(ThreadCB threadCB) {
        FrameTableEntry[] frame_array = get5DirtyFramesbyLRU();

        for (int i=0; i<5; i++){
            if (frame_array[i] != null){
                frame_array[i].setflag(false);
                swap_out(frame_array[i], threadCB);
                frame_array[i].setDirty(false);
            }
        }

    }

    private static void swap_out(FrameTableEntry frame, ThreadCB thread) {
        PageTableEntry page = frame.getPage();
        TaskCB task = page.getTask();
        OpenFile swapfile = task.getSwapFile();
        swapfile.write(page.getID(), page, thread);
    }

    private static FrameTableEntry[] get5DirtyFramesbyLRU(){
        FrameTableEntry tempframe = null;
        FrameTableEntry oldestFrame = null;
        int frameTableSize = MMU.getFrameTableSize();

        FrameTableEntry[] frame_array = new FrameTableEntry[5];

        for (int i=0; i<5; i++){
            frame_array[i]=null;
        }

        for (int i=0; i<5; i++) {
            tempframe = MMU.getFrame(0);
            int isUpdated = 0;
            PageTableEntry temp_page = tempframe.getPage();//picking smallest timestamp
            for (int j = 0; j < frameTableSize; j++) {
                FrameTableEntry frameTableEntry = MMU.getFrame(i);
                PageTableEntry pageTableEntry = frameTableEntry.getPage();
                if (!frameTableEntry.isReserved() && frameTableEntry.getLockCount() == 0 && frameTableEntry.getPage() != null && frameTableEntry.isDirty() && !frameTableEntry.getFlag()) {
                    if (temp_page.getTimestamp() >= pageTableEntry.getTimestamp()) {
                        if (!frameTableEntry.isReserved() && frameTableEntry.getLockCount() == 0 && frameTableEntry.getPage() != null && frameTableEntry.isDirty() && !frameTableEntry.getFlag()) {
                            //flag here
                            isUpdated++;
                            tempframe = frameTableEntry;
                            temp_page = pageTableEntry;
                        }
                    }
                }
            }

            if (isUpdated > 0) {
                oldestFrame = tempframe;
                frame_array[i]=oldestFrame;
            }
        }

        return frame_array;

    }//end of method
}



/*
      Feel free to add local classes to improve the readability of your code
*/

/*
    I pledge my honor that all parts of this project were done by me individually, without collaboration with anyone, and without consulting external sources that help with similar projects.
 */