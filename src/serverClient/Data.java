package serverClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Data {
    //alle data voor goepen
    private static Data data = null;
    private ArrayList<Group> groups;
    private ServerConfiguration conf;
    private Set threads;

    private Data() {
        groups = new ArrayList<>();
        conf = new ServerConfiguration();
        threads = new HashSet();
    }

    public static Data getInstance(){
        if (data == null){
            data = new Data();
        }
        return data;
    }

    public void setConf(ServerConfiguration conf){
        this.conf = conf;
    }

    public void setThreads(Set threads) {
        this.threads = threads;
    }

    public Set getThreads() {
        return threads;
    }

    public void addGroup(Group group){
        groups.add(group);
    }

    public void removeGroup(Group group){
        groups.remove(group);
    }

    public ArrayList<Group> getGroups() {
        return groups;
    }

    public ServerConfiguration getConf() {
        return conf;
    }
}
