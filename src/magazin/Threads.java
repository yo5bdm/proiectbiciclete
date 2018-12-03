/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magazin;

import java.rmi.RemoteException;

/**
 *
 * @author yo5bdm
 */
public class Threads extends Thread {
    
    private String operatie;
    private String ip;
    private static boolean heartbeat = false;
    private static boolean ok=false;
    
    Threads(String tip) {
        operatie = tip;
    }
    Threads(String tip, String ip) {
        operatie = tip;
        this.ip = ip;
    }
    @Override
    public void run() {
        switch(operatie) {
            case "heartbeat": heartbeat(); break;
            case "election": startElection(); break;
            case "ping": pingThread(); break;
            case "biciclete": biciclete(); break;
            default:
                Comunicare.log("Threads Operatie necunoscuta "+operatie);
        }
    }
    private void heartbeat() {
        Comunicare.log("Pornesc Thread de heartbeat...");
        while(true) {
            try { Thread.sleep(2000); } catch (InterruptedException ex) { }
            if(!Comunicare.iAmLeader && !Comunicare.electing) {
                if(Comunicare.lider!=null) {                    
                    heartbeat = true;
                    Thread hb = new Thread(){
                        public void run() {
                            try { 
                                Comunicare.iAmObsolete = Comunicare.lider.con.heartbeat(Comunicare.me.id);
                                heartbeat = false; //lider ok, avem sincronizari?
                                if(Magazin.haveModificari) sendToLeader(); //avem, trimitem
                            } catch (RemoteException ex) { //lider not ok, declansam electia
                                Comunicare.lider = null;
                            }
                        }
                    };
                    hb.start();
                    try { Thread.sleep(1000); } catch (InterruptedException ex) { }
                    if(heartbeat) {//still not connected
                        hb.interrupt();
                        Comunicare.lider=null;                      
                    }                   
                } else if(Comunicare.electing == false) {
                    new Threads("election").start();
                }
            }
            if(Comunicare.iAmLeader && Magazin.haveModificari) {
                sendToLeader();
            }
            if(Comunicare.iAmObsolete) {
                Comunicare.syncWithLeader(false);
            }
        }
    }
    private void startElection() {
        if(Comunicare.electing == true) return;
        Comunicare.log("Incepe thread-ul de electie");
        Comunicare.electing = true;
        ok = false;
        Comunicare.lider = null;
        for(Punct p:Comunicare.noduri) {
            if(p.id < Comunicare.me.id) new Threads("ping",p.ip).start();
        }
        try { Thread.sleep(4000); } catch (InterruptedException ex) { }
        if(!ok) {
            Comunicare.log("Nu am primit nici un ok, eu sunt lider...");
            Comunicare.announceLeader();
        }
        Comunicare.log("Final thread electie");
        Comunicare.electing = false;
    }
    private void pingThread() {
        RemoteInterface con = Comunicare.startClient(ip);
        if(con!=null) {
            try { con.elect(Comunicare.me.id); ok = true; } 
            catch(Exception ex) { ok = false; }
        }
    }
    private void biciclete() {
        Comunicare.log("Pornesc Thread verificare depasiri termene...");
        while(true) {
            long dataAcum = System.currentTimeMillis();
            for(Bicicleta b:Magazin.biciclete) {
                if(b.notificat== false && b.status == Bicicleta.STATUS_INCHIRIAT && b.dataReturnare < dataAcum) {
                    b.notificat = true;
                    System.out.println("  ==  Bicicleta "+b.id+" inchiriata de "+Magazin.user(b.posesie)+" a depasit termenul de predare!  ==");
                }
            }
            try { Thread.sleep(30*1000); } catch (InterruptedException ex) { }
        }
    }
    
    private void sendToLeader(){
        for(Bicicleta b: Magazin.biciclete) {
            if(b.syncWithLeader == false) {
                try {
                    if(Comunicare.iAmLeader)
                        b.syncWithLeader = Comunicare.trimiteSync(b,Comunicare.me.id);
                    else 
                        b.syncWithLeader = Comunicare.lider.con.sync(b,Comunicare.me.id);
                } catch(Exception e){ }                
            }
        }
        Magazin.haveModificari = false;
    }
}
