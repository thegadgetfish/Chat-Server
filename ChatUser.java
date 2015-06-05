/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatserver;

import java.net.Socket;

/**
 *
 * @author Khuu
 */
public class ChatUser {
    private int _num;
    private String _userName = "";
    private ChatRoom _currRoom;
    private ChatServer _thread;
    
    public ChatUser(int num, ChatServer thread)
    {
        _num = num;
        _userName = "";
        _currRoom = null;
        _thread = thread;
    }
    
    public Boolean inChatRoom()
    {
        if(_currRoom != null)
            return true;
        else
            return false;
    }
    public void setChatRoom(ChatRoom room) { _currRoom = room; }
    public ChatRoom getChatRoom() { return _currRoom;}
    
    public ChatServer getThread() { return _thread;}
    
    public void setName(String name) { _userName = name;}
    public String getName() { return _userName; }
    
    public int getNum() { return _num; }
}
