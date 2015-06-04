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
    private final int _num;
    private String _userName = "";
    
    public ChatUser(int num)
    {
        _num = num;
        _userName = "";
    }
    
    public ChatUser(int num, String userName)
    {
        _num = num;
        _userName = userName;
    }
    
    public void setName(String name) { _userName = name;}
    public String getName() { return _userName; }
    public int getNum() { return _num; }
}
