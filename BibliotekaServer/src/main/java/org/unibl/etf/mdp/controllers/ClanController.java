package org.unibl.etf.mdp.controllers;

import org.unibl.etf.mdp.models.Clan;
import org.unibl.etf.mdp.services.ClanService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Path("/clanovi")
public class ClanController {
    private final ClanService clanService = new ClanService();

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(Clan credentials) {
        Optional<Clan> user = clanService.login(credentials.getUsername(), credentials.getPassword());

        if (user.isPresent()) {
            Clan clan = user.get();
            if (!clan.isApproved()) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"message\":\"Korisnik nije odobren od strane administratora\"}").build();
            }
            return Response.ok("{\"message\":\"Prijava uspešna\"}").build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"message\":\"Neispravni kredencijali\"}").build();
        }
    }

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(Clan newUser) {
        boolean registered = clanService.register(newUser);
        if (registered) {
            return Response.ok("{\"message\":\"Registracija uspešna\"}").build();
        } else {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"message\":\"Korisničko ime već postoji\"}").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers() {
        List<Clan> clanovi = clanService.getAllUsers();
        return Response.ok(clanovi).build();
    }

    @PUT
    @Path("/enable/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response enableUser(@PathParam("username") String username) {
        boolean success = clanService.updateApprovalStatus(username, true);
        if (success) {
            return Response.ok("{\"message\":\"Korisnik omogućen\"}").build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"message\":\"Korisnik nije pronađen\"}").build();
        }
    }

    @PUT
    @Path("/disable/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disableUser(@PathParam("username") String username) {
        boolean success = clanService.updateApprovalStatus(username, false);
        if (success) {
            return Response.ok("{\"message\":\"Korisnik onemogućen\"}").build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"message\":\"Korisnik nije pronađen\"}").build();
        }
    }

    @DELETE
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("username") String username) {
        boolean success = clanService.deleteUser(username);
        if (success) {
            return Response.ok("{\"message\":\"Korisnik obrisan\"}").build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"message\":\"Korisnik nije pronađen\"}").build();
        }
    }

    @PUT
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(@PathParam("username") String username, Clan updatedUser) {
        boolean success = clanService.updateUser(username, updatedUser);
        if (success) {
            return Response.ok("{\"message\":\"Podaci korisnika ažurirani\"}").build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"message\":\"Korisnik nije pronađen\"}").build();
        }
    }
}
