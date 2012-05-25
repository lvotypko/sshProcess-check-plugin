package org.jenkinsci.plugins.checkssh;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import hudson.util.ProcessTree.OSProcess;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Lucie Votypkova
 */
@Extension
public class CheckSshProcesses extends ComputerListener {

    private Map<String, String> hostNames = new TreeMap<String, String>();

    @Override
    public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        
        SshProcessManager manager = SshProcessManager.getManager();
        if(manager.getUserName()==null)
            return;
        String hostName = hostNames.get(c.getDisplayName());
        if (hostName != null) {
            taskListener.getLogger().println("Checking ssh processes with argument " + manager.getUserName()+"@"+hostName); 
            List<OSProcess> processes = manager.getSshProcessForSlave(hostName);
            if(processes.isEmpty()){
                taskListener.getLogger().println("No ssh processes");
            }
            for (OSProcess process : processes) {
                taskListener.getLogger().println("Process " + process.getPid() + " with arguments " + process.getArguments() + "was founded");
                taskListener.getLogger().println("Trying to kill process");
                if (!manager.killSshProcesses(process)) {
                    taskListener.getLogger().println("Attempt to kill a process with UID " + process.getPid() + " was not successful");
                } else {
                    taskListener.getLogger().println("Process " + process.getPid() + " was killed");
                }
            }
        }
    }
    

    @Override
    public void onOnline(Computer c, TaskListener taskListener) {
        String host = null;
        try {
            host = c.getEnvironment().get("HOSTNAME");
        } catch (IOException ex) {
            Logger.getLogger(CheckSshProcesses.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(CheckSshProcesses.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(host !=null){
            synchronized (hostNames) {          
                hostNames.put(c.getDisplayName(), host);          
            }
        }
      
    }
   
    
    @Override
    public void onConfigurationChange(){
        Map<String,String> actualized=new TreeMap<String,String>();
        for(Node node:Hudson.getInstance().getNodes()){
            Computer c = node.toComputer();
            if(c==null)
                continue;
            if(c.isOnline()){
                String host=null;
                try {
                    host = c.getEnvironment().get("HOSTNAME");
                } catch (IOException ex) {
                    Logger.getLogger(CheckSshProcesses.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(CheckSshProcesses.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(host!=null){
                    actualized.put(c.getName(), host);
                }
            }
                if(c.getChannel()==null){
                    SshProcessManager manager = SshProcessManager.getManager();
                    String host = hostNames.get(c.getName());
                    if(host!=null){
                        try {
                            for (OSProcess process : manager.getSshProcessForSlave(host)) {
                                manager.killSshProcesses(process);
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(CheckSshProcesses.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(CheckSshProcesses.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        hostNames = actualized;
    }
       
}
