/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magazin;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.*;

/**
 *
 * @author Erdei Rudolf
 */
public class Magazin {
    public static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
  
    public static ArrayList<Client> clienti = new ArrayList();
    public static ArrayList<Bicicleta> biciclete = new ArrayList();
    public static Client clientLogat;
    private static Scanner keyboard = new Scanner(System.in);
    public static boolean haveModificari = false;
        
    public static void main(String[] args) {
        try {
            incarcaSetari();
            incarcaClienti();
            incarcaBiciclete();
            incarcaMagazine();
            Comunicare.initializare();
            new Threads("biciclete").start();
            System.out.println("");
            meniu();
        } catch (RemoteException ex) {
            Logger.getLogger(Magazin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//incarcari fisiere
    private static void incarcaSetari() {
        try (BufferedReader br = new BufferedReader(new FileReader("setari"))) {
            String line;
            String[] elem;
            while ((line = br.readLine()) != null) {
               elem = line.split("=");
               switch(elem[0]) {
                   case "proxyIP": Comunicare.proxyIP = elem[1]; break;
                   default:
                       System.out.println("Setare negasita "+elem[0]+" "+elem[1]);
               }
            }
        } catch (Exception ex) {
            System.out.println("Eroare la procesarea fisierului de setari.");
        } 
    }
    private static void incarcaClienti() {
        try (BufferedReader br = new BufferedReader(new FileReader("clienti"))) {
            String line;
            String[] elem;
            while ((line = br.readLine()) != null) {
               elem = line.split(",");
               clienti.add(new Client(elem));
            }
        } catch (Exception ex) {
            System.out.println("Eroare la procesarea fisierului de clienti");
        }
    }
    private static void incarcaBiciclete() {
        try (BufferedReader br = new BufferedReader(new FileReader("biciclete"))) {
            String line;
            String[] elem;
            while ((line = br.readLine()) != null) {
               elem = line.split(",");
               biciclete.add(new Bicicleta(elem));
            }
        } catch (Exception ex) {
            System.out.println("Eroare la procesarea fisierului cu biciclete");
        } 
    }
    private static void incarcaMagazine() {
        try (BufferedReader br = new BufferedReader(new FileReader("magazine"))) {
            String line;
            String[] elem;
            while ((line = br.readLine()) != null) {
               elem = line.split(",");
               Comunicare.noduri.add(
                       new Punct(Integer.parseInt(elem[0]),elem[1])
               );
            }
        } catch (Exception ex) {
            System.out.println("Erare la procesarea fisierului cu magazine");
        } 
    }
    
//meniul principal
    private static void meniu() {
        while(true) {
            //clear();
            if(clientLogat==null) {
                System.out.println("Client nelogat.");
                System.out.println(" 1 - Logare");
                System.out.println(" 2 - Status aplicatie (debug)");
                System.out.println(" 3 - Iesire din aplicatie");
                System.out.println(" 4 - initDB");
                switch(keyboard.nextInt()){
                    case 1: login(); break;
                    case 2: status(); break;
                    case 3: iesire(); break;
                    case 4: initDB(); break;
                    default:
                        System.out.println("Comanda necunoscuta...");
                }
            } else {
                System.out.println("User "+clientLogat.username+" logat");
                System.out.println("Acest centru are "+countFree()+" biciclete libere");
                System.out.println(" 1 - Vezi biciclete din toate centrele");
                System.out.println(" 2 - Inchiriaza o bicicleta");
                System.out.println(" 3 - Biciclete inchiriate de mine");
                System.out.println(" 4 - Preda o bicicleta");
                System.out.println(" 5 - Logout");
                atentionari();
                switch(keyboard.nextInt()) {
                    case 1: nrBicicleteLibere(); break;
                    case 2: inchiriazaBicicleta(); break;
                    case 3: bicicleteUser(); break;
                    case 4: predaBicicleta(); break;
                    case 5: logout(); break;
                    default:
                        System.out.println("Comanda necunoscuta...");
                }
            }
        }
    }

//metode din meniu    
    private static void status() {
        //clear();   
        System.out.println(" == STATUS ==");
        System.out.println("ID punct:           "+Comunicare.me.id);
        System.out.println("IP punct:           "+Comunicare.myIp);
        System.out.println("Proxy:              "+Comunicare.iAmProxy);
        System.out.println("Obsolete:           "+Comunicare.iAmObsolete);
        System.out.println("I am leader:        "+Comunicare.iAmLeader);
        System.out.println("Lider:              "+Comunicare.lider.ip);
        System.out.println("Electing:           "+Comunicare.electing);
        System.out.println("Have modificari:    "+haveModificari);
        enter();
    }
    private static void nrBicicleteLibere() {
        int[] nrBicicleteDinMagazine = new int[10];
        for(Bicicleta b:biciclete) {
            if(b.status == Bicicleta.STATUS_LIBER) nrBicicleteDinMagazine[b.posesie]++;
        }
        for(int i=0;i<10;i++) {
            if(nrBicicleteDinMagazine[i]>0) System.out.println("Magazin "+i+": "+nrBicicleteDinMagazine[i]+" biciclete libere");
        }
        enter(); 
    }
    private static void addUser() {
        keyboard.nextLine();
        System.out.print("Introduceti numele de utilizator dorit: ");
        String user = keyboard.nextLine();
        int maxid = 0;
        for(Client c:clienti) 
            if(c.id > maxid) 
                maxid=c.id;
        clienti.add(new Client(maxid+1,user));
        save();
        System.out.println("Utilizatorul "+user+" a fost salvat cu succes");
    }
    
//interactiunea cu utilizatorul
    private static void login() {
        //clear(); 
        keyboard.nextLine();
        System.out.print("Numele de utilizator: ");
        String user = keyboard.nextLine();
        for(Client c:clienti) {
            if(c.username.equals(user)) {
                try {
                    if(!Comunicare.iAmLeader) Comunicare.lider.con.loggedUser(c.id);
                } catch (RemoteException ex) {
                    System.out.println("Eroare trimitere date la lider");
                }
                clientLogat = c;
                return;
            }
        }
        System.out.println("Eroare: Nu s-a gasit utilizator cu acest nume");
    }
    private static void logout() { 
        try {
            if(!Comunicare.iAmLeader) Comunicare.lider.con.logoutUser(clientLogat.id);
        } catch (RemoteException ex) {
            System.out.println("Nu pot anunta delogarea clientului");
        }
        clientLogat = null;
    }
    private static void atentionari() {
        long dataAcum = System.currentTimeMillis();
        for(Bicicleta b:biciclete) {
            if(b.status == Bicicleta.STATUS_INCHIRIAT && 
                b.posesie == clientLogat.id && b.dataReturnare < dataAcum
                    ) {
                System.out.println("  ==  Bicicleta "+b.id+" a depasit termenul de predare!  ==");
            }
        }
    }
    private static void inchiriazaBicicleta() {
        if(haveFree()) {
            System.out.println("Exista "+countFree()+" biciclete disponibile in acest magazin.");
            System.out.print("Pentru cate zile doriti inchirierea? ");
            int zile = keyboard.nextInt();
            for(Bicicleta b:biciclete) {
                if(b.status == Bicicleta.STATUS_LIBER && b.posesie == Comunicare.me.id) {
                    b.inchiriaza(Comunicare.me.id,clientLogat.id,zile);
                    b.notificat = false;
                    b.syncWithLeader = false;
                    haveModificari = true;
                    System.out.println("Inchiriere incheiata cu succes. Bucurati-va de miscare...");
                    save();
                    enter();
                    return;
                }
            }
            System.out.println("Eroare inchiriere..."); 
        } else {
            System.out.println("Nu avem biciclete libere, va rugam sa va deplasati la alt centru...");
            System.out.println("Numarul de biciclete disponibile din celelelate centre:");
            nrBicicleteLibere();
        }   
        enter(); 
    }
    private static void predaBicicleta() {
        System.out.println("Introduceti id-ul bicicletei de returnat: ");
        int nr=0;
        for(Bicicleta b:biciclete) {
            if(b.status == Bicicleta.STATUS_INCHIRIAT && b.posesie == clientLogat.id) {
                nr++;
                System.out.println(" - bikeID "+b.id+", data inchiriere "+b.dataInchiriere+", data retur "+b.dataReturnare);
            }
        }
        if(nr==0) {
            System.out.println("Nu aveti nici o bicicleta de returnat. ");
            return;
        }
        System.out.print("id> ");
        int ret = keyboard.nextInt();
        for(Bicicleta b:biciclete) {
            if(b.id == ret) {
                b.preda(Comunicare.me.id,clientLogat.id);
                b.notificat = false;
                b.syncWithLeader = false;
                haveModificari = true;
                System.out.println("Bicicleta returnata cu succes");
                break;
            }
        }
        enter();
    }
    private static boolean haveFree() {
        for(Bicicleta b:biciclete)
            if(b.status==Bicicleta.STATUS_LIBER && b.posesie == Comunicare.me.id) return true;
        return false;
    }
    private static int countFree() {
        int nr=0;
        for(Bicicleta b:biciclete) 
            if(b.status==Bicicleta.STATUS_LIBER && b.posesie == Comunicare.me.id) nr++;
        return nr;
    }
    private static void bicicleteUser() {
        for(Bicicleta b:biciclete) {
            if(b.status == Bicicleta.STATUS_INCHIRIAT && b.posesie == clientLogat.id) {
                System.out.println(" - bikeID "+b.id+", data inchiriere "+data(b.dataInchiriere)+", data retur "+data(b.dataReturnare));
            }
        }
        enter();
    }
    
    
//low level
    private static void iesire() { //din aplicatie
        save();
        System.out.println("Fisiere salvate, iesim din aplicatie...");
        System.exit(0);
    }
    private static void enter() {
        System.out.println("Apasa tasta ENTER pentru a continua...");
        try { System.in.read(); } catch(Exception e) {} 
    }
    private static void clear() { //https://stackoverflow.com/questions/2979383/java-clear-the-console
        System.out.print("\033[H\033[2J");  
        System.out.flush();
    }
    private static String data(long date) {
        Date d = new Date(date);
        return ""+d.getMinutes()+" "+d.getHours()+" "+d.getDay();
    }
    private static void save() {
        try {
            //salveaza fisier setari
            PrintWriter wr = new PrintWriter("setari", "UTF-8");
            wr.println("proxyIP="+Comunicare.proxyIP);
            wr.close();
            //salveaza fisier utilizatori
            wr = new PrintWriter("clienti", "UTF-8");
            for(Client c:clienti) wr.println(c);
            wr.close();
            //salveaza fisier biciclete
            wr = new PrintWriter("biciclete", "UTF-8");
            for(Bicicleta b:biciclete) wr.println(b);
            wr.close();    
            wr = new PrintWriter("magazine", "UTF-8");
            for(Punct c:Comunicare.noduri) wr.println(c);
            wr.close();
        } catch (Exception ex) {
            Logger.getLogger(Magazin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static void initDB() {
        System.out.println(" === Initializez baza de date cu date random ===");
        String[] users = {"rudi","ovi","stefan","mihai"};
        clienti.clear();
        for(int i=0;i<users.length;i++) {
            clienti.add(new Client(i,users[i]));
            System.out.println("Adaugat userul "+users[i]);
        }
        biciclete.clear();
        int magId;
        for(int i=0;i<20;i++) {
            //public Bicicleta(int id, int status, int posesie, long dataInchiriere, long dataReturnare)
            magId=1+(int) (Math.random()*3);
            biciclete.add(new Bicicleta(i,Bicicleta.STATUS_LIBER,magId,0,0));
            System.out.println("Bike "+i+" la mag "+magId);
        }
        save();
    }

    static String user(int posesie) {
        for(Client c:clienti) {
            if(c.id==posesie) return c.username;
        }
        return "";
    }
}
