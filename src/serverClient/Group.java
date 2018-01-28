package serverClient;

import java.util.ArrayList;

public class Group {
    //variabelen
    private String name;
    private String owner;
    private ArrayList<String> users;

    public Group(String name, String owner) {
        this.name = name;
        this.owner = owner;
        this.users = new ArrayList<>();
        addUser(owner);
    }

    //getters en setters/adders
    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner){
        this.owner = owner;
    }

    public ArrayList<String> getUsers() {
        return users;
    }

    public void addUser(String username){
        users.add(username);
    }

    public void removeUser(String username){
        users.remove(username);
    }
}
