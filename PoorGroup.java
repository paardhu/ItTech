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
    String groupId;
    HashSet<Member> members;
    boolean shouldStop;
    //Final keyword improves performance. Not just JVM can cache final variable but also application can cache frequently use final variables
    public static final AtomicInteger atomicInteger = new AtomicInteger(10);

   static class Member
    {
        String memberId;
        int age;

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
    - Finally, This method thread-safe now because of locking and synchornization.
   */ 
    public synchronized String getMembersAsStringWith10xAge()
    {
        String buf = "";
        for (Member member : members)
        {
            Integer age = member.getAge();
            // Don't ask the reason why `age` should be multiplied ;)    
            int atomicval = atomicInteger.get();
            age *= atomicval;
            buf += String.format("memberId=%s, age=%d¥n", member.getMemberId(), age);
        }
        return buf;
    }

    /**
     * Run a background task that writes a member list to specified files 10 times in background thread
     * so that it doesn't block the caller's thread.
     */
    public void startLoggingMemberList10Times(final String outputFilePrimary, final String outputFileSecondary)
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
}
