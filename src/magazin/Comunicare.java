/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magazin;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import static magazin.Magazin.biciclete;
import static magazin.Magazin.lock;

/**
 *
 * @author yo5bdm
 */
public class Comunicare extends UnicastRemoteObject implements RemoteInterface {
    
    static String proxyIP;
    static RemoteInterface proxyCon;
    static boolean iAmProxy = false;
    static boolean iAmObsolete = false;
    
    static Punct lider;
    static Punct me;
    static boolean iAmLeader = false;
    static boolean electing = false;
    static ArrayList<Punct> noduri = new ArrayList();
    static String myIp="";
    
//  ------- Metode NESTATICE  ---------
    public Comunicare() throws RemoteException {
        super(0);
    }
    @Override
    public ArrayList<Bicicleta> getBiciclete() throws RemoteException {
        log("un nod a cerut lista de biciclete");
        return Magazin.biciclete;
    }
    @Override
    public ArrayList<Client> getClienti() throws RemoteException {
        log("un nod a cerut lista de clienti");
        return Magazin.clienti;
    }
    @Override
    public ArrayList<Punct> getPuncte() throws RemoteException {
        log("un nod a cerut lista de puncte");
        return noduri;
    }
    @Override
    public void sendPuncte(ArrayList<Punct> noduri) throws RemoteException {
        log("Primit lista noua de noduri");
        lock.writeLock().lock();
        try {
            Comunicare.noduri = noduri;
        } finally {
            lock.writeLock().unlock();
        }
    }
    @Override
    public Punct getMyId(String myIp) throws RemoteException {
        log(myIp+" a cerut id...");
        int max=0;
        Punct ret=null;
        lock.readLock().lock();
        try {
            for(Punct p:noduri) {
                if(p.ip.equals(myIp)) { ret = p; break; }//found, return id
                if(p.id>max) max = p.id;//get max id
            }
        } finally {
            lock.readLock().unlock();
        }
        if(ret!=null) return ret;
        ret = new Punct(max+1,myIp);
        lock.writeLock().lock();
        try {
            noduri.add(ret);
        } finally {
            lock.writeLock().unlock();
        }
        log("IP-ul "+myIp+" este nodul "+(max+1));
        refreshPuncte();
        return ret;
    }
    @Override
    public void loggedUser(int id) throws RemoteException {
        log("Userul "+id+" s-a logat");
    }
    @Override
    public void logoutUser(int id) throws RemoteException {
        log("Userul "+id+" s-a delogat");
    }
    @Override
    public Punct getLeader() throws RemoteException {
        return lider;
    }
    @Override
    public boolean sync(Bicicleta m, int pointId) throws RemoteException {
        if(me.id == pointId) return true;
        lock.writeLock().lock();
        try {
            for(Bicicleta b:biciclete) {
                if(b.id == m.id) {
                    b.dataInchiriere = m.dataInchiriere;
                    b.posesie = m.posesie;
                    b.status = m.status;
                    b.dataReturnare = m.dataReturnare;
                    b.notificat = false;
                    b.syncWithLeader = true;
                    if(iAmLeader) {
                        if(m.status == Bicicleta.STATUS_INCHIRIAT) {
                            log("Bicicleta "+m.id+" a fost inchiriata de "+Magazin.user(m.posesie));
                        } else {
                            log("Bicicleta "+m.id+" a fost returnata la punctul "+m.posesie);
                        }
                        trimiteSync(m,pointId);
                    }
                    lock.writeLock().unlock();
                    return true;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }
    
//bully
    @Override
    public boolean heartbeat(int id) throws RemoteException {
        for(Punct p:noduri) 
            if(p.id==id) 
                return p.obsolete;
        return false;
    } 
    @Override
    public boolean iAmLeader(Punct lider) throws RemoteException {
        if(me.id > lider.id) {
            log("Primit lider nou "+lider.id);
            iAmLeader = false;
            Comunicare.lider = lider;
            lider.con = startClient(lider.ip);
            return true;
        } else {
            log("Primit lider cu id mai mic, se refuza si incep electia...");
            new Threads("election").start();
            return false;
        }
    }
    @Override
    public boolean elect(int id) throws RemoteException {
        log("primit elect, incep procesul de electie");
        if(!electing) new Threads("election").start();
        return true;
    }
    @Override //anunta liderul ca nodul s-a sincronizat
    public void sincronizat(int id) throws RemoteException {
        for(Punct p:noduri)
            if(p.id==id)
                p.obsolete = false;
    }
    
//  ------- Metode STATICE  ---------
    public static void initializare() throws RemoteException {
        setProperties();
        myIp = getLocalHostAddress();
        log("IP-ul meu: "+myIp);
        if(noduri.size()>0) { //already learned network
            log("Deja cunosc reteaua");
            me = addMe(myIp);
            if(myIp.equals(proxyIP)) {
                iAmProxy = true;
            }
            lider = null;
            iAmObsolete = true;
        } else { //don't know network
            if(myIp.equals(proxyIP)) {
                iAmProxy = true;
                iAmLeader = true;
                lider = null;
                me = addMe(myIp);
            } else {
                proxyCon = startClient(proxyIP);
                me = proxyCon.getMyId(myIp);
                log("ID-ul meu "+me.id);
                noduri = proxyCon.getPuncte();
            }
        }
        startServer();
        syncWithLeader(true);
        new Threads("heartbeat").start();
    }
    public static void syncWithLeader(boolean init) {
        if(lider == null) { 
            getLeaderFromOthers(); 
            try { Thread.sleep(1000); } catch (Exception ex) { }
        }
        if(lider != null) {//sync with leader
            for(int i=0;i<2;i++) {
                try {
                    noduri = lider.con.getPuncte();
                    Magazin.clienti = lider.con.getClienti();
                    Magazin.biciclete = lider.con.getBiciclete();
                    for(Bicicleta b:biciclete)
                        b.syncWithLeader=true;
                    iAmObsolete = false;
                    Magazin.haveModificari = false;
                    lider.con.sincronizat(me.id); //anuntam ca suntem la zi
                    log("Primit "+
                            noduri.size()+" noduri, "+
                            Magazin.clienti.size()+" clienti, "+
                            Magazin.biciclete.size()+" biciclete. Sincronizare incheiata..."
                    );
                    break;
                } catch (Exception ex) {
                    lider.con = startClient(lider.ip);
                }
            }
            if(!init) return;
        } else if(iAmProxy == false) {
            //log("TODO: if no leader, i am first to run, so maybe not obsolete???");
            iAmObsolete = false;
            iAmLeader = true;
            lider = me;
        } else {
            log("I am leader, continue...");
            iAmLeader = true;
            lider = me;
            iAmObsolete = false;
        }
    }
    private static void getLeaderFromOthers() {
        //log("Incerc preluarea liderului de la ceilalti pentru actualizare date");
        for(Punct p:noduri) {
            if(p.id == me.id) continue;
            new Thread(){
                public void run() {
                    for(int i=0;i<2;i++) {//try 2 times
                        if(p.con == null) p.con = startClient(p.ip); //reset connection
                        if(p.con != null) try { 
                            Punct ldr = p.con.getLeader(); 
                            if(lider == null) lider = ldr;
                            //else if(lider.id != ldr.id) Comunicare.log("Primit lider cu id diferit de ce stiam eu...");
                            break;
                        } catch (RemoteException ex) {
                            p.con = null;
                        }
                    }
                }
            }.start();
        }
    }
    private static Punct addMe(String myIp) {
        int max=0;
        for(Punct p:noduri) {
            if(p.ip.equals(myIp)) return p; //found, return id
            if(p.id>max) max = p.id;//get max id
        }
        Punct p = new Punct(max+1,myIp);
        noduri.add(p);
        log("IP-ul "+myIp+" este nodul "+(max+1));
        return p;
    }
    public static void startServer() {
        log("Pornesc serverul RMI");
        if(me == null) {
            log("Nu pot porni serverul, nu-mi cunosc id-ul");
            System.exit(0);
        }
        System.setProperty("java.rmi.server.hostname",me.ip);
        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) { } //registrul exista deja
        Comunicare obj;
        try {
            obj = new Comunicare();
            Naming.rebind("//"+me.ip+"/con", obj);
        } catch (Exception ex) {
            Logger.getLogger(Comunicare.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(0);
        }
    }
    public static RemoteInterface startClient(String ip) {
        try {
            return (RemoteInterface) Naming.lookup("//"+ip+"/con");
        } catch (Exception ex) {
            //System.out.println(ex);
            return null;
        }
    }
    
//metodele liderului
    public static void announceLeader() {
        iAmLeader = true;
        for(Punct p:noduri) {
            if(p.id == me.id) continue;
            new Thread(){
                public void run(){
                    for(int i=0;i<2;i++) {
                        if(p.con == null) p.con = startClient(p.ip); //reset connection
                        if(p.con == null) p.setObsolete();
                        else try { 
                            p.con.iAmLeader(me); 
                            break;
                        } catch (RemoteException ex) {
                            p.con = null;
                            p.setObsolete();
                        }
                    }
                }
            }.start();
        }
    } 
    public static void refreshPuncte() { //trimite lista actualizata de puncte tuturor
        for(Punct p:noduri) {
            if(p.id == me.id) continue;
            new Thread(){
                public void run() {
                    for(int i=0;i<=2;i++) { //incerc de 2 ori sa fac conexiunea
                        if(p.con == null) p.con = startClient(p.ip);
                        if(p.con == null) p.setObsolete();
                        else {
                            try { 
                                p.con.sendPuncte(noduri); 
                                break;
                            } catch (RemoteException ex) {
                                p.setObsolete();
                                p.con = null;
                            }
                        }
                    }
                }            
            }.start();
        }
    }
    
    public static boolean trimiteSync(Bicicleta m, int pointId){
        for(Punct p:noduri) {
            if(p.id == pointId) continue;   //nu retrimitem sursei
            if(p.id == me.id) continue;     //nu ne trimitem noua (evitam bucla de mesaje)
            new Thread(){
                public void run() {
                    for(int i=0;i<2;i++) { //incercam de 2 ori
                        try {
                            p.con.sync(m,pointId);
                            p.obsolete=false;
                            break; //s-a rezolvat, nu mai are sens sa incercam inca o data
                        } catch (Exception ex) {
                            p.con = startClient(p.ip); //reconectare
                            p.obsolete=true;
                        }
                    }
                }
            }.start();
        }
        return true;
    }
    
//low level
    public static void log(String mesaj) {
        System.out.println(" --- LOG: "+mesaj);
    }
    private static String getLocalHostAddress() {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr.getHostAddress();
                        }
                        else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress.getHostAddress();
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new java.net.UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress.getHostAddress();
        } catch (Exception e) {
            java.net.UnknownHostException unknownHostException = new java.net.UnknownHostException("Failed to determine LAN address: " + e);
            System.exit(0);
        }
        return null;
    }
    private static void setProperties() {
        //https://docs.oracle.com/javase/7/docs/technotes/guides/rmi/sunrmiproperties.html#readTimeout
        String[][] properties = {
            {"sun.rmi.transport.tcp.readTimeout",       "2000"},
            {"sun.rmi.transport.tcp.handshakeTimeout",  "2000"},
            {"sun.rmi.transport.proxy.connectTimeout",  "2000"},
            {"sun.rmi.transport.tcp.handshakeTimeout",  "2000"},
            {"sun.rmi.transport.tcp.responseTimeout",   "2000"},
            {"sun.rmi.transport.connectionTimeout",     "2000"},
            {"sun.rmi.log.debug","true"}
        };
        for(String[] prop:properties) {
            System.setProperty(prop[0], prop[1]);
        }   
    }
}