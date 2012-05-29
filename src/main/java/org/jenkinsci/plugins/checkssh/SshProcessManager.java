package org.jenkinsci.plugins.checkssh;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.util.ProcessTree;
import hudson.util.ProcessTree.OSProcess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;


/**
 * 
 * @author Lucie Votypkova
 */

public class SshProcessManager implements Describable<SshProcessManager>{
    
    private String userName;
    
    public SshProcessManager(String userName){
        this.userName=userName;
    }

    /**
     * Find all ssh processes on Jenkins Master, which connect to a given hostname
     * 
     * @param hostname of computer on which is the ssh process connect to
     * @throw IOException and InterruptedException
     */
    public List<OSProcess> getSshProcessForSlave(String hostName) throws IOException, InterruptedException{
        List<OSProcess> processes = new ArrayList<OSProcess>();
        if(userName==null){
            return processes;
        }
        Iterator<OSProcess> iterator = ProcessTree.get().iterator();
        while(iterator.hasNext()){                  
                   OSProcess p= iterator.next();
                   if(p.getArguments().contains("ssh")){
                       for(String s : p.getArguments()){                           
                            if(s.equals(userName + "@" + hostName)){
                                processes.add(p);
                                break;
                            }
                       }
                   }
        }
        return processes;
    }
    
    /**
     * Return instance of class SshProcessManager
     * 
     */
    public static SshProcessManager getManager(){
        DescriptorImpl descriptor = (DescriptorImpl) Hudson.getInstance().getDescriptor(SshProcessManager.class); 
        return descriptor.getManager();
    }
    
    public String getUserName(){
        return userName;
    }
    
    /**
     * Try to kill given process and its subprocesses
     * 
     * @return true if the process was killed, false if not.
     */
    public boolean killSshProcess(OSProcess process) throws InterruptedException, IOException{  
        int pid = process.getPid();
        String command ="kill -s 9 "+pid;
        String arguments[] = command.split(" ");
        ProcessBuilder pb = new ProcessBuilder(arguments);
        Process p = pb.start();
        for(int i=0;i<5;i++){ 
            if(ProcessTree.get().get(pid)==null)
                return true;
            Thread.sleep(100);// wait for command excution
        }
        return false;
    }

    public Descriptor<SshProcessManager> getDescriptor() {
        return (Descriptor<SshProcessManager>) Hudson.getInstance().getDescriptor(getClass());
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<SshProcessManager>{
        private SshProcessManager sshManager;
        
        public DescriptorImpl(){
            load();
            if(sshManager==null){
                sshManager = new SshProcessManager(null);
            }
        }

        @Override
        public String getDisplayName() {
            return "Ssh process check";
        }
        
        public SshProcessManager getManager(){
            return sshManager;
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) {
            SshProcessManager manager = new SshProcessManager(req.getParameter("userName"));
            sshManager=manager;
            save();
            return true;
        }
        
    }
    
   

}

