package org.unibl.etf.mdp.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.rmi.*;

public class AccountingServer {
    public static void main(String[] args) {
        try {
            String host = ConfigLoader.getProperty("rmi.host");    
            int port = Integer.parseInt(ConfigLoader.getProperty("rmi.port"));
            String serviceName = ConfigLoader.getProperty("rmi.serviceName"); 

            AppLogger.getLogger().info("Pokrećem RMI server na " + host + ":" + port 
                + " serviceName: " + serviceName);

            IAccountingService service = new AccountingServiceImpl();

            Registry registry = LocateRegistry.createRegistry(port);

            registry.rebind(serviceName, service);

            AppLogger.getLogger().info("RMI server je pokrenut i usluga je registrovana kao '" 
                + serviceName + "'");
        } catch (Exception e) {
            AppLogger.getLogger().severe("Greška prilikom pokretanja RMI servera: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
