/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magazin;

import java.rmi.*;
import java.util.ArrayList;

/**
 *
 * @author yo5bdm
 */
public interface RemoteInterface extends Remote {
    
    public ArrayList<Bicicleta> getBiciclete() throws RemoteException;
    public ArrayList<Client> getClienti() throws RemoteException;
    public ArrayList<Punct> getPuncte() throws RemoteException;
    public void sincronizat(int id) throws RemoteException;
    public boolean sync(Bicicleta b, int pointId) throws RemoteException;
    public Punct getMyId(String myIp) throws RemoteException;
    public void sendPuncte(ArrayList<Punct> noduri) throws RemoteException;

    public void loggedUser(int id) throws RemoteException;
    public void logoutUser(int id) throws RemoteException;
    
    //bully part
    public boolean heartbeat(int id) throws RemoteException;
    public boolean iAmLeader(Punct lider) throws RemoteException;
    public boolean elect(int id) throws RemoteException;
    public Punct getLeader() throws RemoteException;
    
}
