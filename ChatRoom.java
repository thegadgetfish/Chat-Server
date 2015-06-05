/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatserver;

import java.util.ArrayList;

/**
 *
 * @author Khuu
 */

public class ChatRoom 
{
    //List of all the users in the room
    private ArrayList<ChatUser> _chatUsers;
    private String _roomName;
    
    public ChatRoom(String name)
    {
        _roomName = name;
        _chatUsers = new ArrayList<ChatUser>();
    }
    
    public void addUser(ChatUser user)
    {
        _chatUsers.add(user);
    }
    
    public void removeUser(ChatUser user)
    {
        _chatUsers.remove(user);
    }
    
    public String getName() { return _roomName; }
    public ArrayList<ChatUser> getUsers(){ return _chatUsers; }
    public int getNumUsers(){ return _chatUsers.size(); }

}
