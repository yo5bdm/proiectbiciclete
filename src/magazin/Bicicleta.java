/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magazin;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 *
 * @author yo5bdm
 */
public class Bicicleta implements Serializable {
    
    public static final int STATUS_LIBER = 0;
    public static final int STATUS_INCHIRIAT = 1;
    
    int id;
    int status; //e inchiriata sau libera? liber = in magazin, inchiriat = la client
    int posesie; //la ce ID e bicicleta (in functie de status)
    long dataInchiriere;
    long dataReturnare;
    boolean syncWithLeader=true;
    boolean notificat=false;
    

    Bicicleta(int id) {
        this.id = id;
    }
    public Bicicleta(int id, int status, int posesie, long dataInchiriere, long dataReturnare) {
        this.id = id;
        this.status = status;
        this.posesie = posesie;
        this.dataInchiriere = dataInchiriere;
        this.dataReturnare = dataReturnare;
        notificat=false;
    }
    Bicicleta(String[] elem) {
        this.id = Integer.parseInt(elem[0]);
        this.status = Integer.parseInt(elem[1]);
        this.posesie = Integer.parseInt(elem[2]);
        this.dataInchiriere = Long.parseLong(elem[3]);
        this.dataReturnare = Long.parseLong(elem[4]);
        this.syncWithLeader = Boolean.parseBoolean(elem[5]);
        this.notificat = false;
    }
    @Override
    public String toString() {
        return id+","+status+","+posesie+","+dataInchiriere+","+dataReturnare+","+syncWithLeader+","+notificat;
    }
    void inchiriaza(int magazinId, int userId, int zile) {
        this.status = STATUS_INCHIRIAT;
        this.posesie = userId;
        this.dataInchiriere = System.currentTimeMillis();
        this.dataReturnare = dataInchiriere + zile*1000*60;
        notificat=false;
    }
    void preda(int magazinId, int clientId) {
        long now = System.currentTimeMillis();
        this.status = STATUS_LIBER;
        this.posesie = magazinId;
        if(dataReturnare < now) {
            long intarziere = (now - dataReturnare) / 60000;
            System.out.println("Ati intarziat returul cu "+intarziere+" zile!");
        }
        notificat=false;
    }
}
