/**
 * @Author Weifeng
 * @StudentID 110161112
 */
package osp.Memory;

/**
    The FrameTableEntry class contains information about a specific page
    frame of memory.

    @OSPProject Memory
*/
import osp.Tasks.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.IflFrameTableEntry;

public class FrameTableEntry extends IflFrameTableEntry
{
    /**
       The frame constructor. Must have

       	   super(frameID)
	   
       as its first statement.

       @OSPProject Memory
    */

    private boolean daemon_flag = false;

    public FrameTableEntry(int frameID)
    {
        // your code goes here
        super(frameID);

    }

    public boolean getFlag(){
        return daemon_flag;
    }

    public void setflag(boolean daemon_flag){
        this.daemon_flag = daemon_flag;
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