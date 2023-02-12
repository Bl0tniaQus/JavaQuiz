package com.example.javaproquiz;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ui.Model;

import java.util.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.sql.*;

@Controller
public class QuizController {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @GetMapping("/")
    public String home(Model model, HttpServletRequest request) {

        return "index";
    }
    @GetMapping("/rejestracja")
    public String register(HttpServletRequest request)
    {
        if (request.getSession().getAttribute("logged_in")!=null)
        {
            return "redirect:/";
        }
        return "rejestracja";

    }
    @GetMapping("/logowanie")
    public String logowanie(HttpServletRequest request)
    {
        if (request.getSession().getAttribute("logged_in")!=null)
        {
            return "redirect:/";
        }
        return "logowanie";
    }
    @GetMapping("/dodaj")
    public String dodaj(HttpServletRequest request)
    {
        if ((request.getSession().getAttribute("logged_in")==null) || (request.getSession().getAttribute("logged_in")==null))
        {
            return "redirect:/";
        }
        return "dodaj";
    }
    @PostMapping("/logowanieaction")
    public String logowanieaction(Model model, HttpServletRequest request)
    {
        if (request.getSession().getAttribute("logged_in")!=null)
        {
            return "redirect:/";
        }
        String msg="";
        String haslodb="", numer="";
        int id=0;
        Boolean czyAdmin = false;
        String login = request.getParameter("login");
        String haslo = request.getParameter("haslo");
        String hasloencoded = org.apache.commons.codec.digest.DigestUtils.sha256Hex(haslo);
        Boolean czyLogowac = false;
        SqlRowSet result = jdbcTemplate.queryForRowSet("SELECT * from uzytkownik WHERE nazwa_uzytkownika = ?", login);
        while (result.next())
        {
            if (Objects.equals(result.getString("nazwa_uzytkownika"), login)) {
                haslodb = result.getString("haslo");
                numer = result.getString("numer_indeksu");
                id = result.getInt("id_uzytkownika");
                czyAdmin = result.getBoolean("czy_admin");
                czyLogowac = true;
                break;

            }
        }
        if ((czyLogowac == true) && (Objects.equals(hasloencoded,haslodb))){
            request.getSession().setAttribute("login", login);
            request.getSession().setAttribute("indeks", numer);
            request.getSession().setAttribute("id_uzytkownika", id);
            if (czyAdmin==true){request.getSession().setAttribute("czy_admin", "true");}
            request.getSession().setAttribute("logged_in", "true");
            return "redirect:/";
        }
        else {msg="Niepoprawne dane logowania";}
        model.addAttribute("msg",msg);
        return "logowanie";

    }
    @GetMapping("/wyloguj")
    public String wyloguj(HttpServletRequest request)
    {
        request.getSession().invalidate();
        return "redirect:/";
    }
    @PostMapping("/rejestracjaaction")
    public String registeraction(Model model, HttpServletRequest request){
        if (request.getSession().getAttribute("logged_in")!=null)
        {
            return "redirect:/";
        }
        String msg="";
        String login, haslo, indeks;
        Boolean czyDodac = true;
        login = request.getParameter("login");
        haslo = request.getParameter("haslo");
        indeks = request.getParameter("indeks");
        SqlRowSet result = jdbcTemplate.queryForRowSet("SELECT nazwa_uzytkownika from uzytkownik WHERE nazwa_uzytkownika = ?", login);
        while (result.next())
        {
            if (Objects.equals(result.getString("nazwa_uzytkownika"), login)) {msg = "Istnieje użytkownik o takiej nazwie"; czyDodac = false; break;}
        }
       if (czyDodac==true)
        {
            haslo = org.apache.commons.codec.digest.DigestUtils.sha256Hex(haslo);
            jdbcTemplate.update("INSERT INTO uzytkownik VALUES (default, ?, ?, ?, false)", login,haslo,indeks);
           msg = "Konto utworzone prawidłowo";
        }
        model.addAttribute("msg",msg);
        return "rejestracja";
    }
    @PostMapping("dodajaction")
    public String dodajaction(HttpServletRequest request, @RequestParam("plik") MultipartFile file) throws IOException
    {
        if ((request.getSession().getAttribute("logged_in")==null) || (request.getSession().getAttribute("logged_in")==null))
        {
            return "redirect:/";
        }
        String img="";
        int idpytania=0;
        try {
            if (!file.isEmpty()) {
                byte[] bytes = file.getBytes();
                byte[] encodedimg = Base64.getEncoder().encode(bytes);
                img = new String(encodedimg, "UTF-8");
            }
            jdbcTemplate.update("INSERT INTO pytanie VALUES (default, ?, ?)", request.getParameter("pytanie"), img);
            SqlRowSet result = jdbcTemplate.queryForRowSet("SELECT id_pytania from pytanie where tresc = ? order by id_pytania desc", request.getParameter("pytanie"));
            while (result.next())
            {
                idpytania = result.getInt("id_pytania"); break;
            }
            for (int i=1;i<=Integer.parseInt(request.getParameter("n_odp"));i++) {
                String odp = request.getParameter("odp-"+Integer.toString(i));
                if (!((Objects.equals(odp,""))||(odp==null)))
                {
                    System.out.println(odp.length());
                    String popr = request.getParameter("popr-"+Integer.toString(i));
                    Boolean czyPopr;
                    if (popr==null) {czyPopr=false;}
                    else {czyPopr=true;}
                    jdbcTemplate.update("INSERT INTO odpowiedz VALUES (default, ?, ?, ?)", idpytania, odp,czyPopr);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "redirect:/";
    }
    @GetMapping("/test")
    public String test(Model model, HttpServletRequest request)
    {
        if (request.getSession().getAttribute("logged_in")==null)
        {
            return "redirect:/";
        }
        int n_pytan = 4;

        ArrayList<Pytanie> pytania = new ArrayList<Pytanie>();
        SqlRowSet result = jdbcTemplate.queryForRowSet("SELECT * from pytanie");

        while (result.next())
        {
            Pytanie pyt = new Pytanie();
            pyt.id = result.getInt("id_pytania");
            pyt.tresc = result.getString("tresc");
            pyt.img = result.getString("obraz");
            pyt.odpowiedzi = new ArrayList<Odpowiedz>();
            SqlRowSet odpowiedziresult = jdbcTemplate.queryForRowSet("SELECT * from odpowiedz WHERE odpowiedz_id_pytania = ?", pyt.id);
            while (odpowiedziresult.next())
            {
                Odpowiedz odp = new Odpowiedz();
                odp.id = odpowiedziresult.getInt("id_odpowiedzi");
                odp.id_pytania = odpowiedziresult.getInt("odpowiedz_id_pytania");
                odp.tresc = odpowiedziresult.getString("tresc");
                //odp.poprawnosc = odpowiedziresult.getString("czy_poprawna");
                pyt.odpowiedzi.add(odp);
            }

            pytania.add(pyt);

        }
        Collections.shuffle(pytania);
        if (pytania.size()<n_pytan) {n_pytan=pytania.size();}
        for (int j=1;j<=n_pytan;j++)
        {
            pytania.get(j-1).numer = j;
        }
        model.addAttribute("pytania",pytania);
        return "test";
    }
    @PostMapping("/wynik")
    public String wynik(Model model, HttpServletRequest request)
    {
        if (request.getSession().getAttribute("logged_in")==null)
        {
            return "redirect:/";
        }
        int n_pytan = Integer.parseInt(request.getParameter("n_pytan"));
        int punkty = 0;
        for (int i=1;i<=n_pytan;i++)
        {
            int id_pytania = Integer.parseInt(request.getParameter("nrpytania"+Integer.toString(i)));
            SqlRowSet result = jdbcTemplate.queryForRowSet("SELECT * from odpowiedz where odpowiedz_id_pytania = ?",id_pytania);
            int pomylki=0;
            while (result.next())
            {
                int idodp = result.getInt("id_odpowiedzi");
                Boolean prawd = result.getBoolean("czy_poprawna");
                String odpu = request.getParameter(Integer.toString(id_pytania)+'-'+Integer.toString(idodp));
                Boolean odpu_bl;
                if (odpu == null) {odpu_bl =false;}
                else {odpu_bl = true;}
                if (prawd == odpu_bl) {continue;}
                else {pomylki=1; break;}
            }
            if (pomylki==0) {punkty++;}

        }
        model.addAttribute("punkty",punkty);
        model.addAttribute("n_pytan",n_pytan);
        return "wynik";
    }
    @GetMapping("/lista")
        public String lista(Model model, HttpServletRequest request) {
        if ((request.getSession().getAttribute("logged_in") == null) || (request.getSession().getAttribute("logged_in") == null)) {
            return "redirect:/";
        }
        ArrayList<Pytanie> pytania = new ArrayList<Pytanie>();
        SqlRowSet result = jdbcTemplate.queryForRowSet("SELECT * from pytanie");

        while (result.next()) {
            Pytanie pyt = new Pytanie();
            pyt.id = result.getInt("id_pytania");
            pyt.tresc = result.getString("tresc");
            pyt.img = result.getString("obraz");
            pyt.odpowiedzi = new ArrayList<Odpowiedz>();
            SqlRowSet odpowiedziresult = jdbcTemplate.queryForRowSet("SELECT * from odpowiedz WHERE odpowiedz_id_pytania = ?", pyt.id);
            while (odpowiedziresult.next()) {
                Odpowiedz odp = new Odpowiedz();
                odp.id = odpowiedziresult.getInt("id_odpowiedzi");
                odp.id_pytania = odpowiedziresult.getInt("odpowiedz_id_pytania");
                odp.tresc = odpowiedziresult.getString("tresc");
                odp.poprawnosc = odpowiedziresult.getString("czy_poprawna");
                pyt.odpowiedzi.add(odp);
            }

            pytania.add(pyt);
        }
            model.addAttribute("pytania", pytania);
            return "lista";
        }
    @PostMapping("/usun")
    public String usun(HttpServletRequest request)
    {
        if ((request.getSession().getAttribute("logged_in") == null) || (request.getSession().getAttribute("logged_in") == null)) {
            return "redirect:/";
        }
        if (request.getParameter("usun")!=null)
        {
            int id = Integer.parseInt(request.getParameter("usun"));
            jdbcTemplate.update("delete from odpowiedz where odpowiedz_id_pytania= ?", id);
            jdbcTemplate.update("delete from pytanie where id_pytania = ?", id);
        }
        return "redirect:/lista";
    }
    @GetMapping("/modyfikuj")
    public String modyfikuj(Model model, HttpServletRequest request)
    {
        if (((request.getSession().getAttribute("logged_in") == null) || (request.getSession().getAttribute("logged_in") == null)) && (request.getParameter("modyfikuj")!=null))
        {
            return "redirect:/";
        }
        int id = Integer.parseInt(request.getParameter("modyfikuj"));
        Pytanie pyt = new Pytanie();
        SqlRowSet result = jdbcTemplate.queryForRowSet("SELECT * from pytanie where id_pytania = ?", id);
        while (result.next())
        {
            pyt.id = id;
            pyt.tresc = result.getString("tresc");
            pyt.img = result.getString("obraz");
            pyt.odpowiedzi = new ArrayList<Odpowiedz>();
            SqlRowSet odpowiedziresult = jdbcTemplate.queryForRowSet("SELECT * from odpowiedz WHERE odpowiedz_id_pytania = ?", pyt.id);
            int nrodp=0;
            while (odpowiedziresult.next()) {
                nrodp++;
                Odpowiedz odp = new Odpowiedz();
                odp.id = odpowiedziresult.getInt("id_odpowiedzi");
                odp.id_pytania = odpowiedziresult.getInt("odpowiedz_id_pytania");
                odp.tresc = odpowiedziresult.getString("tresc");
                odp.nr_odp = nrodp;
                odp.poprawnosc = odpowiedziresult.getString("czy_poprawna");
                pyt.odpowiedzi.add(odp);
            }
        }
        model.addAttribute("pytanie", pyt);
        return "modyfikuj";
    }
    @PostMapping("/modyfikujaction")
    public String Modyfikujaction(HttpServletRequest request, @RequestParam("plik") MultipartFile file) throws IOException
    {
        if (((request.getSession().getAttribute("logged_in") == null) || (request.getSession().getAttribute("logged_in") == null)) && (request.getParameter("modyfikuj")!=null))
        {
            return "redirect:/";
        }

        int idpytania=Integer.parseInt(request.getParameter("id_pyt"));
            String img="";
            try {
                if (!file.isEmpty()) {
                    byte[] bytes = file.getBytes();
                    byte[] encodedimg = Base64.getEncoder().encode(bytes);
                    img = new String(encodedimg, "UTF-8");
                }
                if (img=="")
                {

                    jdbcTemplate.update("UPDATE pytanie set tresc= ? where id_pytania = ?", request.getParameter("pytanie"), idpytania);
                    if (request.getParameter("usunobraz")!=null) {jdbcTemplate.update("UPDATE pytanie set tresc= ?, obraz = ? where id_pytania = ?", request.getParameter("pytanie"), "", idpytania);}
                }
                else
                {
                    jdbcTemplate.update("UPDATE pytanie set tresc = ?, obraz = ? where id_pytania = ?", request.getParameter("pytanie"), img, idpytania);
                }
                jdbcTemplate.update("DELETE from odpowiedz WHERE odpowiedz_id_pytania = ?", idpytania);

                for (int i=1;i<=Integer.parseInt(request.getParameter("n_odp"));i++) {
                    String odp = request.getParameter("odp-"+Integer.toString(i));

                    if (!((Objects.equals(odp,""))||(odp==null)))
                   {
                        String popr = request.getParameter("popr-"+Integer.toString(i));
                    Boolean czyPopr;
                    if (popr==null) {czyPopr=false;}
                    else {czyPopr=true;}
                    jdbcTemplate.update("INSERT INTO odpowiedz VALUES (default, ?, ?, ?)", idpytania, odp,czyPopr);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        return "redirect:/lista";
    }
}