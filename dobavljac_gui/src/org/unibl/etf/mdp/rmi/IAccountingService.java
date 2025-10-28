package org.unibl.etf.mdp.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.unibl.etf.mdp.model.Invoice;

public interface IAccountingService extends Remote {
    
    double processInvoice(Invoice invoice) throws RemoteException;
}
