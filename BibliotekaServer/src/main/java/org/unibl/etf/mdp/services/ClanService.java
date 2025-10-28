package org.unibl.etf.mdp.services;

import org.unibl.etf.mdp.models.Clan;
import org.unibl.etf.mdp.repositories.ClanRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClanService {
	
	public String getEmailForUsername(String username) {
	    List<Clan> clanovi = ClanRepository.getAllUsers();

	    Optional<String> email = clanovi.stream()
	        .filter(clan -> clan.getUsername().equals(username))
	        .map(Clan::getEmail)
	        .filter(e -> e != null && e.contains("@") && e.contains("."))
	        .findFirst();

	    return email.orElse("danielmajstorovic033@gmail.com"); 
	}


    public Optional<Clan> login(String username, String password) {
        return ClanRepository.getAllUsers().stream()
                .filter(clan -> clan.getUsername().equals(username) && clan.getPassword().equals(password))
                .findFirst();
    }

    public boolean register(Clan newClan) {
        List<Clan> clanovi = ClanRepository.getAllUsers();

        if (clanovi.stream().anyMatch(c -> c.getUsername().equals(newClan.getUsername()))) {
            return false;
        }

        clanovi.add(newClan);
        ClanRepository.saveAllUsers(clanovi);
        return true;
    }

    public List<Clan> getAllUsers() {
        return ClanRepository.getAllUsers();
    }

    public boolean updateApprovalStatus(String username, boolean approved) {
        List<Clan> clanovi = ClanRepository.getAllUsers();
        boolean updated = false;

        for (Clan clan : clanovi) {
            if (clan.getUsername().equals(username)) {
                clan.setApproved(approved);
                updated = true;
                break;
            }
        }

        if (updated) {
            ClanRepository.saveAllUsers(clanovi);
        }

        return updated;
    }

    public boolean deleteUser(String username) {
        List<Clan> clanovi = ClanRepository.getAllUsers();
        List<Clan> updatedList = clanovi.stream()
                .filter(clan -> !clan.getUsername().equals(username))
                .collect(Collectors.toList());

        if (updatedList.size() != clanovi.size()) {
            ClanRepository.saveAllUsers(updatedList);
            return true;
        }

        return false;
    }

    public boolean updateUser(String username, Clan updatedUser) {
        List<Clan> clanovi = ClanRepository.getAllUsers();
        for (Clan clan : clanovi) {
            if (clan.getUsername().equals(username)) {
                clan.setFirstName(updatedUser.getFirstName());
                clan.setLastName(updatedUser.getLastName());
                clan.setEmail(updatedUser.getEmail());
                clan.setAddress(updatedUser.getAddress());
                clan.setPassword(updatedUser.getPassword());
                ClanRepository.saveAllUsers(clanovi);
                return true;
            }
        }
        return false;
    }
}
