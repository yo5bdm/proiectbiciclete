/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magazin;

import java.io.Serializable;

/**
 * punct de punere in inchiriere (magazin).
 * @author yo5bdm
 */
public class Punct implements Serializable { 
    public int id;
    public String ip;
    public boolean obsolete;
    public RemoteInterface con;

    public Punct(int id, String ip) {
        this.id = id;
        this.ip = ip;
        obsolete = false;
    }
    
    public void setObsolete() {
        obsolete = true;
    }
    
    public void refresh() {
        obsolete = false;
    }

    @Override
    public String toString() {
        return id+","+ip;
    }
}
