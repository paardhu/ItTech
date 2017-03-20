package com.examplefoobar.utils;

import javax.annotation.concurrent.ThreadSafe;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe container that stores a group ID and members.
 *
 * It can be added <tt>Member</tt> and return a member list as String.
 * Also, it can start and stop a background task that writes a member list to specified files.
 *
 * This class is called a lot, so we need improve it.
 */
@ThreadSafe
public class PoorGroup
{
	//if you make the data private will you be able to control access to that data by controlling access to the code that manipulates the data.
    private String groupId;
    private HashSet<Member> members;
    boolean shouldStop;
    //Final keyword improves performance. Not just JVM can cache final variable but also application can cache frequently use final variables
    private static final AtomicInteger atomicInteger = new AtomicInteger(10);

    class Member
    {
	 //if you make the data private will you be able to control access to that data by controlling access to the code that manipulates the data.
        private String memberId;
        private Integer age;//changed it from int to Integer -  An immutable object is one whose state can't be changed once the object is created.

        Member(String memberId, int age)
        {
            this.memberId = memberId;
            this.age = age;
        }

        public String getMemberId()
        {
            return memberId;
        }

        public int getAge()
        {
            return age;
        }

        public boolean equals(Object o)
        {
            // If `memberId` matches the other's one, they should be treated as the same `Member` objects.
            Member member = (Member) o;
            return this.memberId == member.memberId;
        }
    }

    public PoorGroup(String groupId)
    {
        this.groupId = groupId;
        this.members = new HashSet<>();
    }

    public void addMember(Member member)
    {
        members.add(member);
    }

    /*
    This method is not thread-safe because age *= 10 is not an atomic operation,
    -If we use synchronized keyword so that only one thread can execute it at a time which removes possibility of coinciding or interleaving.
    -Using Atomic Integer, which makes arithmetic operation(*) atomic, and since atomic operations are thread-safe and saves cost of external synchronization.
    - Finally, This method is thread-safe now because of locking and synchornization.
   */ 
    public synchronized String getMembersAsStringWith10xAge()
    {
    	StringBuffer buf = new StringBuffer();// changed from String to StringBuffer to make it threadSafe
        for (Member member : members)
        {
            Integer age = member.getAge();
            // Don't ask the reason why `age` should be multiplied ;)    
            Integer atomicval = atomicInteger.get();
            age *= atomicval;
            buf = buf.append(String.format("memberId=%s, age=%d¥n", member.getMemberId(), age));
        }
        return buf.toString();
    }

    /**
     * Run a background task that writes a member list to specified files 10 times in background thread
     * so that it doesn't block the caller's thread.
     */
    public synchronized void startLoggingMemberList10Times(final String outputFilePrimary, final String outputFileSecondary)
    {
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                int i = 0;
                while (!shouldStop)
                {
                    if (i++ >= 10)
                        break;

                    FileWriter writer0 = null;
                    FileWriter writer1 = null;
                    try {
                        writer0 = new FileWriter(new File(outputFilePrimary));
                        writer0.append(PoorGroup.this.getMembersAsStringWith10xAge());

                        writer1 = new FileWriter(new File(outputFileSecondary));
                        writer1.append(PoorGroup.this.getMembersAsStringWith10xAge());
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Unexpected error occurred. Please check these file names. outputFilePrimary="
                                + outputFilePrimary + ", outputFileSecondary=" + outputFileSecondary);
                    }
                    finally {
                        try {
                            if (writer0 != null)
                                writer0.close();

                            if (writer1 != null)
                                writer1.close();
                        }
                        catch (Exception e) {
                            // Do nothing since there isn't anything we can do here, right?
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * Stop the background task started by <tt>startPrintingMemberList()</tt>
     * Of course, <tt>startLoggingMemberList</tt> can be called again after calling this method.
     */
    public void stopPrintingMemberList()
    {
        shouldStop = true;
    }
    
 /*public static void main(String args[]){
    	
    	PoorGroup pgroup = new PoorGroup("threadGroup");
    	
    	for(int i=1;i<=10;i++){
    		String memid ="thread";
    		Integer age = 20;
    		memid+=i;
    		age+=i;
    		Member  m1 = pgroup.new Member(memid,age);
    		pgroup.addMember(m1);
    	}
    	
    	pgroup.startLoggingMemberList10Times("C:\\pardhu\\testFile1.txt","C:\\pardhu\\testFile2.txt");
    	//pgroup.stopPrintingMemberList();
    	pgroup.startLoggingMemberList10Times("C:\\pardhu\\testFile3.txt","C:\\pardhu\\testFile4.txt");
    }*/
}
