/**
 * @Author Weifeng
 * @StudentID 110161112
 */

package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {
        // your code goes here

		if (page.isValid()){
			return FAILURE;
		}

		MyOut.print(thread, "Current page is "+page+ ", and page.isValid()="+page.isValid());
		FrameTableEntry frame = getFramebyLRU();

		if (frame == null){
			return NotEnoughMemory;
		}

		SystemEvent event = new SystemEvent("PageFault");
		thread.suspend(event);

		if (thread.getStatus() == ThreadKill){
			return FAILURE;
		}
		
		page.setValidatingThread(thread);
		frame.setReserved(thread.getTask());
		
		PageTableEntry frame_page = frame.getPage();
		
		if (frame_page != null){
			
			if (frame.isDirty()){//dirty page, swap out
				swap_out(frame, thread);

				if (thread.getStatus()==ThreadKill){
					page.notifyThreads();
					event.notifyThreads();
					ThreadCB.dispatch();
					return FAILURE;
				}
			}

			//clean page, free it
			frame.setReferenced(false);
			frame.setPage(null);
			frame.setDirty(false);
			frame_page.setValid(false);
			frame_page.setFrame(null);
		}

		//perform swap in
		page.setFrame(frame);
		swap_in(page, thread);
		page.setTimestamp(HClock.get());
		if (thread.getStatus()==ThreadKill){
			page.notifyThreads();
			page.setValidatingThread(null);
			event.notifyThreads();
			ThreadCB.dispatch();
			return FAILURE;
		}

		frame.setPage(page);
		page.setValid(true);

		frame.setUnreserved(thread.getTask());
		page.setValidatingThread(null);
		page.notifyThreads();
		event.notifyThreads();
		ThreadCB.dispatch();
		return SUCCESS;

		


    }

	private static void swap_out(FrameTableEntry frame, ThreadCB thread) {
    	PageTableEntry page = frame.getPage();
    	TaskCB task = page.getTask();
    	OpenFile swapfile = task.getSwapFile();
    	swapfile.write(page.getID(), page, thread);
	}

	private static void swap_in(PageTableEntry page, ThreadCB thread) {
		TaskCB task = page.getTask();
		OpenFile swapFile = task.getSwapFile();
		swapFile.read(page.getID(), page, thread);
	}


	private static FrameTableEntry getFramebyLRU(){
    	FrameTableEntry tempframe = null;
    	FrameTableEntry oldestFrame = null;
    	int frameTableSize = MMU.getFrameTableSize();

    	for (int i=0; i <frameTableSize; i++){
    		//looking for free frame
			oldestFrame = MMU.getFrame(i);
			if (oldestFrame.getPage()==null && !oldestFrame.isReserved() && oldestFrame.getLockCount()==0){
				return oldestFrame;
			}
		}

//		for (int i=0; i <frameTableSize; i++){
//			//no free frame, but clean frame, this clean frame should be freed.
//			oldestFrame = MMU.getFrame(i);
//			if (!oldestFrame.isDirty() && !oldestFrame.isReserved() && oldestFrame.getLockCount()==0){
//				return oldestFrame;
//			}
//		}


    	int isUpdated = 0;

    	int index = 0;
		tempframe = MMU.getFrame(index);
    	PageTableEntry temp_page = tempframe.getPage();//picking smallest not null timestamp
		while(temp_page == null){
			index++;
			tempframe = MMU.getFrame(index);
			temp_page = tempframe.getPage();
		}




		for (int i=0; i <frameTableSize; i++){
			//at this point, no free frames, use LRU
			FrameTableEntry frameTableEntry = MMU.getFrame(i);
			//PageTableEntry pageTableEntry = frameTableEntry.getPage();
			if (!frameTableEntry.isReserved() && frameTableEntry.getLockCount()==0) {
				if (temp_page.getTimestamp() >= frameTableEntry.getPage().getTimestamp()) {
					if (!frameTableEntry.isReserved() && frameTableEntry.getLockCount() == 0) {
						//flag here
						isUpdated++;
						tempframe = frameTableEntry;
						temp_page = frameTableEntry.getPage();
					}
				}
			}
		}

		if (isUpdated>0){
			oldestFrame = tempframe;
			return oldestFrame;
		}else {
			return null;
		}

	}//end of method


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