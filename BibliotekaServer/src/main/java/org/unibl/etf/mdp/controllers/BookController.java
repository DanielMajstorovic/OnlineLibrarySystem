package org.unibl.etf.mdp.controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.models.ApprovalRequest;
import org.unibl.etf.mdp.models.Book;
import org.unibl.etf.mdp.models.Reservation;
import org.unibl.etf.mdp.models.ReservationRequest;
import org.unibl.etf.mdp.services.BookService;
import org.unibl.etf.mdp.services.ClanService;

@Path("/knjige")
public class BookController {

    private BookService bookService = new BookService();
    
    @POST
    @Path("/createReservation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createReservation(ReservationRequest req) {
        try {
            AppLogger.getLogger().info("POST /createReservation, memberName=" + req.getMemberName());

            if(req.getMemberName() == null || req.getMemberName().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("memberName nedostaje!").build();
            }
            if(req.getTitles() == null || req.getTitles().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Lista knjiga je prazna!").build();
            }
            
            bookService.addReservation(req.getMemberName(), req.getTitles());

            return Response.ok().entity("{\"status\":\"ok\"}").build();
        } catch(Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "Greška createReservation: ", e);
            return Response.serverError().entity("Greška pri kreiranju rezervacije.").build();
        }
    }

    @POST
    @Path("/postBook")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addIt(Book book) {
      
    	 AppLogger.getLogger().info("POST /knjige - kreiranje knjige: " + book);

         if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
             return Response.serverError().build();
         }

         bookService.saveBook(book);

         return Response.ok().build();
        
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        AppLogger.getLogger().info("GET /knjige - listanje svih knjiga");
        return Response.ok(bookService.getAllBooks()).build();
    }

    @GET
    @Path("/{title}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOne(@PathParam("title") String title) {
        AppLogger.getLogger().info("GET /knjige/" + title);
        String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8);

        Book found = bookService.getBookByTitle(decodedTitle);
        if (found == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Knjiga sa naslovom '" + decodedTitle + "' nije pronađena.")
                    .build();
        }
        return Response.ok(found).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBook(Book book) {
        AppLogger.getLogger().info("POST /knjige - kreiranje knjige: " + book);

        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Naslov knjige je obavezan.")
                    .build();
        }

        bookService.saveBook(book);

        return Response.status(Response.Status.CREATED)
                .entity("{\"title\":\"" + book.getTitle() + "\"}")
                .build();
    }

    @PUT
    @Path("/{title}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateBook(@PathParam("title") String title, Book book) {
        AppLogger.getLogger().info("PUT /knjige/" + title + " - ažuriranje knjige: " + book);

        String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8);
        
        if (!bookService.exists(decodedTitle)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Knjiga sa naslovom '" + decodedTitle + "' ne postoji.")
                    .build();
        }

        book.setTitle(decodedTitle); 
        bookService.saveBook(book);

        return Response.ok().build();
    }


    @DELETE
    @Path("/{title}")
    public Response deleteBook(@PathParam("title") String title) {
        AppLogger.getLogger().info("DELETE /knjige/" + title);
        String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8);
        boolean deleted = bookService.deleteBookByTitle(decodedTitle);
        
        
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Knjiga sa naslovom '" + decodedTitle + "' ne postoji.")
                    .build();
        }
        return Response.ok().build();
    }
    
    
    @GET
    @Path("/reservations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllReservations() {
        AppLogger.getLogger().info("GET /knjige/reservations");
        return Response.ok(bookService.getAllReservations()).build();
    }


    @POST
    @Path("/approveReservation")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response approveReservation(ApprovalRequest req) {
        AppLogger.getLogger().info("approveReservation: id=" + req.getId());

        Reservation r = bookService.getReservationById(req.getId());
        if(r == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Rezervacija ID=" + req.getId() + " ne postoji.")
                    .build();
        }
        try {
            byte[] zipData = createZipFromBooks(r.getTitles());

            sendMailWithJavaMail(r.getMemberName(), zipData, false);

        } catch(Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "Greška pri odobravanju rezervacije:", e);
            return Response.serverError().entity("Greška pri slanju maila / kreiranju ZIP.").build();
        }
        bookService.removeReservation(r.getId());
        return Response.ok().build();
    }


    @POST
    @Path("/rejectReservation")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response rejectReservation(ApprovalRequest req) {
        AppLogger.getLogger().info("rejectReservation: id=" + req.getId());

        Reservation r = bookService.getReservationById(req.getId());
        if(r == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Rezervacija ID=" + req.getId() + " ne postoji.")
                    .build();
        }
        try {
            sendMailWithJavaMail(r.getMemberName(), null, true);
        } catch(Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "Greška pri odbijanju rezervacije:", e);
            return Response.serverError().entity("Greška pri slanju maila.").build();
        }
        bookService.removeReservation(r.getId());
        return Response.ok().build();
    }

    // =============== PRIVATNE METODE ZA ZIP I MAIL ===============

    private byte[] createZipFromBooks(List<String> titles) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for(String t : titles) {
                Book b = bookService.getFullBook(t);
                String text = (b.getText() != null) ? b.getText() : "N/A";

                ZipEntry entry = new ZipEntry(t.replace(" ", "_") + ".txt");
                zos.putNextEntry(entry);
                byte[] data = text.getBytes(StandardCharsets.UTF_8);
                zos.write(data, 0, data.length);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private void sendMailWithJavaMail(String memberName, byte[] zipData, boolean reject) throws MessagingException {
        
        String host = ConfigLoader.getProperty("mail.host");          
        String port = ConfigLoader.getProperty("mail.port");          
        String username = ConfigLoader.getProperty("mail.username");  
        String password = ConfigLoader.getProperty("mail.password");  
        String mailAuth = ConfigLoader.getProperty("mail.smtp.auth"); 
        String mailTls = ConfigLoader.getProperty("mail.smtp.starttls.enable"); 

        String subject = reject ? "Rezervacija ODBIJENA" : "Rezervacija ODOBRENA";
        String msgText = reject 
                ? "Poštovani " + memberName + ",\n\nNažalost ne možemo poslati tražene knjige.\nRezervacija je odbijena!\nIzvinjavamo se!"
                : "Poštovani " + memberName + ",\n\nVaše knjige su u prilogu (zip). Uživajte!";

        Properties props = new Properties();
        props.put("mail.smtp.host", host != null ? host : "smtp.gmail.com");
        props.put("mail.smtp.port", port != null ? port : "587");
        props.put("mail.smtp.auth", mailAuth != null ? mailAuth : "true");
        props.put("mail.smtp.starttls.enable", mailTls != null ? mailTls : "true");
        
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.starttls.required", "true");

        
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        
        MimeMessage message = new MimeMessage(session);
        
        message.setFrom(new InternetAddress(username));
        
        
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(new ClanService().getEmailForUsername(memberName)));
        
        message.setSubject(subject, "UTF-8");

        if(reject) {
            
            message.setText(msgText, "UTF-8");
        } else {
            
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(msgText, "UTF-8");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setFileName("books.zip");
            
            attachmentPart.setDataHandler(
                new javax.activation.DataHandler(new javax.activation.DataSource() {
                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new ByteArrayInputStream(zipData);
                    }
                    @Override
                    public OutputStream getOutputStream() throws IOException {
                        throw new IOException("Not Supported");
                    }
                    @Override
                    public String getContentType() {
                        return "application/zip";
                    }
                    @Override
                    public String getName() {
                        return "books.zip";
                    }
                })
            );

            
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(textPart);
            mp.addBodyPart(attachmentPart);

            message.setContent(mp);
        }

        
        Transport.send(message);
        AppLogger.getLogger().info("Mail poslat (" + (reject ? "ODBIJENO" : "ODOBRENO") + ") korisniku=" + memberName);
    }

}

