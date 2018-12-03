/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magazin;

import java.io.Serializable;

/**
 *
 * @author yo5bdm
 */
public class Client implements Serializable {
    
    public int id;
    public String username;
    public boolean logat;

    Client(String[] elem) {
        this.id = Integer.parseInt(elem[0]);
        this.username = elem[1];
    }

    Client(int i, String string) {
        this.id = i;
        this.username = string;
    }

    @Override
    public String toString() {
        return id + "," + username;
    }
    
    public boolean login() {
        if(logat == true) return false;
        logat = true;
        return true;
    }
    
    public void logout() {
        logat = false;
    }
    
}
